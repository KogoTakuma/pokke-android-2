package com.kumanodormitory.pokke.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kumanodormitory.pokke.data.local.dao.ParcelDao
import com.kumanodormitory.pokke.data.local.entity.ParcelEntity

@Database(
    entities = [ParcelEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PokkeDatabase : RoomDatabase() {
    abstract fun parcelDao(): ParcelDao

    companion object {
        @Volatile
        private var INSTANCE: PokkeDatabase? = null

        fun getInstance(context: Context): PokkeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PokkeDatabase::class.java,
                    "pokke.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
