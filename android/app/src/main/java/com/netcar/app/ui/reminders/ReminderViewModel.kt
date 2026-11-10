package com.netcar.app.ui.reminders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netcar.app.data.NetCarRepository
import com.netcar.app.model.Reminder
import com.netcar.app.model.ServiceType
import com.netcar.app.model.SupportedLanguage
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

data class ReminderUiState(
    val activeReminders: List<Reminder> = emptyList(),
    val completedReminders: List<Reminder> = emptyList(),
    val selectedVehicle: Vehicle? = null,
    val selectedTab: Int = 0, // 0 = Active, 1 = Completed
    val overdueCount: Int = 0,
    val userSettings: UserSettings = UserSettings(),
    val notificationFeedback: String? = null,
    val isLoading: Boolean = false
)

data class AddReminderFormState(
    val editingReminderId: String? = null,
    val title: String = "",
    val type: ServiceType = ServiceType.OIL_CHANGE,
    val intervalKm: String = "15000",
    val intervalDays: String = "365",
    val dueOdometer: String = "",
    val dueDateMillis: Long? = null,
    val dueDaysInFuture: String = "90",
    val lastServiceOdometer: String = "",
    val lastServiceDateMillis: Long? = null,
    val isRecurring: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val repository: NetCarRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var originalReminder: Reminder? = null

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _notificationFeedback = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ReminderUiState> = combine(
        repository.reminders,
        repository.vehicles,
        repository.selectedVehicleId,
        _selectedTab,
        combine(repository.userSettings, _notificationFeedback) { s, fb -> s to fb }
    ) { reminders, vehicles, selectedId, tab, settingsAndFb ->
        val settings = settingsAndFb.first
        val feedback = settingsAndFb.second
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
            userSettings = settings,
            notificationFeedback = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReminderUiState(isLoading = true)
    )

    private val _formState = MutableStateFlow(AddReminderFormState())
    val formState: StateFlow<AddReminderFormState> = _formState.asStateFlow()

    init {
        val reminderId = savedStateHandle.get<String>("reminderId")
        if (!reminderId.isNullOrBlank()) {
            loadReminderForEdit(reminderId)
        }
    }

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun updateTitle(title: String) = _formState.apply { value = value.copy(title = title) }
    fun updateType(type: ServiceType) = _formState.apply { value = value.copy(type = type) }
    fun updateIntervalKm(km: String) = _formState.apply { value = value.copy(intervalKm = km) }
    fun updateIntervalDays(days: String) = _formState.apply { value = value.copy(intervalDays = days) }
    fun updateDueOdometer(odo: String) = _formState.apply { value = value.copy(dueOdometer = odo) }
    fun updateDueDate(millis: Long?) = _formState.apply { value = value.copy(dueDateMillis = millis) }
    fun updateDueDays(days: String) = _formState.apply { value = value.copy(dueDaysInFuture = days) }
    fun updateLastServiceOdometer(odo: String) = _formState.apply { value = value.copy(lastServiceOdometer = odo) }
    fun updateLastServiceDate(millis: Long?) = _formState.apply { value = value.copy(lastServiceDateMillis = millis) }
    fun updateIsRecurring(recurring: Boolean) = _formState.apply { value = value.copy(isRecurring = recurring) }

    private fun loadReminderForEdit(id: String) {
        val reminder = repository.reminders.value.find { it.id == id } ?: return
        originalReminder = reminder
        _formState.value = AddReminderFormState(
            editingReminderId = reminder.id,
            title = reminder.title,
            type = reminder.type,
            intervalKm = reminder.intervalKm?.toString() ?: "",
            intervalDays = reminder.intervalDays?.toString() ?: "",
            dueOdometer = reminder.nextDueOdometer?.toString() ?: "",
            dueDateMillis = reminder.nextDueDate,
            dueDaysInFuture = "",
            lastServiceOdometer = reminder.lastServiceOdometer?.toString() ?: "",
            lastServiceDateMillis = reminder.lastServiceDate,
            isRecurring = reminder.isRecurring
        )
    }

    fun toggleComplete(id: String) {
        repository.toggleReminderCompleted(id)
    }

    fun deleteReminder(id: String) {
        repository.deleteReminder(id)
    }

    fun triggerNativeNotificationsNow() {
        val isFr = repository.userSettings.value.language == SupportedLanguage.FR
        val sentCount = repository.triggerReminderNotificationsNow(forceAllActive = true)
        _notificationFeedback.value = if (sentCount > 0) {
            if (isFr) "$sentCount notification(s) native(s) envoyée(s) sur votre téléphone."
            else "$sentCount native Android notification(s) sent to your phone."
        } else {
            if (isFr) "Aucun rappel actif à notifier pour le moment."
            else "No active reminders to notify right now."
        }
    }

    fun saveReminder(onSuccess: () -> Unit) {
        val state = _formState.value
        val original = originalReminder
        val isEditing = state.editingReminderId != null
        val vehicle = uiState.value.selectedVehicle
        val vehicleId = original?.vehicleId ?: vehicle?.id ?: return

        val title = if (state.title.isNotBlank()) state.title.trim() else state.type.displayName

        val intervalKmInt = state.intervalKm.toIntOrNull()
        val intervalDaysInt = state.intervalDays.toIntOrNull()

        val nextDueOdo = when {
            state.dueOdometer.isNotBlank() -> state.dueOdometer.toIntOrNull()
            isEditing -> null
            intervalKmInt != null -> (vehicle?.currentOdometer ?: 0) + intervalKmInt
            else -> null
        }

        val nextDueDate = when {
            state.dueDateMillis != null -> state.dueDateMillis
            isEditing -> null
            else -> {
                val daysFuture = state.dueDaysInFuture.toLongOrNull() ?: 90L
                System.currentTimeMillis() + (daysFuture * 86400000L)
            }
        }

        if (isEditing && nextDueOdo == null && nextDueDate == null) {
            val isFr = repository.userSettings.value.language == SupportedLanguage.FR
            _formState.value = state.copy(
                errorMessage = if (isFr) {
                    "Indiquez une date d'échéance ou un kilométrage cible."
                } else {
                    "Provide a due date or a target odometer."
                }
            )
            return
        }

        val lastServiceOdometer = state.lastServiceOdometer.toIntOrNull()
            ?: if (isEditing) original?.lastServiceOdometer else vehicle?.currentOdometer
        val lastServiceDate = state.lastServiceDateMillis
            ?: if (isEditing) original?.lastServiceDate else System.currentTimeMillis()

        val reminder = (original ?: Reminder(vehicleId = vehicleId, title = title)).copy(
            vehicleId = vehicleId,
            title = title,
            type = state.type,
            intervalKm = intervalKmInt,
            intervalDays = intervalDaysInt,
            lastServiceDate = lastServiceDate,
            lastServiceOdometer = lastServiceOdometer,
            nextDueOdometer = nextDueOdo,
            nextDueDate = nextDueDate,
            isRecurring = state.isRecurring,
            isCompleted = original?.isCompleted ?: false
        )

        if (isEditing) {
            repository.updateReminder(reminder)
        } else {
            repository.addReminder(reminder)
        }
        originalReminder = null
        _formState.value = AddReminderFormState()
        onSuccess()
    }
}
