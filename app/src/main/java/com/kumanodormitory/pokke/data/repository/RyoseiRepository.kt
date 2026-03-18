package com.kumanodormitory.pokke.data.repository

import com.kumanodormitory.pokke.data.local.dao.RyoseiDao
import com.kumanodormitory.pokke.data.local.entity.RyoseiEntity
import kotlinx.coroutines.flow.Flow

class RyoseiRepository(private val ryoseiDao: RyoseiDao) {

    fun getByBlock(block: String): Flow<List<RyoseiEntity>> =
        ryoseiDao.getByBlock(block)

    fun getByRoom(room: String): Flow<List<RyoseiEntity>> =
        ryoseiDao.getByRoom(room)

    fun search(query: String): Flow<List<RyoseiEntity>> =
        ryoseiDao.search(query)

    fun getRyoseiWithParcels(): Flow<List<RyoseiEntity>> =
        ryoseiDao.getRyoseiWithParcels()

    fun getAllBlocks(): Flow<List<String>> =
        ryoseiDao.getAllBlocks()

    fun getRoomsByBlock(block: String): Flow<List<String>> =
        ryoseiDao.getRoomsByBlock(block)

    suspend fun insertAll(ryoseiList: List<RyoseiEntity>) {
        ryoseiDao.insertAll(ryoseiList)
    }

    suspend fun deleteSeedData() {
        ryoseiDao.deleteByIds(com.kumanodormitory.pokke.data.local.SeedData.SEED_RYOSEI_IDS)
    }

    suspend fun replaceAll(ryoseiList: List<RyoseiEntity>) {
        ryoseiDao.deleteAll()
        ryoseiDao.insertAll(ryoseiList)
    }
}
