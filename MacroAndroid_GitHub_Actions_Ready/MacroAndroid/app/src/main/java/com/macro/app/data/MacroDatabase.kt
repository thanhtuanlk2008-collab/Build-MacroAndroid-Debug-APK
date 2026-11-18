package com.macro.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MacroEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MacroDatabase : RoomDatabase() {

    abstract fun macroDao(): MacroDao

    companion object {
        @Volatile
        private var INSTANCE: MacroDatabase? = null

        fun getInstance(context: Context): MacroDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MacroDatabase::class.java,
                    "macro_engine.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
