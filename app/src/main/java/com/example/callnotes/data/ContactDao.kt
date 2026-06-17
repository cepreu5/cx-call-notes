package com.example.callnotes.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts WHERE phoneNumber = :phone LIMIT 1")
    suspend fun findByPhone(phone: String): ContactEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(contact: ContactEntity): Long
    @Query("SELECT * FROM contacts ORDER BY updatedAt DESC")
    fun getAllFlow(): Flow<List<ContactEntity>>
    @Query("SELECT * FROM contacts ORDER BY updatedAt DESC")
    suspend fun getAll(): List<ContactEntity>
    @Query("SELECT * FROM contacts WHERE displayName LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    suspend fun search(query: String): List<ContactEntity>
    @Delete
    suspend fun delete(contact: ContactEntity)
}
