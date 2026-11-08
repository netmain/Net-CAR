package com.netcar.app.ui.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netcar.app.data.NetCarRepository
import com.netcar.app.model.ServiceRecord
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
import javax.inject.Inject

data class MaintenanceUiState(
    val serviceRecords: List<ServiceRecord> = emptyList(),
    val selectedVehicle: Vehicle? = null,
    val totalMaintenanceCost: Double = 0.0,
    val totalServicesCount: Int = 0,
    val userSettings: UserSettings = UserSettings(),
    val isLoading: Boolean = false
)

data class AddServiceFormState(
    val type: ServiceType = ServiceType.OIL_CHANGE,
    val odometer: String = "",
    val cost: String = "",
    val description: String = "",
    val parts: String = "",
    val notes: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class MaintenanceViewModel @Inject constructor(
    private val repository: NetCarRepository
) : ViewModel() {

    val uiState: StateFlow<MaintenanceUiState> = combine(
        repository.serviceRecords,
        repository.vehicles,
        repository.selectedVehicleId,
        repository.userSettings
    ) { services, vehicles, selectedId, settings ->
        val selected = vehicles.find { it.id == selectedId } ?: vehicles.firstOrNull()
        val vehicleServices = services
            .filter { it.vehicleId == selected?.id }
            .sortedByDescending { it.date }

        val totalCost = vehicleServices.sumOf { it.cost }

        MaintenanceUiState(
            serviceRecords = vehicleServices,
            selectedVehicle = selected,
            totalMaintenanceCost = totalCost,
            totalServicesCount = vehicleServices.size,
            userSettings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MaintenanceUiState(isLoading = true)
    )

    private val _formState = MutableStateFlow(AddServiceFormState())
    val formState: StateFlow<AddServiceFormState> = _formState.asStateFlow()

    fun updateType(type: ServiceType) = _formState.apply { value = value.copy(type = type) }
    fun updateOdometer(odo: String) = _formState.apply { value = value.copy(odometer = odo) }
    fun updateCost(cost: String) = _formState.apply { value = value.copy(cost = cost) }
    fun updateDescription(desc: String) = _formState.apply { value = value.copy(description = desc) }
    fun updateParts(parts: String) = _formState.apply { value = value.copy(parts = parts) }
    fun updateNotes(notes: String) = _formState.apply { value = value.copy(notes = notes) }

    fun deleteServiceRecord(id: String) {
        repository.deleteServiceRecord(id)
    }

    fun saveServiceRecord(onSuccess: () -> Unit) {
        val state = _formState.value
        val vehicle = uiState.value.selectedVehicle ?: return

        val odoInt = state.odometer.toIntOrNull()
        if (odoInt == null || odoInt <= 0) {
            _formState.value = state.copy(errorMessage = "Please enter an odometer reading")
            return
        }

        val costDouble = state.cost.toDoubleOrNull()
        if (costDouble == null || costDouble < 0) {
            _formState.value = state.copy(errorMessage = "Please enter a valid cost")
            return
        }

        val desc = if (state.description.isNotBlank()) state.description.trim() else state.type.displayName

        val partsList = state.parts.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val service = ServiceRecord(
            vehicleId = vehicle.id,
            type = state.type,
            odometer = odoInt,
            cost = costDouble,
            description = desc,
            parts = partsList,
            notes = state.notes.trim()
        )

        repository.addServiceRecord(service)
        _formState.value = AddServiceFormState()
        onSuccess()
    }
}
