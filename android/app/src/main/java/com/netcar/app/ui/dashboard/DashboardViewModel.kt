package com.netcar.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netcar.app.data.NetCarRepository
import com.netcar.app.model.DashboardStats
import com.netcar.app.model.FillUp
import com.netcar.app.model.Reminder
import com.netcar.app.model.UserSettings
import com.netcar.app.model.Vehicle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardUiState(
    val selectedVehicle: Vehicle? = null,
    val vehicles: List<Vehicle> = emptyList(),
    val stats: DashboardStats = DashboardStats(),
    val upcomingReminders: List<Reminder> = emptyList(),
    val recentFillUps: List<FillUp> = emptyList(),
    val userSettings: UserSettings = UserSettings(),
    val isLoading: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: NetCarRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.vehicles,
        repository.selectedVehicleId,
        repository.fillUps,
        repository.expenses,
        repository.serviceRecords,
        repository.reminders,
        repository.userSettings
    ) { vehicles, selectedId, fillUps, expenses, services, reminders, settings ->
        val selected = vehicles.find { it.id == selectedId } ?: vehicles.firstOrNull()
        val vehicleFillUps = fillUps.filter { it.vehicleId == selected?.id }
        val vehicleExpenses = expenses.filter { it.vehicleId == selected?.id }
        val vehicleServices = services.filter { it.vehicleId == selected?.id }
        val vehicleReminders = reminders.filter { it.vehicleId == selected?.id }

        // Monthly cost calculations (past 30 days)
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 86400000L)
        val monthlyFuel = vehicleFillUps.filter { it.date >= thirtyDaysAgo }.sumOf { it.totalCost }
        val monthlyExp = vehicleExpenses.filter { it.date >= thirtyDaysAgo }.sumOf { it.amount }
        val monthlySvc = vehicleServices.filter { it.date >= thirtyDaysAgo }.sumOf { it.cost }
        val totalMonthly = monthlyFuel + monthlyExp + monthlySvc

        // Avg consumption calculation (from fillups)
        val sortedFillups = vehicleFillUps.sortedBy { it.odometer }
        var avgConsumption = 6.4 // realistic default
        if (sortedFillups.size >= 2) {
            val dist = sortedFillups.last().odometer - sortedFillups.first().odometer
            val fuelVol = sortedFillups.drop(1).sumOf { it.volume }
            if (dist > 0) {
                avgConsumption = (fuelVol / dist) * 100.0
            }
        }

        val currentOdo = selected?.currentOdometer ?: 0
        val dueCount = vehicleReminders.count { it.isOverdue(currentOdo) || it.isDueSoon(currentOdo) }

        val stats = DashboardStats(
            totalVehicles = vehicles.size,
            averageConsumption = avgConsumption,
            monthlyFuelCost = monthlyFuel,
            monthlyExpensesCost = monthlyExp,
            monthlyServiceCost = monthlySvc,
            totalMonthlyCost = totalMonthly,
            dueRemindersCount = dueCount,
            currentOdometer = currentOdo
        )

        val upcoming = vehicleReminders
            .filter { !it.isCompleted }
            .sortedBy { it.nextDueOdometer ?: Int.MAX_VALUE }
            .take(3)

        val recentFuel = vehicleFillUps.sortedByDescending { it.date }.take(3)

        DashboardUiState(
            selectedVehicle = selected,
            vehicles = vehicles,
            stats = stats,
            upcomingReminders = upcoming,
            recentFillUps = recentFuel,
            userSettings = settings,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun selectVehicle(vehicleId: String) {
        repository.selectVehicle(vehicleId)
    }

    fun completeReminder(reminderId: String) {
        repository.toggleReminderCompleted(reminderId)
    }
}
