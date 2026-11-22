package com.macro.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MacroEntity::class, BiomeFeedbackEntity::class],
    version = 2,
    exportSchema = false
)
abstract class MacroDatabase : RoomDatabase() {

    abstract fun macroDao(): MacroDao
    abstract fun biomeFeedbackDao(): BiomeFeedbackDao

    companion object {
        /**
         * Adds biome feedback without modifying or deleting existing macro records.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS biome_feedback (
                        id TEXT NOT NULL,
                        predictedBiome TEXT,
                        confirmedBiome TEXT,
                        featureVector TEXT NOT NULL,
                        similarity REAL,
                        status TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_biome_feedback_status_createdAt
                    ON biome_feedback(status, createdAt)
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: MacroDatabase? = null

        fun getInstance(context: Context): MacroDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MacroDatabase::class.java,
                    "macro_engine.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
