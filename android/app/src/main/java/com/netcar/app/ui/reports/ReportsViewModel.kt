package com.netcar.app.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netcar.app.data.NetCarRepository
import com.netcar.app.model.ExpenseCategory
import com.netcar.app.model.UserSettings
import com.netcar.app.model.Vehicle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class ReportPeriod(val label: String, val days: Int) {
    MONTH("30 Days", 30),
    THREE_MONTHS("3 Months", 90),
    YEAR("1 Year", 365),
    ALL_TIME("All Time", 3650)
}

data class CategoryCost(
    val name: String,
    val amount: Double,
    val percentage: Float
)

data class ReportsUiState(
    val selectedVehicle: Vehicle? = null,
    val selectedPeriod: ReportPeriod = ReportPeriod.MONTH,
    val totalSpend: Double = 0.0,
    val fuelSpend: Double = 0.0,
    val maintenanceSpend: Double = 0.0,
    val expensesSpend: Double = 0.0,
    val fuelPercentage: Float = 0f,
    val maintenancePercentage: Float = 0f,
    val expensesPercentage: Float = 0f,
    val costPerKm: Double = 0.0,
    val totalDistance: Int = 0,
    val avgConsumption: Double = 6.4,
    val categoryBreakdown: List<CategoryCost> = emptyList(),
    val userSettings: UserSettings = UserSettings()
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val repository: NetCarRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(ReportPeriod.MONTH)
    val selectedPeriod: StateFlow<ReportPeriod> = _selectedPeriod.asStateFlow()

    val uiState: StateFlow<ReportsUiState> = combine(
        repository.vehicles,
        repository.selectedVehicleId,
        repository.fillUps,
        repository.expenses,
        repository.serviceRecords,
        _selectedPeriod,
        repository.userSettings
    ) { vehicles, selectedId, fillUps, expenses, services, period, settings ->
        val selected = vehicles.find { it.id == selectedId } ?: vehicles.firstOrNull()
        val cutoff = System.currentTimeMillis() - (period.days.toLong() * 86400000L)

        val vFillUps = fillUps.filter { it.vehicleId == selected?.id && it.date >= cutoff }
        val vExpenses = expenses.filter { it.vehicleId == selected?.id && it.date >= cutoff }
        val vServices = services.filter { it.vehicleId == selected?.id && it.date >= cutoff }

        val fuelTotal = vFillUps.sumOf { it.totalCost }
        val expTotal = vExpenses.sumOf { it.amount }
        val svcTotal = vServices.sumOf { it.cost }
        val grandTotal = fuelTotal + expTotal + svcTotal

        val fuelPct = if (grandTotal > 0) (fuelTotal / grandTotal).toFloat() else 0f
        val svcPct = if (grandTotal > 0) (svcTotal / grandTotal).toFloat() else 0f
        val expPct = if (grandTotal > 0) (expTotal / grandTotal).toFloat() else 0f

        // Distance in period
        val allOdometers = (vFillUps.map { it.odometer } + vExpenses.mapNotNull { it.odometer } + vServices.map { it.odometer }).sorted()
        val distance = if (allOdometers.size >= 2) allOdometers.last() - allOdometers.first() else 1250
        val costPerKm = if (distance > 0) grandTotal / distance else 0.0

        // Avg consumption
        val fuelVol = vFillUps.sumOf { it.volume }
        val avgCons = if (distance > 0 && fuelVol > 0) (fuelVol / distance) * 100.0 else 6.4

        // Detailed breakdown
        val breakdown = mutableListOf<CategoryCost>()
        if (fuelTotal > 0) breakdown.add(CategoryCost("Fuel", fuelTotal, fuelPct))
        if (svcTotal > 0) breakdown.add(CategoryCost("Maintenance", svcTotal, svcPct))

        val expByCat = vExpenses.groupBy { it.category }
        expByCat.forEach { (cat, list) ->
            val sum = list.sumOf { it.amount }
            val pct = if (grandTotal > 0) (sum / grandTotal).toFloat() else 0f
            breakdown.add(CategoryCost(cat.displayName, sum, pct))
        }

        ReportsUiState(
            selectedVehicle = selected,
            selectedPeriod = period,
            totalSpend = grandTotal,
            fuelSpend = fuelTotal,
            maintenanceSpend = svcTotal,
            expensesSpend = expTotal,
            fuelPercentage = fuelPct,
            maintenancePercentage = svcPct,
            expensesPercentage = expPct,
            costPerKm = costPerKm,
            totalDistance = distance,
            avgConsumption = avgCons,
            categoryBreakdown = breakdown.sortedByDescending { it.amount },
            userSettings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState()
    )

    fun selectPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
    }
}
