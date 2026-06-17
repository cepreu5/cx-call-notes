package com.example.callnotes

import android.app.Application
import com.example.callnotes.data.DatabaseProvider

class CallNotesApp : Application() {
    val container by lazy { AppContainer(this) }
    override fun onCreate() {
        super.onCreate()
        DatabaseProvider.init(this)
    }
}
