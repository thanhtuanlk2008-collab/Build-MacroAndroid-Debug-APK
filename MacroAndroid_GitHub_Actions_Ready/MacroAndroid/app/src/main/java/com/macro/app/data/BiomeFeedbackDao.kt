package com.macro.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BiomeFeedbackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sample: BiomeFeedbackEntity)

    @Query("SELECT * FROM biome_feedback WHERE status = 'PENDING' ORDER BY createdAt DESC")
    suspend fun getPending(): List<BiomeFeedbackEntity>

    @Query("SELECT * FROM biome_feedback WHERE status = 'CONFIRMED' AND confirmedBiome IS NOT NULL")
    suspend fun getConfirmed(): List<BiomeFeedbackEntity>

    @Query("""
        UPDATE biome_feedback
        SET confirmedBiome = :biome, status = 'CONFIRMED'
        WHERE id = :id AND status = 'PENDING'
    """)
    suspend fun confirmPending(id: String, biome: String): Int

    @Query("DELETE FROM biome_feedback WHERE id = :id AND status = 'PENDING'")
    suspend fun discardPending(id: String): Int
}
