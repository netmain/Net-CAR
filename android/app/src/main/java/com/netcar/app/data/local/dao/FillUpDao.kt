package com.netcar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.netcar.app.data.local.entity.FillUpEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FillUpDao {

    @Query("SELECT * FROM fillups WHERE vehicleId = :vehicleId ORDER BY date DESC, odometer DESC")
    fun getFillUpsForVehicle(vehicleId: String): Flow<List<FillUpEntity>>

    @Query("SELECT * FROM fillups WHERE userId = :userId ORDER BY date DESC")
    fun getAllFillUps(userId: String): Flow<List<FillUpEntity>>

    @Query("SELECT * FROM fillups WHERE id = :id LIMIT 1")
    fun getFillUpById(id: String): Flow<FillUpEntity?>

    @Query("SELECT * FROM fillups WHERE id = :id LIMIT 1")
    suspend fun getFillUpByIdOnce(id: String): FillUpEntity?

    @Query("SELECT * FROM fillups WHERE vehicleId = :vehicleId ORDER BY date DESC, odometer DESC LIMIT 1")
    suspend fun getLatestFillUp(vehicleId: String): FillUpEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFillUp(fillUp: FillUpEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFillUps(fillUps: List<FillUpEntity>)

    @Update
    suspend fun updateFillUp(fillUp: FillUpEntity)

    @Delete
    suspend fun deleteFillUp(fillUp: FillUpEntity)

    @Query("DELETE FROM fillups WHERE id = :id")
    suspend fun deleteFillUpById(id: String)

    @Query("DELETE FROM fillups WHERE vehicleId = :vehicleId")
    suspend fun deleteFillUpsForVehicle(vehicleId: String)

    @Query("DELETE FROM fillups WHERE userId = :userId")
    suspend fun clearFillUps(userId: String)
}
