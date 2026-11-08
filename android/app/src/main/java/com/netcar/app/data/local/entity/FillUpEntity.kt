package com.netcar.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.netcar.app.data.model.FillUp
import com.netcar.app.data.model.FuelType
import java.util.Date

@Entity(
    tableName = "fillups",
    indices = [
        Index(value = ["vehicleId"]),
        Index(value = ["userId"]),
        Index(value = ["date"])
    ]
)
data class FillUpEntity(
    @PrimaryKey
    val id: String,
    val vehicleId: String,
    val userId: String,
    val date: Date,
    val odometer: Double,
    val volume: Double,
    val pricePerUnit: Double,
    val totalCost: Double,
    val fuelType: FuelType,
    val isFullTank: Boolean,
    val station: String,
    val notes: String,
    val createdAt: Date
) {
    fun toDomainModel(): FillUp = FillUp(
        id = id,
        vehicleId = vehicleId,
        userId = userId,
        date = date,
        odometer = odometer,
        volume = volume,
        pricePerUnit = pricePerUnit,
        totalCost = totalCost,
        fuelType = fuelType,
        isFullTank = isFullTank,
        station = station,
        notes = notes,
        createdAt = createdAt
    )

    companion object {
        fun fromDomainModel(fillUp: FillUp): FillUpEntity = FillUpEntity(
            id = fillUp.id,
            vehicleId = fillUp.vehicleId,
            userId = fillUp.userId,
            date = fillUp.date,
            odometer = fillUp.odometer,
            volume = fillUp.volume,
            pricePerUnit = fillUp.pricePerUnit,
            totalCost = fillUp.totalCost,
            fuelType = fillUp.fuelType,
            isFullTank = fillUp.isFullTank,
            station = fillUp.station,
            notes = fillUp.notes,
            createdAt = fillUp.createdAt
        )
    }
}

fun FillUp.toEntity(): FillUpEntity = FillUpEntity.fromDomainModel(this)
