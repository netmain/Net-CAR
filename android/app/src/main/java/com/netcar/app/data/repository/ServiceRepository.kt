package com.netcar.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.netcar.app.data.local.dao.ServiceDao
import com.netcar.app.data.local.dao.VehicleDao
import com.netcar.app.data.local.entity.toEntity
import com.netcar.app.data.model.Service
import com.netcar.app.data.model.ServiceType
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

interface ServiceRepository {
    fun getServicesForVehicle(vehicleId: String): Flow<List<Service>>
    fun getAllServices(userId: String): Flow<List<Service>>
    fun getServicesByType(vehicleId: String, type: ServiceType): Flow<List<Service>>
    fun getServiceById(id: String): Flow<Service?>
    suspend fun getServiceByIdOnce(id: String): Service?
    suspend fun saveService(service: Service): Result<String>
    suspend fun updateService(service: Service): Result<Unit>
    suspend fun deleteService(id: String): Result<Unit>
    suspend fun syncServices(userId: String): Result<Unit>
}

@Singleton
class ServiceRepositoryImpl @Inject constructor(
    private val serviceDao: ServiceDao,
    private val vehicleDao: VehicleDao,
    private val firestore: FirebaseFirestore
) : ServiceRepository {

    private val collection = firestore.collection("services")

    override fun getServicesForVehicle(vehicleId: String): Flow<List<Service>> {
        return serviceDao.getServicesForVehicle(vehicleId)
            .map { list -> list.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getAllServices(userId: String): Flow<List<Service>> {
        return serviceDao.getAllServices(userId)
            .map { list -> list.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getServicesByType(vehicleId: String, type: ServiceType): Flow<List<Service>> {
        return serviceDao.getServicesByType(vehicleId, type)
            .map { list -> list.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getServiceById(id: String): Flow<Service?> {
        return serviceDao.getServiceById(id)
            .map { it?.toDomainModel() }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getServiceByIdOnce(id: String): Service? = withContext(Dispatchers.IO) {
        serviceDao.getServiceByIdOnce(id)?.toDomainModel()
    }

    override suspend fun saveService(service: Service): Result<String> = withContext(Dispatchers.IO) {
        try {
            val serviceId = service.id.ifBlank { UUID.randomUUID().toString() }
            val now = Date()
            val toSave = service.copy(
                id = serviceId,
                createdAt = if (service.id.isBlank()) now else service.createdAt
            )

            serviceDao.insertService(toSave.toEntity())

            // Update vehicle current odometer if service has a higher odometer reading
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

            try {
                collection.document(serviceId).set(toSave).await()
            } catch (e: Exception) {
                // Offline fallback
            }

            Result.success(serviceId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateService(service: Service): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            serviceDao.insertService(service.toEntity())

            try {
                collection.document(service.id).set(service).await()
            } catch (e: Exception) {
                // Offline fallback
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteService(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            serviceDao.deleteServiceById(id)

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

    override suspend fun syncServices(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val snapshot = collection.whereEqualTo("userId", userId).get().await()
            val remoteServices = snapshot.documents.mapNotNull { it.toObject(Service::class.java) }
            val entities = remoteServices.map { it.toEntity() }

            if (entities.isNotEmpty()) {
                serviceDao.insertServices(entities)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
