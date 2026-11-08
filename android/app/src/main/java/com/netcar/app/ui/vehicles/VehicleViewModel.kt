package com.netcar.app.ui.vehicles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netcar.app.data.NetCarRepository
import com.netcar.app.model.FuelType
import com.netcar.app.model.UserSettings
import com.netcar.app.model.Vehicle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VehicleUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicleId: String? = null,
    val userSettings: UserSettings = UserSettings(),
    val isSuccess: Boolean = false
)

data class AddVehicleFormState(
    val name: String = "",
    val make: String = "",
    val model: String = "",
    val year: String = "2023",
    val plate: String = "",
    val vin: String = "",
    val fuelType: FuelType = FuelType.GASOLINE,
    val odometer: String = "0",
    val notes: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class VehicleViewModel @Inject constructor(
    private val repository: NetCarRepository
) : ViewModel() {

    val uiState: StateFlow<VehicleUiState> = combine(
        repository.vehicles,
        repository.selectedVehicleId,
        repository.userSettings
    ) { vehicles, selectedId, settings ->
        VehicleUiState(
            vehicles = vehicles,
            selectedVehicleId = selectedId,
            userSettings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VehicleUiState()
    )

    private val _formState = MutableStateFlow(AddVehicleFormState())
    val formState: StateFlow<AddVehicleFormState> = _formState.asStateFlow()

    fun updateName(name: String) = _formState.apply { value = value.copy(name = name) }
    fun updateMake(make: String) = _formState.apply { value = value.copy(make = make) }
    fun updateModel(model: String) = _formState.apply { value = value.copy(model = model) }
    fun updateYear(year: String) = _formState.apply { value = value.copy(year = year) }
    fun updatePlate(plate: String) = _formState.apply { value = value.copy(plate = plate) }
    fun updateVin(vin: String) = _formState.apply { value = value.copy(vin = vin) }
    fun updateFuelType(type: FuelType) = _formState.apply { value = value.copy(fuelType = type) }
    fun updateOdometer(odo: String) = _formState.apply { value = value.copy(odometer = odo) }
    fun updateNotes(notes: String) = _formState.apply { value = value.copy(notes = notes) }

    fun selectVehicle(id: String) {
        repository.selectVehicle(id)
    }

    fun deleteVehicle(id: String) {
        repository.deleteVehicle(id)
    }

    fun saveVehicle(onSuccess: () -> Unit) {
        val state = _formState.value
        if (state.make.isBlank() || state.model.isBlank()) {
            _formState.value = state.copy(errorMessage = "Make and model are required")
            return
        }

        val yearInt = state.year.toIntOrNull() ?: 2023
        val odoInt = state.odometer.toIntOrNull() ?: 0

        val newVehicle = Vehicle(
            name = state.name.trim(),
            make = state.make.trim(),
            model = state.model.trim(),
            year = yearInt,
            plate = state.plate.trim().uppercase(),
            vin = state.vin.trim().uppercase(),
            fuelType = state.fuelType,
            currentOdometer = odoInt,
            notes = state.notes.trim()
        )

        repository.addVehicle(newVehicle)
        _formState.value = AddVehicleFormState()
        onSuccess()
    }
}
