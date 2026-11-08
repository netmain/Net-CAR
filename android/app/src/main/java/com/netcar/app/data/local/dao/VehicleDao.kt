package com.netcar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.netcar.app.data.local.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface VehicleDao {

    @Query("SELECT * FROM vehicles WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getAllVehicles(userId: String): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    fun getVehicleById(id: String): Flow<VehicleEntity?>

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    suspend fun getVehicleByIdOnce(id: String): VehicleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: VehicleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicles(vehicles: List<VehicleEntity>)

    @Update
    suspend fun updateVehicle(vehicle: VehicleEntity)

    @Delete
    suspend fun deleteVehicle(vehicle: VehicleEntity)

    @Query("DELETE FROM vehicles WHERE id = :id")
    suspend fun deleteVehicleById(id: String)

    @Query("DELETE FROM vehicles WHERE userId = :userId")
    suspend fun clearVehicles(userId: String)

    @Query("UPDATE vehicles SET currentOdometer = :odometer, updatedAt = :updatedAt WHERE id = :vehicleId")
    suspend fun updateOdometer(vehicleId: String, odometer: Double, updatedAt: Date = Date())
}
