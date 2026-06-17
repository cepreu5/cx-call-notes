package com.example.callnotes.data

import androidx.room.*

@Dao
interface CallSessionDao {
    @Insert
    suspend fun insert(session: CallSessionEntity): Long
    @Query("UPDATE call_sessions SET endedAt = :endedAt, state = :state WHERE id = :id")
    suspend fun markEnded(id: Long, endedAt: Long, state: Int = 0)
    @Query("UPDATE call_sessions SET screenedKnown = 1, knownContactId = :contactId WHERE id = :id")
    suspend fun markKnown(id: Long, contactId: Long)
}
