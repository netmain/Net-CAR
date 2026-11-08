package com.netcar.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.netcar.app.data.local.dao.VehicleDao
import com.netcar.app.data.local.entity.toEntity
import com.netcar.app.data.model.Vehicle
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

interface VehicleRepository {
    fun getVehicles(userId: String): Flow<List<Vehicle>>
    fun getVehicleById(id: String): Flow<Vehicle?>
    suspend fun getVehicleByIdOnce(id: String): Vehicle?
    suspend fun saveVehicle(vehicle: Vehicle): Result<String>
    suspend fun updateVehicle(vehicle: Vehicle): Result<Unit>
    suspend fun deleteVehicle(vehicleId: String): Result<Unit>
    suspend fun syncVehicles(userId: String): Result<Unit>
}

@Singleton
class VehicleRepositoryImpl @Inject constructor(
    private val vehicleDao: VehicleDao,
    private val firestore: FirebaseFirestore
) : VehicleRepository {

    private val collection = firestore.collection("vehicles")

    override fun getVehicles(userId: String): Flow<List<Vehicle>> {
        return vehicleDao.getAllVehicles(userId)
            .map { list -> list.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getVehicleById(id: String): Flow<Vehicle?> {
        return vehicleDao.getVehicleById(id)
            .map { it?.toDomainModel() }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getVehicleByIdOnce(id: String): Vehicle? = withContext(Dispatchers.IO) {
        vehicleDao.getVehicleByIdOnce(id)?.toDomainModel()
    }

    override suspend fun saveVehicle(vehicle: Vehicle): Result<String> = withContext(Dispatchers.IO) {
        try {
            val vehicleId = vehicle.id.ifBlank { UUID.randomUUID().toString() }
            val now = Date()
            val toSave = vehicle.copy(
                id = vehicleId,
                createdAt = if (vehicle.id.isBlank()) now else vehicle.createdAt,
                updatedAt = now
            )

            // Local cache first for instant UI response and offline capability
            vehicleDao.insertVehicle(toSave.toEntity())

            // Sync to remote Firestore
            try {
                collection.document(vehicleId).set(toSave).await()
            } catch (e: Exception) {
                // If offline, Firestore will queue or fail gracefully, local write succeeded
            }

            Result.success(vehicleId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateVehicle(vehicle: Vehicle): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val toUpdate = vehicle.copy(updatedAt = Date())
            vehicleDao.insertVehicle(toUpdate.toEntity())

            try {
                collection.document(vehicle.id).set(toUpdate).await()
            } catch (e: Exception) {
                // Fallback to local
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteVehicle(vehicleId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            vehicleDao.deleteVehicleById(vehicleId)

            try {
                collection.document(vehicleId).delete().await()
            } catch (e: Exception) {
                // Ignore remote deletion error if offline
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncVehicles(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val snapshot = collection.whereEqualTo("userId", userId).get().await()
            val remoteVehicles = snapshot.documents.mapNotNull { it.toObject(Vehicle::class.java) }
            val entities = remoteVehicles.map { it.toEntity() }

            if (entities.isNotEmpty()) {
                vehicleDao.insertVehicles(entities)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
