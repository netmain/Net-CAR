package com.netcar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.netcar.app.data.local.entity.ServiceEntity
import com.netcar.app.data.model.ServiceType
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceDao {

    @Query("SELECT * FROM services WHERE vehicleId = :vehicleId ORDER BY date DESC, odometer DESC")
    fun getServicesForVehicle(vehicleId: String): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM services WHERE userId = :userId ORDER BY date DESC")
    fun getAllServices(userId: String): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM services WHERE vehicleId = :vehicleId AND type = :type ORDER BY date DESC")
    fun getServicesByType(vehicleId: String, type: ServiceType): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM services WHERE id = :id LIMIT 1")
    fun getServiceById(id: String): Flow<ServiceEntity?>

    @Query("SELECT * FROM services WHERE id = :id LIMIT 1")
    suspend fun getServiceByIdOnce(id: String): ServiceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: ServiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServices(services: List<ServiceEntity>)

    @Update
    suspend fun updateService(service: ServiceEntity)

    @Delete
    suspend fun deleteService(service: ServiceEntity)

    @Query("DELETE FROM services WHERE id = :id")
    suspend fun deleteServiceById(id: String)

    @Query("DELETE FROM services WHERE vehicleId = :vehicleId")
    suspend fun deleteServicesForVehicle(vehicleId: String)

    @Query("DELETE FROM services WHERE userId = :userId")
    suspend fun clearServices(userId: String)
}
