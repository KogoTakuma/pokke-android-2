package com.kumanodormitory.pokke.data.local.dao

import com.kumanodormitory.pokke.data.local.entity.DutyPersonEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class DutyPersonDaoTransactionTest {

    private inner class FakeDutyPersonDao : DutyPersonDao {
        private val storage = mutableListOf<DutyPersonEntity>()
        var upsertShouldThrow = false

        override fun getCurrent(): Flow<DutyPersonEntity?> = flowOf(storage.firstOrNull())
        override suspend fun getCurrentSync(): DutyPersonEntity? = storage.firstOrNull()

        override suspend fun upsert(dutyPerson: DutyPersonEntity) {
            if (upsertShouldThrow) throw RuntimeException("DB constraint violation")
            storage.removeAll { true }
            storage.add(dutyPerson)
        }

        override suspend fun deleteAll() {
            storage.clear()
        }

        // Simulate Room @Transaction rollback: restore snapshot on failure
        override suspend fun replaceCurrent(newDutyPerson: DutyPersonEntity) {
            val snapshot = storage.toList()
            try {
                deleteAll()
                upsert(newDutyPerson)
            } catch (e: Exception) {
                storage.clear()
                storage.addAll(snapshot)
                throw e
            }
        }

        fun getAll(): List<DutyPersonEntity> = storage.toList()

        fun seed(entity: DutyPersonEntity) {
            storage.add(entity)
        }
    }

    @Test
    fun `replaceCurrent rolls back deleteAll when upsert throws`() = runTest {
        val dao = FakeDutyPersonDao()
        val original = DutyPersonEntity(id = "duty_person", name = "田中 太郎", updatedAt = 1000L)
        dao.seed(original)
        dao.upsertShouldThrow = true

        try {
            dao.replaceCurrent(DutyPersonEntity(id = "duty_person", name = "鈴木 花子", updatedAt = 2000L))
            fail("Expected RuntimeException to be propagated")
        } catch (_: RuntimeException) {
            // Expected
        }

        val remaining = dao.getAll()
        assertEquals(1, remaining.size)
        assertEquals("田中 太郎", remaining.first().name)
    }
}
