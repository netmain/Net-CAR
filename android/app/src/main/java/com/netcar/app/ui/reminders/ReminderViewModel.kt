package com.netcar.app.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netcar.app.data.NetCarRepository
import com.netcar.app.model.Reminder
import com.netcar.app.model.ServiceType
import com.netcar.app.model.UserSettings
import com.netcar.app.model.Vehicle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class ReminderUiState(
    val activeReminders: List<Reminder> = emptyList(),
    val completedReminders: List<Reminder> = emptyList(),
    val selectedVehicle: Vehicle? = null,
    val selectedTab: Int = 0, // 0 = Active, 1 = Completed
    val overdueCount: Int = 0,
    val userSettings: UserSettings = UserSettings(),
    val isLoading: Boolean = false
)

data class AddReminderFormState(
    val title: String = "",
    val type: ServiceType = ServiceType.OIL_CHANGE,
    val intervalKm: String = "15000",
    val intervalDays: String = "365",
    val dueOdometer: String = "",
    val dueDaysInFuture: String = "90",
    val isRecurring: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val repository: NetCarRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    val uiState: StateFlow<ReminderUiState> = combine(
        repository.reminders,
        repository.vehicles,
        repository.selectedVehicleId,
        _selectedTab,
        repository.userSettings
    ) { reminders, vehicles, selectedId, tab, settings ->
        val selected = vehicles.find { it.id == selectedId } ?: vehicles.firstOrNull()
        val vehicleReminders = reminders.filter { it.vehicleId == selected?.id }

        val active = vehicleReminders.filter { !it.isCompleted }
            .sortedBy { it.nextDueOdometer ?: Int.MAX_VALUE }
        val completed = vehicleReminders.filter { it.isCompleted }

        val currentOdo = selected?.currentOdometer ?: 0
        val overdue = active.count { it.isOverdue(currentOdo) }

        ReminderUiState(
            activeReminders = active,
            completedReminders = completed,
            selectedVehicle = selected,
            selectedTab = tab,
            overdueCount = overdue,
            userSettings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReminderUiState(isLoading = true)
    )

    private val _formState = MutableStateFlow(AddReminderFormState())
    val formState: StateFlow<AddReminderFormState> = _formState.asStateFlow()

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun updateTitle(title: String) = _formState.apply { value = value.copy(title = title) }
    fun updateType(type: ServiceType) = _formState.apply { value = value.copy(type = type) }
    fun updateIntervalKm(km: String) = _formState.apply { value = value.copy(intervalKm = km) }
    fun updateIntervalDays(days: String) = _formState.apply { value = value.copy(intervalDays = days) }
    fun updateDueOdometer(odo: String) = _formState.apply { value = value.copy(dueOdometer = odo) }
    fun updateDueDays(days: String) = _formState.apply { value = value.copy(dueDaysInFuture = days) }
    fun updateIsRecurring(recurring: Boolean) = _formState.apply { value = value.copy(isRecurring = recurring) }

    fun toggleComplete(id: String) {
        repository.toggleReminderCompleted(id)
    }

    fun deleteReminder(id: String) {
        repository.deleteReminder(id)
    }

    fun saveReminder(onSuccess: () -> Unit) {
        val state = _formState.value
        val vehicle = uiState.value.selectedVehicle ?: return

        val title = if (state.title.isNotBlank()) state.title.trim() else state.type.displayName

        val intervalKmInt = state.intervalKm.toIntOrNull()
        val intervalDaysInt = state.intervalDays.toIntOrNull()

        val nextDueOdo = state.dueOdometer.toIntOrNull()
            ?: if (intervalKmInt != null) vehicle.currentOdometer + intervalKmInt else null

        val daysFuture = state.dueDaysInFuture.toLongOrNull() ?: 90L
        val nextDueDate = System.currentTimeMillis() + (daysFuture * 86400000L)

        val reminder = Reminder(
            vehicleId = vehicle.id,
            title = title,
            type = state.type,
            intervalKm = intervalKmInt,
            intervalDays = intervalDaysInt,
            lastServiceOdometer = vehicle.currentOdometer,
            lastServiceDate = System.currentTimeMillis(),
            nextDueOdometer = nextDueOdo,
            nextDueDate = nextDueDate,
            isRecurring = state.isRecurring,
            isCompleted = false
        )

        repository.addReminder(reminder)
        _formState.value = AddReminderFormState()
        onSuccess()
    }
}
