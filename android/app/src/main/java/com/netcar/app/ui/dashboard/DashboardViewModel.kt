package com.netcar.app.ui.dashboard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netcar.app.data.NetCarRepository
import com.netcar.app.model.DashboardStats
import com.netcar.app.model.Expense
import com.netcar.app.model.FillUp
import com.netcar.app.model.Reminder
import com.netcar.app.model.ServiceRecord
import com.netcar.app.model.UserSettings
import com.netcar.app.model.Vehicle
import com.netcar.app.ui.components.VehiclePhotoState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val selectedVehicle: Vehicle? = null,
    val vehicles: List<Vehicle> = emptyList(),
    val stats: DashboardStats = DashboardStats(),
    val upcomingReminders: List<Reminder> = emptyList(),
    val recentFillUps: List<FillUp> = emptyList(),
    val userSettings: UserSettings = UserSettings(),
    val photoState: VehiclePhotoState = VehiclePhotoState(),
    val isLoading: Boolean = false
)

private data class DashboardRecords(
    val fillUps: List<FillUp>,
    val expenses: List<Expense>,
    val services: List<ServiceRecord>,
    val reminders: List<Reminder>
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: NetCarRepository
) : ViewModel() {

    private val vehicleDataFlow = combine(
        repository.vehicles,
        repository.selectedVehicleId,
        repository.userSettings
    ) { vehicles, selectedId, settings ->
        Triple(vehicles, selectedId, settings)
    }

    private val recordsFlow = combine(
        repository.fillUps,
        repository.expenses,
        repository.serviceRecords,
        repository.reminders
    ) { fillUps, expenses, services, reminders ->
        DashboardRecords(fillUps, expenses, services, reminders)
    }

    private val _photoState = MutableStateFlow(VehiclePhotoState())

    val uiState: StateFlow<DashboardUiState> = combine(
        vehicleDataFlow,
        recordsFlow,
        _photoState
    ) { (vehicles, selectedId, settings), records, photoState ->
        val fillUps = records.fillUps
        val expenses = records.expenses
        val services = records.services
        val reminders = records.reminders

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
            photoState = photoState,
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

    fun saveVehiclePhotoLocally(vehicleId: String, uri: Uri) {
        viewModelScope.launch {
            _photoState.value = VehiclePhotoState(vehicleId = vehicleId, isBusy = true)
            val result = repository.saveVehiclePhotoLocally(vehicleId, uri)
            _photoState.value = VehiclePhotoState(
                vehicleId = vehicleId,
                isBusy = false,
                errorMessage = result.exceptionOrNull()?.message
            )
        }
    }

    fun applyVehiclePhotoUrl(vehicleId: String, rawUrl: String) {
        viewModelScope.launch {
            _photoState.value = VehiclePhotoState(vehicleId = vehicleId, isBusy = true)
            val result = repository.validateVehiclePhotoUrl(rawUrl)
            result.getOrNull()?.let { validatedUrl ->
                repository.updateVehiclePhoto(vehicleId, validatedUrl)
            }
            _photoState.value = VehiclePhotoState(
                vehicleId = vehicleId,
                isBusy = false,
                errorMessage = result.exceptionOrNull()?.message
            )
        }
    }

    fun removeVehiclePhoto(vehicleId: String) {
        repository.updateVehiclePhoto(vehicleId, "")
        _photoState.value = VehiclePhotoState()
    }

    fun clearPhotoMessage() {
        _photoState.value = VehiclePhotoState()
    }
}
