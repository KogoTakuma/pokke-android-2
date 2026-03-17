package com.kumanodormitory.pokke.data.repository

import com.kumanodormitory.pokke.data.local.dao.ParcelDao
import com.kumanodormitory.pokke.data.local.entity.ParcelEntity
import kotlinx.coroutines.flow.Flow

class ParcelRepository(private val parcelDao: ParcelDao) {

    fun getActiveParcels(): Flow<List<ParcelEntity>> = parcelDao.getActiveParcels()

    fun getAllParcels(): Flow<List<ParcelEntity>> = parcelDao.getAllParcels()

    suspend fun getById(id: Long): ParcelEntity? = parcelDao.getById(id)

    suspend fun registerParcel(parcel: ParcelEntity): Long = parcelDao.insert(parcel)

    suspend fun releaseParcel(id: Long, releasedTo: String, isProxy: Boolean = false) {
        val parcel = parcelDao.getById(id) ?: return
        parcelDao.update(
            parcel.copy(
                status = "released",
                releasedAt = System.currentTimeMillis(),
                releasedTo = releasedTo,
                isProxy = isProxy
            )
        )
    }

    fun search(query: String): Flow<List<ParcelEntity>> = parcelDao.search(query)
}
