package com.netcar.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netcar.app.data.NetCarRepository
import com.netcar.app.model.SupportedCurrency
import com.netcar.app.model.SupportedLanguage
import com.netcar.app.model.Units
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

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val activeVehicle: Vehicle? = null,
    val isSyncing: Boolean = false,
    val syncSuccessMessage: String? = null,
    val syncErrorMessage: String? = null,
    val showGoogleOAuthDialog: Boolean = false,
    val googleOAuthEmailHint: String? = null,
    val csvStatusMessage: String? = null,
    val csvIsError: Boolean = false
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

    private val _showGoogleOAuthDialog = MutableStateFlow(false)
    private val _googleOAuthEmailHint = MutableStateFlow<String?>(null)

    private val _csvStatusMessage = MutableStateFlow<String?>(null)
    private val _csvIsError = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.userSettings,
        repository.vehicles,
        repository.selectedVehicleId,
        _isSyncing,
        combine(
            _syncMessage,
            _syncError,
            _showGoogleOAuthDialog,
            _googleOAuthEmailHint,
            combine(_csvStatusMessage, _csvIsError) { msg, isErr -> msg to isErr }
        ) { syncMsg, syncErr, showDialog, hint, csvStatus ->
             AuthAndCsvExtraState(
                syncMessage = syncMsg,
                syncError = syncErr,
                showGoogleOAuthDialog = showDialog,
                googleOAuthEmailHint = hint,
                csvMessage = csvStatus.first,
                csvIsError = csvStatus.second
            )
        }
    ) { settings, vehicles, selectedId, syncing, extra ->
        val activeVehicle = vehicles.find { it.id == selectedId } ?: vehicles.firstOrNull()
        SettingsUiState(
            settings = settings,
            activeVehicle = activeVehicle,
            isSyncing = syncing,
            syncSuccessMessage = extra.syncMessage,
            syncErrorMessage = extra.syncError,
            showGoogleOAuthDialog = extra.showGoogleOAuthDialog,
            googleOAuthEmailHint = extra.googleOAuthEmailHint,
            csvStatusMessage = extra.csvMessage,
            csvIsError = extra.csvIsError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    private data class AuthAndCsvExtraState(
        val syncMessage: String?,
        val syncError: String?,
        val showGoogleOAuthDialog: Boolean,
        val googleOAuthEmailHint: String?,
        val csvMessage: String?,
        val csvIsError: Boolean
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
        _csvStatusMessage.value = null
    }

    fun openGoogleOAuthDialog(emailHint: String? = null) {
        _syncMessage.value = null
        _syncError.value = null
        _googleOAuthEmailHint.value = emailHint?.trim()?.takeIf { it.isNotBlank() }
        _showGoogleOAuthDialog.value = true
    }

    fun dismissGoogleOAuthDialog() {
        _showGoogleOAuthDialog.value = false
    }

    fun onGoogleOAuthError(message: String) {
        _showGoogleOAuthDialog.value = false
        _isSyncing.value = false
        _syncError.value = message
    }

    fun completeGoogleSignInWithIdToken(idToken: String, emailHint: String? = null) {
        _showGoogleOAuthDialog.value = false
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = null
            _syncError.value = null
            try {
                val result = repository.signInWithGoogleIdToken(idToken, emailHint ?: _googleOAuthEmailHint.value)
                result.fold(
                    onSuccess = { msg -> _syncMessage.value = msg },
                    onFailure = { err ->
                        _syncError.value = err.localizedMessage
                            ?: if (repository.userSettings.value.language == SupportedLanguage.FR)
                                "Échec de la connexion Google."
                            else
                                "Google sign-in failed."
                    }
                )
            } catch (t: Throwable) {
                _syncError.value = t.localizedMessage ?: "Google authentication error"
            } finally {
                _isSyncing.value = false
            }
        }
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
                        if (err is NetCarRepository.GoogleOAuthFallbackRequiredException) {
                            _isSyncing.value = false
                            openGoogleOAuthDialog(err.emailHint)
                            return@launch
                        }
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
                        if (err is NetCarRepository.GoogleOAuthFallbackRequiredException) {
                            _isSyncing.value = false
                            openGoogleOAuthDialog(err.emailHint)
                            return@launch
                        }
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

    fun exportActiveVehicleCsv(): Pair<String, String> {
        return repository.exportActiveVehicleFuelioCsv()
    }

    fun reportCsvExportSuccess(fileName: String) {
        val isFr = repository.userSettings.value.language == SupportedLanguage.FR
        _csvIsError.value = false
        _csvStatusMessage.value = if (isFr) {
            "Fichier CSV exporté avec succès ($fileName)."
        } else {
            "CSV file exported successfully ($fileName)."
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
}
