package com.kumanodormitory.pokke.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kumanodormitory.pokke.data.local.entity.DutyPersonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DutyPersonDao {

    @Query("SELECT * FROM duty_person LIMIT 1")
    fun getCurrent(): Flow<DutyPersonEntity?>

    @Query("SELECT * FROM duty_person LIMIT 1")
    suspend fun getCurrentSync(): DutyPersonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(dutyPerson: DutyPersonEntity)

    @Query("DELETE FROM duty_person")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceCurrent(newDutyPerson: DutyPersonEntity) {
        deleteAll()
        upsert(newDutyPerson)
    }
}
