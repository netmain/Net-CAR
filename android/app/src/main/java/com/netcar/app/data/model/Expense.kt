package com.netcar.app.data.model

import java.util.Date

data class Expense(
    val id: String = "",
    val vehicleId: String = "",
    val userId: String = "",
    val date: Date = Date(),
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val amount: Double = 0.0,
    val description: String = "",
    val receiptUrl: String? = null,
    val odometer: Double? = null,
    val notes: String = "",
    val createdAt: Date = Date()
)
