package com.netcar.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netcar.app.MainActivity
import com.netcar.app.data.NetCarRepository
import com.netcar.app.model.AppUpdateInfo
import com.netcar.app.model.SupportedCurrency
import com.netcar.app.model.SupportedLanguage
import com.netcar.app.model.ThemeMode
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
    val isWaitingForBrowserAuth: Boolean = false,
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

    private val _isWaitingForBrowserAuth = MutableStateFlow(false)

    private val _csvStatusMessage = MutableStateFlow<String?>(null)
    private val _csvIsError = MutableStateFlow(false)

    private val _isCheckingUpdate = MutableStateFlow(false)
    private val _availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    private val _updateError = MutableStateFlow<String?>(null)

    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()
    val availableUpdate: StateFlow<AppUpdateInfo?> = _availableUpdate.asStateFlow()
    val updateError: StateFlow<String?> = _updateError.asStateFlow()
    val appVersion: String = repository.localVersionName()

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.userSettings,
        repository.vehicles,
        repository.selectedVehicleId,
        combine(_isSyncing, repository.externalAuthLoading) { a, b -> a || b },
        combine(
            combine(_syncMessage, repository.externalAuthMessage) { localMsg, extMsg -> extMsg ?: localMsg },
            combine(_syncError, repository.externalAuthError) { localErr, extErr -> extErr ?: localErr },
            _isWaitingForBrowserAuth,
            combine(_csvStatusMessage, _csvIsError) { msg, isErr -> msg to isErr }
        ) { syncMsg, syncErr, waitingBrowser, csvStatus ->
            AuthAndCsvExtraState(
                syncMessage = syncMsg,
                syncError = syncErr,
                isWaitingForBrowserAuth = waitingBrowser,
                csvMessage = csvStatus.first,
                csvIsError = csvStatus.second
            )
        }
    ) { settings, vehicles, selectedId, syncing, extra ->
        val activeVehicle = vehicles.find { it.id == selectedId } ?: vehicles.firstOrNull()
        val isNowConnected = settings.isDriveBackupConnected && !settings.connectedEmail.isNullOrBlank()
        SettingsUiState(
            settings = settings,
            activeVehicle = activeVehicle,
            isSyncing = syncing,
            syncSuccessMessage = extra.syncMessage,
            syncErrorMessage = extra.syncError,
            isWaitingForBrowserAuth = extra.isWaitingForBrowserAuth && !isNowConnected,
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
        val isWaitingForBrowserAuth: Boolean,
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

    fun setThemeMode(themeMode: ThemeMode) {
        repository.updateThemeMode(themeMode)
    }

    fun checkForUpdates() {
        if (_isCheckingUpdate.value) return
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            _updateError.value = null
            val isFr = repository.userSettings.value.language == SupportedLanguage.FR
            try {
                val result = repository.checkForAppUpdate()
                result.fold(
                    onSuccess = { info ->
                        _availableUpdate.value = info
                        if (info == null) {
                            _updateError.value = null
                        }
                    },
                    onFailure = { err ->
                        _updateError.value = err.localizedMessage
                            ?: if (isFr) {
                                "Impossible de vérifier les mises à jour."
                            } else {
                                "Could not check for updates."
                            }
                    }
                )
            } finally {
                _isCheckingUpdate.value = false
            }
        }
    }

    fun clearMessages() {
        _syncMessage.value = null
        _syncError.value = null
        _csvStatusMessage.value = null
        repository.clearExternalAuthStatus()
    }

    fun startExternalGoogleSignIn(context: Context, emailHint: String? = null) {
        val isFr = repository.userSettings.value.language == SupportedLanguage.FR
        _syncError.value = null
        repository.clearExternalAuthStatus()
        val url = repository.buildExternalGoogleOAuthUrl(emailHint)
        val launched = MainActivity.launchExternalUrl(context, url)
        if (launched) {
            _isWaitingForBrowserAuth.value = true
            _syncMessage.value = if (isFr) {
                "Navigateur ouvert : sélectionnez votre compte Google pour synchroniser automatiquement Net-CAR."
            } else {
                "Browser opened: select your Google account to automatically sync Net-CAR."
            }
        } else {
            _syncError.value = if (isFr) {
                "Impossible d'ouvrir le navigateur système sur cet appareil."
            } else {
                "Unable to open system browser on this device."
            }
        }
    }

    fun completeGoogleSignInFromCallbackInput(rawInput: String) {
        if (rawInput.isBlank()) return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            _syncMessage.value = null
            try {
                val res = repository.completeOAuthFromCallbackUri(rawInput)
                res.fold(
                    onSuccess = { msg ->
                        _isWaitingForBrowserAuth.value = false
                        _syncMessage.value = msg
                    },
                    onFailure = { err ->
                        _syncError.value = err.localizedMessage
                    }
                )
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun signInWithEmail(context: Context, email: String, password: String) {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = null
            _syncError.value = null
            repository.clearExternalAuthStatus()
            try {
                val result = repository.signInWithEmailPassword(email, password)
                result.fold(
                    onSuccess = { msg -> _syncMessage.value = msg },
                    onFailure = { err ->
                        if (err is NetCarRepository.GoogleOAuthFallbackRequiredException) {
                            _isSyncing.value = false
                            startExternalGoogleSignIn(context, err.emailHint)
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

    fun signUpWithEmail(context: Context, email: String, password: String) {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = null
            _syncError.value = null
            repository.clearExternalAuthStatus()
            try {
                val result = repository.signUpWithEmailPassword(email, password)
                result.fold(
                    onSuccess = { msg -> _syncMessage.value = msg },
                    onFailure = { err ->
                        if (err is NetCarRepository.GoogleOAuthFallbackRequiredException) {
                            _isSyncing.value = false
                            startExternalGoogleSignIn(context, err.emailHint)
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
        _isWaitingForBrowserAuth.value = false
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
