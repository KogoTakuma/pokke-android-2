package com.kumanodormitory.pokke.data.repository

import com.kumanodormitory.pokke.data.local.dao.DutyPersonDao
import com.kumanodormitory.pokke.data.local.entity.DutyPersonEntity
import kotlinx.coroutines.flow.Flow

class DutyPersonRepository(private val dutyPersonDao: DutyPersonDao) {

    fun getCurrentDutyPerson(): Flow<DutyPersonEntity?> =
        dutyPersonDao.getCurrent()

    suspend fun changeDutyPerson(newDutyPerson: DutyPersonEntity) {
        dutyPersonDao.replaceCurrent(newDutyPerson)
    }
}
