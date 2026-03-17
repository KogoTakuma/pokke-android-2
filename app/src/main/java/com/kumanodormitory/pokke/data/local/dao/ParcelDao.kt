package com.kumanodormitory.pokke.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kumanodormitory.pokke.data.local.entity.ParcelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParcelDao {
    @Query("SELECT * FROM parcels WHERE status = 'registered' ORDER BY registeredAt DESC")
    fun getActiveParcels(): Flow<List<ParcelEntity>>

    @Query("SELECT * FROM parcels ORDER BY registeredAt DESC")
    fun getAllParcels(): Flow<List<ParcelEntity>>

    @Query("SELECT * FROM parcels WHERE id = :id")
    suspend fun getById(id: Long): ParcelEntity?

    @Insert
    suspend fun insert(parcel: ParcelEntity): Long

    @Update
    suspend fun update(parcel: ParcelEntity)

    @Query("SELECT * FROM parcels WHERE recipientName LIKE '%' || :query || '%' OR roomNumber LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<ParcelEntity>>
}
