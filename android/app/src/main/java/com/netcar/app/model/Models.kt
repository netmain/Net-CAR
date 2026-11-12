package com.netcar.app.model

import java.util.UUID

enum class FuelType(val displayName: String, val displayNameFr: String) {
    GASOLINE("Gasoline", "Essence"),
    DIESEL("Diesel", "Diesel"),
    LPG("LPG", "GPL"),
    ELECTRIC("Electric", "Électrique"),
    HYBRID("Hybrid", "Hybride"),
    ETHANOL("Ethanol", "Éthanol"),
    CNG("CNG", "GNV")
}

enum class ExpenseCategory(val displayName: String, val displayNameFr: String) {
    INSURANCE("Insurance", "Assurance"),
    PARKING("Parking", "Stationnement"),
    TOLLS("Tolls", "Péage"),
    FINES("Fines", "Amendes"),
    CAR_WASH("Car Wash", "Lavage"),
    TAXES("Taxes", "Taxes"),
    REGISTRATION("Registration", "Carte grise"),
    FINANCING("Financing", "Financement"),
    REPAIRS("Repairs", "Réparations"),
    MAINTENANCE("Maintenance", "Entretien"),
    OTHER("Other", "Autre")
}

enum class ServiceType(val displayName: String, val displayNameFr: String) {
    OIL_CHANGE("Oil Change", "Vidange"),
    TIRE_ROTATION("Tire Rotation", "Permutation pneus"),
    BRAKES("Brakes", "Freins"),
    FILTERS("Filters", "Filtres"),
    BATTERY("Battery", "Batterie"),
    TRANSMISSION("Transmission", "Boîte de vitesses"),
    COOLANT("Coolant", "Liquide de refroidissement"),
    SPARK_PLUGS("Spark Plugs", "Bougies"),
    TIMING_BELT("Timing Belt", "Courroie de distribution"),
    INSPECTION("Inspection", "Contrôle technique"),
    INSURANCE("Insurance", "Assurance"),
    OTHER("Other", "Autre")
}

enum class Units(val label: String, val distUnit: String, val volUnit: String, val consUnit: String) {
    METRIC("Metric", "km", "L", "L/100km"),
    IMPERIAL("Imperial", "mi", "gal", "MPG")
}

enum class SupportedCurrency(val code: String, val symbol: String) {
    DZD("DZD", "DA"),
    EUR("EUR", "€"),
    USD("USD", "$"),
    CAD("CAD", "CA$"),
    GBP("GBP", "£"),
    MAD("MAD", "MAD"),
    CHF("CHF", "CHF")
}

enum class SupportedLanguage(val code: String, val displayName: String) {
    EN("en", "English"),
    FR("fr", "Français")
}

enum class ThemeMode(val code: String, val displayName: String, val displayNameFr: String) {
    SYSTEM("system", "System", "Système"),
    LIGHT("light", "Light", "Clair"),
    DARK("dark", "Dark", "Sombre")
}

data class Vehicle(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val make: String,
    val model: String,
    val year: Int,
    val plate: String,
    val vin: String = "",
    val fuelType: FuelType = FuelType.GASOLINE,
    val photoUrl: String = "",
    val currentOdometer: Int = 0,
    val notes: String = "",
    val updatedAt: Long = 0L
) {
    val displayName: String
        get() = if (name.isNotBlank()) name else "$make $model"
}

data class FillUp(
    val id: String = UUID.randomUUID().toString(),
    val vehicleId: String,
    val date: Long = System.currentTimeMillis(),
    val odometer: Int,
    val volume: Double,
    val pricePerUnit: Double,
    val totalCost: Double = volume * pricePerUnit,
    val fuelType: FuelType = FuelType.GASOLINE,
    val isFullTank: Boolean = true,
    val station: String = "",
    val notes: String = "",
    val updatedAt: Long = 0L
)

data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val vehicleId: String,
    val date: Long = System.currentTimeMillis(),
    val category: ExpenseCategory,
    val customCategory: String = "",
    val amount: Double,
    val description: String,
    val receiptUrl: String = "",
    val odometer: Int? = null,
    val notes: String = "",
    val updatedAt: Long = 0L
) {
    fun categoryLabel(isFr: Boolean): String =
        customCategory.ifBlank { if (isFr) category.displayNameFr else category.displayName }
}

data class ServiceRecord(
    val id: String = UUID.randomUUID().toString(),
    val vehicleId: String,
    val date: Long = System.currentTimeMillis(),
    val type: ServiceType,
    val customType: String = "",
    val odometer: Int,
    val cost: Double,
    val description: String,
    val parts: List<String> = emptyList(),
    val receiptUrl: String = "",
    val notes: String = "",
    val updatedAt: Long = 0L
) {
    fun typeLabel(isFr: Boolean): String =
        customType.ifBlank { if (isFr) type.displayNameFr else type.displayName }
}

data class Reminder(
    val id: String = UUID.randomUUID().toString(),
    val vehicleId: String,
    val title: String,
    val type: ServiceType = ServiceType.OTHER,
    val customType: String = "",
    val intervalKm: Int? = null,
    val intervalDays: Int? = null,
    val lastServiceDate: Long? = null,
    val lastServiceOdometer: Int? = null,
    val nextDueDate: Long? = null,
    val nextDueOdometer: Int? = null,
    val isRecurring: Boolean = false,
    val isCompleted: Boolean = false,
    val updatedAt: Long = 0L
) {
    fun typeLabel(isFr: Boolean): String =
        customType.ifBlank { if (isFr) type.displayNameFr else type.displayName }

    fun isOverdue(currentOdometer: Int, currentDate: Long = System.currentTimeMillis()): Boolean {
        if (isCompleted) return false
        val odometerOverdue = nextDueOdometer?.let { currentOdometer >= it } ?: false
        val dateOverdue = nextDueDate?.let { currentDate >= it } ?: false
        return odometerOverdue || dateOverdue
    }

    fun isDueSoon(currentOdometer: Int, currentDate: Long = System.currentTimeMillis()): Boolean {
        if (isCompleted || isOverdue(currentOdometer, currentDate)) return false
        val kmRemaining = nextDueOdometer?.let { it - currentOdometer }
        val daysRemaining = nextDueDate?.let { (it - currentDate) / (1000 * 60 * 60 * 24) }
        return (kmRemaining != null && kmRemaining in 1..1000) || (daysRemaining != null && daysRemaining in 0..14)
    }

    fun calculateProgress(currentOdometer: Int): Float {
        if (isCompleted) return 1f
        val lastOdo = lastServiceOdometer ?: (nextDueOdometer?.minus(intervalKm ?: 10000) ?: 0)
        val nextOdo = nextDueOdometer ?: return 0f
        if (nextOdo <= lastOdo) return 1f
        val progress = (currentOdometer - lastOdo).toFloat() / (nextOdo - lastOdo).toFloat()
        return progress.coerceIn(0f, 1f)
    }
}

data class DashboardStats(
    val totalVehicles: Int = 0,
    val averageConsumption: Double = 0.0,
    val monthlyFuelCost: Double = 0.0,
    val monthlyExpensesCost: Double = 0.0,
    val monthlyServiceCost: Double = 0.0,
    val totalMonthlyCost: Double = 0.0,
    val dueRemindersCount: Int = 0,
    val currentOdometer: Int = 0
)

data class AppUpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val sizeBytes: Long,
    val downloadUrl: String,
    val releasePageUrl: String
)

data class UserSettings(
    val language: SupportedLanguage = SupportedLanguage.FR,
    val units: Units = Units.METRIC,
    val currency: SupportedCurrency = SupportedCurrency.DZD,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isDriveBackupConnected: Boolean = false,
    val connectedEmail: String? = null,
    val userId: String? = null,
    val lastBackupTimestamp: Long? = null,
    val customExpenseCategories: List<String> = emptyList(),
    val customServiceTypes: List<String> = emptyList()
)
