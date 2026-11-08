package com.netcar.app.data.model

import java.util.Date

data class Service(
    val id: String = "",
    val vehicleId: String = "",
    val userId: String = "",
    val date: Date = Date(),
    val type: ServiceType = ServiceType.OTHER,
    val odometer: Double = 0.0,
    val cost: Double = 0.0,
    val description: String = "",
    val parts: List<String> = emptyList(),
    val receiptUrl: String? = null,
    val notes: String = "",
    val createdAt: Date = Date()
)
