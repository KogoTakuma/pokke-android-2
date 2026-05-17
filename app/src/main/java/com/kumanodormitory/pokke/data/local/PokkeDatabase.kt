package com.kumanodormitory.pokke.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverters
import com.kumanodormitory.pokke.data.local.converter.Converters
import com.kumanodormitory.pokke.data.local.dao.DutyPersonDao
import com.kumanodormitory.pokke.data.local.dao.OperationLogDao
import com.kumanodormitory.pokke.data.local.dao.ParcelDao
import com.kumanodormitory.pokke.data.local.dao.RyoseiDao
import com.kumanodormitory.pokke.data.local.entity.DutyPersonEntity
import com.kumanodormitory.pokke.data.local.entity.OperationLogEntity
import com.kumanodormitory.pokke.data.local.entity.ParcelEntity
import com.kumanodormitory.pokke.data.local.entity.RyoseiEntity

@Database(
    entities = [
        RyoseiEntity::class,
        ParcelEntity::class,
        DutyPersonEntity::class,
        OperationLogEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PokkeDatabase : RoomDatabase() {
    abstract fun ryoseiDao(): RyoseiDao
    abstract fun parcelDao(): ParcelDao
    abstract fun dutyPersonDao(): DutyPersonDao
    abstract fun operationLogDao(): OperationLogDao

    @Transaction
    suspend fun registerParcelWithLog(
        parcel: ParcelEntity,
        log: OperationLogEntity
    ) {
        parcelDao().insert(parcel)
        operationLogDao().insert(log)
    }

    @Transaction
    suspend fun deliverParcelsWithLogs(
        updatedParcels: List<ParcelEntity>,
        logs: List<OperationLogEntity>
    ) {
        for (p in updatedParcels) parcelDao().update(p)
        for (log in logs) operationLogDao().insert(log)
    }

    @Transaction
    suspend fun completeNightDutyAtomic(
        confirmedParcels: List<ParcelEntity>,
        lostUpdates: List<ParcelEntity>,
        lostLogs: List<OperationLogEntity>,
        nightDutyLog: OperationLogEntity
    ) {
        for (p in confirmedParcels) parcelDao().update(p)
        for (p in lostUpdates) parcelDao().update(p)
        for (log in lostLogs) operationLogDao().insert(log)
        operationLogDao().insert(nightDutyLog)
    }

    companion object {
        @Volatile
        private var INSTANCE: PokkeDatabase? = null

        fun getInstance(context: Context): PokkeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PokkeDatabase::class.java,
                    "pokke.db"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
