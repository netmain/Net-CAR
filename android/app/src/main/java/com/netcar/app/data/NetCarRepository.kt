package com.netcar.app.data

import android.app.Activity
import android.content.Context
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
        val lastSync = prefs.getLong("last_sync_ts", -1L).takeIf { it > 0L }

        val lang = SupportedLanguage.values().find { it.code == langCode } ?: SupportedLanguage.FR
        val units = runCatching { Units.valueOf(unitsName) }.getOrDefault(Units.METRIC)
        val currency = runCatching { SupportedCurrency.valueOf(currencyName) }.getOrDefault(SupportedCurrency.DZD)

        return UserSettings(
            language = lang,
            units = units,
            currency = currency,
            isDriveBackupConnected = false,
            connectedEmail = null,
            userId = null,
            lastBackupTimestamp = lastSync
        )
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
                .apply()
        }
    }

    fun selectVehicle(vehicleId: String) {
        _selectedVehicleId.value = vehicleId
        persistLocalData()
    }

    fun addVehicle(vehicle: Vehicle) {
        _vehicles.value = _vehicles.value + vehicle
        if (_selectedVehicleId.value == null) {
            _selectedVehicleId.value = vehicle.id
        }
        persistLocalData()
    }

    fun deleteVehicle(vehicleId: String) {
        _vehicles.value = _vehicles.value.filter { it.id != vehicleId }
        if (_selectedVehicleId.value == vehicleId) {
            _selectedVehicleId.value = _vehicles.value.firstOrNull()?.id
        }
        persistLocalData()
    }

    fun updateVehicleOdometer(vehicleId: String, newOdometer: Int) {
        _vehicles.value = _vehicles.value.map { v ->
            if (v.id == vehicleId && newOdometer > v.currentOdometer) {
                v.copy(currentOdometer = newOdometer)
            } else v
        }
        persistLocalData()
        checkRemindersAndNotify(forceAllActive = false)
    }

    fun addFillUp(fillUp: FillUp) {
        _fillUps.value = listOf(fillUp) + _fillUps.value
        updateVehicleOdometer(fillUp.vehicleId, fillUp.odometer)
        persistLocalData()
    }

    fun deleteFillUp(id: String) {
        _fillUps.value = _fillUps.value.filter { it.id != id }
        persistLocalData()
    }

    fun addExpense(expense: Expense) {
        _expenses.value = listOf(expense) + _expenses.value
        expense.odometer?.let { updateVehicleOdometer(expense.vehicleId, it) }
        persistLocalData()
    }

    fun deleteExpense(id: String) {
        _expenses.value = _expenses.value.filter { it.id != id }
        persistLocalData()
    }

    fun addServiceRecord(service: ServiceRecord) {
        _serviceRecords.value = listOf(service) + _serviceRecords.value
        updateVehicleOdometer(service.vehicleId, service.odometer)
        persistLocalData()
    }

    fun deleteServiceRecord(id: String) {
        _serviceRecords.value = _serviceRecords.value.filter { it.id != id }
        persistLocalData()
    }

    fun addReminder(reminder: Reminder) {
        _reminders.value = listOf(reminder) + _reminders.value
        persistLocalData()
        checkRemindersAndNotify(forceAllActive = false)
    }

    fun updateReminder(reminder: Reminder) {
        _reminders.value = _reminders.value.map {
            if (it.id == reminder.id) reminder else it
        }
        markReminderPendingUpdate(reminder.id)
        persistLocalData()
        checkRemindersAndNotify(forceAllActive = false)
    }

    fun toggleReminderCompleted(id: String) {
        _reminders.value = _reminders.value.map {
            if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it
        }
        markReminderPendingUpdate(id)
        persistLocalData()
        checkRemindersAndNotify(forceAllActive = false)
    }

    fun deleteReminder(id: String) {
        _reminders.value = _reminders.value.filter { it.id != id }
        markReminderPendingDelete(id)
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

    private fun serviceTypeToCloud(type: ServiceType): String = when (type) {
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
        ServiceType.OTHER -> "other"
    }

    private fun serviceTypeFromCloud(raw: Any?): ServiceType {
        val key = (raw as? String)?.trim()?.lowercase(Locale.US)
            ?.replace("_", "")?.replace("-", "") ?: return ServiceType.OTHER
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
            else -> ServiceType.OTHER
        }
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
            "type" to serviceTypeToCloud(reminder.type),
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
            "type" to serviceTypeToCloud(reminder.type),
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
            val userDoc = hashMapOf<String, Any>(
                "userId" to uid,
                "name" to displayName,
                "email" to email,
                "preferredLang" to _userSettings.value.language.code,
                "units" to if (_userSettings.value.units == Units.METRIC) "metric" else "imperial",
                "currency" to _userSettings.value.currency.code,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            _selectedVehicleId.value?.let { userDoc["activeVehicleId"] = sanitizeDocId(it, "veh") }
            Tasks.await(userDocRef.set(userDoc), 15, TimeUnit.SECONDS)

            // 2. Sync Vehicles (/users/{uid}/vehicles)
            val vehiclesCol = userDocRef.collection("vehicles")
            val remoteVehSnap = Tasks.await(
                vehiclesCol.whereEqualTo("userId", uid).get(),
                15,
                TimeUnit.SECONDS
            )
            val remoteVehicles = mutableListOf<Vehicle>()
            val remoteVehicleIds = mutableSetOf<String>()

            for (doc in remoteVehSnap.documents) {
                val data = doc.data ?: continue
                val id = (data["id"] as? String) ?: doc.id
                remoteVehicleIds.add(id)
                val ftStr = (data["fuelType"] as? String)?.uppercase(Locale.US) ?: "GASOLINE"
                val ft = runCatching { FuelType.valueOf(ftStr) }.getOrDefault(FuelType.GASOLINE)
                remoteVehicles.add(
                    Vehicle(
                        id = id,
                        name = (data["name"] as? String) ?: "",
                        make = (data["make"] as? String) ?: "Vehicle",
                        model = (data["model"] as? String) ?: "Model",
                        year = (data["year"] as? Number)?.toInt() ?: 2020,
                        plate = (data["plate"] as? String) ?: "",
                        vin = (data["vin"] as? String) ?: "",
                        fuelType = ft,
                        currentOdometer = (data["currentOdometer"] as? Number)?.toInt() ?: 0,
                        notes = (data["notes"] as? String) ?: ""
                    )
                )
            }

            // Push any local vehicles not yet on remote
            val mergedVehicles = remoteVehicles.toMutableList()
            for (localV in _vehicles.value) {
                val cleanVehId = sanitizeDocId(localV.id, "veh")
                if (cleanVehId !in remoteVehicleIds) {
                    val vMap = hashMapOf<String, Any>(
                        "id" to cleanVehId,
                        "userId" to uid,
                        "make" to localV.make.ifBlank { "Vehicle" }.take(100),
                        "model" to localV.model.ifBlank { "Model" }.take(100),
                        "year" to localV.year.coerceIn(1900, 2100),
                        "fuelType" to localV.fuelType.name.lowercase(Locale.US),
                        "currentOdometer" to localV.currentOdometer.coerceAtLeast(0),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (localV.name.isNotBlank()) vMap["name"] = localV.name.take(100)
                    if (localV.plate.isNotBlank()) vMap["plate"] = localV.plate.take(30)
                    if (localV.vin.isNotBlank()) vMap["vin"] = localV.vin.take(30)
                    if (localV.notes.isNotBlank()) vMap["notes"] = localV.notes.take(5000)

                    Tasks.await(vehiclesCol.document(cleanVehId).set(vMap), 15, TimeUnit.SECONDS)
                    mergedVehicles.add(localV.copy(id = cleanVehId))
                    remoteVehicleIds.add(cleanVehId)
                }
            }
            if (mergedVehicles.isNotEmpty()) {
                _vehicles.value = mergedVehicles
                if (_selectedVehicleId.value == null || mergedVehicles.none { it.id == _selectedVehicleId.value }) {
                    _selectedVehicleId.value = mergedVehicles.first().id
                }
            }
            val defaultVehicleId = remoteVehicleIds.firstOrNull() ?: "veh-1"

            // 3. Sync Fill-Ups (/users/{uid}/fillups)
            val fillupsCol = userDocRef.collection("fillups")
            val remoteFillSnap = Tasks.await(
                fillupsCol.whereEqualTo("userId", uid).get(),
                15,
                TimeUnit.SECONDS
            )
            val remoteFillUps = mutableListOf<FillUp>()
            val remoteFillIds = mutableSetOf<String>()

            for (doc in remoteFillSnap.documents) {
                val data = doc.data ?: continue
                val id = (data["id"] as? String) ?: doc.id
                remoteFillIds.add(id)
                val ftStr = (data["fuelType"] as? String)?.uppercase(Locale.US) ?: "GASOLINE"
                val ft = runCatching { FuelType.valueOf(ftStr) }.getOrDefault(FuelType.GASOLINE)
                remoteFillUps.add(
                    FillUp(
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
                        notes = (data["notes"] as? String) ?: ""
                    )
                )
            }

            val mergedFillUps = remoteFillUps.toMutableList()
            for (localF in _fillUps.value) {
                val cleanFillId = sanitizeDocId(localF.id, "f")
                val targetVehId = if (localF.vehicleId in remoteVehicleIds) localF.vehicleId else defaultVehicleId
                if (cleanFillId !in remoteFillIds && targetVehId in remoteVehicleIds && localF.volume >= 0) {
                    val fMap = hashMapOf<String, Any>(
                        "id" to cleanFillId,
                        "userId" to uid,
                        "vehicleId" to targetVehId,
                        "date" to formatIsoDate(localF.date),
                        "odometer" to localF.odometer.coerceAtLeast(0),
                        "volume" to localF.volume.coerceAtLeast(0.0),
                        "pricePerUnit" to localF.pricePerUnit.coerceAtLeast(0.0),
                        "totalCost" to localF.totalCost.coerceAtLeast(0.0),
                        "fuelType" to localF.fuelType.name.lowercase(Locale.US),
                        "isFullTank" to localF.isFullTank,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (localF.station.isNotBlank()) fMap["station"] = localF.station.take(200)
                    if (localF.notes.isNotBlank()) fMap["notes"] = localF.notes.take(5000)

                    runCatching {
                        Tasks.await(fillupsCol.document(cleanFillId).set(fMap), 15, TimeUnit.SECONDS)
                        mergedFillUps.add(localF.copy(id = cleanFillId, vehicleId = targetVehId))
                    }
                }
            }
            if (mergedFillUps.isNotEmpty()) {
                _fillUps.value = mergedFillUps.sortedByDescending { it.odometer }
            }

            // 4. Sync Expenses (/users/{uid}/expenses)
            val expensesCol = userDocRef.collection("expenses")
            val remoteExpSnap = Tasks.await(
                expensesCol.whereEqualTo("userId", uid).get(),
                15,
                TimeUnit.SECONDS
            )
            val remoteExpenses = mutableListOf<Expense>()
            val remoteExpIds = mutableSetOf<String>()

            for (doc in remoteExpSnap.documents) {
                val data = doc.data ?: continue
                val id = (data["id"] as? String) ?: doc.id
                remoteExpIds.add(id)
                val catStr = (data["category"] as? String)?.uppercase(Locale.US)?.replace("-", "_") ?: "OTHER"
                val cat = runCatching { ExpenseCategory.valueOf(catStr) }.getOrDefault(ExpenseCategory.OTHER)
                remoteExpenses.add(
                    Expense(
                        id = id,
                        vehicleId = (data["vehicleId"] as? String) ?: defaultVehicleId,
                        date = parseIsoDate(data["date"]),
                        category = cat,
                        amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                        description = (data["description"] as? String) ?: "",
                        odometer = (data["odometer"] as? Number)?.toInt(),
                        notes = (data["notes"] as? String) ?: ""
                    )
                )
            }

            val mergedExpenses = remoteExpenses.toMutableList()
            for (localE in _expenses.value) {
                val cleanExpId = sanitizeDocId(localE.id, "e")
                val targetVehId = if (localE.vehicleId in remoteVehicleIds) localE.vehicleId else defaultVehicleId
                if (cleanExpId !in remoteExpIds && targetVehId in remoteVehicleIds && localE.amount >= 0) {
                    val eMap = hashMapOf<String, Any>(
                        "id" to cleanExpId,
                        "userId" to uid,
                        "vehicleId" to targetVehId,
                        "date" to formatIsoDate(localE.date),
                        "category" to localE.category.name.lowercase(Locale.US),
                        "amount" to localE.amount.coerceAtLeast(0.0),
                        "description" to localE.description.ifBlank { localE.category.displayName }.ifBlank { "Expense" }.take(500),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    localE.odometer?.let { eMap["odometer"] = it.coerceAtLeast(0) }
                    if (localE.notes.isNotBlank()) eMap["notes"] = localE.notes.take(5000)

                    runCatching {
                        Tasks.await(expensesCol.document(cleanExpId).set(eMap), 15, TimeUnit.SECONDS)
                        mergedExpenses.add(localE.copy(id = cleanExpId, vehicleId = targetVehId))
                    }
                }
            }
            if (mergedExpenses.isNotEmpty()) {
                _expenses.value = mergedExpenses.sortedByDescending { it.date }
            }

            // 5. Sync Services (/users/{uid}/services)
            val servicesCol = userDocRef.collection("services")
            val remoteSvcSnap = Tasks.await(
                servicesCol.whereEqualTo("userId", uid).get(),
                15,
                TimeUnit.SECONDS
            )
            val remoteServices = mutableListOf<ServiceRecord>()
            val remoteSvcIds = mutableSetOf<String>()

            for (doc in remoteSvcSnap.documents) {
                val data = doc.data ?: continue
                val id = (data["id"] as? String) ?: doc.id
                remoteSvcIds.add(id)
                val svcType = serviceTypeFromCloud(data["type"])
                val rawParts = (data["parts"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                remoteServices.add(
                    ServiceRecord(
                        id = id,
                        vehicleId = (data["vehicleId"] as? String) ?: defaultVehicleId,
                        date = parseIsoDate(data["date"]),
                        type = svcType,
                        odometer = (data["odometer"] as? Number)?.toInt() ?: 0,
                        cost = (data["cost"] as? Number)?.toDouble() ?: 0.0,
                        description = (data["description"] as? String) ?: "",
                        parts = rawParts,
                        notes = (data["notes"] as? String) ?: ""
                    )
                )
            }

            val mergedServices = remoteServices.toMutableList()
            for (localS in _serviceRecords.value) {
                val cleanSvcId = sanitizeDocId(localS.id, "s")
                val targetVehId = if (localS.vehicleId in remoteVehicleIds) localS.vehicleId else defaultVehicleId
                if (cleanSvcId !in remoteSvcIds && targetVehId in remoteVehicleIds) {
                    val sMap = hashMapOf<String, Any>(
                        "id" to cleanSvcId,
                        "userId" to uid,
                        "vehicleId" to targetVehId,
                        "date" to formatIsoDate(localS.date),
                        "type" to serviceTypeToCloud(localS.type),
                        "odometer" to localS.odometer.coerceAtLeast(0),
                        "cost" to localS.cost.coerceAtLeast(0.0),
                        "description" to localS.description.ifBlank { localS.type.displayName }.ifBlank { "Service" }.take(500),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (localS.parts.isNotEmpty()) {
                        sMap["parts"] = localS.parts.take(50).map { it.take(200) }
                    }
                    if (localS.notes.isNotBlank()) sMap["notes"] = localS.notes.take(5000)

                    runCatching {
                        Tasks.await(servicesCol.document(cleanSvcId).set(sMap), 15, TimeUnit.SECONDS)
                        mergedServices.add(localS.copy(id = cleanSvcId, vehicleId = targetVehId))
                    }
                }
            }
            if (mergedServices.isNotEmpty()) {
                _serviceRecords.value = mergedServices.sortedByDescending { it.date }
            }

            // 6. Sync Reminders (/users/{uid}/reminders)
            val remindersCol = userDocRef.collection("reminders")
            val remoteRemSnap = Tasks.await(
                remindersCol.whereEqualTo("userId", uid).get(),
                15,
                TimeUnit.SECONDS
            )
            val remoteReminders = mutableListOf<Reminder>()
            val remoteRemIds = mutableSetOf<String>()

            for (doc in remoteRemSnap.documents) {
                val data = doc.data ?: continue
                val id = (data["id"] as? String) ?: doc.id
                remoteRemIds.add(id)
                val remType = serviceTypeFromCloud(data["type"])
                remoteReminders.add(
                    Reminder(
                        id = id,
                        vehicleId = (data["vehicleId"] as? String) ?: defaultVehicleId,
                        title = (data["title"] as? String) ?: remType.displayName,
                        type = remType,
                        intervalKm = (data["intervalKm"] as? Number)?.toInt(),
                        intervalDays = (data["intervalDays"] as? Number)?.toInt(),
                        lastServiceDate = parseIsoDateOrNull(data["lastServiceDate"]),
                        lastServiceOdometer = (data["lastServiceOdometer"] as? Number)?.toInt(),
                        nextDueDate = parseIsoDateOrNull(data["nextDueDate"]),
                        nextDueOdometer = (data["nextDueOdometer"] as? Number)?.toInt(),
                        isRecurring = (data["isRecurring"] as? Boolean) ?: false,
                        isCompleted = !((data["isActive"] as? Boolean) ?: true)
                    )
                )
            }

            val pendingReminderUpdates = prefs
                .getStringSet(KEY_PENDING_REMINDER_UPDATES, emptySet())
                .orEmpty().toMutableSet()
            val pendingReminderDeletes = prefs
                .getStringSet(KEY_PENDING_REMINDER_DELETES, emptySet())
                .orEmpty().toMutableSet()

            val mergedRemindersById = remoteReminders.associateBy { it.id }.toMutableMap()

            // Push local deletions to the cloud (and hide them locally even if a push fails).
            for (docId in pendingReminderDeletes.toList()) {
                mergedRemindersById.remove(docId)
                remoteRemIds.remove(docId)
                val deleted = runCatching {
                    Tasks.await(remindersCol.document(docId).delete(), 15, TimeUnit.SECONDS)
                    true
                }.getOrDefault(false)
                if (deleted) {
                    pendingReminderDeletes.remove(docId)
                }
            }

            for (localR in _reminders.value) {
                val cleanRemId = sanitizeDocId(localR.id, "r")
                if (cleanRemId in pendingReminderDeletes) continue
                val targetVehId = if (localR.vehicleId in remoteVehicleIds) localR.vehicleId else defaultVehicleId
                if (targetVehId !in remoteVehicleIds) continue

                if (cleanRemId in remoteRemIds) {
                    // Remote wins unless the user edited this reminder locally since the last sync.
                    if (cleanRemId in pendingReminderUpdates) {
                        val updated = runCatching {
                            Tasks.await(
                                remindersCol.document(cleanRemId)
                                    .update(buildReminderUpdateMap(localR, targetVehId)),
                                15,
                                TimeUnit.SECONDS
                            )
                            true
                        }.getOrDefault(false)
                        if (updated) pendingReminderUpdates.remove(cleanRemId)
                        mergedRemindersById[cleanRemId] = localR.copy(id = cleanRemId, vehicleId = targetVehId)
                    }
                } else {
                    val created = runCatching {
                        Tasks.await(
                            remindersCol.document(cleanRemId)
                                .set(buildReminderCreateMap(uid, cleanRemId, localR, targetVehId)),
                            15,
                            TimeUnit.SECONDS
                        )
                        true
                    }.getOrDefault(false)
                    if (created) {
                        remoteRemIds.add(cleanRemId)
                        pendingReminderUpdates.remove(cleanRemId)
                    }
                    mergedRemindersById[cleanRemId] = localR.copy(id = cleanRemId, vehicleId = targetVehId)
                }
            }

            // Drop pending edits for reminders that no longer exist locally.
            val localReminderIds = _reminders.value.map { sanitizeDocId(it.id, "r") }.toSet()
            pendingReminderUpdates.retainAll(localReminderIds)

            prefs.edit()
                .putStringSet(KEY_PENDING_REMINDER_UPDATES, pendingReminderUpdates)
                .putStringSet(KEY_PENDING_REMINDER_DELETES, pendingReminderDeletes)
                .apply()

            if (mergedRemindersById.isNotEmpty()) {
                _reminders.value = mergedRemindersById.values.toList()
            }

            val syncTime = System.currentTimeMillis()
            prefs.edit().putLong("last_sync_ts", syncTime).apply()
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
