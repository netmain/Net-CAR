package com.netcar.app.ui.fillups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netcar.app.data.NetCarRepository
import com.netcar.app.model.FillUp
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
import javax.inject.Inject

data class FillUpUiState(
    val fillUps: List<FillUp> = emptyList(),
    val selectedVehicle: Vehicle? = null,
    val totalSpent: Double = 0.0,
    val totalVolume: Double = 0.0,
    val averagePricePerUnit: Double = 0.0,
    val userSettings: UserSettings = UserSettings(),
    val isLoading: Boolean = false
)

data class AddFillUpFormState(
    val odometer: String = "",
    val volume: String = "",
    val pricePerUnit: String = "",
    val totalCost: String = "",
    val isFullTank: Boolean = true,
    val station: String = "",
    val fuelType: FuelType = FuelType.GASOLINE,
    val notes: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class FillUpViewModel @Inject constructor(
    private val repository: NetCarRepository
) : ViewModel() {

    val uiState: StateFlow<FillUpUiState> = combine(
        repository.fillUps,
        repository.vehicles,
        repository.selectedVehicleId,
        repository.userSettings
    ) { fillUps, vehicles, selectedId, settings ->
        val selected = vehicles.find { it.id == selectedId } ?: vehicles.firstOrNull()
        val vehicleFillUps = fillUps
            .filter { it.vehicleId == selected?.id }
            .sortedByDescending { it.date }

        val totalSpent = vehicleFillUps.sumOf { it.totalCost }
        val totalVol = vehicleFillUps.sumOf { it.volume }
        val avgPrice = if (totalVol > 0) totalSpent / totalVol else 0.0

        FillUpUiState(
            fillUps = vehicleFillUps,
            selectedVehicle = selected,
            totalSpent = totalSpent,
            totalVolume = totalVol,
            averagePricePerUnit = avgPrice,
            userSettings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FillUpUiState(isLoading = true)
    )

    private val _formState = MutableStateFlow(AddFillUpFormState())
    val formState: StateFlow<AddFillUpFormState> = _formState.asStateFlow()

    fun updateOdometer(odo: String) = _formState.apply { value = value.copy(odometer = odo) }

    fun updateVolume(vol: String) {
        val volDouble = vol.toDoubleOrNull()
        val priceDouble = _formState.value.pricePerUnit.toDoubleOrNull()
        val newTotal = if (volDouble != null && priceDouble != null) {
            String.format(java.util.Locale.US, "%.2f", volDouble * priceDouble)
        } else _formState.value.totalCost

        _formState.value = _formState.value.copy(volume = vol, totalCost = newTotal)
    }

    fun updatePricePerUnit(price: String) {
        val priceDouble = price.toDoubleOrNull()
        val totalDouble = _formState.value.totalCost.toDoubleOrNull()
        val volDouble = _formState.value.volume.toDoubleOrNull()

        val newVol = if (priceDouble != null && priceDouble > 0 && totalDouble != null && totalDouble > 0) {
            String.format(java.util.Locale.US, "%.2f", totalDouble / priceDouble)
        } else _formState.value.volume

        val newTotal = if (volDouble != null && priceDouble != null && (totalDouble == null || totalDouble <= 0)) {
            String.format(java.util.Locale.US, "%.2f", volDouble * priceDouble)
        } else _formState.value.totalCost

        _formState.value = _formState.value.copy(pricePerUnit = price, volume = newVol, totalCost = newTotal)
    }

    fun updateTotalCost(total: String) {
        val totalDouble = total.toDoubleOrNull()
        val priceDouble = _formState.value.pricePerUnit.toDoubleOrNull()
        val volDouble = _formState.value.volume.toDoubleOrNull()

        val newVol = if (totalDouble != null && priceDouble != null && priceDouble > 0) {
            // Calculate fuel volume automatically when user enters total cost and unit price!
            String.format(java.util.Locale.US, "%.2f", totalDouble / priceDouble)
        } else _formState.value.volume

        val newPrice = if (newVol == _formState.value.volume && totalDouble != null && volDouble != null && volDouble > 0) {
            String.format(java.util.Locale.US, "%.3f", totalDouble / volDouble)
        } else _formState.value.pricePerUnit

        _formState.value = _formState.value.copy(totalCost = total, volume = newVol, pricePerUnit = newPrice)
    }

    fun updateIsFullTank(full: Boolean) = _formState.apply { value = value.copy(isFullTank = full) }
    fun updateStation(station: String) = _formState.apply { value = value.copy(station = station) }
    fun updateFuelType(fuelType: FuelType) = _formState.apply { value = value.copy(fuelType = fuelType) }
    fun updateNotes(notes: String) = _formState.apply { value = value.copy(notes = notes) }

    fun deleteFillUp(id: String) {
        repository.deleteFillUp(id)
    }

    fun saveFillUp(onSuccess: () -> Unit) {
        val state = _formState.value
        val vehicle = uiState.value.selectedVehicle ?: return

        val odoInt = state.odometer.toIntOrNull()
        if (odoInt == null || odoInt <= 0) {
            _formState.value = state.copy(errorMessage = "Please enter a valid odometer reading")
            return
        }

        val volDouble = state.volume.toDoubleOrNull()
        if (volDouble == null || volDouble <= 0) {
            _formState.value = state.copy(errorMessage = "Please enter fuel volume")
            return
        }

        val totalDouble = state.totalCost.toDoubleOrNull()
            ?: (volDouble * (state.pricePerUnit.toDoubleOrNull() ?: 1.80))
        val priceDouble = state.pricePerUnit.toDoubleOrNull() ?: (totalDouble / volDouble)

        val fillUp = FillUp(
            vehicleId = vehicle.id,
            odometer = odoInt,
            volume = volDouble,
            pricePerUnit = priceDouble,
            totalCost = totalDouble,
            fuelType = state.fuelType,
            isFullTank = state.isFullTank,
            station = state.station.trim(),
            notes = state.notes.trim()
        )

        repository.addFillUp(fillUp)
        _formState.value = AddFillUpFormState()
        onSuccess()
    }
}
