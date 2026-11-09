package com.netcar.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netcar.app.data.NetCarRepository
import com.netcar.app.model.SupportedCurrency
import com.netcar.app.model.SupportedLanguage
import com.netcar.app.model.Units
import com.netcar.app.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val syncSuccessMessage: String? = null,
    val syncErrorMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: NetCarRepository
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.userSettings,
        _isSyncing,
        _syncMessage,
        _syncError
    ) { settings, syncing, msg, err ->
        SettingsUiState(
            settings = settings,
            isSyncing = syncing,
            syncSuccessMessage = msg,
            syncErrorMessage = err
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

    fun clearMessages() {
        _syncMessage.value = null
        _syncError.value = null
    }

    fun signInWithEmail(email: String, password: String) {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = null
            _syncError.value = null
            try {
                val result = repository.signInWithEmailPassword(email, password)
                result.fold(
                    onSuccess = { msg -> _syncMessage.value = msg },
                    onFailure = { err ->
                        _syncError.value = err.localizedMessage
                            ?: if (repository.userSettings.value.language == SupportedLanguage.FR)
                                "Échec de la connexion Cloud. Vérifiez votre e-mail et mot de passe."
                            else
                                "Cloud sign-in failed. Check your email and password."
                    }
                )
            } catch (t: Throwable) {
                _syncError.value = t.localizedMessage ?: "Authentication error"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = null
            _syncError.value = null
            try {
                val result = repository.signUpWithEmailPassword(email, password)
                result.fold(
                    onSuccess = { msg -> _syncMessage.value = msg },
                    onFailure = { err ->
                        _syncError.value = err.localizedMessage
                            ?: if (repository.userSettings.value.language == SupportedLanguage.FR)
                                "Impossible de créer le compte Cloud."
                            else
                                "Could not create Cloud account."
                    }
                )
            } catch (t: Throwable) {
                _syncError.value = t.localizedMessage ?: "Account creation error"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun signOut() {
        repository.signOutCloud()
        _syncError.value = null
        _syncMessage.value = if (repository.userSettings.value.language == SupportedLanguage.FR) {
            "Déconnecté du Cloud. Mode local actif."
        } else {
            "Signed out of Cloud. Local mode active."
        }
    }

    fun syncWithGoogleDrive() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = null
            _syncError.value = null
            try {
                val result = repository.syncWithCloud()
                result.fold(
                    onSuccess = { msg -> _syncMessage.value = msg },
                    onFailure = { err ->
                        _syncError.value = err.localizedMessage
                            ?: if (repository.userSettings.value.language == SupportedLanguage.FR)
                                "Synchronisation impossible : connectez d'abord votre compte."
                            else
                                "Sync failed: please sign in to your account first."
                    }
                )
            } catch (t: Throwable) {
                _syncError.value = t.localizedMessage ?: "Cloud sync failed."
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
