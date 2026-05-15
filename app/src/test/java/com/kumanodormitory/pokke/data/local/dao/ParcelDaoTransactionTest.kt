package com.kumanodormitory.pokke.data.local.dao

import com.kumanodormitory.pokke.data.local.entity.ParcelEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ParcelDaoTransactionTest {

    private class FakeParcelDao(
        private val throwOnBatch: Int = Int.MAX_VALUE
    ) : ParcelDao {
        var batchCallCount = 0
        val processedIds = mutableListOf<String>()

        override suspend fun archiveLostParcelsBatch(parcelIds: List<String>, confirmedAt: Long) {
            batchCallCount++
            if (batchCallCount >= throwOnBatch) throw RuntimeException("Simulated DB error on batch $batchCallCount")
            processedIds.addAll(parcelIds)
        }

        override fun getRegistered(): Flow<List<ParcelEntity>> = emptyFlow()
        override fun getRegisteredByBuilding(building: String): Flow<List<ParcelEntity>> = emptyFlow()
        override fun getRegisteredByRyosei(ryoseiId: String): Flow<List<ParcelEntity>> = emptyFlow()
        override fun getAll(): Flow<List<ParcelEntity>> = emptyFlow()
        override suspend fun getAllSync(): List<ParcelEntity> = emptyList()
        override suspend fun getById(id: String): ParcelEntity? = null
        override suspend fun insert(parcel: ParcelEntity) {}
        override suspend fun update(parcel: ParcelEntity) {}
        override suspend fun deleteById(id: String) {}
        override fun getByDateRange(fromMillis: Long, toMillis: Long): Flow<List<ParcelEntity>> = emptyFlow()
        override fun getByDateRangeAndBuilding(fromMillis: Long, toMillis: Long, building: String): Flow<List<ParcelEntity>> = emptyFlow()
        override fun getLostParcels(): Flow<List<ParcelEntity>> = emptyFlow()
        override fun getArchivedLostParcels(): Flow<List<ParcelEntity>> = emptyFlow()
        override suspend fun updateLastConfirmedAtBatch(parcelIds: List<String>, confirmedAt: Long) {}
        override suspend fun updateSyncedAtBatch(ids: List<String>, syncedAt: Long) {}
        override suspend fun getUnsyncedParcels(olderThan: Long): List<ParcelEntity> = emptyList()
    }

    /**
     * @Transaction が付与された archiveLostParcels は、途中のバッチで例外が発生した場合に
     * 例外を呼び出し元へ伝播させる。実 DB では Room がトランザクション全体を rollback する。
     * 本テストはその前提条件（例外伝播・以降バッチ停止）を検証する。
     */
    @Test
    fun `archiveLostParcels propagates exception and stops further batches`() = runBlocking {
        val ids = (1..ParcelDao.BATCH_SIZE + 1).map { "id_$it" }
        val fakeDao = FakeParcelDao(throwOnBatch = 2)

        var thrown: Exception? = null
        try {
            fakeDao.archiveLostParcels(ids, 1000L)
        } catch (e: RuntimeException) {
            thrown = e
        }

        assertNotNull("Exception must propagate from archiveLostParcels", thrown)
        assertEquals("Only first batch should be processed", ParcelDao.BATCH_SIZE, fakeDao.processedIds.size)
        assertEquals("Second batch call should throw", 2, fakeDao.batchCallCount)
    }
}
