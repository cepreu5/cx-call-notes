package com.example.callnotes.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "call_sessions",
    indices = [Index(value = ["phoneNumber"]), Index(value = ["knownContactId"])]
)
data class CallSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val callType: Int,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val screenedKnown: Boolean = false,
    val knownContactId: Long? = null,
    val state: Int = 0
)
