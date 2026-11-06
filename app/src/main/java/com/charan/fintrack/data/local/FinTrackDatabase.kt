package com.charan.fintrack.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AccountEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FinTrackDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao

    companion object {
        @Volatile
        private var INSTANCE: FinTrackDatabase? = null

        fun getDatabase(context: Context): FinTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinTrackDatabase::class.java,
                    "fintrack_database"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}
