package com.example.data.db

import kotlinx.coroutines.flow.Flow

class SavedCreationRepository(private val dao: SavedCreationDao) {
    val allCreations: Flow<List<SavedCreation>> = dao.getAllCreations()

    fun getCreationsByTool(toolType: String): Flow<List<SavedCreation>> {
        return dao.getCreationsByTool(toolType)
    }

    suspend fun insert(creation: SavedCreation): Long {
        return dao.insertCreation(creation)
    }

    suspend fun update(creation: SavedCreation) {
        dao.updateCreation(creation)
    }

    suspend fun deleteById(id: Int) {
        dao.deleteCreationById(id)
    }

    fun search(query: String): Flow<List<SavedCreation>> {
        return dao.searchCreations(query)
    }
}
