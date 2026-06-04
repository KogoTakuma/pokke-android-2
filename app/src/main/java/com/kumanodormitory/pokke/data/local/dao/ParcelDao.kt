package com.kumanodormitory.pokke.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.kumanodormitory.pokke.data.local.entity.ParcelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParcelDao {

    companion object {
        const val BATCH_SIZE = 100
    }

    @Query("SELECT * FROM parcels WHERE status = 'REGISTERED' AND lost_confirmed_at IS NULL ORDER BY created_at DESC")
    fun getRegistered(): Flow<List<ParcelEntity>>

    @Query("SELECT * FROM parcels WHERE status = 'REGISTERED' AND lost_confirmed_at IS NULL AND owner_block LIKE :building || '%' ORDER BY created_at DESC")
    fun getRegisteredByBuilding(building: String): Flow<List<ParcelEntity>>

    @Query("SELECT * FROM parcels WHERE ryosei_id = :ryoseiId AND status = 'REGISTERED' AND lost_confirmed_at IS NULL ORDER BY created_at DESC")
    fun getRegisteredByRyosei(ryoseiId: String): Flow<List<ParcelEntity>>

    @Query("SELECT * FROM parcels ORDER BY created_at DESC")
    fun getAll(): Flow<List<ParcelEntity>>

    @Query("SELECT * FROM parcels ORDER BY created_at ASC")
    suspend fun getAllSync(): List<ParcelEntity>

    @Query("SELECT * FROM parcels WHERE id = :id")
    suspend fun getById(id: String): ParcelEntity?

    @Insert
    suspend fun insert(parcel: ParcelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllBatch(parcels: List<ParcelEntity>)

    @Transaction
    suspend fun upsertAll(parcels: List<ParcelEntity>) {
        parcels.chunked(BATCH_SIZE).forEach { batch ->
            upsertAllBatch(batch)
        }
    }

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

    @Query("SELECT * FROM parcels WHERE is_lost = 1 AND lost_confirmed_at IS NULL ORDER BY created_at DESC")
    fun getLostParcels(): Flow<List<ParcelEntity>>

    @Query("UPDATE parcels SET lost_confirmed_at = :confirmedAt, updated_at = :confirmedAt, synced_at = NULL WHERE id IN (:parcelIds)")
    suspend fun archiveLostParcelsBatch(parcelIds: List<String>, confirmedAt: Long)

    @Transaction
    suspend fun archiveLostParcels(parcelIds: List<String>, confirmedAt: Long) {
        parcelIds.chunked(BATCH_SIZE).forEach { batch ->
            archiveLostParcelsBatch(batch, confirmedAt)
        }
    }

    @Query("SELECT * FROM parcels WHERE is_lost = 1 AND lost_confirmed_at IS NOT NULL ORDER BY lost_confirmed_at DESC")
    fun getArchivedLostParcels(): Flow<List<ParcelEntity>>

    @Query("UPDATE parcels SET last_confirmed_at = :confirmedAt WHERE id IN (:parcelIds)")
    suspend fun updateLastConfirmedAtBatch(parcelIds: List<String>, confirmedAt: Long)

    suspend fun updateLastConfirmedAt(parcelIds: List<String>, confirmedAt: Long) {
        parcelIds.chunked(BATCH_SIZE).forEach { batch ->
            updateLastConfirmedAtBatch(batch, confirmedAt)
        }
    }

    @Query(
        """
        SELECT * FROM parcels
        WHERE synced_at IS NULL
          AND created_at < :olderThan
        ORDER BY created_at ASC
        LIMIT 50
        """
    )
    suspend fun getUnsyncedParcels(olderThan: Long): List<ParcelEntity>

    @Query("UPDATE parcels SET synced_at = :syncedAt WHERE id IN (:ids)")
    suspend fun updateSyncedAtBatch(ids: List<String>, syncedAt: Long)

    suspend fun updateSyncedAt(ids: List<String>, syncedAt: Long) {
        ids.chunked(BATCH_SIZE).forEach { batch ->
            updateSyncedAtBatch(batch, syncedAt)
        }
    }
}
