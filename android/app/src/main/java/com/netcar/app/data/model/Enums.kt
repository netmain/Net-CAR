package com.netcar.app.data.model

enum class FuelType {
    GASOLINE,
    DIESEL,
    ELECTRIC,
    HYBRID,
    LPG,
    ETHANOL,
    CNG
}

enum class ExpenseCategory {
    INSURANCE,
    PARKING,
    TOLLS,
    FINES,
    CAR_WASH,
    TAXES,
    REGISTRATION,
    FINANCING,
    REPAIRS,
    MAINTENANCE,
    OTHER
}

enum class ServiceType {
    OIL_CHANGE,
    TIRE_ROTATION,
    BRAKES,
    FILTERS,
    BATTERY,
    TRANSMISSION,
    COOLANT,
    SPARK_PLUGS,
    TIMING_BELT,
    INSPECTION,
    OTHER
}

enum class DistanceUnit {
    KM,
    MI
}

enum class VolumeUnit {
    LITERS,
    GALLONS
}

enum class AppCurrency {
    EUR,
    USD,
    CAD,
    GBP,
    MAD
}
