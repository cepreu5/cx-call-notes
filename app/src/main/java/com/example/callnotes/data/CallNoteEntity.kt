package com.example.callnotes.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "call_notes",
    indices = [Index(value = ["phoneNumber"]), Index(value = ["callSessionId"])]
)
data class CallNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val callerName: String? = null,
    val noteText: String,
    val createdAt: Long = System.currentTimeMillis(),
    val callSessionId: Long? = null
)
