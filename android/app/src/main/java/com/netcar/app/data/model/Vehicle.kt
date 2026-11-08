package com.netcar.app.data.model

import java.util.Date

data class Vehicle(
    val id: String = "",
    val userId: String = "",
    val make: String = "",
    val model: String = "",
    val year: Int = 0,
    val plate: String = "",
    val vin: String = "",
    val fuelType: FuelType = FuelType.GASOLINE,
    val currentOdometer: Double = 0.0,
    val photoUrl: String? = null,
    val notes: String = "",
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    val displayName: String
        get() = if (make.isBlank() && model.isBlank()) "Vehicle" else "$make $model".trim()

    val fullTitle: String
        get() = if (year > 0) "$year $displayName" else displayName
}
