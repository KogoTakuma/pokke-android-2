package com.kumanodormitory.pokke.data.repository

import com.kumanodormitory.pokke.data.local.dao.ParcelDao
import com.kumanodormitory.pokke.data.local.entity.ParcelEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ParcelRepository(private val parcelDao: ParcelDao) {

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

    suspend fun deliverParcel(parcelId: String, deliveredByName: String) {
        val parcel = parcelDao.getById(parcelId) ?: return
        parcelDao.update(
            parcel.copy(
                status = "RECEIVED",
                deliveredAt = System.currentTimeMillis(),
                deliveredByName = deliveredByName,
                updatedAt = System.currentTimeMillis()
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
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun markLost(parcelId: String) {
        val parcel = parcelDao.getById(parcelId) ?: return
        parcelDao.update(
            parcel.copy(
                isLost = true,
                updatedAt = System.currentTimeMillis()
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
                    updatedAt = now
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
}
