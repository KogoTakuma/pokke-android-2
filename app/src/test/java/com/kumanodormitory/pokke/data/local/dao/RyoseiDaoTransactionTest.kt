package com.kumanodormitory.pokke.data.local.dao

import com.kumanodormitory.pokke.data.local.entity.RyoseiEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Unit test for RyoseiDao.replaceAll @Transaction logic.
 *
 * Actual DB transaction/rollback is Room's guarantee (requires instrumented test).
 * This test verifies:
 *   1. insertAll exception propagates out of replaceAll (transaction would rollback in Room)
 *   2. deleteAll is called before insertAll
 */
class RyoseiDaoTransactionTest {

    private fun makeEntity(id: String) = RyoseiEntity(
        id = id,
        name = "テスト",
        nameKana = "テスト",
        nameAlphabet = "test",
        room = "101",
        block = "A"
    )

    private inner class FakeRyoseiDao(
        private val throwOnInsert: Boolean = false
    ) : RyoseiDao {
        val callOrder = mutableListOf<String>()
        val stored = mutableListOf<RyoseiEntity>()

        override suspend fun deleteAll() {
            callOrder += "deleteAll"
            stored.clear()
        }

        override suspend fun insertAll(ryoseiList: List<RyoseiEntity>) {
            callOrder += "insertAll"
            if (throwOnInsert) throw RuntimeException("DB full")
            stored.addAll(ryoseiList)
        }

        // --- stubs for unused interface members ---
        override fun getAll(): Flow<List<RyoseiEntity>> = flowOf(stored.toList())
        override fun getByBlock(block: String): Flow<List<RyoseiEntity>> = flowOf(emptyList())
        override fun getByRoom(room: String): Flow<List<RyoseiEntity>> = flowOf(emptyList())
        override fun search(query: String): Flow<List<RyoseiEntity>> = flowOf(emptyList())
        override fun searchIncludingLeft(query: String): Flow<List<RyoseiEntity>> = flowOf(emptyList())
        override fun getRyoseiWithParcels(): Flow<List<RyoseiEntity>> = flowOf(emptyList())
        override fun getAllBlocks(): Flow<List<String>> = flowOf(emptyList())
        override fun getRoomsByBlock(block: String): Flow<List<String>> = flowOf(emptyList())
        override fun getByNonAlphanumericRoom(): Flow<List<RyoseiEntity>> = flowOf(emptyList())
        override fun getNonAlphanumericRooms(): Flow<List<String>> = flowOf(emptyList())
        override suspend fun getById(id: String): RyoseiEntity? = null
        override suspend fun insert(ryosei: RyoseiEntity) = Unit
        override suspend fun update(ryosei: RyoseiEntity) = Unit
        override suspend fun deleteByIds(ids: List<String>) = Unit
    }

    @Test
    fun `replaceAll calls deleteAll then insertAll in order`() = runBlocking {
        val dao = FakeRyoseiDao(throwOnInsert = false)
        dao.replaceAll(listOf(makeEntity("1"), makeEntity("2")))

        assertTrue(
            "deleteAll must be called before insertAll",
            dao.callOrder == listOf("deleteAll", "insertAll")
        )
        assertTrue(dao.stored.size == 2)
    }

    @Test
    fun `replaceAll propagates insertAll exception (Room would rollback)`() = runBlocking {
        val dao = FakeRyoseiDao(throwOnInsert = true)
        try {
            dao.replaceAll(listOf(makeEntity("1")))
            fail("Expected RuntimeException to be propagated")
        } catch (e: RuntimeException) {
            assertTrue("insertAll must have been attempted", dao.callOrder.contains("insertAll"))
        }
        Unit
    }
}
