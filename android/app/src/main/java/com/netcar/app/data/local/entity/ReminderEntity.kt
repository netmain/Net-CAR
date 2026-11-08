package com.netcar.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.netcar.app.data.model.Reminder
import com.netcar.app.data.model.ServiceType
import java.util.Date

@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["vehicleId"]),
        Index(value = ["userId"]),
        Index(value = ["isActive"]),
        Index(value = ["nextDueDate"])
    ]
)
data class ReminderEntity(
    @PrimaryKey
    val id: String,
    val vehicleId: String,
    val userId: String,
    val title: String,
    val type: ServiceType,
    val intervalKm: Double?,
    val intervalDays: Int?,
    val lastServiceDate: Date?,
    val lastServiceOdometer: Double?,
    val nextDueDate: Date?,
    val nextDueOdometer: Double?,
    val isRecurring: Boolean,
    val isActive: Boolean,
    val createdAt: Date
) {
    fun toDomainModel(): Reminder = Reminder(
        id = id,
        vehicleId = vehicleId,
        userId = userId,
        title = title,
        type = type,
        intervalKm = intervalKm,
        intervalDays = intervalDays,
        lastServiceDate = lastServiceDate,
        lastServiceOdometer = lastServiceOdometer,
        nextDueDate = nextDueDate,
        nextDueOdometer = nextDueOdometer,
        isRecurring = isRecurring,
        isActive = isActive,
        createdAt = createdAt
    )

    companion object {
        fun fromDomainModel(reminder: Reminder): ReminderEntity = ReminderEntity(
            id = reminder.id,
            vehicleId = reminder.vehicleId,
            userId = reminder.userId,
            title = reminder.title,
            type = reminder.type,
            intervalKm = reminder.intervalKm,
            intervalDays = reminder.intervalDays,
            lastServiceDate = reminder.lastServiceDate,
            lastServiceOdometer = reminder.lastServiceOdometer,
            nextDueDate = reminder.nextDueDate,
            nextDueOdometer = reminder.nextDueOdometer,
            isRecurring = reminder.isRecurring,
            isActive = reminder.isActive,
            createdAt = reminder.createdAt
        )
    }
}

fun Reminder.toEntity(): ReminderEntity = ReminderEntity.fromDomainModel(this)
