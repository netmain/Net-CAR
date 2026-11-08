package com.netcar.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.netcar.app.data.local.dao.ExpenseDao
import com.netcar.app.data.local.entity.toEntity
import com.netcar.app.data.model.Expense
import com.netcar.app.data.model.ExpenseCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface ExpenseRepository {
    fun getExpensesForVehicle(vehicleId: String): Flow<List<Expense>>
    fun getAllExpenses(userId: String): Flow<List<Expense>>
    fun getExpensesByCategory(vehicleId: String, category: ExpenseCategory): Flow<List<Expense>>
    fun getExpenseById(id: String): Flow<Expense?>
    suspend fun getExpenseByIdOnce(id: String): Expense?
    suspend fun saveExpense(expense: Expense): Result<String>
    suspend fun updateExpense(expense: Expense): Result<Unit>
    suspend fun deleteExpense(id: String): Result<Unit>
    suspend fun syncExpenses(userId: String): Result<Unit>
}

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val firestore: FirebaseFirestore
) : ExpenseRepository {

    private val collection = firestore.collection("expenses")

    override fun getExpensesForVehicle(vehicleId: String): Flow<List<Expense>> {
        return expenseDao.getExpensesForVehicle(vehicleId)
            .map { list -> list.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getAllExpenses(userId: String): Flow<List<Expense>> {
        return expenseDao.getAllExpenses(userId)
            .map { list -> list.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getExpensesByCategory(vehicleId: String, category: ExpenseCategory): Flow<List<Expense>> {
        return expenseDao.getExpensesByCategory(vehicleId, category)
            .map { list -> list.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getExpenseById(id: String): Flow<Expense?> {
        return expenseDao.getExpenseById(id)
            .map { it?.toDomainModel() }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getExpenseByIdOnce(id: String): Expense? = withContext(Dispatchers.IO) {
        expenseDao.getExpenseByIdOnce(id)?.toDomainModel()
    }

    override suspend fun saveExpense(expense: Expense): Result<String> = withContext(Dispatchers.IO) {
        try {
            val expenseId = expense.id.ifBlank { UUID.randomUUID().toString() }
            val now = Date()
            val toSave = expense.copy(
                id = expenseId,
                createdAt = if (expense.id.isBlank()) now else expense.createdAt
            )

            expenseDao.insertExpense(toSave.toEntity())

            try {
                collection.document(expenseId).set(toSave).await()
            } catch (e: Exception) {
                // Offline fallback
            }

            Result.success(expenseId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateExpense(expense: Expense): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            expenseDao.insertExpense(expense.toEntity())

            try {
                collection.document(expense.id).set(expense).await()
            } catch (e: Exception) {
                // Offline fallback
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteExpense(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            expenseDao.deleteExpenseById(id)

            try {
                collection.document(id).delete().await()
            } catch (e: Exception) {
                // Offline fallback
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncExpenses(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val snapshot = collection.whereEqualTo("userId", userId).get().await()
            val remoteExpenses = snapshot.documents.mapNotNull { it.toObject(Expense::class.java) }
            val entities = remoteExpenses.map { it.toEntity() }

            if (entities.isNotEmpty()) {
                expenseDao.insertExpenses(entities)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
