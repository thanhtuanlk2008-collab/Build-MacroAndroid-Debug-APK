package com.macro.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MacroDao {

    @Query("SELECT * FROM macros ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<MacroEntity>>

    @Query("SELECT * FROM macros ORDER BY updatedAt DESC")
    suspend fun getAllOnce(): List<MacroEntity>

    @Query("SELECT * FROM macros WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MacroEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MacroEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<MacroEntity>)

    @Query("DELETE FROM macros WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM macros")
    suspend fun count(): Int
}
