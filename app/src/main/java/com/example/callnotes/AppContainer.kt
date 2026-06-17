package com.example.callnotes

import android.content.Context
import com.example.callnotes.data.CallNotesRepository
import com.example.callnotes.data.DatabaseProvider

class AppContainer(context: Context) {
    val repository: CallNotesRepository = CallNotesRepository(DatabaseProvider.get(context))
}
