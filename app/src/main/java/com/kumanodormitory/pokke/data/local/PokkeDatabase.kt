package com.kumanodormitory.pokke.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kumanodormitory.pokke.data.local.converter.Converters
import com.kumanodormitory.pokke.data.local.dao.DutyPersonDao
import com.kumanodormitory.pokke.data.local.dao.OperationLogDao
import com.kumanodormitory.pokke.data.local.dao.ParcelDao
import com.kumanodormitory.pokke.data.local.dao.RyoseiDao
import com.kumanodormitory.pokke.data.local.entity.DutyPersonEntity
import com.kumanodormitory.pokke.data.local.entity.OperationLogEntity
import com.kumanodormitory.pokke.data.local.entity.ParcelEntity
import com.kumanodormitory.pokke.data.local.entity.RyoseiEntity
import java.util.concurrent.Executors

@Database(
    entities = [
        RyoseiEntity::class,
        ParcelEntity::class,
        DutyPersonEntity::class,
        OperationLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PokkeDatabase : RoomDatabase() {
    abstract fun ryoseiDao(): RyoseiDao
    abstract fun parcelDao(): ParcelDao
    abstract fun dutyPersonDao(): DutyPersonDao
    abstract fun operationLogDao(): OperationLogDao

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
                    .fallbackToDestructiveMigration()
                    .addCallback(SeedCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            Executors.newSingleThreadExecutor().execute {
                seedDummyData(db)
            }
        }

        private fun seedDummyData(db: SupportSQLiteDatabase) {
            // ── 寮生シードデータ（ハードコードUUID） ──
            for (r in SeedData.buildSeedRyoseiList()) {
                db.execSQL(
                    """
                    INSERT INTO ryosei (id, name, name_kana, name_alphabet, room, block, leaving_date, discord_status)
                    VALUES (?, ?, ?, ?, ?, ?, NULL, 'UNLINKED')
                    """.trimIndent(),
                    arrayOf(r.id, r.name, r.nameKana, r.nameAlphabet, r.room, r.block)
                )
            }

            // ── 事務当番の初期データ ──
            val now = System.currentTimeMillis()
            db.execSQL(
                """
                INSERT INTO duty_person (id, name, updated_at)
                VALUES (?, ?, ?)
                """.trimIndent(),
                arrayOf("dp001", "田中一郎", now)
            )
        }
    }
}
