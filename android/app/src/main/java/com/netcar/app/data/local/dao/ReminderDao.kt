package com.netcar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.netcar.app.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE vehicleId = :vehicleId ORDER BY nextDueDate ASC, nextDueOdometer ASC")
    fun getRemindersForVehicle(vehicleId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE userId = :userId ORDER BY nextDueDate ASC, nextDueOdometer ASC")
    fun getAllReminders(userId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE userId = :userId AND isActive = 1 ORDER BY nextDueDate ASC, nextDueOdometer ASC")
    fun getActiveReminders(userId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE vehicleId = :vehicleId AND isActive = 1 ORDER BY nextDueDate ASC, nextDueOdometer ASC")
    fun getActiveRemindersForVehicle(vehicleId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    fun getReminderById(id: String): Flow<ReminderEntity?>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderByIdOnce(id: String): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderEntity>)

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: String)

    @Query("DELETE FROM reminders WHERE vehicleId = :vehicleId")
    suspend fun deleteRemindersForVehicle(vehicleId: String)

    @Query("DELETE FROM reminders WHERE userId = :userId")
    suspend fun clearReminders(userId: String)
}
