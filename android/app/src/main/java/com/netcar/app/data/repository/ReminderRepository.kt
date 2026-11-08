package com.netcar.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.netcar.app.data.local.dao.ReminderDao
import com.netcar.app.data.local.entity.toEntity
import com.netcar.app.data.model.Reminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface ReminderRepository {
    fun getRemindersForVehicle(vehicleId: String): Flow<List<Reminder>>
    fun getAllReminders(userId: String): Flow<List<Reminder>>
    fun getActiveReminders(userId: String): Flow<List<Reminder>>
    fun getActiveRemindersForVehicle(vehicleId: String): Flow<List<Reminder>>
    fun getReminderById(id: String): Flow<Reminder?>
    suspend fun getReminderByIdOnce(id: String): Reminder?
    suspend fun saveReminder(reminder: Reminder): Result<String>
    suspend fun updateReminder(reminder: Reminder): Result<Unit>
    suspend fun deleteReminder(id: String): Result<Unit>
    suspend fun completeReminder(reminderId: String, currentOdometer: Double, completionDate: Date = Date()): Result<Unit>
    suspend fun syncReminders(userId: String): Result<Unit>
}

@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val reminderDao: ReminderDao,
    private val firestore: FirebaseFirestore
) : ReminderRepository {

    private val collection = firestore.collection("reminders")

    override fun getRemindersForVehicle(vehicleId: String): Flow<List<Reminder>> {
        return reminderDao.getRemindersForVehicle(vehicleId)
            .map { list -> list.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getAllReminders(userId: String): Flow<List<Reminder>> {
        return reminderDao.getAllReminders(userId)
            .map { list -> list.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getActiveReminders(userId: String): Flow<List<Reminder>> {
        return reminderDao.getActiveReminders(userId)
            .map { list -> list.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getActiveRemindersForVehicle(vehicleId: String): Flow<List<Reminder>> {
        return reminderDao.getActiveRemindersForVehicle(vehicleId)
            .map { list -> list.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getReminderById(id: String): Flow<Reminder?> {
        return reminderDao.getReminderById(id)
            .map { it?.toDomainModel() }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getReminderByIdOnce(id: String): Reminder? = withContext(Dispatchers.IO) {
        reminderDao.getReminderByIdOnce(id)?.toDomainModel()
    }

    override suspend fun saveReminder(reminder: Reminder): Result<String> = withContext(Dispatchers.IO) {
        try {
            val reminderId = reminder.id.ifBlank { UUID.randomUUID().toString() }
            val now = Date()
            val toSave = reminder.copy(
                id = reminderId,
                createdAt = if (reminder.id.isBlank()) now else reminder.createdAt
            )

            reminderDao.insertReminder(toSave.toEntity())

            try {
                collection.document(reminderId).set(toSave).await()
            } catch (e: Exception) {
                // Offline fallback
            }

            Result.success(reminderId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateReminder(reminder: Reminder): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            reminderDao.insertReminder(reminder.toEntity())

            try {
                collection.document(reminder.id).set(reminder).await()
            } catch (e: Exception) {
                // Offline fallback
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteReminder(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            reminderDao.deleteReminderById(id)

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

    override suspend fun completeReminder(
        reminderId: String,
        currentOdometer: Double,
        completionDate: Date
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = reminderDao.getReminderByIdOnce(reminderId)?.toDomainModel()
                ?: return@withContext Result.failure(IllegalArgumentException("Reminder not found: $reminderId"))

            val updated = if (existing.isRecurring) {
                val nextOdometer = existing.intervalKm?.let { currentOdometer + it }
                val nextDate = existing.intervalDays?.let { days ->
                    Calendar.getInstance().apply {
                        time = completionDate
                        add(Calendar.DAY_OF_YEAR, days)
                    }.time
                }

                existing.copy(
                    lastServiceDate = completionDate,
                    lastServiceOdometer = currentOdometer,
                    nextDueDate = nextDate,
                    nextDueOdometer = nextOdometer,
                    isActive = true
                )
            } else {
                existing.copy(
                    lastServiceDate = completionDate,
                    lastServiceOdometer = currentOdometer,
                    isActive = false
                )
            }

            reminderDao.insertReminder(updated.toEntity())

            try {
                collection.document(reminderId).set(updated).await()
            } catch (e: Exception) {
                // Offline fallback
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncReminders(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val snapshot = collection.whereEqualTo("userId", userId).get().await()
            val remoteReminders = snapshot.documents.mapNotNull { it.toObject(Reminder::class.java) }
            val entities = remoteReminders.map { it.toEntity() }

            if (entities.isNotEmpty()) {
                reminderDao.insertReminders(entities)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
