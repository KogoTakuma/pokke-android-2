package com.kumanodormitory.pokke.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kumanodormitory.pokke.data.local.entity.RyoseiEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RyoseiDao {

    @Query("SELECT * FROM ryosei WHERE leaving_date IS NULL ORDER BY block, room")
    fun getAll(): Flow<List<RyoseiEntity>>

    @Query("SELECT * FROM ryosei WHERE block = :block AND leaving_date IS NULL ORDER BY room")
    fun getByBlock(block: String): Flow<List<RyoseiEntity>>

    @Query("SELECT * FROM ryosei WHERE room = :room AND leaving_date IS NULL ORDER BY name")
    fun getByRoom(room: String): Flow<List<RyoseiEntity>>

    @Query(
        """
        SELECT * FROM ryosei
        WHERE leaving_date IS NULL
          AND (name LIKE '%' || :query || '%'
            OR name_kana LIKE '%' || :query || '%'
            OR name_alphabet LIKE '%' || :query || '%'
            OR room LIKE '%' || :query || '%')
        ORDER BY block, room
        """
    )
    fun search(query: String): Flow<List<RyoseiEntity>>

    @Query(
        """
        SELECT DISTINCT r.* FROM ryosei r
        INNER JOIN parcels p ON r.id = p.ryosei_id
        WHERE p.status = 'REGISTERED'
          AND r.leaving_date IS NULL
        ORDER BY r.block, r.room
        """
    )
    fun getRyoseiWithParcels(): Flow<List<RyoseiEntity>>

    @Query("SELECT DISTINCT block FROM ryosei WHERE leaving_date IS NULL ORDER BY block")
    fun getAllBlocks(): Flow<List<String>>

    @Query("SELECT DISTINCT room FROM ryosei WHERE block = :block AND leaving_date IS NULL ORDER BY room")
    fun getRoomsByBlock(block: String): Flow<List<String>>

    @Query("SELECT * FROM ryosei WHERE id = :id")
    suspend fun getById(id: String): RyoseiEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ryoseiList: List<RyoseiEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ryosei: RyoseiEntity)

    @Update
    suspend fun update(ryosei: RyoseiEntity)

    @Query("DELETE FROM ryosei WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM ryosei")
    suspend fun deleteAll()
}
