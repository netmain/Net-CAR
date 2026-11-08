package com.netcar.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.netcar.app.data.model.Service
import com.netcar.app.data.model.ServiceType
import java.util.Date

@Entity(
    tableName = "services",
    indices = [
        Index(value = ["vehicleId"]),
        Index(value = ["userId"]),
        Index(value = ["date"]),
        Index(value = ["type"])
    ]
)
data class ServiceEntity(
    @PrimaryKey
    val id: String,
    val vehicleId: String,
    val userId: String,
    val date: Date,
    val type: ServiceType,
    val odometer: Double,
    val cost: Double,
    val description: String,
    val parts: List<String>,
    val receiptUrl: String?,
    val notes: String,
    val createdAt: Date
) {
    fun toDomainModel(): Service = Service(
        id = id,
        vehicleId = vehicleId,
        userId = userId,
        date = date,
        type = type,
        odometer = odometer,
        cost = cost,
        description = description,
        parts = parts,
        receiptUrl = receiptUrl,
        notes = notes,
        createdAt = createdAt
    )

    companion object {
        fun fromDomainModel(service: Service): ServiceEntity = ServiceEntity(
            id = service.id,
            vehicleId = service.vehicleId,
            userId = service.userId,
            date = service.date,
            type = service.type,
            odometer = service.odometer,
            cost = service.cost,
            description = service.description,
            parts = service.parts,
            receiptUrl = service.receiptUrl,
            notes = service.notes,
            createdAt = service.createdAt
        )
    }
}

fun Service.toEntity(): ServiceEntity = ServiceEntity.fromDomainModel(this)
