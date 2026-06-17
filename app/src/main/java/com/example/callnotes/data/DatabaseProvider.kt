package com.example.callnotes.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile private var instance: AppDatabase? = null
    private lateinit var appContext: Context
    fun init(context: Context) {
        appContext = context.applicationContext
    }
    fun get(context: Context = appContext): AppDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "callnotes.db"
            ).build().also { instance = it }
        }
}
