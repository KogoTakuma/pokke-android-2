package com.kumanodormitory.pokke.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kumanodormitory.pokke.data.local.PokkeDatabase
import com.kumanodormitory.pokke.data.local.entity.ParcelEntity
import com.kumanodormitory.pokke.data.remote.PokkeApiClient
import com.kumanodormitory.pokke.data.remote.dto.ParcelDto
import com.kumanodormitory.pokke.data.remote.dto.ParcelSyncRequest
import java.io.IOException

class ParcelSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = PokkeDatabase.getInstance(applicationContext)
            val parcelDao = db.parcelDao()

            val tenMinutesAgo = System.currentTimeMillis() - 10 * 60 * 1000
            val unsyncedParcels = parcelDao.getUnsyncedParcels(tenMinutesAgo)

            if (unsyncedParcels.isEmpty()) {
                Log.d(TAG, "No unsynced parcels found")
                return Result.success()
            }

            Log.d(TAG, "Syncing ${unsyncedParcels.size} parcels")

            val dtos = unsyncedParcels.map { it.toDto() }
            val response = PokkeApiClient.service.syncParcels(ParcelSyncRequest(parcels = dtos))

            if (response.isSuccessful) {
                val syncedIds = response.body()?.syncedIds ?: unsyncedParcels.map { it.id }
                if (syncedIds.isNotEmpty()) {
                    parcelDao.updateSyncedAt(syncedIds, System.currentTimeMillis())
                }
                Log.d(TAG, "Successfully synced ${syncedIds.size} parcels")
                Result.success()
            } else {
                Log.w(TAG, "Sync failed with code ${response.code()}")
                Result.retry()
            }
        } catch (e: IOException) {
            Log.w(TAG, "Sync failed due to network error", e)
            Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed with unexpected error", e)
            Result.failure()
        }
    }

    private fun ParcelEntity.toDto() = ParcelDto(
        id = id,
        createdAt = createdAt,
        updatedAt = updatedAt,
        ryoseiId = ryoseiId,
        ownerBlock = ownerBlock,
        ownerRoomName = ownerRoomName,
        ownerName = ownerName,
        parcelType = parcelType,
        note = note,
        status = status,
        isLost = isLost,
        registeredByName = registeredByName,
        deliveredAt = deliveredAt,
        deliveredByName = deliveredByName,
        lastConfirmedAt = lastConfirmedAt,
        lostConfirmedAt = lostConfirmedAt
    )

    companion object {
        const val WORK_NAME = "parcel_sync_worker"
        private const val TAG = "ParcelSyncWorker"
    }
}
