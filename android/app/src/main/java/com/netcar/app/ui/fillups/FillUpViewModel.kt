package com.netcar.app.ui.fillups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netcar.app.data.NetCarRepository
import com.netcar.app.model.FillUp
import com.netcar.app.model.FuelType
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
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FillUpUiState(
    val fillUps: List<FillUp> = emptyList(),
    val selectedVehicle: Vehicle? = null,
    val totalSpent: Double = 0.0,
    val totalVolume: Double = 0.0,
    val averagePricePerUnit: Double = 0.0,
    val userSettings: UserSettings = UserSettings(),
    val csvStatusMessage: String? = null,
    val csvIsError: Boolean = false,
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

    private val _csvStatusMessage = MutableStateFlow<String?>(null)
    private val _csvIsError = MutableStateFlow(false)

    val uiState: StateFlow<FillUpUiState> = combine(
        repository.fillUps,
        repository.vehicles,
        repository.selectedVehicleId,
        repository.userSettings,
        combine(_csvStatusMessage, _csvIsError) { msg, isErr -> msg to isErr }
    ) { fillUps, vehicles, selectedId, settings, csvState ->
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
            userSettings = settings,
            csvStatusMessage = csvState.first,
            csvIsError = csvState.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FillUpUiState(isLoading = true)
    )

    private val _formState = MutableStateFlow(AddFillUpFormState())
    val formState: StateFlow<AddFillUpFormState> = _formState.asStateFlow()

    fun updateOdometer(odo: String) = _formState.apply { value = value.copy(odometer = odo, errorMessage = null) }

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

    fun exportActiveVehicleCsv(): Pair<String, String> {
        return repository.exportActiveVehicleFuelioCsv()
    }

    fun reportCsvExportSuccess(fileName: String) {
        val isFr = repository.userSettings.value.language == SupportedLanguage.FR
        _csvIsError.value = false
        _csvStatusMessage.value = if (isFr) {
            "CSV exporté avec succès ($fileName)."
        } else {
            "CSV exported successfully ($fileName)."
        }
    }

    fun reportCsvError(message: String) {
        _csvIsError.value = true
        _csvStatusMessage.value = message
    }

    fun importCsvContent(csvContent: String) {
        viewModelScope.launch {
            _csvStatusMessage.value = null
            val res = repository.importFuelioCsv(csvContent)
            res.fold(
                onSuccess = { msg ->
                    _csvIsError.value = false
                    _csvStatusMessage.value = msg
                },
                onFailure = { err ->
                    _csvIsError.value = true
                    _csvStatusMessage.value = err.localizedMessage ?: "CSV import error"
                }
            )
        }
    }

    fun saveFillUp(onSuccess: () -> Unit) {
        val state = _formState.value
        val vehicle = uiState.value.selectedVehicle ?: return
        val isFr = uiState.value.userSettings.language == SupportedLanguage.FR

        val odoInt = state.odometer.toIntOrNull()
        if (odoInt == null || odoInt <= 0) {
            _formState.value = state.copy(
                errorMessage = if (isFr) "Veuillez saisir un kilométrage valide" else "Please enter a valid odometer reading"
            )
            return
        }

        val maxExistingOdo = maxOf(
            vehicle.currentOdometer,
            uiState.value.fillUps.maxOfOrNull { it.odometer } ?: 0
        )
        if (odoInt < maxExistingOdo) {
            _formState.value = state.copy(
                errorMessage = if (isFr) {
                    "Le kilométrage ($odoInt) ne peut pas être inférieur au dernier relevé ($maxExistingOdo)"
                } else {
                    "Odometer ($odoInt) cannot be less than the latest record ($maxExistingOdo)"
                }
            )
            return
        }

        val volDouble = state.volume.toDoubleOrNull()
        if (volDouble == null || volDouble <= 0) {
            _formState.value = state.copy(
                errorMessage = if (isFr) "Veuillez saisir le volume de carburant" else "Please enter fuel volume"
            )
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
