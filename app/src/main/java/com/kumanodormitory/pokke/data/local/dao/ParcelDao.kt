package com.kumanodormitory.pokke.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kumanodormitory.pokke.data.local.entity.ParcelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParcelDao {

    @Query("SELECT * FROM parcels WHERE status = 'REGISTERED' ORDER BY created_at DESC")
    fun getRegistered(): Flow<List<ParcelEntity>>

    @Query("SELECT * FROM parcels WHERE status = 'REGISTERED' AND owner_block LIKE :building || '%' ORDER BY created_at DESC")
    fun getRegisteredByBuilding(building: String): Flow<List<ParcelEntity>>

    @Query("SELECT * FROM parcels WHERE ryosei_id = :ryoseiId AND status = 'REGISTERED' ORDER BY created_at DESC")
    fun getRegisteredByRyosei(ryoseiId: String): Flow<List<ParcelEntity>>

    @Query("SELECT * FROM parcels ORDER BY created_at DESC")
    fun getAll(): Flow<List<ParcelEntity>>

    @Query("SELECT * FROM parcels WHERE id = :id")
    suspend fun getById(id: String): ParcelEntity?

    @Insert
    suspend fun insert(parcel: ParcelEntity)

    @Update
    suspend fun update(parcel: ParcelEntity)

    @Query("DELETE FROM parcels WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query(
        """
        SELECT * FROM parcels
        WHERE created_at BETWEEN :fromMillis AND :toMillis
        ORDER BY created_at DESC
        """
    )
    fun getByDateRange(fromMillis: Long, toMillis: Long): Flow<List<ParcelEntity>>

    @Query(
        """
        SELECT * FROM parcels
        WHERE created_at BETWEEN :fromMillis AND :toMillis
          AND owner_block LIKE :building || '%'
        ORDER BY created_at DESC
        """
    )
    fun getByDateRangeAndBuilding(fromMillis: Long, toMillis: Long, building: String): Flow<List<ParcelEntity>>

    @Query("SELECT * FROM parcels WHERE is_lost = 1 ORDER BY created_at DESC")
    fun getLostParcels(): Flow<List<ParcelEntity>>

    @Query("UPDATE parcels SET last_confirmed_at = :confirmedAt WHERE id IN (:parcelIds)")
    suspend fun updateLastConfirmedAt(parcelIds: List<String>, confirmedAt: Long)
}
