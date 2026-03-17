package com.kumanodormitory.pokke.data.repository

import com.kumanodormitory.pokke.data.local.dao.OperationLogDao
import com.kumanodormitory.pokke.data.local.entity.OperationLogEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class OperationLogRepository(private val operationLogDao: OperationLogDao) {

    fun getRecentLogs(limit: Int = 50): Flow<List<OperationLogEntity>> =
        operationLogDao.getRecent(limit)

    suspend fun addLog(
        type: String,
        parcelId: String?,
        operatedByName: String?,
        metadata: String?
    ) {
        val entity = OperationLogEntity(
            id = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            parcelId = parcelId,
            operationType = type,
            operatedByName = operatedByName,
            metadata = metadata
        )
        operationLogDao.insert(entity)
    }

    fun getLogsByDateRangeAndBlock(
        startDate: Long,
        endDate: Long,
        block: String?
    ): Flow<List<OperationLogEntity>> =
        if (block != null) {
            operationLogDao.getByDateRangeAndBlock(startDate, endDate, block)
        } else {
            operationLogDao.getByDateRange(startDate, endDate)
        }
}
