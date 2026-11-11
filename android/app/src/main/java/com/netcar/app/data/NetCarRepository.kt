package com.netcar.app.data

import android.app.Activity
import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.netcar.app.di.FirebaseModule
import com.netcar.app.model.Expense
import com.netcar.app.model.ExpenseCategory
import com.netcar.app.model.FillUp
import com.netcar.app.model.FuelType
import com.netcar.app.model.Reminder
import com.netcar.app.model.ServiceRecord
import com.netcar.app.model.ServiceType
import com.netcar.app.model.SupportedCurrency
import com.netcar.app.model.SupportedLanguage
import com.netcar.app.model.ThemeMode
import com.netcar.app.model.Units
import com.netcar.app.model.UserSettings
import com.netcar.app.model.Vehicle
import com.netcar.app.notifications.ReminderNotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetCarRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("netcar_local_store", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val now = System.currentTimeMillis()
    private val oneDay = 86400000L

    private val defaultVehicle1 = Vehicle(
        id = "veh-1",
        name = "Daily TDI",
        make = "Volkswagen",
        model = "Golf VII 2.0 TDI",
        year = 2019,
        plate = "00124-116-16",
        vin = "WVWZZZAUZKW123456",
        fuelType = FuelType.DIESEL,
        currentOdometer = 64250,
        notes = "Finition Carat, boîte DSG6, entretien à jour"
    )

    private val defaultVehicle2 = Vehicle(
        id = "veh-2",
        name = "Citadine",
        make = "Renault",
        model = "Clio V 1.0 TCe",
        year = 2021,
        plate = "09834-118-31",
        vin = "VF1RJA00067890123",
        fuelType = FuelType.GASOLINE,
        currentOdometer = 29100,
        notes = "Véhicule urbain"
    )

    private val defaultFillUps = listOf(
        FillUp(
            id = "f-1",
            vehicleId = "veh-1",
            date = now - (2 * oneDay),
            odometer = 64250,
            volume = 50.0,
            pricePerUnit = 29.0,
            totalCost = 1450.0,
            fuelType = FuelType.DIESEL,
            isFullTank = true,
            station = "Naftal Sidi Fredj",
            notes = "Plein de gasoil Naftal"
        ),
        FillUp(
            id = "f-2",
            vehicleId = "veh-1",
            date = now - (12 * oneDay),
            odometer = 63480,
            volume = 48.0,
            pricePerUnit = 29.0,
            totalCost = 1392.0,
            fuelType = FuelType.DIESEL,
            isFullTank = true,
            station = "Naftal Autoroute Est-Ouest",
            notes = ""
        ),
        FillUp(
            id = "f-3",
            vehicleId = "veh-1",
            date = now - (24 * oneDay),
            odometer = 62710,
            volume = 47.5,
            pricePerUnit = 29.0,
            totalCost = 1377.5,
            fuelType = FuelType.DIESEL,
            isFullTank = true,
            station = "Naftal Chéraga",
            notes = "Trajet mixte ville / rocade"
        ),
        FillUp(
            id = "f-4",
            vehicleId = "veh-1",
            date = now - (38 * oneDay),
            odometer = 61900,
            volume = 46.2,
            pricePerUnit = 29.0,
            totalCost = 1339.8,
            fuelType = FuelType.DIESEL,
            isFullTank = true,
            station = "Naftal Hydra",
            notes = ""
        ),
        FillUp(
            id = "f-5",
            vehicleId = "veh-1",
            date = now - (52 * oneDay),
            odometer = 61090,
            volume = 49.0,
            pricePerUnit = 29.0,
            totalCost = 1421.0,
            fuelType = FuelType.DIESEL,
            isFullTank = true,
            station = "Naftal Dar El Beïda",
            notes = ""
        ),
        FillUp(
            id = "f-6",
            vehicleId = "veh-2",
            date = now - (5 * oneDay),
            odometer = 29100,
            volume = 38.0,
            pricePerUnit = 45.6,
            totalCost = 1732.8,
            fuelType = FuelType.GASOLINE,
            isFullTank = true,
            station = "Naftal Hydra",
            notes = "Sans Plomb"
        )
    )

    private val defaultExpenses = listOf(
        Expense(
            id = "e-1",
            vehicleId = "veh-1",
            date = now - (5 * oneDay),
            category = ExpenseCategory.PARKING,
            amount = 300.0,
            description = "Parking souterrain Alger Centre",
            odometer = 64150
        ),
        Expense(
            id = "e-2",
            vehicleId = "veh-1",
            date = now - (14 * oneDay),
            category = ExpenseCategory.CAR_WASH,
            amount = 800.0,
            description = "Lavage complet intérieur + extérieur",
            odometer = 63600
        ),
        Expense(
            id = "e-3",
            vehicleId = "veh-1",
            date = now - (30 * oneDay),
            category = ExpenseCategory.INSURANCE,
            amount = 42000.0,
            description = "Assurance tous risques annuelle SAA",
            odometer = 62000
        )
    )

    private val defaultServices = listOf(
        ServiceRecord(
            id = "s-1",
            vehicleId = "veh-1",
            date = now - (45 * oneDay),
            type = ServiceType.OIL_CHANGE,
            odometer = 60000,
            cost = 14500.0,
            description = "Vidange huile moteur Castrol 5W-30 + filtres complets",
            parts = listOf("Castrol Edge 5W-30", "Filtre à huile Mann", "Filtre habitacle"),
            notes = "Effectué à 60 000 km"
        ),
        ServiceRecord(
            id = "s-2",
            vehicleId = "veh-1",
            date = now - (120 * oneDay),
            type = ServiceType.BRAKES,
            odometer = 52000,
            cost = 18000.0,
            description = "Remplacement plaquettes de frein avant",
            parts = listOf("Plaquettes Brembo Avant"),
            notes = "Disques en bon état"
        )
    )

    private val defaultReminders = listOf(
        Reminder(
            id = "r-1",
            vehicleId = "veh-1",
            title = "Vidange & Filtre à huile",
            type = ServiceType.OIL_CHANGE,
            intervalKm = 10000,
            intervalDays = 365,
            lastServiceDate = now - (45 * oneDay),
            lastServiceOdometer = 60000,
            nextDueDate = now + (320 * oneDay),
            nextDueOdometer = 70000,
            isRecurring = true,
            isCompleted = false
        ),
        Reminder(
            id = "r-2",
            vehicleId = "veh-1",
            title = "Permutation & équilibrage pneus",
            type = ServiceType.TIRE_ROTATION,
            intervalKm = 10000,
            intervalDays = 180,
            lastServiceDate = now - (150 * oneDay),
            lastServiceOdometer = 54000,
            nextDueDate = now + (30 * oneDay),
            nextDueOdometer = 64000,
            isRecurring = true,
            isCompleted = false
        ),
        Reminder(
            id = "r-3",
            vehicleId = "veh-1",
            title = "Contrôle technique périodique",
            type = ServiceType.INSPECTION,
            intervalDays = 365,
            lastServiceDate = now - (370 * oneDay),
            nextDueDate = now - (5 * oneDay),
            isRecurring = true,
            isCompleted = false
        )
    )

    private val _vehicles = MutableStateFlow(loadVehicles())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()

    private val _selectedVehicleId = MutableStateFlow<String?>(
        prefs.getString("selected_vehicle_id", null) ?: _vehicles.value.firstOrNull()?.id
    )
    val selectedVehicleId: StateFlow<String?> = _selectedVehicleId.asStateFlow()

    private val _fillUps = MutableStateFlow(loadFillUps())
    val fillUps: StateFlow<List<FillUp>> = _fillUps.asStateFlow()

    private val _expenses = MutableStateFlow(loadExpenses())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _serviceRecords = MutableStateFlow(loadServices())
    val serviceRecords: StateFlow<List<ServiceRecord>> = _serviceRecords.asStateFlow()

    private val _reminders = MutableStateFlow(loadReminders())
    val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()

    private val _userSettings = MutableStateFlow(loadSettings())
    val userSettings: StateFlow<UserSettings> = _userSettings.asStateFlow()

    private val _externalAuthLoading = MutableStateFlow(false)
    val externalAuthLoading: StateFlow<Boolean> = _externalAuthLoading.asStateFlow()

    private val _externalAuthMessage = MutableStateFlow<String?>(null)
    val externalAuthMessage: StateFlow<String?> = _externalAuthMessage.asStateFlow()

    private val _externalAuthError = MutableStateFlow<String?>(null)
    val externalAuthError: StateFlow<String?> = _externalAuthError.asStateFlow()

    @Volatile
    private var pendingOAuthEmailHint: String? = null

    init {
        refreshAuthStatusSafely()
        checkRemindersAndNotify(forceAllActive = false)
    }

    fun clearExternalAuthStatus() {
        _externalAuthMessage.value = null
        _externalAuthError.value = null
        _externalAuthLoading.value = false
    }

    fun buildExternalGoogleOAuthUrl(emailHint: String? = null): String {
        val cleanHint = emailHint?.trim()?.takeIf { it.isNotBlank() && it.contains("@") }
        pendingOAuthEmailHint = cleanHint
        val base = "https://axiomatic-serenity-8lvvl.firebaseapp.com/__/auth/handler" +
            "?apiKey=AIzaSyDy28X7_hNYpn3BmxjdzUHOzmmAnITMUC4" +
            "&authType=signInViaRedirect" +
            "&providerId=google.com" +
            "&ibi=com.netcar.app" +
            "&clientId=1033623803537-9aqcjj37isscfde2aq3dp1t0nhj2hict.apps.googleusercontent.com" +
            "&appId=1:1033623803537:web:cade975943dc5c1084d130" +
            "&appDisplayName=Net-CAR"
        return if (cleanHint != null) {
            val customJson = """{"prompt":"select_account","login_hint":"$cleanHint"}"""
            "$base&customParameters=${URLEncoder.encode(customJson, "UTF-8")}"
        } else {
            val customJson = """{"prompt":"select_account"}"""
            "$base&customParameters=${URLEncoder.encode(customJson, "UTF-8")}"
        }
    }

    fun extractIdTokenFromDeepLink(rawInput: String?): String? {
        if (rawInput.isNullOrBlank()) return null
        val trimmed = rawInput.trim()
        // If user pasted a raw JWT id_token directly (3 base64url segments separated by dots)
        if (!trimmed.contains("://") && !trimmed.contains(" ") && trimmed.count { it == '.' } == 2 && trimmed.startsWith("eyJ")) {
            return trimmed
        }
        var current = trimmed
        repeat(5) {
            val match = Regex("""(?:[?#&]|^)id_token=([^&#\s]+)""").find(current)
            if (match != null) {
                val rawToken = match.groupValues.getOrNull(1) ?: ""
                val decodedToken = runCatching { URLDecoder.decode(rawToken, "UTF-8") }.getOrDefault(rawToken)
                if (decodedToken.count { it == '.' } >= 2) {
                    return decodedToken
                }
            }
            val next = runCatching { URLDecoder.decode(current, "UTF-8") }.getOrDefault(current)
            if (next == current) return null
            current = next
        }
        return null
    }

    suspend fun completeOAuthFromCallbackUri(rawUriOrToken: String): Result<String> {
        _externalAuthLoading.value = true
        _externalAuthError.value = null
        _externalAuthMessage.value = null
        return try {
            val idToken = extractIdTokenFromDeepLink(rawUriOrToken)
            if (idToken.isNullOrBlank()) {
                val isFr = _userSettings.value.language == SupportedLanguage.FR
                val err = IllegalArgumentException(
                    if (isFr) "Jeton Google introuvable dans le lien de retour."
                    else "Google ID token not found in callback link."
                )
                _externalAuthError.value = err.message
                Result.failure(err)
            } else {
                val res = signInWithGoogleIdToken(idToken, pendingOAuthEmailHint)
                res.fold(
                    onSuccess = { msg ->
                        _externalAuthMessage.value = msg
                        _externalAuthError.value = null
                        Result.success(msg)
                    },
                    onFailure = { err ->
                        _externalAuthError.value = err.localizedMessage ?: "Google Sign-In error"
                        Result.failure(err)
                    }
                )
            }
        } catch (t: Throwable) {
            val friendly = formatFriendlyAuthError(t)
            _externalAuthError.value = friendly.localizedMessage
            Result.failure(friendly)
        } finally {
            _externalAuthLoading.value = false
        }
    }

    fun checkRemindersAndNotify(forceAllActive: Boolean = false): Int {
        return runCatching {
            ReminderNotificationHelper.checkAndNotifyDueReminders(
                context = context,
                vehicles = _vehicles.value,
                reminders = _reminders.value,
                isFr = _userSettings.value.language == SupportedLanguage.FR,
                forceNotifyAllActive = forceAllActive
            )
        }.getOrDefault(0)
    }

    fun triggerReminderNotificationsNow(forceAllActive: Boolean = true): Int {
        return checkRemindersAndNotify(forceAllActive = forceAllActive)
    }

    private fun refreshAuthStatusSafely() {
        runCatching {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null && !currentUser.isAnonymous && !currentUser.email.isNullOrBlank()) {
                _userSettings.value = _userSettings.value.copy(
                    isDriveBackupConnected = true,
                    connectedEmail = currentUser.email,
                    userId = currentUser.uid
                )
            } else {
                _userSettings.value = _userSettings.value.copy(
                    isDriveBackupConnected = false,
                    connectedEmail = null,
                    userId = null
                )
            }
        }.onFailure {
            _userSettings.value = _userSettings.value.copy(
                isDriveBackupConnected = false,
                connectedEmail = null,
                userId = null
            )
        }
    }

    private fun loadVehicles(): List<Vehicle> {
        val raw = prefs.getString("vehicles_json", null) ?: return listOf(defaultVehicle1, defaultVehicle2)
        return runCatching {
            val type = object : TypeToken<List<Vehicle>>() {}.type
            gson.fromJson<List<Vehicle>>(raw, type)?.takeIf { it.isNotEmpty() }
                ?: listOf(defaultVehicle1, defaultVehicle2)
        }.getOrElse { listOf(defaultVehicle1, defaultVehicle2) }
    }

    private fun loadFillUps(): List<FillUp> {
        val raw = prefs.getString("fillups_json", null) ?: return defaultFillUps
        return runCatching {
            val type = object : TypeToken<List<FillUp>>() {}.type
            gson.fromJson<List<FillUp>>(raw, type) ?: defaultFillUps
        }.getOrElse { defaultFillUps }
    }

    private fun loadExpenses(): List<Expense> {
        val raw = prefs.getString("expenses_json", null) ?: return defaultExpenses
        return runCatching {
            val type = object : TypeToken<List<Expense>>() {}.type
            gson.fromJson<List<Expense>>(raw, type) ?: defaultExpenses
        }.getOrElse { defaultExpenses }
    }

    private fun loadServices(): List<ServiceRecord> {
        val raw = prefs.getString("services_json", null) ?: return defaultServices
        return runCatching {
            val type = object : TypeToken<List<ServiceRecord>>() {}.type
            gson.fromJson<List<ServiceRecord>>(raw, type) ?: defaultServices
        }.getOrElse { defaultServices }
    }

    private fun loadReminders(): List<Reminder> {
        val raw = prefs.getString("reminders_json", null) ?: return defaultReminders
        return runCatching {
            val type = object : TypeToken<List<Reminder>>() {}.type
            gson.fromJson<List<Reminder>>(raw, type) ?: defaultReminders
        }.getOrElse { defaultReminders }
    }

    private fun loadSettings(): UserSettings {
        val langCode = prefs.getString("lang", "fr") ?: "fr"
        val unitsName = prefs.getString("units", Units.METRIC.name) ?: Units.METRIC.name
        val currencyName = prefs.getString("currency", SupportedCurrency.DZD.name) ?: SupportedCurrency.DZD.name
        val themeModeName = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val lastSync = prefs.getLong("last_sync_ts", -1L).takeIf { it > 0L }

        val lang = SupportedLanguage.values().find { it.code == langCode } ?: SupportedLanguage.FR
        val units = runCatching { Units.valueOf(unitsName) }.getOrDefault(Units.METRIC)
        val currency = runCatching { SupportedCurrency.valueOf(currencyName) }.getOrDefault(SupportedCurrency.DZD)
        val themeMode = runCatching { ThemeMode.valueOf(themeModeName) }.getOrDefault(ThemeMode.SYSTEM)

        return UserSettings(
            language = lang,
            units = units,
            currency = currency,
            themeMode = themeMode,
            isDriveBackupConnected = false,
            connectedEmail = null,
            userId = null,
            lastBackupTimestamp = lastSync,
            customExpenseCategories = loadStringList("custom_expense_categories_json"),
            customServiceTypes = loadStringList("custom_service_types_json")
        )
    }

    private fun loadStringList(key: String): List<String> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(raw, type) ?: emptyList()
        }.getOrElse { emptyList() }
    }

    private fun persistLocalData() {
        runCatching {
            prefs.edit()
                .putString("vehicles_json", gson.toJson(_vehicles.value))
                .putString("selected_vehicle_id", _selectedVehicleId.value)
                .putString("fillups_json", gson.toJson(_fillUps.value))
                .putString("expenses_json", gson.toJson(_expenses.value))
                .putString("services_json", gson.toJson(_serviceRecords.value))
                .putString("reminders_json", gson.toJson(_reminders.value))
                .putString("lang", _userSettings.value.language.code)
                .putString("units", _userSettings.value.units.name)
                .putString("currency", _userSettings.value.currency.name)
                .putString("theme_mode", _userSettings.value.themeMode.name)
                .putString(
                    "custom_expense_categories_json",
                    gson.toJson(_userSettings.value.customExpenseCategories)
                )
                .putString(
                    "custom_service_types_json",
                    gson.toJson(_userSettings.value.customServiceTypes)
                )
                .apply()
        }
    }

    fun selectVehicle(vehicleId: String) {
        _selectedVehicleId.value = vehicleId
        persistLocalData()
    }

    fun addVehicle(vehicle: Vehicle) {
        _vehicles.value = _vehicles.value + vehicle.copy(updatedAt = localMutationTimestamp())
        if (_selectedVehicleId.value == null) {
            _selectedVehicleId.value = vehicle.id
        }
        persistLocalData()
    }

    fun deleteVehicle(vehicleId: String) {
        val photoUrl = _vehicles.value.firstOrNull { it.id == vehicleId }?.photoUrl.orEmpty()
        markPendingDelete("vehicles", vehicleId)
        _vehicles.value = _vehicles.value.filter { it.id != vehicleId }
        if (_selectedVehicleId.value == vehicleId) {
            _selectedVehicleId.value = _vehicles.value.firstOrNull()?.id
        }
        deleteLocalVehiclePhotoFile(photoUrl)
        persistLocalData()
    }

    fun updateVehicleOdometer(vehicleId: String, newOdometer: Int) {
        _vehicles.value = _vehicles.value.map { v ->
            if (v.id == vehicleId && newOdometer > v.currentOdometer) {
                v.copy(currentOdometer = newOdometer, updatedAt = localMutationTimestamp())
            } else v
        }
        persistLocalData()
        checkRemindersAndNotify(forceAllActive = false)
    }

    fun updateVehiclePhoto(vehicleId: String, photoUrl: String) {
        val normalized = photoUrl.trim().take(2048)
        val previousPhotoUrl = _vehicles.value
            .firstOrNull { it.id == vehicleId }
            ?.photoUrl
            .orEmpty()
        if (previousPhotoUrl != normalized) {
            deleteLocalVehiclePhotoFile(previousPhotoUrl)
        }
        _vehicles.value = _vehicles.value.map { vehicle ->
            if (vehicle.id == vehicleId) {
                vehicle.copy(photoUrl = normalized, updatedAt = localMutationTimestamp())
            } else {
                vehicle
            }
        }
        persistLocalData()
    }

    /**
     * Copies a photo picked from the device into the app's private storage and
     * stores its file URI on the vehicle. No cloud storage is required.
     */
    suspend fun saveVehiclePhotoLocally(vehicleId: String, uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            val isFr = _userSettings.value.language == SupportedLanguage.FR
            var target: File? = null
            try {
                val photoFile = localVehiclePhotoFile(vehicleId)
                target = photoFile
                photoFile.parentFile?.mkdirs()

                val input = context.contentResolver.openInputStream(uri)
                    ?: return@withContext Result.failure(
                        IllegalStateException(
                            if (isFr) {
                                "Impossible de lire l'image sélectionnée."
                            } else {
                                "Could not read the selected image."
                            }
                        )
                    )

                input.use { source ->
                    photoFile.outputStream().use { destination ->
                        source.copyTo(destination)
                    }
                }

                if (photoFile.length() == 0L) {
                    photoFile.delete()
                    return@withContext Result.failure(
                        IllegalStateException(
                            if (isFr) {
                                "Le fichier image est vide."
                            } else {
                                "The selected image file is empty."
                            }
                        )
                    )
                }

                val localUri = Uri.fromFile(photoFile).toString()
                updateVehiclePhoto(vehicleId, localUri)
                Result.success(localUri)
            } catch (e: Exception) {
                target?.delete()
                Result.failure(
                    IllegalStateException(
                        if (isFr) {
                            "Échec de l'enregistrement de la photo sur l'appareil."
                        } else {
                            "Failed to save the photo on this device."
                        },
                        e
                    )
                )
            }
        }

    private fun localVehiclePhotoFile(vehicleId: String): File {
        val directory = File(context.filesDir, "vehicle_photos")
        // Unique names keep image loaders from reusing a stale cached bitmap
        // when the user replaces a vehicle photo.
        return File(
            directory,
            "${sanitizeDocId(vehicleId, "veh")}-${System.currentTimeMillis()}.jpg"
        )
    }

    private fun deleteLocalVehiclePhotoFile(photoUrl: String) {
        if (!photoUrl.startsWith("file://")) return
        val path = runCatching { Uri.parse(photoUrl).path }.getOrNull() ?: return
        val photosDirectory = File(context.filesDir, "vehicle_photos")
        val file = File(path)
        if (file.parentFile?.absolutePath == photosDirectory.absolutePath) {
            runCatching { file.delete() }
        }
    }

    suspend fun validateVehiclePhotoUrl(rawUrl: String): Result<String> =
        withContext(Dispatchers.IO) {
            val isFr = _userSettings.value.language == SupportedLanguage.FR
            val urlText = rawUrl.trim()
            if (urlText.isBlank()) {
                return@withContext Result.failure(
                    IllegalArgumentException(
                        if (isFr) "Saisissez une adresse d'image." else "Enter an image address."
                    )
                )
            }
            if (!urlText.startsWith("http://") && !urlText.startsWith("https://")) {
                return@withContext Result.failure(
                    IllegalArgumentException(
                        if (isFr) {
                            "L'adresse doit commencer par http:// ou https://."
                        } else {
                            "The address must start with http:// or https://."
                        }
                    )
                )
            }

            var connection: HttpURLConnection? = null
            try {
                val conn = URL(urlText).openConnection() as HttpURLConnection
                connection = conn
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.instanceFollowRedirects = true
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "Net-CAR/1.0 (Android)")

                val code = conn.responseCode
                if (code !in 200..299) {
                    return@withContext Result.failure(
                        IOException(
                            if (isFr) {
                                "Impossible de charger l'image (HTTP $code)."
                            } else {
                                "Could not load the image (HTTP $code)."
                            }
                        )
                    )
                }

                val contentType = conn.contentType?.lowercase(Locale.US).orEmpty()
                if (contentType.isNotBlank() && !contentType.startsWith("image/")) {
                    return@withContext Result.failure(
                        IllegalArgumentException(
                            if (isFr) {
                                "Le lien ne pointe pas vers une image valide."
                            } else {
                                "The link does not point to a valid image."
                            }
                        )
                    )
                }

                Result.success(urlText.take(2048))
            } catch (e: Exception) {
                Result.failure(
                    IllegalStateException(
                        if (isFr) {
                            "Impossible de charger l'image depuis cette adresse."
                        } else {
                            "Could not load the image from this address."
                        },
                        e
                    )
                )
            } finally {
                connection?.disconnect()
            }
        }

    fun addFillUp(fillUp: FillUp) {
        _fillUps.value = listOf(fillUp.copy(updatedAt = localMutationTimestamp())) + _fillUps.value
        updateVehicleOdometer(fillUp.vehicleId, fillUp.odometer)
        persistLocalData()
    }

    fun deleteFillUp(id: String) {
        markPendingDelete("fillups", id)
        _fillUps.value = _fillUps.value.filter { it.id != id }
        persistLocalData()
    }

    fun addExpense(expense: Expense) {
        _expenses.value = listOf(expense.copy(updatedAt = localMutationTimestamp())) + _expenses.value
        expense.odometer?.let { updateVehicleOdometer(expense.vehicleId, it) }
        persistLocalData()
    }

    fun deleteExpense(id: String) {
        markPendingDelete("expenses", id)
        _expenses.value = _expenses.value.filter { it.id != id }
        persistLocalData()
    }

    fun addServiceRecord(service: ServiceRecord) {
        _serviceRecords.value = listOf(service.copy(updatedAt = localMutationTimestamp())) + _serviceRecords.value
        updateVehicleOdometer(service.vehicleId, service.odometer)
        persistLocalData()
    }

    fun deleteServiceRecord(id: String) {
        markPendingDelete("services", id)
        _serviceRecords.value = _serviceRecords.value.filter { it.id != id }
        persistLocalData()
    }

    fun addReminder(reminder: Reminder) {
        _reminders.value = listOf(reminder.copy(updatedAt = localMutationTimestamp())) + _reminders.value
        persistLocalData()
        checkRemindersAndNotify(forceAllActive = false)
    }

    fun updateReminder(reminder: Reminder) {
        _reminders.value = _reminders.value.map {
            if (it.id == reminder.id) reminder.copy(updatedAt = localMutationTimestamp()) else it
        }
        markReminderPendingUpdate(reminder.id)
        persistLocalData()
        checkRemindersAndNotify(forceAllActive = false)
    }

    fun toggleReminderCompleted(id: String) {
        _reminders.value = _reminders.value.map {
            if (it.id == id) it.copy(isCompleted = !it.isCompleted, updatedAt = localMutationTimestamp()) else it
        }
        markReminderPendingUpdate(id)
        persistLocalData()
        checkRemindersAndNotify(forceAllActive = false)
    }

    fun deleteReminder(id: String) {
        markReminderPendingDelete(id)
        _reminders.value = _reminders.value.filter { it.id != id }
        persistLocalData()
    }

    private fun markReminderPendingUpdate(localId: String) {
        val docId = sanitizeDocId(localId, "r")
        val updates = prefs.getStringSet(KEY_PENDING_REMINDER_UPDATES, emptySet())
            .orEmpty().toMutableSet()
        val deletes = prefs.getStringSet(KEY_PENDING_REMINDER_DELETES, emptySet())
            .orEmpty().toMutableSet()
        deletes.remove(docId)
        updates.add(docId)
        prefs.edit()
            .putStringSet(KEY_PENDING_REMINDER_UPDATES, updates)
            .putStringSet(KEY_PENDING_REMINDER_DELETES, deletes)
            .apply()
    }

    private fun markReminderPendingDelete(localId: String) {
        val docId = sanitizeDocId(localId, "r")
        val updates = prefs.getStringSet(KEY_PENDING_REMINDER_UPDATES, emptySet())
            .orEmpty().toMutableSet()
        val deletes = prefs.getStringSet(KEY_PENDING_REMINDER_DELETES, emptySet())
            .orEmpty().toMutableSet()
        updates.remove(docId)
        deletes.add(docId)
        prefs.edit()
            .putStringSet(KEY_PENDING_REMINDER_UPDATES, updates)
            .putStringSet(KEY_PENDING_REMINDER_DELETES, deletes)
            .apply()
    }

    fun updateLanguage(language: SupportedLanguage) {
        _userSettings.value = _userSettings.value.copy(language = language)
        persistLocalData()
    }

    fun updateUnits(units: Units) {
        _userSettings.value = _userSettings.value.copy(units = units)
        persistLocalData()
    }

    fun updateCurrency(currency: SupportedCurrency) {
        _userSettings.value = _userSettings.value.copy(currency = currency)
        persistLocalData()
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        _userSettings.value = _userSettings.value.copy(themeMode = themeMode)
        persistLocalData()
    }

    fun addCustomExpenseCategory(name: String) {
        val clean = sanitizeCustomCategory(name)
        if (clean.isBlank()) return
        val builtIn = ExpenseCategory.values().flatMap {
            listOf(it.name, it.displayName, it.displayNameFr)
        }
        val current = _userSettings.value.customExpenseCategories
        if ((builtIn + current).any { it.equals(clean, ignoreCase = true) }) return
        _userSettings.value = _userSettings.value.copy(
            customExpenseCategories = (current + clean).takeLast(100)
        )
        persistLocalData()
    }

    fun addCustomServiceType(name: String) {
        val clean = sanitizeCustomCategory(name)
        if (clean.isBlank()) return
        val builtIn = ServiceType.values().flatMap {
            listOf(it.name, it.displayName, it.displayNameFr)
        }
        val current = _userSettings.value.customServiceTypes
        if ((builtIn + current).any { it.equals(clean, ignoreCase = true) }) return
        _userSettings.value = _userSettings.value.copy(
            customServiceTypes = (current + clean).takeLast(100)
        )
        persistLocalData()
    }

    private fun sanitizeCustomCategory(raw: String): String =
        raw.trim().replace(Regex("\\s+"), " ").take(60)

    private fun mergeCustomCategoryLists(vararg lists: List<String>): List<String> {
        val merged = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        for (list in lists) {
            for (raw in list) {
                val clean = sanitizeCustomCategory(raw)
                if (clean.isBlank()) continue
                if (!seen.add(clean.lowercase(Locale.US))) continue
                merged.add(clean)
                if (merged.size >= 100) return merged
            }
        }
        return merged
    }

    fun signOutCloud() {
        runCatching {
            FirebaseAuth.getInstance().signOut()
        }
        _userSettings.value = _userSettings.value.copy(
            isDriveBackupConnected = false,
            connectedEmail = null,
            userId = null
        )
        persistLocalData()
    }

    class GoogleOAuthFallbackRequiredException(val emailHint: String) : Exception("GOOGLE_OAUTH_REQUIRED")

    fun exportActiveVehicleFuelioCsv(vehicleId: String? = null): Pair<String, String> {
        val targetId = vehicleId ?: _selectedVehicleId.value ?: _vehicles.value.firstOrNull()?.id
        val vehicle = _vehicles.value.find { it.id == targetId }
            ?: _vehicles.value.firstOrNull()
            ?: defaultVehicle1
        val vFillUps = _fillUps.value.filter { it.vehicleId == vehicle.id }
        val vExpenses = _expenses.value.filter { it.vehicleId == vehicle.id }
        val csv = FuelioCsvHelper.exportFuelioCsv(
            vehicle = vehicle,
            fillUps = vFillUps,
            expenses = vExpenses,
            units = _userSettings.value.units
        )
        val slug = "${vehicle.make}-${vehicle.model}"
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "vehicle" }
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val fileName = "netcar-fuelio-$slug-$dateStr.csv"
        return fileName to csv
    }

    suspend fun importFuelioCsv(csvContent: String, targetVehicleId: String? = null): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val isFr = _userSettings.value.language == SupportedLanguage.FR
                val targetId = targetVehicleId ?: _selectedVehicleId.value ?: _vehicles.value.firstOrNull()?.id
                val vehicle = _vehicles.value.find { it.id == targetId } ?: _vehicles.value.firstOrNull()
                    ?: return@withContext Result.failure(
                        IllegalStateException(
                            if (isFr) "Aucun véhicule sélectionné pour l'import CSV."
                            else "No active vehicle found for CSV import."
                        )
                    )

                val parsed = FuelioCsvHelper.parseFuelioCsv(
                    csvContent = csvContent,
                    vehicleId = vehicle.id,
                    defaultFuelType = vehicle.fuelType
                )

                if (parsed.fillUps.isEmpty() && parsed.expenses.isEmpty()) {
                    return@withContext Result.failure(
                        IllegalArgumentException(
                            if (isFr) "Aucune donnée valide trouvée dans le fichier CSV (format Fuelio ou table CSV attendu)."
                            else "No valid fill-up or expense rows found in the CSV file."
                        )
                    )
                }

                // Deduplicate against existing entries for this vehicle
                val existingFillKeys = _fillUps.value
                    .filter { it.vehicleId == vehicle.id }
                    .map { "${it.odometer}_${String.format(Locale.US, "%.2f", it.volume)}" }
                    .toSet()

                val newFillUps = parsed.fillUps.filter {
                    "${it.odometer}_${String.format(Locale.US, "%.2f", it.volume)}" !in existingFillKeys
                }

                val existingExpKeys = _expenses.value
                    .filter { it.vehicleId == vehicle.id }
                    .map { "${it.date / 60000}_${String.format(Locale.US, "%.2f", it.amount)}_${it.description}" }
                    .toSet()

                val newExpenses = parsed.expenses.filter {
                    "${it.date / 60000}_${String.format(Locale.US, "%.2f", it.amount)}_${it.description}" !in existingExpKeys
                }

                if (newFillUps.isNotEmpty()) {
                    _fillUps.value = (newFillUps + _fillUps.value).sortedByDescending { it.date }
                }
                if (newExpenses.isNotEmpty()) {
                    _expenses.value = (newExpenses + _expenses.value).sortedByDescending { it.date }
                }
                if (parsed.maxOdometer > vehicle.currentOdometer) {
                    updateVehicleOdometer(vehicle.id, parsed.maxOdometer)
                }
                persistLocalData()

                if (_userSettings.value.isDriveBackupConnected) {
                    runCatching { syncWithCloud() }
                }

                val vehLabel = "${vehicle.make} ${vehicle.model}".trim()
                val msg = if (isFr) {
                    "Import CSV réussi : ${newFillUps.size} plein(s) et ${newExpenses.size} dépense(s) importés sur $vehLabel."
                } else {
                    "CSV imported: ${newFillUps.size} fill-up(s) and ${newExpenses.size} expense(s) added to $vehLabel."
                }
                Result.success(msg)
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    private fun isOperationNotAllowedError(t: Throwable): Boolean {
        val cause = t.cause ?: t
        if (cause is FirebaseAuthException &&
            (cause.errorCode.equals("ERROR_OPERATION_NOT_ALLOWED", ignoreCase = true) ||
                cause.errorCode.equals("ERROR_CONFIGURATION_NOT_FOUND", ignoreCase = true))
        ) {
            return true
        }
        val msg = (t.message ?: "") + " " + (cause.message ?: "")
        return msg.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ||
            msg.contains("operation is not allowed", ignoreCase = true) ||
            msg.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ||
            msg.contains("INVALID_APP_ID", ignoreCase = true)
    }

    private fun formatFriendlyAuthError(t: Throwable): Exception {
        val isFr = _userSettings.value.language == SupportedLanguage.FR
        val cause = t.cause ?: t
        val msg = ((t.message ?: "") + " " + (cause.message ?: "")).lowercase(Locale.US)

        val friendly = when {
            isOperationNotAllowedError(t) -> if (isFr) {
                "Veuillez finaliser la connexion via la fenêtre sécurisée Google Sign-In."
            } else {
                "Please complete sign-in using the secure Google Sign-In window."
            }
            msg.contains("email-already-in-use") || msg.contains("already in use") -> if (isFr) {
                "Cette adresse e-mail possède déjà un compte. Basculez sur « Se connecter » ou utilisez « Continuer avec Google »."
            } else {
                "This email is already registered. Switch to 'Sign In' or tap 'Continue with Google'."
            }
            msg.contains("invalid-credential") || msg.contains("wrong-password") || msg.contains("user-not-found") -> if (isFr) {
                "Identifiants incorrects. Vérifiez votre mot de passe, créez un compte ou utilisez « Continuer avec Google »."
            } else {
                "Invalid email or password. Check your password, create an account, or tap 'Continue with Google'."
            }
            msg.contains("network") -> if (isFr) {
                "Erreur réseau. Vérifiez votre connexion Internet et réessayez."
            } else {
                "Network error. Please check your internet connection and try again."
            }
            else -> cause.localizedMessage ?: if (isFr) "Erreur d'authentification Cloud." else "Cloud authentication error."
        }
        return IllegalStateException(friendly)
    }

    suspend fun signInWithGoogleIdToken(idToken: String, fallbackEmail: String? = null): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val auth = FirebaseAuth.getInstance()
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = Tasks.await(auth.signInWithCredential(credential), 25, TimeUnit.SECONDS)
                val user = authResult.user
                    ?: return@withContext Result.failure(IllegalStateException("Google authentication failed"))

                val resolvedEmail = user.email?.takeIf { it.isNotBlank() }
                    ?: fallbackEmail?.takeIf { it.isNotBlank() }
                    ?: "driver@netcar.app"
                _userSettings.value = _userSettings.value.copy(
                    isDriveBackupConnected = true,
                    connectedEmail = resolvedEmail,
                    userId = user.uid
                )
                persistLocalData()

                val syncRes = syncWithCloud()
                syncRes.fold(
                    onSuccess = { msg -> Result.success(msg) },
                    onFailure = {
                        val fallbackMsg = if (_userSettings.value.language == SupportedLanguage.FR) {
                            "Connecté avec Google ($resolvedEmail)"
                        } else {
                            "Signed in with Google ($resolvedEmail)"
                        }
                        Result.success(fallbackMsg)
                    }
                )
            } catch (t: Throwable) {
                Result.failure(formatFriendlyAuthError(t))
            }
        }

    suspend fun signInWithEmailPassword(
        email: String,
        password: String
    ): Result<String> =
        withContext(Dispatchers.IO) {
            val cleanEmail = email.trim()
            try {
                if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
                    return@withContext Result.failure(
                        IllegalArgumentException(
                            if (_userSettings.value.language == SupportedLanguage.FR)
                                "Veuillez saisir une adresse e-mail valide."
                            else
                                "Please enter a valid email address."
                        )
                    )
                }
                if (password.length < 6) {
                    return@withContext Result.failure(
                        IllegalArgumentException(
                            if (_userSettings.value.language == SupportedLanguage.FR)
                                "Le mot de passe doit contenir au moins 6 caractères."
                            else
                                "Password must be at least 6 characters."
                        )
                    )
                }

                val auth = FirebaseAuth.getInstance()
                val authResult = Tasks.await(
                    auth.signInWithEmailAndPassword(cleanEmail, password),
                    15,
                    TimeUnit.SECONDS
                )
                val user = authResult.user
                    ?: return@withContext Result.failure(IllegalStateException("Authentication failed"))

                _userSettings.value = _userSettings.value.copy(
                    isDriveBackupConnected = true,
                    connectedEmail = user.email ?: cleanEmail,
                    userId = user.uid
                )
                persistLocalData()

                val syncRes = syncWithCloud()
                syncRes.fold(
                    onSuccess = { msg -> Result.success(msg) },
                    onFailure = {
                        val fallbackMsg = if (_userSettings.value.language == SupportedLanguage.FR) {
                            "Connecté au compte Cloud (${user.email ?: cleanEmail})"
                        } else {
                            "Connected to Cloud account (${user.email ?: cleanEmail})"
                        }
                        Result.success(fallbackMsg)
                    }
                )
            } catch (t: Throwable) {
                if (isOperationNotAllowedError(t)) {
                    return@withContext Result.failure(GoogleOAuthFallbackRequiredException(cleanEmail))
                }
                Result.failure(formatFriendlyAuthError(t))
            }
        }

    suspend fun signUpWithEmailPassword(
        email: String,
        password: String
    ): Result<String> =
        withContext(Dispatchers.IO) {
            val cleanEmail = email.trim()
            try {
                if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
                    return@withContext Result.failure(
                        IllegalArgumentException(
                            if (_userSettings.value.language == SupportedLanguage.FR)
                                "Veuillez saisir une adresse e-mail valide."
                            else
                                "Please enter a valid email address."
                        )
                    )
                }
                if (password.length < 6) {
                    return@withContext Result.failure(
                        IllegalArgumentException(
                            if (_userSettings.value.language == SupportedLanguage.FR)
                                "Le mot de passe doit contenir au moins 6 caractères."
                            else
                                "Password must be at least 6 characters."
                        )
                    )
                }

                val auth = FirebaseAuth.getInstance()
                val authResult = Tasks.await(
                    auth.createUserWithEmailAndPassword(cleanEmail, password),
                    15,
                    TimeUnit.SECONDS
                )
                val user = authResult.user
                    ?: return@withContext Result.failure(IllegalStateException("Account creation failed"))

                _userSettings.value = _userSettings.value.copy(
                    isDriveBackupConnected = true,
                    connectedEmail = user.email ?: cleanEmail,
                    userId = user.uid
                )
                persistLocalData()

                val syncRes = syncWithCloud()
                syncRes.fold(
                    onSuccess = { msg -> Result.success(msg) },
                    onFailure = {
                        val fallbackMsg = if (_userSettings.value.language == SupportedLanguage.FR) {
                            "Compte Cloud créé et connecté (${user.email ?: cleanEmail})"
                        } else {
                            "Cloud account created and signed in (${user.email ?: cleanEmail})"
                        }
                        Result.success(fallbackMsg)
                    }
                )
            } catch (t: Throwable) {
                if (isOperationNotAllowedError(t)) {
                    return@withContext Result.failure(GoogleOAuthFallbackRequiredException(cleanEmail))
                }
                Result.failure(formatFriendlyAuthError(t))
            }
        }

    private fun getFirestoreSafely(): FirebaseFirestore {
        return runCatching {
            FirebaseFirestore.getInstance(FirebaseApp.getInstance(), FirebaseModule.FIRESTORE_DATABASE_ID)
        }.getOrElse {
            FirebaseFirestore.getInstance()
        }
    }

    private fun sanitizeDocId(rawId: String, prefix: String): String {
        val cleaned = rawId.replace(Regex("[^a-zA-Z0-9_\\-]"), "-").take(120)
        return cleaned.ifBlank { "$prefix-${System.currentTimeMillis()}" }
    }

    private fun formatIsoDate(epochMillis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(epochMillis))
    }

    private fun parseIsoDate(raw: Any?): Long {
        if (raw is Number) return raw.toLong()
        val str = raw as? String ?: return System.currentTimeMillis()
        runCatching {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(str)?.time?.let { return it }
        }
        runCatching {
            val sdfShort = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdfShort.parse(str.take(10))?.time?.let { return it }
        }
        return System.currentTimeMillis()
    }

    private fun serviceTypeKeyToCloud(type: ServiceType): String = when (type) {
        ServiceType.OIL_CHANGE -> "oilChange"
        ServiceType.TIRE_ROTATION -> "tireRotation"
        ServiceType.BRAKES -> "brakes"
        ServiceType.FILTERS -> "filters"
        ServiceType.BATTERY -> "battery"
        ServiceType.TRANSMISSION -> "transmission"
        ServiceType.COOLANT -> "coolant"
        ServiceType.SPARK_PLUGS -> "sparkPlugs"
        ServiceType.TIMING_BELT -> "timing"
        ServiceType.INSPECTION -> "inspection"
        ServiceType.INSURANCE -> "insurance"
        ServiceType.OTHER -> "other"
    }

    private fun serviceTypeKeyFromCloud(raw: Any?): ServiceType? {
        val key = (raw as? String)?.trim()?.lowercase(Locale.US)
            ?.replace("_", "")?.replace("-", "")?.replace(" ", "") ?: return null
        return when (key) {
            "oilchange" -> ServiceType.OIL_CHANGE
            "tirerotation" -> ServiceType.TIRE_ROTATION
            "brakes" -> ServiceType.BRAKES
            "filters" -> ServiceType.FILTERS
            "battery" -> ServiceType.BATTERY
            "transmission" -> ServiceType.TRANSMISSION
            "coolant" -> ServiceType.COOLANT
            "sparkplugs" -> ServiceType.SPARK_PLUGS
            "timing", "timingbelt" -> ServiceType.TIMING_BELT
            "inspection" -> ServiceType.INSPECTION
            "insurance" -> ServiceType.INSURANCE
            "other" -> ServiceType.OTHER
            else -> null
        }
    }

    private fun serviceTypeFromCloud(raw: Any?): ServiceType =
        serviceTypeKeyFromCloud(raw) ?: ServiceType.OTHER

    private fun serviceTypeAndCustomFromCloud(raw: Any?): Pair<ServiceType, String> {
        val known = serviceTypeKeyFromCloud(raw)
        if (known != null) return known to ""
        val custom = (raw as? String)?.trim().orEmpty().take(60)
        return ServiceType.OTHER to custom
    }

    private fun serviceTypeToCloud(service: ServiceRecord): String =
        service.customType.trim().ifBlank { serviceTypeKeyToCloud(service.type) }

    private fun reminderTypeToCloud(reminder: Reminder): String =
        reminder.customType.trim().ifBlank { serviceTypeKeyToCloud(reminder.type) }

    private fun expenseCategoryKeyToCloud(category: ExpenseCategory): String = when (category) {
        ExpenseCategory.INSURANCE -> "insurance"
        ExpenseCategory.PARKING -> "parking"
        ExpenseCategory.TOLLS -> "tolls"
        ExpenseCategory.FINES -> "fines"
        ExpenseCategory.CAR_WASH -> "carWash"
        ExpenseCategory.TAXES -> "taxes"
        ExpenseCategory.REGISTRATION -> "registration"
        ExpenseCategory.FINANCING -> "financing"
        ExpenseCategory.REPAIRS -> "repairs"
        ExpenseCategory.MAINTENANCE -> "maintenance"
        ExpenseCategory.OTHER -> "other"
    }

    private fun expenseCategoryFromCloud(raw: Any?): Pair<ExpenseCategory, String> {
        val str = (raw as? String)?.trim().orEmpty()
        if (str.isBlank()) return ExpenseCategory.OTHER to ""
        val key = str.lowercase(Locale.US)
            .replace("_", "").replace("-", "").replace(" ", "")
        val known = when (key) {
            "insurance" -> ExpenseCategory.INSURANCE
            "parking" -> ExpenseCategory.PARKING
            "tolls", "toll" -> ExpenseCategory.TOLLS
            "fines", "fine" -> ExpenseCategory.FINES
            "carwash" -> ExpenseCategory.CAR_WASH
            "taxes", "tax" -> ExpenseCategory.TAXES
            "registration" -> ExpenseCategory.REGISTRATION
            "financing", "loan" -> ExpenseCategory.FINANCING
            "repairs", "repair" -> ExpenseCategory.REPAIRS
            "maintenance" -> ExpenseCategory.MAINTENANCE
            "other" -> ExpenseCategory.OTHER
            else -> null
        }
        return if (known != null) known to "" else ExpenseCategory.OTHER to str.take(60)
    }

    private fun expenseCategoryToCloud(expense: Expense): String =
        expense.customCategory.trim().ifBlank {
            expenseCategoryKeyToCloud(expense.category)
        }

    private fun parseIsoDateOrNull(raw: Any?): Long? {
        if (raw == null) return null
        if (raw is Number) return raw.toLong()
        val str = raw as? String ?: return null
        if (str.isBlank()) return null
        runCatching {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(str)?.time?.let { return it }
        }
        runCatching {
            val sdfShort = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdfShort.parse(str.take(10))?.time?.let { return it }
        }
        return null
    }

    private fun parseTimestampMillis(raw: Any?): Long = when (raw) {
        null -> 0L
        is com.google.firebase.Timestamp -> raw.seconds * 1000L + raw.nanoseconds / 1_000_000L
        is Number -> raw.toLong()
        is String -> parseIsoDateOrNull(raw) ?: 0L
        is Map<*, *> -> {
            val seconds = (raw["seconds"] as? Number)?.toLong() ?: 0L
            val nanos = (raw["nanoseconds"] as? Number)?.toLong() ?: 0L
            seconds * 1000L + nanos / 1_000_000L
        }
        else -> 0L
    }

    private fun idPrefixFor(collection: String): String = when (collection) {
        "vehicles" -> "veh"
        "fillups" -> "f"
        "expenses" -> "e"
        "services" -> "s"
        "reminders" -> "r"
        else -> "x"
    }

    private fun pendingDeletesKey(collection: String): String =
        if (collection == "reminders") KEY_PENDING_REMINDER_DELETES
        else "pending_deletes_$collection"

    private fun markPendingDelete(collection: String, rawId: String) {
        val key = pendingDeletesKey(collection)
        val set = prefs.getStringSet(key, emptySet()).orEmpty().toMutableSet()
        set.add(sanitizeDocId(rawId, idPrefixFor(collection)))
        prefs.edit().putStringSet(key, set).apply()
    }

    private fun readPendingDeletes(collection: String): MutableSet<String> =
        prefs.getStringSet(pendingDeletesKey(collection), emptySet()).orEmpty().toMutableSet()

    private fun writePendingDeletes(collection: String, ids: Set<String>) {
        prefs.edit().putStringSet(pendingDeletesKey(collection), ids).apply()
    }

    private fun syncedIdsKey(collection: String): String = "synced_ids_$collection"

    private fun readSyncedIds(collection: String): Set<String>? {
        val key = syncedIdsKey(collection)
        return if (prefs.contains(key)) {
            prefs.getStringSet(key, emptySet()).orEmpty()
        } else {
            null
        }
    }

    private fun writeSyncedIds(collection: String, ids: Set<String>) {
        prefs.edit().putStringSet(syncedIdsKey(collection), ids).apply()
    }

    private fun localMutationTimestamp(): Long {
        val lastSync = prefs.getLong("last_sync_ts", 0L)
        return maxOf(System.currentTimeMillis(), lastSync + 1L)
    }

    private data class RemoteEntry<T>(val model: T, val updatedAt: Long)

    private data class MergeOutcome<T>(
        val items: List<T>,
        val hadFailure: Boolean,
        val pushedIds: Set<String>,
        val ids: Set<String>
    )

    private suspend fun <T> mergeCollection(
        collectionKey: String,
        remoteEntries: Map<String, RemoteEntry<T>>,
        localItems: List<T>,
        idOf: (T) -> String,
        updatedAtOf: (T) -> Long,
        copyWithId: (T, String) -> T,
        withUpdatedAt: (T, Long) -> T,
        forceLocalUpdate: (String) -> Boolean = { false },
        defaultIds: Set<String> = emptySet(),
        createRemote: suspend (String, T) -> Boolean,
        updateRemote: suspend (String, T) -> Boolean
    ): MergeOutcome<T> {
        val pendingDeletes = readPendingDeletes(collectionKey)
        val previousSyncedIds = readSyncedIds(collectionKey)
        val lastSyncAt = prefs.getLong("last_sync_ts", 0L)
        val mutationTs = localMutationTimestamp()
        val prefix = idPrefixFor(collectionKey)

        val merged = linkedMapOf<String, T>()
        for ((id, entry) in remoteEntries) {
            if (id !in pendingDeletes) {
                merged[id] = copyWithId(entry.model, id)
            }
        }

        val pushedIds = mutableSetOf<String>()
        var hadFailure = false

        for (local in localItems) {
            val docId = sanitizeDocId(idOf(local), prefix)
            if (docId in pendingDeletes) continue

            val localUpdatedAt = updatedAtOf(local)
            val remote = remoteEntries[docId]

            if (remote != null) {
                val localChanged = localUpdatedAt > 0L && localUpdatedAt > lastSyncAt
                val remoteChanged = remote.updatedAt > 0L && remote.updatedAt > lastSyncAt
                val localWins = forceLocalUpdate(docId) || when {
                    lastSyncAt <= 0L -> localUpdatedAt > 0L &&
                        (remote.updatedAt <= 0L || localUpdatedAt > remote.updatedAt)
                    localChanged && !remoteChanged -> true
                    !localChanged && remoteChanged -> false
                    localChanged && remoteChanged -> localUpdatedAt >= remote.updatedAt
                    else -> false
                }
                if (localWins) {
                    val ok = runCatching { updateRemote(docId, local) }.getOrDefault(false)
                    if (ok) {
                        pushedIds.add(docId)
                        merged[docId] = withUpdatedAt(copyWithId(local, docId), mutationTs)
                    } else {
                        hadFailure = true
                        merged[docId] = withUpdatedAt(copyWithId(local, docId), mutationTs)
                    }
                } else {
                    merged[docId] = remote.model
                }
            } else {
                val shouldPush = when {
                    forceLocalUpdate(docId) -> true
                    previousSyncedIds == null ->
                        remoteEntries.isEmpty() || localUpdatedAt > 0L || docId !in defaultIds
                    else -> docId !in previousSyncedIds || localUpdatedAt > lastSyncAt
                }
                if (shouldPush) {
                    val ok = runCatching { createRemote(docId, local) }.getOrDefault(false)
                    if (ok) {
                        pushedIds.add(docId)
                        merged[docId] = withUpdatedAt(copyWithId(local, docId), mutationTs)
                    } else {
                        hadFailure = true
                        merged[docId] = withUpdatedAt(copyWithId(local, docId), mutationTs)
                    }
                }
            }
        }

        return MergeOutcome(
            items = merged.values.toList(),
            hadFailure = hadFailure,
            pushedIds = pushedIds,
            ids = merged.keys.toSet()
        )
    }

    private suspend fun processPendingDeletes(
        collection: String,
        colRef: com.google.firebase.firestore.CollectionReference
    ): MutableSet<String> {
        val pending = readPendingDeletes(collection)
        for (docId in pending.toList()) {
            val deleted = runCatching {
                Tasks.await(colRef.document(docId).delete(), 15, TimeUnit.SECONDS)
                true
            }.getOrDefault(false)
            if (deleted) pending.remove(docId)
        }
        writePendingDeletes(collection, pending)
        return pending
    }

    private fun buildVehicleCreateMap(
        uid: String,
        docId: String,
        vehicle: Vehicle
    ): HashMap<String, Any> {
        val map = hashMapOf<String, Any>(
            "id" to docId,
            "userId" to uid,
            "make" to vehicle.make.ifBlank { "Vehicle" }.take(100),
            "model" to vehicle.model.ifBlank { "Model" }.take(100),
            "year" to vehicle.year.coerceIn(1900, 2100),
            "fuelType" to vehicle.fuelType.name.lowercase(Locale.US),
            "currentOdometer" to vehicle.currentOdometer.coerceAtLeast(0),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (vehicle.name.isNotBlank()) map["name"] = vehicle.name.take(100)
        if (vehicle.plate.isNotBlank()) map["plate"] = vehicle.plate.take(30)
        if (vehicle.vin.isNotBlank()) map["vin"] = vehicle.vin.take(30)
        if (vehicle.photoUrl.isNotBlank()) map["photoUrl"] = vehicle.photoUrl.take(2048)
        if (vehicle.notes.isNotBlank()) map["notes"] = vehicle.notes.take(5000)
        return map
    }

    private fun buildVehicleUpdateMap(vehicle: Vehicle): HashMap<String, Any> {
        val map = hashMapOf<String, Any>(
            "make" to vehicle.make.ifBlank { "Vehicle" }.take(100),
            "model" to vehicle.model.ifBlank { "Model" }.take(100),
            "year" to vehicle.year.coerceIn(1900, 2100),
            "fuelType" to vehicle.fuelType.name.lowercase(Locale.US),
            "currentOdometer" to vehicle.currentOdometer.coerceAtLeast(0),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        map["name"] = vehicle.name.takeIf { it.isNotBlank() }?.take(100) ?: FieldValue.delete()
        map["plate"] = vehicle.plate.takeIf { it.isNotBlank() }?.take(30) ?: FieldValue.delete()
        map["vin"] = vehicle.vin.takeIf { it.isNotBlank() }?.take(30) ?: FieldValue.delete()
        map["photoUrl"] = vehicle.photoUrl.takeIf { it.isNotBlank() }?.take(2048) ?: FieldValue.delete()
        map["notes"] = vehicle.notes.takeIf { it.isNotBlank() }?.take(5000) ?: FieldValue.delete()
        return map
    }

    private fun buildFillUpCreateMap(
        uid: String,
        docId: String,
        fillUp: FillUp,
        vehicleId: String
    ): HashMap<String, Any> {
        val map = hashMapOf<String, Any>(
            "id" to docId,
            "userId" to uid,
            "vehicleId" to vehicleId,
            "date" to formatIsoDate(fillUp.date),
            "odometer" to fillUp.odometer.coerceAtLeast(0),
            "volume" to fillUp.volume.coerceAtLeast(0.0),
            "pricePerUnit" to fillUp.pricePerUnit.coerceAtLeast(0.0),
            "totalCost" to fillUp.totalCost.coerceAtLeast(0.0),
            "fuelType" to fillUp.fuelType.name.lowercase(Locale.US),
            "isFullTank" to fillUp.isFullTank,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (fillUp.station.isNotBlank()) map["station"] = fillUp.station.take(200)
        if (fillUp.notes.isNotBlank()) map["notes"] = fillUp.notes.take(5000)
        return map
    }

    private fun buildFillUpUpdateMap(fillUp: FillUp, vehicleId: String): HashMap<String, Any> {
        val map = hashMapOf<String, Any>(
            "vehicleId" to vehicleId,
            "date" to formatIsoDate(fillUp.date),
            "odometer" to fillUp.odometer.coerceAtLeast(0),
            "volume" to fillUp.volume.coerceAtLeast(0.0),
            "pricePerUnit" to fillUp.pricePerUnit.coerceAtLeast(0.0),
            "totalCost" to fillUp.totalCost.coerceAtLeast(0.0),
            "fuelType" to fillUp.fuelType.name.lowercase(Locale.US),
            "isFullTank" to fillUp.isFullTank,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        map["station"] = fillUp.station.takeIf { it.isNotBlank() }?.take(200) ?: FieldValue.delete()
        map["notes"] = fillUp.notes.takeIf { it.isNotBlank() }?.take(5000) ?: FieldValue.delete()
        return map
    }

    private fun buildExpenseCreateMap(
        uid: String,
        docId: String,
        expense: Expense,
        vehicleId: String
    ): HashMap<String, Any> {
        val map = hashMapOf<String, Any>(
            "id" to docId,
            "userId" to uid,
            "vehicleId" to vehicleId,
            "date" to formatIsoDate(expense.date),
            "category" to expenseCategoryToCloud(expense),
            "amount" to expense.amount.coerceAtLeast(0.0),
            "description" to expense.description.ifBlank { expense.categoryLabel(false) }
                .ifBlank { "Expense" }.take(500),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        expense.odometer?.let { map["odometer"] = it.coerceAtLeast(0) }
        if (expense.notes.isNotBlank()) map["notes"] = expense.notes.take(5000)
        return map
    }

    private fun buildExpenseUpdateMap(expense: Expense, vehicleId: String): HashMap<String, Any> {
        val map = hashMapOf<String, Any>(
            "vehicleId" to vehicleId,
            "date" to formatIsoDate(expense.date),
            "category" to expenseCategoryToCloud(expense),
            "amount" to expense.amount.coerceAtLeast(0.0),
            "description" to expense.description.ifBlank { expense.categoryLabel(false) }
                .ifBlank { "Expense" }.take(500),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        map["odometer"] = expense.odometer?.coerceAtLeast(0) ?: FieldValue.delete()
        map["notes"] = expense.notes.takeIf { it.isNotBlank() }?.take(5000) ?: FieldValue.delete()
        return map
    }

    private fun buildServiceCreateMap(
        uid: String,
        docId: String,
        service: ServiceRecord,
        vehicleId: String
    ): HashMap<String, Any> {
        val map = hashMapOf<String, Any>(
            "id" to docId,
            "userId" to uid,
            "vehicleId" to vehicleId,
            "date" to formatIsoDate(service.date),
            "type" to serviceTypeToCloud(service),
            "odometer" to service.odometer.coerceAtLeast(0),
            "cost" to service.cost.coerceAtLeast(0.0),
            "description" to service.description.ifBlank { service.typeLabel(false) }
                .ifBlank { "Service" }.take(500),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (service.parts.isNotEmpty()) {
            map["parts"] = service.parts.take(50).map { it.take(200) }
        }
        if (service.receiptUrl.isNotBlank()) map["receiptUrl"] = service.receiptUrl.take(2048)
        if (service.notes.isNotBlank()) map["notes"] = service.notes.take(5000)
        return map
    }

    private fun buildServiceUpdateMap(service: ServiceRecord, vehicleId: String): HashMap<String, Any> {
        val map = hashMapOf<String, Any>(
            "vehicleId" to vehicleId,
            "date" to formatIsoDate(service.date),
            "type" to serviceTypeToCloud(service),
            "odometer" to service.odometer.coerceAtLeast(0),
            "cost" to service.cost.coerceAtLeast(0.0),
            "description" to service.description.ifBlank { service.typeLabel(false) }
                .ifBlank { "Service" }.take(500),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        map["parts"] = if (service.parts.isNotEmpty()) {
            service.parts.take(50).map { it.take(200) }
        } else {
            FieldValue.delete()
        }
        map["receiptUrl"] = service.receiptUrl.takeIf { it.isNotBlank() }?.take(2048) ?: FieldValue.delete()
        map["notes"] = service.notes.takeIf { it.isNotBlank() }?.take(5000) ?: FieldValue.delete()
        return map
    }

    private fun buildReminderCreateMap(
        uid: String,
        docId: String,
        reminder: Reminder,
        vehicleId: String
    ): HashMap<String, Any> {
        val map = hashMapOf<String, Any>(
            "id" to docId,
            "userId" to uid,
            "vehicleId" to vehicleId,
            "type" to reminderTypeToCloud(reminder),
            "isRecurring" to reminder.isRecurring,
            "isActive" to !reminder.isCompleted,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (reminder.title.isNotBlank()) map["title"] = reminder.title.take(200)
        reminder.intervalKm?.let { map["intervalKm"] = it.coerceAtLeast(0) }
        reminder.intervalDays?.let { map["intervalDays"] = it.coerceAtLeast(0) }
        reminder.lastServiceDate?.let { map["lastServiceDate"] = formatIsoDate(it) }
        reminder.lastServiceOdometer?.let { map["lastServiceOdometer"] = it.coerceAtLeast(0) }
        reminder.nextDueDate?.let { map["nextDueDate"] = formatIsoDate(it) }
        reminder.nextDueOdometer?.let { map["nextDueOdometer"] = it.coerceAtLeast(0) }
        return map
    }

    private fun buildReminderUpdateMap(
        reminder: Reminder,
        vehicleId: String
    ): HashMap<String, Any> {
        val map = hashMapOf<String, Any>(
            "vehicleId" to vehicleId,
            "type" to reminderTypeToCloud(reminder),
            "isRecurring" to reminder.isRecurring,
            "isActive" to !reminder.isCompleted,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        map["title"] = if (reminder.title.isNotBlank()) reminder.title.take(200) else FieldValue.delete()
        map["intervalKm"] = reminder.intervalKm?.coerceAtLeast(0) ?: FieldValue.delete()
        map["intervalDays"] = reminder.intervalDays?.coerceAtLeast(0) ?: FieldValue.delete()
        map["lastServiceDate"] = reminder.lastServiceDate?.let { formatIsoDate(it) } ?: FieldValue.delete()
        map["lastServiceOdometer"] = reminder.lastServiceOdometer?.coerceAtLeast(0) ?: FieldValue.delete()
        map["nextDueDate"] = reminder.nextDueDate?.let { formatIsoDate(it) } ?: FieldValue.delete()
        map["nextDueOdometer"] = reminder.nextDueOdometer?.coerceAtLeast(0) ?: FieldValue.delete()
        return map
    }

    suspend fun syncWithCloud(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fbUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
            if (fbUser == null || fbUser.isAnonymous) {
                _userSettings.value = _userSettings.value.copy(
                    isDriveBackupConnected = false,
                    connectedEmail = null,
                    userId = null
                )
                return@withContext Result.failure(
                    IllegalStateException(
                        if (_userSettings.value.language == SupportedLanguage.FR)
                            "Non connecté : veuillez vous connecter avec Google ou Email avant de synchroniser."
                        else
                            "Not signed in: please sign in with Google or Email before syncing."
                    )
                )
            }

            val uid = fbUser.uid
            val email = (fbUser.email?.takeIf { it.isNotBlank() } ?: _userSettings.value.connectedEmail ?: "driver@netcar.app").take(320)
            val displayName = (fbUser.displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")).ifBlank { "Pilot" }.take(200)
            val db = getFirestoreSafely()

            // 1. Ensure parent user profile document exists in /users/{uid}
            val userDocRef = db.collection("users").document(uid)
            val userSnap = Tasks.await(userDocRef.get(), 15, TimeUnit.SECONDS)
            if (!userSnap.exists()) {
                val userDoc = hashMapOf<String, Any>(
                    "userId" to uid,
                    "name" to displayName,
                    "email" to email,
                    "preferredLang" to _userSettings.value.language.code,
                    "units" to if (_userSettings.value.units == Units.METRIC) "metric" else "imperial",
                    "currency" to _userSettings.value.currency.code,
                    "themeMode" to _userSettings.value.themeMode.code,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                _selectedVehicleId.value?.let { userDoc["activeVehicleId"] = sanitizeDocId(it, "veh") }
                Tasks.await(userDocRef.set(userDoc), 15, TimeUnit.SECONDS)
            }

            // 1b. Best-effort merge of user-defined categories with the profile
            // document. Older Firestore rules may reject the new fields, so a
            // failure here must never abort the rest of the sync.
            val profileData: Map<String, Any> =
                if (userSnap.exists()) userSnap.data.orEmpty() else emptyMap()
            val remoteExpenseCategories = (profileData["customExpenseCategories"] as? List<*>)
                ?.mapNotNull { it as? String }.orEmpty()
            val remoteServiceTypes = (profileData["customServiceTypes"] as? List<*>)
                ?.mapNotNull { it as? String }.orEmpty()
            val mergedExpenseCategories = mergeCustomCategoryLists(
                _userSettings.value.customExpenseCategories,
                remoteExpenseCategories
            )
            val mergedServiceTypes = mergeCustomCategoryLists(
                _userSettings.value.customServiceTypes,
                remoteServiceTypes
            )
            if (mergedExpenseCategories != _userSettings.value.customExpenseCategories ||
                mergedServiceTypes != _userSettings.value.customServiceTypes
            ) {
                _userSettings.value = _userSettings.value.copy(
                    customExpenseCategories = mergedExpenseCategories,
                    customServiceTypes = mergedServiceTypes
                )
                persistLocalData()
            }
            if (remoteExpenseCategories != mergedExpenseCategories ||
                remoteServiceTypes != mergedServiceTypes
            ) {
                runCatching {
                    Tasks.await(
                        userDocRef.update(
                            mapOf(
                                "customExpenseCategories" to mergedExpenseCategories,
                                "customServiceTypes" to mergedServiceTypes,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ),
                        15,
                        TimeUnit.SECONDS
                    )
                }
            }

            // 1c. Best-effort sync of the theme preference with the profile.
            val remoteThemeMode = (profileData["themeMode"] as? String)?.lowercase(Locale.US)
            if (!remoteThemeMode.isNullOrBlank() && remoteThemeMode != _userSettings.value.themeMode.code) {
                val parsedTheme = ThemeMode.values().firstOrNull { it.code == remoteThemeMode }
                if (parsedTheme != null) {
                    _userSettings.value = _userSettings.value.copy(themeMode = parsedTheme)
                    persistLocalData()
                }
            }
            runCatching {
                Tasks.await(
                    userDocRef.update(
                        mapOf(
                            "themeMode" to _userSettings.value.themeMode.code,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ),
                    15,
                    TimeUnit.SECONDS
                )
            }

            // 2. Sync Vehicles (/users/{uid}/vehicles)
            val vehiclesCol = userDocRef.collection("vehicles")
            val remoteVehSnap = Tasks.await(
                vehiclesCol.whereEqualTo("userId", uid).get(),
                15,
                TimeUnit.SECONDS
            )
            val remoteVehicles = linkedMapOf<String, RemoteEntry<Vehicle>>()

            for (doc in remoteVehSnap.documents) {
                val data = doc.data ?: continue
                val id = (data["id"] as? String) ?: doc.id
                val ftStr = (data["fuelType"] as? String)?.uppercase(Locale.US) ?: "GASOLINE"
                val ft = runCatching { FuelType.valueOf(ftStr) }.getOrDefault(FuelType.GASOLINE)
                val updatedAt = parseTimestampMillis(data["updatedAt"])
                remoteVehicles[id] = RemoteEntry(
                    model = Vehicle(
                        id = id,
                        name = (data["name"] as? String) ?: "",
                        make = (data["make"] as? String) ?: "Vehicle",
                        model = (data["model"] as? String) ?: "Model",
                        year = (data["year"] as? Number)?.toInt() ?: 2020,
                        plate = (data["plate"] as? String) ?: "",
                        vin = (data["vin"] as? String) ?: "",
                        fuelType = ft,
                        photoUrl = (data["photoUrl"] as? String) ?: "",
                        currentOdometer = (data["currentOdometer"] as? Number)?.toInt() ?: 0,
                        notes = (data["notes"] as? String) ?: "",
                        updatedAt = updatedAt
                    ),
                    updatedAt = updatedAt
                )
            }

            val pendingVehDeletes = processPendingDeletes("vehicles", vehiclesCol)
            remoteVehicles.keys.removeAll(pendingVehDeletes)

            val vehicleOutcome = mergeCollection(
                collectionKey = "vehicles",
                remoteEntries = remoteVehicles,
                localItems = _vehicles.value,
                idOf = { it.id },
                updatedAtOf = { it.updatedAt },
                copyWithId = { v, id -> v.copy(id = id) },
                withUpdatedAt = { v, ts -> v.copy(updatedAt = ts) },
                defaultIds = setOf("veh-1", "veh-2"),
                createRemote = { docId, vehicle ->
                    Tasks.await(
                        vehiclesCol.document(docId)
                            .set(buildVehicleCreateMap(uid, docId, vehicle)),
                        15,
                        TimeUnit.SECONDS
                    )
                    true
                },
                updateRemote = { docId, vehicle ->
                    Tasks.await(
                        vehiclesCol.document(docId).update(buildVehicleUpdateMap(vehicle)),
                        15,
                        TimeUnit.SECONDS
                    )
                    true
                }
            )
            var hadPushFailure = vehicleOutcome.hadFailure
            _vehicles.value = vehicleOutcome.items
            if (_selectedVehicleId.value == null ||
                vehicleOutcome.items.none { it.id == _selectedVehicleId.value }
            ) {
                _selectedVehicleId.value = vehicleOutcome.items.firstOrNull()?.id
            }
            writeSyncedIds("vehicles", vehicleOutcome.ids)
            val remoteVehicleIds = vehicleOutcome.ids.toMutableSet()
            val defaultVehicleId = remoteVehicleIds.firstOrNull() ?: "veh-1"

            // 3. Sync Fill-Ups (/users/{uid}/fillups)
            val fillupsCol = userDocRef.collection("fillups")
            val remoteFillSnap = Tasks.await(
                fillupsCol.whereEqualTo("userId", uid).get(),
                15,
                TimeUnit.SECONDS
            )
            val remoteFillUps = linkedMapOf<String, RemoteEntry<FillUp>>()

            for (doc in remoteFillSnap.documents) {
                val data = doc.data ?: continue
                val id = (data["id"] as? String) ?: doc.id
                val ftStr = (data["fuelType"] as? String)?.uppercase(Locale.US) ?: "GASOLINE"
                val ft = runCatching { FuelType.valueOf(ftStr) }.getOrDefault(FuelType.GASOLINE)
                val updatedAt = parseTimestampMillis(data["updatedAt"])
                remoteFillUps[id] = RemoteEntry(
                    model = FillUp(
                        id = id,
                        vehicleId = (data["vehicleId"] as? String) ?: defaultVehicleId,
                        date = parseIsoDate(data["date"]),
                        odometer = (data["odometer"] as? Number)?.toInt() ?: 0,
                        volume = (data["volume"] as? Number)?.toDouble() ?: 0.0,
                        pricePerUnit = (data["pricePerUnit"] as? Number)?.toDouble() ?: 0.0,
                        totalCost = (data["totalCost"] as? Number)?.toDouble() ?: 0.0,
                        fuelType = ft,
                        isFullTank = (data["isFullTank"] as? Boolean) ?: true,
                        station = (data["station"] as? String) ?: "",
                        notes = (data["notes"] as? String) ?: "",
                        updatedAt = updatedAt
                    ),
                    updatedAt = updatedAt
                )
            }

            val pendingFillDeletes = processPendingDeletes("fillups", fillupsCol)
            remoteFillUps.keys.removeAll(pendingFillDeletes)

            val fillOutcome = mergeCollection(
                collectionKey = "fillups",
                remoteEntries = remoteFillUps,
                localItems = _fillUps.value,
                idOf = { it.id },
                updatedAtOf = { it.updatedAt },
                copyWithId = { f, id ->
                    f.copy(
                        id = id,
                        vehicleId = if (f.vehicleId in remoteVehicleIds) f.vehicleId else defaultVehicleId
                    )
                },
                withUpdatedAt = { f, ts -> f.copy(updatedAt = ts) },
                defaultIds = setOf("f-1", "f-2", "f-3", "f-4", "f-5", "f-6"),
                createRemote = { docId, fillUp ->
                    val targetVehId =
                        if (fillUp.vehicleId in remoteVehicleIds) fillUp.vehicleId else defaultVehicleId
                    if (targetVehId !in remoteVehicleIds || fillUp.volume < 0) {
                        false
                    } else {
                        Tasks.await(
                            fillupsCol.document(docId)
                                .set(buildFillUpCreateMap(uid, docId, fillUp, targetVehId)),
                            15,
                            TimeUnit.SECONDS
                        )
                        true
                    }
                },
                updateRemote = { docId, fillUp ->
                    val targetVehId =
                        if (fillUp.vehicleId in remoteVehicleIds) fillUp.vehicleId else defaultVehicleId
                    if (targetVehId !in remoteVehicleIds) {
                        false
                    } else {
                        Tasks.await(
                            fillupsCol.document(docId)
                                .update(buildFillUpUpdateMap(fillUp, targetVehId)),
                            15,
                            TimeUnit.SECONDS
                        )
                        true
                    }
                }
            )
            hadPushFailure = hadPushFailure || fillOutcome.hadFailure
            _fillUps.value = fillOutcome.items.sortedByDescending { it.odometer }
            writeSyncedIds("fillups", fillOutcome.ids)

            // 4. Sync Expenses (/users/{uid}/expenses)
            val expensesCol = userDocRef.collection("expenses")
            val remoteExpSnap = Tasks.await(
                expensesCol.whereEqualTo("userId", uid).get(),
                15,
                TimeUnit.SECONDS
            )
            val remoteExpenses = linkedMapOf<String, RemoteEntry<Expense>>()

            for (doc in remoteExpSnap.documents) {
                val data = doc.data ?: continue
                val id = (data["id"] as? String) ?: doc.id
                val (cat, customCat) = expenseCategoryFromCloud(data["category"])
                val updatedAt = parseTimestampMillis(data["updatedAt"])
                remoteExpenses[id] = RemoteEntry(
                    model = Expense(
                        id = id,
                        vehicleId = (data["vehicleId"] as? String) ?: defaultVehicleId,
                        date = parseIsoDate(data["date"]),
                        category = cat,
                        customCategory = customCat,
                        amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                        description = (data["description"] as? String) ?: "",
                        odometer = (data["odometer"] as? Number)?.toInt(),
                        notes = (data["notes"] as? String) ?: "",
                        updatedAt = updatedAt
                    ),
                    updatedAt = updatedAt
                )
            }

            val pendingExpDeletes = processPendingDeletes("expenses", expensesCol)
            remoteExpenses.keys.removeAll(pendingExpDeletes)

            val expenseOutcome = mergeCollection(
                collectionKey = "expenses",
                remoteEntries = remoteExpenses,
                localItems = _expenses.value,
                idOf = { it.id },
                updatedAtOf = { it.updatedAt },
                copyWithId = { e, id ->
                    e.copy(
                        id = id,
                        vehicleId = if (e.vehicleId in remoteVehicleIds) e.vehicleId else defaultVehicleId
                    )
                },
                withUpdatedAt = { e, ts -> e.copy(updatedAt = ts) },
                defaultIds = setOf("e-1", "e-2", "e-3"),
                createRemote = { docId, expense ->
                    val targetVehId =
                        if (expense.vehicleId in remoteVehicleIds) expense.vehicleId else defaultVehicleId
                    if (targetVehId !in remoteVehicleIds || expense.amount < 0) {
                        false
                    } else {
                        Tasks.await(
                            expensesCol.document(docId)
                                .set(buildExpenseCreateMap(uid, docId, expense, targetVehId)),
                            15,
                            TimeUnit.SECONDS
                        )
                        true
                    }
                },
                updateRemote = { docId, expense ->
                    val targetVehId =
                        if (expense.vehicleId in remoteVehicleIds) expense.vehicleId else defaultVehicleId
                    if (targetVehId !in remoteVehicleIds) {
                        false
                    } else {
                        Tasks.await(
                            expensesCol.document(docId)
                                .update(buildExpenseUpdateMap(expense, targetVehId)),
                            15,
                            TimeUnit.SECONDS
                        )
                        true
                    }
                }
            )
            hadPushFailure = hadPushFailure || expenseOutcome.hadFailure
            _expenses.value = expenseOutcome.items.sortedByDescending { it.date }
            writeSyncedIds("expenses", expenseOutcome.ids)

            // 5. Sync Services (/users/{uid}/services)
            val servicesCol = userDocRef.collection("services")
            val remoteSvcSnap = Tasks.await(
                servicesCol.whereEqualTo("userId", uid).get(),
                15,
                TimeUnit.SECONDS
            )
            val remoteServices = linkedMapOf<String, RemoteEntry<ServiceRecord>>()

            for (doc in remoteSvcSnap.documents) {
                val data = doc.data ?: continue
                val id = (data["id"] as? String) ?: doc.id
                val (svcType, customSvcType) = serviceTypeAndCustomFromCloud(data["type"])
                val rawParts = (data["parts"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                val updatedAt = parseTimestampMillis(data["updatedAt"])
                remoteServices[id] = RemoteEntry(
                    model = ServiceRecord(
                        id = id,
                        vehicleId = (data["vehicleId"] as? String) ?: defaultVehicleId,
                        date = parseIsoDate(data["date"]),
                        type = svcType,
                        customType = customSvcType,
                        odometer = (data["odometer"] as? Number)?.toInt() ?: 0,
                        cost = (data["cost"] as? Number)?.toDouble() ?: 0.0,
                        description = (data["description"] as? String) ?: "",
                        parts = rawParts,
                        receiptUrl = (data["receiptUrl"] as? String) ?: "",
                        notes = (data["notes"] as? String) ?: "",
                        updatedAt = updatedAt
                    ),
                    updatedAt = updatedAt
                )
            }

            val pendingSvcDeletes = processPendingDeletes("services", servicesCol)
            remoteServices.keys.removeAll(pendingSvcDeletes)

            val serviceOutcome = mergeCollection(
                collectionKey = "services",
                remoteEntries = remoteServices,
                localItems = _serviceRecords.value,
                idOf = { it.id },
                updatedAtOf = { it.updatedAt },
                copyWithId = { s, id ->
                    s.copy(
                        id = id,
                        vehicleId = if (s.vehicleId in remoteVehicleIds) s.vehicleId else defaultVehicleId
                    )
                },
                withUpdatedAt = { s, ts -> s.copy(updatedAt = ts) },
                defaultIds = setOf("s-1", "s-2"),
                createRemote = { docId, service ->
                    val targetVehId =
                        if (service.vehicleId in remoteVehicleIds) service.vehicleId else defaultVehicleId
                    if (targetVehId !in remoteVehicleIds) {
                        false
                    } else {
                        Tasks.await(
                            servicesCol.document(docId)
                                .set(buildServiceCreateMap(uid, docId, service, targetVehId)),
                            15,
                            TimeUnit.SECONDS
                        )
                        true
                    }
                },
                updateRemote = { docId, service ->
                    val targetVehId =
                        if (service.vehicleId in remoteVehicleIds) service.vehicleId else defaultVehicleId
                    if (targetVehId !in remoteVehicleIds) {
                        false
                    } else {
                        Tasks.await(
                            servicesCol.document(docId)
                                .update(buildServiceUpdateMap(service, targetVehId)),
                            15,
                            TimeUnit.SECONDS
                        )
                        true
                    }
                }
            )
            hadPushFailure = hadPushFailure || serviceOutcome.hadFailure
            _serviceRecords.value = serviceOutcome.items.sortedByDescending { it.date }
            writeSyncedIds("services", serviceOutcome.ids)

            // 6. Sync Reminders (/users/{uid}/reminders)
            val remindersCol = userDocRef.collection("reminders")
            val remoteRemSnap = Tasks.await(
                remindersCol.whereEqualTo("userId", uid).get(),
                15,
                TimeUnit.SECONDS
            )
            val remoteReminders = linkedMapOf<String, RemoteEntry<Reminder>>()

            for (doc in remoteRemSnap.documents) {
                val data = doc.data ?: continue
                val id = (data["id"] as? String) ?: doc.id
                val (remType, customRemType) = serviceTypeAndCustomFromCloud(data["type"])
                val updatedAt = parseTimestampMillis(data["updatedAt"])
                remoteReminders[id] = RemoteEntry(
                    model = Reminder(
                        id = id,
                        vehicleId = (data["vehicleId"] as? String) ?: defaultVehicleId,
                        title = (data["title"] as? String) ?: customRemType.ifBlank { remType.displayName },
                        type = remType,
                        customType = customRemType,
                        intervalKm = (data["intervalKm"] as? Number)?.toInt(),
                        intervalDays = (data["intervalDays"] as? Number)?.toInt(),
                        lastServiceDate = parseIsoDateOrNull(data["lastServiceDate"]),
                        lastServiceOdometer = (data["lastServiceOdometer"] as? Number)?.toInt(),
                        nextDueDate = parseIsoDateOrNull(data["nextDueDate"]),
                        nextDueOdometer = (data["nextDueOdometer"] as? Number)?.toInt(),
                        isRecurring = (data["isRecurring"] as? Boolean) ?: false,
                        isCompleted = !((data["isActive"] as? Boolean) ?: true),
                        updatedAt = updatedAt
                    ),
                    updatedAt = updatedAt
                )
            }

            val pendingReminderUpdates = prefs
                .getStringSet(KEY_PENDING_REMINDER_UPDATES, emptySet())
                .orEmpty().toMutableSet()
            val pendingReminderDeletes = processPendingDeletes("reminders", remindersCol)
            remoteReminders.keys.removeAll(pendingReminderDeletes)

            val reminderOutcome = mergeCollection(
                collectionKey = "reminders",
                remoteEntries = remoteReminders,
                localItems = _reminders.value,
                idOf = { it.id },
                updatedAtOf = { it.updatedAt },
                copyWithId = { r, id ->
                    r.copy(
                        id = id,
                        vehicleId = if (r.vehicleId in remoteVehicleIds) r.vehicleId else defaultVehicleId
                    )
                },
                withUpdatedAt = { r, ts -> r.copy(updatedAt = ts) },
                defaultIds = setOf("r-1", "r-2", "r-3"),
                forceLocalUpdate = { docId -> docId in pendingReminderUpdates },
                createRemote = { docId, reminder ->
                    val targetVehId =
                        if (reminder.vehicleId in remoteVehicleIds) reminder.vehicleId else defaultVehicleId
                    if (targetVehId !in remoteVehicleIds) {
                        false
                    } else {
                        Tasks.await(
                            remindersCol.document(docId)
                                .set(buildReminderCreateMap(uid, docId, reminder, targetVehId)),
                            15,
                            TimeUnit.SECONDS
                        )
                        true
                    }
                },
                updateRemote = { docId, reminder ->
                    val targetVehId =
                        if (reminder.vehicleId in remoteVehicleIds) reminder.vehicleId else defaultVehicleId
                    if (targetVehId !in remoteVehicleIds) {
                        false
                    } else {
                        Tasks.await(
                            remindersCol.document(docId)
                                .update(buildReminderUpdateMap(reminder, targetVehId)),
                            15,
                            TimeUnit.SECONDS
                        )
                        true
                    }
                }
            )
            hadPushFailure = hadPushFailure || reminderOutcome.hadFailure
            pendingReminderUpdates.removeAll(reminderOutcome.pushedIds)
            pendingReminderUpdates.retainAll(
                reminderOutcome.items.map { sanitizeDocId(it.id, "r") }.toSet()
            )
            prefs.edit()
                .putStringSet(KEY_PENDING_REMINDER_UPDATES, pendingReminderUpdates)
                .apply()
            _reminders.value = reminderOutcome.items
            writeSyncedIds("reminders", reminderOutcome.ids)

            val maxUpdatedAt = sequenceOf(
                _vehicles.value.maxOfOrNull { it.updatedAt } ?: 0L,
                _fillUps.value.maxOfOrNull { it.updatedAt } ?: 0L,
                _expenses.value.maxOfOrNull { it.updatedAt } ?: 0L,
                _serviceRecords.value.maxOfOrNull { it.updatedAt } ?: 0L,
                _reminders.value.maxOfOrNull { it.updatedAt } ?: 0L
            ).maxOrNull() ?: 0L
            val syncTime = maxOf(System.currentTimeMillis(), maxUpdatedAt + 1L)
            if (!hadPushFailure) {
                prefs.edit().putLong("last_sync_ts", syncTime).apply()
            }
            _userSettings.value = _userSettings.value.copy(
                isDriveBackupConnected = true,
                connectedEmail = email,
                userId = uid,
                lastBackupTimestamp = syncTime
            )
            persistLocalData()
            checkRemindersAndNotify(forceAllActive = false)

            val msg = if (_userSettings.value.language == SupportedLanguage.FR) {
                "Synchronisation Cloud réussie (${_vehicles.value.size} véhicules, ${_fillUps.value.size} pleins, ${_reminders.value.size} rappels)"
            } else {
                "Cloud sync complete (${_vehicles.value.size} vehicles, ${_fillUps.value.size} fill-ups, ${_reminders.value.size} reminders)"
            }
            Result.success(msg)
        } catch (t: Throwable) {
            Result.failure(formatFriendlyAuthError(t))
        }
    }

    private companion object {
        const val KEY_PENDING_REMINDER_UPDATES = "pending_reminder_updates"
        const val KEY_PENDING_REMINDER_DELETES = "pending_reminder_deletes"
    }
}
