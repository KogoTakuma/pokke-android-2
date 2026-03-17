package com.kumanodormitory.pokke.data.repository

import com.kumanodormitory.pokke.data.local.dao.DutyPersonDao
import com.kumanodormitory.pokke.data.local.entity.DutyPersonEntity
import kotlinx.coroutines.flow.Flow

class DutyPersonRepository(private val dutyPersonDao: DutyPersonDao) {

    fun getCurrentDutyPerson(): Flow<DutyPersonEntity?> =
        dutyPersonDao.get()

    suspend fun changeDutyPerson(name: String, updatedAt: Long) {
        val entity = DutyPersonEntity(
            id = "duty_person",
            name = name,
            updatedAt = updatedAt
        )
        dutyPersonDao.upsert(entity)
    }
}
