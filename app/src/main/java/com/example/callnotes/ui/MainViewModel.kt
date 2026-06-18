package com.example.callnotes.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.callnotes.data.CallNoteEntity
import com.example.callnotes.data.CallNotesRepository
import com.example.callnotes.data.ContactEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MainUiState(
    val contacts: List<ContactEntity> = emptyList(),
    val notes: List<CallNoteEntity> = emptyList(),
    val selectedTab: Int = 0,
    val searchQuery: String = "",
    val contactsLimit: Int = 20,
    val notesLimit: Int = 20,
    val appBgColor: String = "#FFFFFF",
    val contactsBgColor: String = "#FFFFFF",
    val notesBgColor: String = "#E8F5E9",
    val tags: List<String> = emptyList()
)

class MainViewModel(
    private val repository: CallNotesRepository,
    private val context: Context
) : ViewModel() {
    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state
    private val prefs = context.getSharedPreferences("cx_call_notes_prefs", Context.MODE_PRIVATE)
    init {
        loadSettings()
        load()
    }
    fun loadSettings() {
        val appBg = prefs.getString("app_bg_color", "default") ?: "default"
        val contactsBg = prefs.getString("contacts_bg_color", "default") ?: "default"
        val notesBg = prefs.getString("notes_bg_color", "default") ?: "default"
        val tagsStr = prefs.getString("tags_list", "Клиент,Важно,Партньор,Доставчик,Лично") ?: "Клиент,Важно,Партньор,Доставчик,Лично"
        val tagsList = tagsStr.split(",").filter { it.isNotBlank() }
        _state.value = _state.value.copy(
            appBgColor = appBg,
            contactsBgColor = contactsBg,
            notesBgColor = notesBg,
            tags = tagsList
        )
    }
    fun saveSettings(appBg: String, contactsBg: String, notesBg: String, tagsList: List<String>) {
        prefs.edit().apply {
            putString("app_bg_color", appBg)
            putString("contacts_bg_color", contactsBg)
            putString("notes_bg_color", notesBg)
            putString("tags_list", tagsList.joinToString(","))
            apply()
        }
        loadSettings()
    }
    fun load() {
        viewModelScope.launch {
            val q = _state.value.searchQuery.trim()
            val allContacts = repository.getAllContacts()
            val allNotes = repository.getAllNotes()
            val contacts = if (q.isBlank()) {
                allContacts
            } else {
                val isDate = q.matches(Regex("\\d{2}\\.\\d{2}\\.\\d{4}"))
                allContacts.filter { c ->
                    if (isDate) {
                        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(c.updatedAt)) == q
                    } else {
                        c.displayName.contains(q, true) || c.phoneNumber.contains(q, true) || (c.note?.contains(q, true) == true) || (c.tags?.contains(q, true) == true)
                    }
                }
            }
            val notes = if (q.isBlank()) {
                allNotes
            } else {
                val isDate = q.matches(Regex("\\d{2}\\.\\d{2}\\.\\d{4}"))
                allNotes.filter { n ->
                    if (isDate) {
                        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(n.createdAt)) == q
                    } else {
                        (n.callerName?.contains(q, true) == true) || n.phoneNumber.contains(q, true) || n.noteText.contains(q, true)
                    }
                }
            }
            _state.value = _state.value.copy(
                contacts = contacts,
                notes = notes
            )
        }
    }
    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        load()
    }
    fun selectTab(index: Int) {
        _state.value = _state.value.copy(selectedTab = index)
    }
    fun loadMoreContacts() {
        _state.value = _state.value.copy(contactsLimit = _state.value.contactsLimit + 20)
    }
    fun loadMoreNotes() {
        _state.value = _state.value.copy(notesLimit = _state.value.notesLimit + 20)
    }
    fun deleteContact(contact: ContactEntity) {
        viewModelScope.launch {
            repository.deleteContact(contact)
            load()
        }
    }
    fun deleteNote(note: CallNoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
            load()
        }
    }
}

class MainViewModelFactory(
    private val repository: CallNotesRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
