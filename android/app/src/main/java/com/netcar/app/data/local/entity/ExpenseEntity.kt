package com.netcar.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.netcar.app.data.model.Expense
import com.netcar.app.data.model.ExpenseCategory
import java.util.Date

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["vehicleId"]),
        Index(value = ["userId"]),
        Index(value = ["date"]),
        Index(value = ["category"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey
    val id: String,
    val vehicleId: String,
    val userId: String,
    val date: Date,
    val category: ExpenseCategory,
    val amount: Double,
    val description: String,
    val receiptUrl: String?,
    val odometer: Double?,
    val notes: String,
    val createdAt: Date
) {
    fun toDomainModel(): Expense = Expense(
        id = id,
        vehicleId = vehicleId,
        userId = userId,
        date = date,
        category = category,
        amount = amount,
        description = description,
        receiptUrl = receiptUrl,
        odometer = odometer,
        notes = notes,
        createdAt = createdAt
    )

    companion object {
        fun fromDomainModel(expense: Expense): ExpenseEntity = ExpenseEntity(
            id = expense.id,
            vehicleId = expense.vehicleId,
            userId = expense.userId,
            date = expense.date,
            category = expense.category,
            amount = expense.amount,
            description = expense.description,
            receiptUrl = expense.receiptUrl,
            odometer = expense.odometer,
            notes = expense.notes,
            createdAt = expense.createdAt
        )
    }
}

fun Expense.toEntity(): ExpenseEntity = ExpenseEntity.fromDomainModel(this)
