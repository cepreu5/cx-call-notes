package com.example.callnotes.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CallNoteDao {
    @Insert
    suspend fun insert(note: CallNoteEntity): Long
    @Query("SELECT * FROM call_notes ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<CallNoteEntity>>
    @Query("SELECT * FROM call_notes ORDER BY createdAt DESC")
    suspend fun getAll(): List<CallNoteEntity>
    @Query("SELECT * FROM call_notes WHERE callerName LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%' OR noteText LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    suspend fun search(query: String): List<CallNoteEntity>
    @Query("SELECT * FROM call_notes WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): CallNoteEntity?
    @Update
    suspend fun update(note: CallNoteEntity)
    @Delete
    suspend fun delete(note: CallNoteEntity)
    @Query("DELETE FROM call_notes")
    suspend fun deleteAll()
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(notes: List<CallNoteEntity>)
}
