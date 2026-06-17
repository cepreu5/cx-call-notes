package com.example.callnotes.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ContactEntity::class, CallSessionEntity::class, CallNoteEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun callSessionDao(): CallSessionDao
    abstract fun callNoteDao(): CallNoteDao
}
