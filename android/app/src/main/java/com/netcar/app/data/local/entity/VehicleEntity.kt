package com.netcar.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.netcar.app.data.model.FuelType
import com.netcar.app.data.model.Vehicle
import java.util.Date

@Entity(
    tableName = "vehicles",
    indices = [Index(value = ["userId"])]
)
data class VehicleEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val make: String,
    val model: String,
    val year: Int,
    val plate: String,
    val vin: String,
    val fuelType: FuelType,
    val currentOdometer: Double,
    val photoUrl: String?,
    val notes: String,
    val createdAt: Date,
    val updatedAt: Date
) {
    fun toDomainModel(): Vehicle = Vehicle(
        id = id,
        userId = userId,
        make = make,
        model = model,
        year = year,
        plate = plate,
        vin = vin,
        fuelType = fuelType,
        currentOdometer = currentOdometer,
        photoUrl = photoUrl,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomainModel(vehicle: Vehicle): VehicleEntity = VehicleEntity(
            id = vehicle.id,
            userId = vehicle.userId,
            make = vehicle.make,
            model = vehicle.model,
            year = vehicle.year,
            plate = vehicle.plate,
            vin = vehicle.vin,
            fuelType = vehicle.fuelType,
            currentOdometer = vehicle.currentOdometer,
            photoUrl = vehicle.photoUrl,
            notes = vehicle.notes,
            createdAt = vehicle.createdAt,
            updatedAt = vehicle.updatedAt
        )
    }
}

fun Vehicle.toEntity(): VehicleEntity = VehicleEntity.fromDomainModel(this)
