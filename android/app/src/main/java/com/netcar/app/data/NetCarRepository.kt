package com.netcar.app.data

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
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

    init {
        refreshAuthStatusSafely()
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
    }

    fun toggleReminderCompleted(id: String) {
        _reminders.value = _reminders.value.map {
            if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it
        }
        persistLocalData()
    }

    fun deleteReminder(id: String) {
        _reminders.value = _reminders.value.filter { it.id != id }
        persistLocalData()
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

    suspend fun signInWithEmailPassword(email: String, password: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val cleanEmail = email.trim()
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

                // Perform immediate cloud sync after signing in
                syncWithCloud()
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    suspend fun signUpWithEmailPassword(email: String, password: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val cleanEmail = email.trim()
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

                // Perform immediate cloud sync after account creation
                syncWithCloud()
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    private fun getFirestoreSafely(): FirebaseFirestore {
        return runCatching {
            FirebaseFirestore.getInstance(FirebaseApp.getInstance(), FirebaseModule.FIRESTORE_DATABASE_ID)
        }.getOrElse {
            FirebaseFirestore.getInstance()
        }
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

    suspend fun syncWithCloud(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fbUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
            if (fbUser == null || fbUser.isAnonymous || fbUser.email.isNullOrBlank()) {
                _userSettings.value = _userSettings.value.copy(
                    isDriveBackupConnected = false,
                    connectedEmail = null,
                    userId = null
                )
                return@withContext Result.failure(
                    IllegalStateException(
                        if (_userSettings.value.language == SupportedLanguage.FR)
                            "Non connecté : veuillez vous connecter avec votre e-mail et mot de passe avant de synchroniser."
                        else
                            "Not signed in: please sign in with your email and password before syncing."
                    )
                )
            }

            val uid = fbUser.uid
            val email = fbUser.email ?: "driver@netcar.app"
            val displayName = (fbUser.displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")).take(200)
            val db = getFirestoreSafely()

            // 1. Ensure parent user profile document exists in /users/{uid}
            val userDocRef = db.collection("users").document(uid)
            val userSnap = Tasks.await(userDocRef.get(), 15, TimeUnit.SECONDS)
            if (!userSnap.exists()) {
                val userDoc = hashMapOf<String, Any>(
                    "userId" to uid,
                    "name" to displayName,
                    "email" to email.take(320),
                    "preferredLang" to _userSettings.value.language.code,
                    "units" to if (_userSettings.value.units == Units.METRIC) "metric" else "imperial",
                    "currency" to _userSettings.value.currency.code,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                _selectedVehicleId.value?.let { userDoc["activeVehicleId"] = it.take(128) }
                Tasks.await(userDocRef.set(userDoc), 15, TimeUnit.SECONDS)
            }

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
                if (localV.id !in remoteVehicleIds) {
                    val vMap = hashMapOf<String, Any>(
                        "id" to localV.id,
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

                    Tasks.await(vehiclesCol.document(localV.id).set(vMap), 15, TimeUnit.SECONDS)
                    mergedVehicles.add(localV)
                    remoteVehicleIds.add(localV.id)
                }
            }
            if (mergedVehicles.isNotEmpty()) {
                _vehicles.value = mergedVehicles
                if (_selectedVehicleId.value == null || mergedVehicles.none { it.id == _selectedVehicleId.value }) {
                    _selectedVehicleId.value = mergedVehicles.first().id
                }
            }

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
                        vehicleId = (data["vehicleId"] as? String) ?: (_selectedVehicleId.value ?: "veh-1"),
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
                if (localF.id !in remoteFillIds && localF.vehicleId in remoteVehicleIds && localF.volume > 0) {
                    val fMap = hashMapOf<String, Any>(
                        "id" to localF.id,
                        "userId" to uid,
                        "vehicleId" to localF.vehicleId,
                        "date" to formatIsoDate(localF.date),
                        "odometer" to localF.odometer.coerceAtLeast(0),
                        "volume" to localF.volume,
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
                        Tasks.await(fillupsCol.document(localF.id).set(fMap), 15, TimeUnit.SECONDS)
                        mergedFillUps.add(localF)
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
                        vehicleId = (data["vehicleId"] as? String) ?: (_selectedVehicleId.value ?: "veh-1"),
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
                if (localE.id !in remoteExpIds && localE.vehicleId in remoteVehicleIds && localE.amount > 0) {
                    val eMap = hashMapOf<String, Any>(
                        "id" to localE.id,
                        "userId" to uid,
                        "vehicleId" to localE.vehicleId,
                        "date" to formatIsoDate(localE.date),
                        "category" to localE.category.name.lowercase(Locale.US),
                        "amount" to localE.amount,
                        "description" to localE.description.ifBlank { localE.category.displayName }.take(500),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    localE.odometer?.let { eMap["odometer"] = it.coerceAtLeast(0) }
                    if (localE.notes.isNotBlank()) eMap["notes"] = localE.notes.take(5000)

                    runCatching {
                        Tasks.await(expensesCol.document(localE.id).set(eMap), 15, TimeUnit.SECONDS)
                        mergedExpenses.add(localE)
                    }
                }
            }
            if (mergedExpenses.isNotEmpty()) {
                _expenses.value = mergedExpenses.sortedByDescending { it.date }
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

            val msg = if (_userSettings.value.language == SupportedLanguage.FR) {
                "Synchronisation Cloud réussie (${_vehicles.value.size} véhicules, ${_fillUps.value.size} pleins)"
            } else {
                "Cloud sync complete (${_vehicles.value.size} vehicles, ${_fillUps.value.size} fill-ups)"
            }
            Result.success(msg)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
