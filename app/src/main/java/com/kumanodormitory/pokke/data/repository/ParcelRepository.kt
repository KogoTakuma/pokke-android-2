package com.kumanodormitory.pokke.data.repository

import com.kumanodormitory.pokke.data.local.PokkeDatabase
import com.kumanodormitory.pokke.data.local.dao.ParcelDao
import com.kumanodormitory.pokke.data.local.entity.OperationLogEntity
import com.kumanodormitory.pokke.data.local.entity.ParcelEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ParcelRepository(
    private val parcelDao: ParcelDao,
    private val pokkeDatabase: PokkeDatabase
) {

    fun getRegisteredParcels(): Flow<List<ParcelEntity>> =
        parcelDao.getRegistered()

    fun getParcelsByRyosei(ryoseiId: String): Flow<List<ParcelEntity>> =
        parcelDao.getRegisteredByRyosei(ryoseiId)

    suspend fun registerParcel(parcel: ParcelEntity): String {
        val id = UUID.randomUUID().toString()
        val entityWithId = parcel.copy(id = id)
        parcelDao.insert(entityWithId)
        return id
    }

    suspend fun registerParcelWithLog(parcel: ParcelEntity, log: OperationLogEntity) {
        pokkeDatabase.registerParcelWithLog(parcel, log)
    }

    suspend fun deliverParcelsWithLogs(
        updatedParcels: List<ParcelEntity>,
        logs: List<OperationLogEntity>
    ) {
        pokkeDatabase.deliverParcelsWithLogs(updatedParcels, logs)
    }

    suspend fun completeNightDutyAtomic(
        confirmedParcels: List<ParcelEntity>,
        lostUpdates: List<ParcelEntity>,
        lostLogs: List<OperationLogEntity>,
        nightDutyLog: OperationLogEntity
    ) {
        pokkeDatabase.completeNightDutyAtomic(confirmedParcels, lostUpdates, lostLogs, nightDutyLog)
    }

    suspend fun deliverParcel(parcelId: String, deliveredByName: String) {
        val parcel = parcelDao.getById(parcelId) ?: return
        parcelDao.update(
            parcel.copy(
                status = "RECEIVED",
                deliveredAt = System.currentTimeMillis(),
                deliveredByName = deliveredByName,
                updatedAt = System.currentTimeMillis(),
                syncedAt = null
            )
        )
    }

    suspend fun cancelRegister(parcelId: String) {
        parcelDao.deleteById(parcelId)
    }

    suspend fun cancelDeliver(parcelId: String) {
        val parcel = parcelDao.getById(parcelId) ?: return
        parcelDao.update(
            parcel.copy(
                status = "REGISTERED",
                deliveredAt = null,
                deliveredByName = null,
                updatedAt = System.currentTimeMillis(),
                syncedAt = null
            )
        )
    }

    suspend fun markLost(parcelId: String) {
        val parcel = parcelDao.getById(parcelId) ?: return
        parcelDao.update(
            parcel.copy(
                isLost = true,
                updatedAt = System.currentTimeMillis(),
                syncedAt = null
            )
        )
    }

    suspend fun confirmNightDuty(parcelIds: List<String>) {
        val now = System.currentTimeMillis()
        for (parcelId in parcelIds) {
            val parcel = parcelDao.getById(parcelId) ?: continue
            parcelDao.update(
                parcel.copy(
                    lastConfirmedAt = now,
                    updatedAt = now,
                    syncedAt = null
                )
            )
        }
    }

    fun getParcelsByDateRangeAndBlock(
        startDate: Long,
        endDate: Long,
        block: String?
    ): Flow<List<ParcelEntity>> =
        if (block != null) {
            parcelDao.getByDateRangeAndBuilding(startDate, endDate, block)
        } else {
            parcelDao.getByDateRange(startDate, endDate)
        }

    fun getLostParcels(): Flow<List<ParcelEntity>> =
        parcelDao.getLostParcels()

    suspend fun archiveLostParcels(parcelIds: List<String>) {
        parcelDao.archiveLostParcels(parcelIds, System.currentTimeMillis())
    }

    fun getArchivedLostParcels(): Flow<List<ParcelEntity>> =
        parcelDao.getArchivedLostParcels()

    suspend fun getUnsyncedParcels(olderThan: Long): List<ParcelEntity> =
        parcelDao.getUnsyncedParcels(olderThan)

    suspend fun getAllParcels(): List<ParcelEntity> =
        parcelDao.getAllSync()

    suspend fun updateSyncedAt(ids: List<String>) {
        parcelDao.updateSyncedAt(ids, System.currentTimeMillis())
    }
}
