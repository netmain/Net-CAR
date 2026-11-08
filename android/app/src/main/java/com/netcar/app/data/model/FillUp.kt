package com.netcar.app.data.model

import java.util.Date

data class FillUp(
    val id: String = "",
    val vehicleId: String = "",
    val userId: String = "",
    val date: Date = Date(),
    val odometer: Double = 0.0,
    val volume: Double = 0.0,
    val pricePerUnit: Double = 0.0,
    val totalCost: Double = 0.0,
    val fuelType: FuelType = FuelType.GASOLINE,
    val isFullTank: Boolean = true,
    val station: String = "",
    val notes: String = "",
    val createdAt: Date = Date()
) {
    val calculatedTotalCost: Double
        get() = if (totalCost > 0.0) totalCost else volume * pricePerUnit

    val calculatedPricePerUnit: Double
        get() = if (pricePerUnit > 0.0) pricePerUnit else if (volume > 0.0) totalCost / volume else 0.0
}
