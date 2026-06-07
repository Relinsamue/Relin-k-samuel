package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_creations")
data class SavedCreation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val toolType: String, // CAPTION, SCRIPT, POST, THUMBNAIL, BLOG
    val inputs: String, // JSON string or descriptive parameters
    val outputContent: String, // Full text of the generated content
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
