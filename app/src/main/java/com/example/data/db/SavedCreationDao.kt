package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedCreationDao {
    @Query("SELECT * FROM saved_creations ORDER BY timestamp DESC")
    fun getAllCreations(): Flow<List<SavedCreation>>

    @Query("SELECT * FROM saved_creations WHERE toolType = :toolType ORDER BY timestamp DESC")
    fun getCreationsByTool(toolType: String): Flow<List<SavedCreation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreation(creation: SavedCreation): Long

    @Update
    suspend fun updateCreation(creation: SavedCreation)

    @Query("DELETE FROM saved_creations WHERE id = :id")
    suspend fun deleteCreationById(id: Int)

    @Query("SELECT * FROM saved_creations WHERE title LIKE '%' || :query || '%' OR outputContent LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchCreations(query: String): Flow<List<SavedCreation>>
}
