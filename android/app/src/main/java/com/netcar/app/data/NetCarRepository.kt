package com.netcar.app.data

import com.netcar.app.model.DashboardStats
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetCarRepository @Inject constructor() {

    private val sampleVehicle1 = Vehicle(
        id = "v-1",
        name = "Daily Driver",
        make = "Peugeot",
        model = "208 GT",
        year = 2022,
        plate = "AB-123-CD",
        vin = "VF3CCHNZ6LT123456",
        fuelType = FuelType.GASOLINE,
        currentOdometer = 45200,
        notes = "Main commuter vehicle"
    )

    private val sampleVehicle2 = Vehicle(
        id = "v-2",
        name = "Family Cruiser",
        make = "Volkswagen",
        model = "Golf VII",
        year = 2020,
        plate = "EF-456-GH",
        vin = "WVWZZZAUZLP789012",
        fuelType = FuelType.DIESEL,
        currentOdometer = 89650,
        notes = "Road trip car"
    )

    private val _vehicles = MutableStateFlow<List<Vehicle>>(listOf(sampleVehicle1, sampleVehicle2))
    val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()

    private val _selectedVehicleId = MutableStateFlow<String?>(sampleVehicle1.id)
    val selectedVehicleId: StateFlow<String?> = _selectedVehicleId.asStateFlow()

    private val now = System.currentTimeMillis()
    private val oneDay = 86400000L

    private val _fillUps = MutableStateFlow<List<FillUp>>(
        listOf(
            FillUp(
                id = "f-1",
                vehicleId = "v-1",
                date = now - (3 * oneDay),
                odometer = 45200,
                volume = 42.5,
                pricePerUnit = 1.82,
                totalCost = 77.35,
                fuelType = FuelType.GASOLINE,
                isFullTank = true,
                station = "TotalEnergies",
                notes = "SP98 Excellence"
            ),
            FillUp(
                id = "f-2",
                vehicleId = "v-1",
                date = now - (14 * oneDay),
                odometer = 44550,
                volume = 41.0,
                pricePerUnit = 1.79,
                totalCost = 73.39,
                fuelType = FuelType.GASOLINE,
                isFullTank = true,
                station = "Shell Station",
                notes = "Highway service area"
            ),
            FillUp(
                id = "f-3",
                vehicleId = "v-1",
                date = now - (27 * oneDay),
                odometer = 43900,
                volume = 43.0,
                pricePerUnit = 1.84,
                totalCost = 79.12,
                fuelType = FuelType.GASOLINE,
                isFullTank = true,
                station = "BP Express",
                notes = ""
            ),
            FillUp(
                id = "f-4",
                vehicleId = "v-2",
                date = now - (5 * oneDay),
                odometer = 89650,
                volume = 50.0,
                pricePerUnit = 1.68,
                totalCost = 84.00,
                fuelType = FuelType.DIESEL,
                isFullTank = true,
                station = "Avia",
                notes = "B7 Diesel"
            )
        )
    )
    val fillUps: StateFlow<List<FillUp>> = _fillUps.asStateFlow()

    private val _expenses = MutableStateFlow<List<Expense>>(
        listOf(
            Expense(
                id = "e-1",
                vehicleId = "v-1",
                date = now - (5 * oneDay),
                category = ExpenseCategory.PARKING,
                amount = 18.50,
                description = "City Center underground parking",
                odometer = 45150
            ),
            Expense(
                id = "e-2",
                vehicleId = "v-1",
                date = now - (10 * oneDay),
                category = ExpenseCategory.CAR_WASH,
                amount = 22.00,
                description = "Premium foam wash + interior vacuum",
                odometer = 44800
            ),
            Expense(
                id = "e-3",
                vehicleId = "v-1",
                date = now - (20 * oneDay),
                category = ExpenseCategory.TOLLS,
                amount = 34.60,
                description = "Highway toll A13 weekend trip",
                odometer = 44200
            ),
            Expense(
                id = "e-4",
                vehicleId = "v-1",
                date = now - (35 * oneDay),
                category = ExpenseCategory.INSURANCE,
                amount = 68.00,
                description = "Monthly comprehensive insurance policy",
                odometer = 43700
            )
        )
    )
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _serviceRecords = MutableStateFlow<List<ServiceRecord>>(
        listOf(
            ServiceRecord(
                id = "s-1",
                vehicleId = "v-1",
                date = now - (60 * oneDay),
                type = ServiceType.OIL_CHANGE,
                odometer = 40000,
                cost = 145.00,
                description = "Full synthetic oil change and multi-point inspection",
                parts = listOf("0W-20 Total Quartz", "Purflux Oil Filter", "Drain Plug Gasket"),
                notes = "Performed at official dealership"
            ),
            ServiceRecord(
                id = "s-2",
                vehicleId = "v-1",
                date = now - (180 * oneDay),
                type = ServiceType.BRAKES,
                odometer = 32000,
                cost = 210.00,
                description = "Front brake pads replacement",
                parts = listOf("Brembo Front Brake Pads", "Brake cleaner"),
                notes = "Rotors in good condition"
            ),
            ServiceRecord(
                id = "s-3",
                vehicleId = "v-1",
                date = now - (220 * oneDay),
                type = ServiceType.FILTERS,
                odometer = 30000,
                cost = 55.00,
                description = "Cabin air filter and engine air filter change",
                parts = listOf("Mann Filter Cabin Carbon", "Mann Engine Air Filter"),
                notes = "DIY in garage"
            )
        )
    )
    val serviceRecords: StateFlow<List<ServiceRecord>> = _serviceRecords.asStateFlow()

    private val _reminders = MutableStateFlow<List<Reminder>>(
        listOf(
            Reminder(
                id = "r-1",
                vehicleId = "v-1",
                title = "Oil & Filter Change",
                type = ServiceType.OIL_CHANGE,
                intervalKm = 15000,
                intervalDays = 365,
                lastServiceDate = now - (60 * oneDay),
                lastServiceOdometer = 40000,
                nextDueDate = now + (305 * oneDay),
                nextDueOdometer = 55000,
                isRecurring = true,
                isCompleted = false
            ),
            Reminder(
                id = "r-2",
                vehicleId = "v-1",
                title = "Tire Rotation & Balancing",
                type = ServiceType.TIRE_ROTATION,
                intervalKm = 10000,
                intervalDays = 180,
                lastServiceDate = now - (150 * oneDay),
                lastServiceOdometer = 35000,
                nextDueDate = now + (30 * oneDay),
                nextDueOdometer = 45000, // Current is 45200 -> Overdue by 200 km!
                isRecurring = true,
                isCompleted = false
            ),
            Reminder(
                id = "r-3",
                vehicleId = "v-1",
                title = "Annual Technical Inspection",
                type = ServiceType.INSPECTION,
                intervalDays = 730,
                lastServiceDate = now - (700 * oneDay),
                nextDueDate = now + (30 * oneDay),
                isRecurring = true,
                isCompleted = false
            ),
            Reminder(
                id = "r-4",
                vehicleId = "v-1",
                title = "Brake Fluid Flush",
                type = ServiceType.BRAKES,
                intervalDays = 730,
                lastServiceDate = now - (750 * oneDay),
                nextDueDate = now - (20 * oneDay), // Overdue by date!
                isRecurring = true,
                isCompleted = false
            ),
            Reminder(
                id = "r-5",
                vehicleId = "v-1",
                title = "Spark Plugs Check",
                type = ServiceType.SPARK_PLUGS,
                lastServiceDate = now - (365 * oneDay),
                lastServiceOdometer = 30000,
                nextDueDate = now - (50 * oneDay),
                nextDueOdometer = 40000,
                isRecurring = false,
                isCompleted = true
            )
        )
    )
    val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()

    private val _userSettings = MutableStateFlow(UserSettings())
    val userSettings: StateFlow<UserSettings> = _userSettings.asStateFlow()

    fun selectVehicle(vehicleId: String) {
        _selectedVehicleId.value = vehicleId
    }

    fun addVehicle(vehicle: Vehicle) {
        _vehicles.value = _vehicles.value + vehicle
        if (_selectedVehicleId.value == null) {
            _selectedVehicleId.value = vehicle.id
        }
    }

    fun deleteVehicle(vehicleId: String) {
        _vehicles.value = _vehicles.value.filter { it.id != vehicleId }
        if (_selectedVehicleId.value == vehicleId) {
            _selectedVehicleId.value = _vehicles.value.firstOrNull()?.id
        }
    }

    fun updateVehicleOdometer(vehicleId: String, newOdometer: Int) {
        _vehicles.value = _vehicles.value.map { v ->
            if (v.id == vehicleId && newOdometer > v.currentOdometer) {
                v.copy(currentOdometer = newOdometer)
            } else v
        }
    }

    fun addFillUp(fillUp: FillUp) {
        _fillUps.value = listOf(fillUp) + _fillUps.value
        updateVehicleOdometer(fillUp.vehicleId, fillUp.odometer)
    }

    fun deleteFillUp(id: String) {
        _fillUps.value = _fillUps.value.filter { it.id != id }
    }

    fun addExpense(expense: Expense) {
        _expenses.value = listOf(expense) + _expenses.value
        expense.odometer?.let { updateVehicleOdometer(expense.vehicleId, it) }
    }

    fun deleteExpense(id: String) {
        _expenses.value = _expenses.value.filter { it.id != id }
    }

    fun addServiceRecord(service: ServiceRecord) {
        _serviceRecords.value = listOf(service) + _serviceRecords.value
        updateVehicleOdometer(service.vehicleId, service.odometer)
    }

    fun deleteServiceRecord(id: String) {
        _serviceRecords.value = _serviceRecords.value.filter { it.id != id }
    }

    fun addReminder(reminder: Reminder) {
        _reminders.value = listOf(reminder) + _reminders.value
    }

    fun toggleReminderCompleted(id: String) {
        _reminders.value = _reminders.value.map {
            if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it
        }
    }

    fun deleteReminder(id: String) {
        _reminders.value = _reminders.value.filter { it.id != id }
    }

    fun updateLanguage(language: SupportedLanguage) {
        _userSettings.value = _userSettings.value.copy(language = language)
    }

    fun updateUnits(units: Units) {
        _userSettings.value = _userSettings.value.copy(units = units)
    }

    fun updateCurrency(currency: SupportedCurrency) {
        _userSettings.value = _userSettings.value.copy(currency = currency)
    }

    fun triggerCloudBackup() {
        _userSettings.value = _userSettings.value.copy(
            isDriveBackupConnected = true,
            lastBackupTimestamp = System.currentTimeMillis()
        )
    }
}
