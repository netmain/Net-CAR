package com.netcar.app.data.model

import java.util.Date

data class Reminder(
    val id: String = "",
    val vehicleId: String = "",
    val userId: String = "",
    val title: String = "",
    val type: ServiceType = ServiceType.OTHER,
    val intervalKm: Double? = null,
    val intervalDays: Int? = null,
    val lastServiceDate: Date? = null,
    val lastServiceOdometer: Double? = null,
    val nextDueDate: Date? = null,
    val nextDueOdometer: Double? = null,
    val isRecurring: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Date = Date()
) {
    fun isOverdue(currentOdometer: Double, currentDate: Date = Date()): Boolean {
        if (!isActive) return false
        val isOdometerOverdue = nextDueOdometer?.let { it > 0 && currentOdometer >= it } ?: false
        val isDateOverdue = nextDueDate?.let { currentDate.after(it) } ?: false
        return isOdometerOverdue || isDateOverdue
    }

    fun isDueSoon(currentOdometer: Double, thresholdKm: Double = 500.0, thresholdDays: Long = 14, currentDate: Date = Date()): Boolean {
        if (!isActive || isOverdue(currentOdometer, currentDate)) return false
        val isOdometerSoon = nextDueOdometer?.let { (it - currentOdometer) in 0.0..thresholdKm } ?: false
        val isDateSoon = nextDueDate?.let {
            val diffMs = it.time - currentDate.time
            val diffDays = diffMs / (1000 * 60 * 60 * 24)
            diffDays in 0..thresholdDays
        } ?: false
        return isOdometerSoon || isDateSoon
    }
}
