package com.netcar.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netcar.app.data.NetCarRepository
import com.netcar.app.model.SupportedCurrency
import com.netcar.app.model.SupportedLanguage
import com.netcar.app.model.Units
import com.netcar.app.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val isSyncing: Boolean = false,
    val syncSuccessMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: NetCarRepository
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.userSettings,
        _isSyncing,
        _syncMessage
    ) { settings, syncing, msg ->
        SettingsUiState(
            settings = settings,
            isSyncing = syncing,
            syncSuccessMessage = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setLanguage(language: SupportedLanguage) {
        repository.updateLanguage(language)
    }

    fun setUnits(units: Units) {
        repository.updateUnits(units)
    }

    fun setCurrency(currency: SupportedCurrency) {
        repository.updateCurrency(currency)
    }

    fun syncWithGoogleDrive() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = null
            delay(1200)
            repository.triggerCloudBackup()
            _isSyncing.value = false
            _syncMessage.value = "Backup uploaded to Google Drive successfully!"
        }
    }
}
