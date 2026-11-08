package com.netcar.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.netcar.app.data.local.dao.FillUpDao
import com.netcar.app.data.local.dao.VehicleDao
import com.netcar.app.data.local.entity.toEntity
import com.netcar.app.data.model.FillUp
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

interface FillUpRepository {
    fun getFillUpsForVehicle(vehicleId: String): Flow<List<FillUp>>
    fun getAllFillUps(userId: String): Flow<List<FillUp>>
    fun getFillUpById(id: String): Flow<FillUp?>
    suspend fun getFillUpByIdOnce(id: String): FillUp?
    suspend fun saveFillUp(fillUp: FillUp): Result<String>
    suspend fun updateFillUp(fillUp: FillUp): Result<Unit>
    suspend fun deleteFillUp(id: String): Result<Unit>
    suspend fun syncFillUps(userId: String): Result<Unit>
}

@Singleton
class FillUpRepositoryImpl @Inject constructor(
    private val fillUpDao: FillUpDao,
    private val vehicleDao: VehicleDao,
    private val firestore: FirebaseFirestore
) : FillUpRepository {

    private val collection = firestore.collection("fillups")

    override fun getFillUpsForVehicle(vehicleId: String): Flow<List<FillUp>> {
        return fillUpDao.getFillUpsForVehicle(vehicleId)
            .map { list -> list.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getAllFillUps(userId: String): Flow<List<FillUp>> {
        return fillUpDao.getAllFillUps(userId)
            .map { list -> list.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getFillUpById(id: String): Flow<FillUp?> {
        return fillUpDao.getFillUpById(id)
            .map { it?.toDomainModel() }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getFillUpByIdOnce(id: String): FillUp? = withContext(Dispatchers.IO) {
        fillUpDao.getFillUpByIdOnce(id)?.toDomainModel()
    }

    override suspend fun saveFillUp(fillUp: FillUp): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fillUpId = fillUp.id.ifBlank { UUID.randomUUID().toString() }
            val now = Date()
            val toSave = fillUp.copy(
                id = fillUpId,
                createdAt = if (fillUp.id.isBlank()) now else fillUp.createdAt
            )

            // Save to local cache
            fillUpDao.insertFillUp(toSave.toEntity())

            // Update vehicle current odometer if higher
            val vehicle = vehicleDao.getVehicleByIdOnce(toSave.vehicleId)
            if (vehicle != null && toSave.odometer > vehicle.currentOdometer) {
                vehicleDao.updateOdometer(vehicle.id, toSave.odometer, now)
                try {
                    firestore.collection("vehicles")
                        .document(vehicle.id)
                        .update(mapOf("currentOdometer" to toSave.odometer, "updatedAt" to now))
                        .await()
                } catch (e: Exception) {
                    // Ignored if offline
                }
            }

            // Sync to Firestore
            try {
                collection.document(fillUpId).set(toSave).await()
            } catch (e: Exception) {
                // Offline fallback
            }

            Result.success(fillUpId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateFillUp(fillUp: FillUp): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            fillUpDao.insertFillUp(fillUp.toEntity())

            try {
                collection.document(fillUp.id).set(fillUp).await()
            } catch (e: Exception) {
                // Offline fallback
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteFillUp(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            fillUpDao.deleteFillUpById(id)

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

    override suspend fun syncFillUps(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val snapshot = collection.whereEqualTo("userId", userId).get().await()
            val remoteFillUps = snapshot.documents.mapNotNull { it.toObject(FillUp::class.java) }
            val entities = remoteFillUps.map { it.toEntity() }

            if (entities.isNotEmpty()) {
                fillUpDao.insertFillUps(entities)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
