package com.netcar.app.ui.vehicles

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netcar.app.data.NetCarRepository
import com.netcar.app.model.FuelType
import com.netcar.app.model.SupportedLanguage
import com.netcar.app.model.UserSettings
import com.netcar.app.model.Vehicle
import com.netcar.app.ui.components.VehiclePhotoState
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
    val isSuccess: Boolean = false,
    val photoState: VehiclePhotoState = VehiclePhotoState()
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
    val photoUrl: String = "",
    val photoUri: String = "",
    val isPhotoBusy: Boolean = false,
    val isSaving: Boolean = false,
    val photoErrorMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class VehicleViewModel @Inject constructor(
    private val repository: NetCarRepository
) : ViewModel() {

    private val _photoState = MutableStateFlow(VehiclePhotoState())

    val uiState: StateFlow<VehicleUiState> = combine(
        repository.vehicles,
        repository.selectedVehicleId,
        repository.userSettings,
        _photoState
    ) { vehicles, selectedId, settings, photoState ->
        VehicleUiState(
            vehicles = vehicles,
            selectedVehicleId = selectedId,
            userSettings = settings,
            photoState = photoState
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
    fun updatePhotoUrl(url: String) = _formState.apply {
        value = value.copy(photoUrl = url, photoUri = "", photoErrorMessage = null)
    }

    fun updatePhotoUri(uri: String) = _formState.apply {
        value = value.copy(photoUri = uri, photoUrl = "", photoErrorMessage = null)
    }

    fun removeFormPhoto() = _formState.apply {
        value = value.copy(photoUri = "", photoUrl = "", photoErrorMessage = null)
    }

    fun applyPhotoUrl(rawUrl: String) {
        if (_formState.value.isPhotoBusy || _formState.value.isSaving) return
        viewModelScope.launch {
            _formState.value = _formState.value.copy(isPhotoBusy = true, photoErrorMessage = null)
            val result = repository.validateVehiclePhotoUrl(rawUrl)
            val validatedUrl = result.getOrNull()
            _formState.value = _formState.value.copy(
                isPhotoBusy = false,
                photoUrl = validatedUrl ?: _formState.value.photoUrl,
                photoUri = if (validatedUrl != null) "" else _formState.value.photoUri,
                photoErrorMessage = result.exceptionOrNull()?.message
            )
        }
    }

    fun selectVehicle(id: String) {
        repository.selectVehicle(id)
    }

    fun deleteVehicle(id: String) {
        repository.deleteVehicle(id)
    }

    fun uploadVehiclePhoto(vehicleId: String, uri: Uri) {
        viewModelScope.launch {
            _photoState.value = VehiclePhotoState(vehicleId = vehicleId, isBusy = true)
            val result = repository.uploadVehiclePhoto(vehicleId, uri)
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

    fun saveVehicle(onSuccess: () -> Unit) {
        val state = _formState.value
        if (state.isSaving) return

        if (state.make.isBlank() || state.model.isBlank()) {
            val isFr = repository.userSettings.value.language == SupportedLanguage.FR
            _formState.value = state.copy(
                errorMessage = if (isFr) {
                    "La marque et le modèle sont requis"
                } else {
                    "Make and model are required"
                }
            )
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
            photoUrl = state.photoUrl.trim(),
            currentOdometer = odoInt,
            notes = state.notes.trim()
        )

        repository.addVehicle(newVehicle)

        if (state.photoUri.isBlank()) {
            _formState.value = AddVehicleFormState()
            onSuccess()
            return
        }

        _formState.value = state.copy(isSaving = true)
        viewModelScope.launch {
            _photoState.value = VehiclePhotoState(vehicleId = newVehicle.id, isBusy = true)
            val result = repository.uploadVehiclePhoto(newVehicle.id, Uri.parse(state.photoUri))
            _photoState.value = VehiclePhotoState(
                vehicleId = newVehicle.id,
                isBusy = false,
                errorMessage = result.exceptionOrNull()?.message
            )
            _formState.value = AddVehicleFormState()
            onSuccess()
        }
    }
}
