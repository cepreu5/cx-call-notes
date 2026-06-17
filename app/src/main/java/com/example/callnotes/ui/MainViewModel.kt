package com.example.callnotes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.callnotes.data.CallNoteEntity
import com.example.callnotes.data.CallNotesRepository
import com.example.callnotes.data.ContactEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MainUiState(
    val contacts: List<ContactEntity> = emptyList(),
    val notes: List<CallNoteEntity> = emptyList(),
    val selectedTab: Int = 0,
    val searchQuery: String = ""
)

class MainViewModel(private val repository: CallNotesRepository) : ViewModel() {
    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state
    init {
        load()
    }
    fun load() {
        viewModelScope.launch {
            val q = _state.value.searchQuery
            val contacts = if (q.isBlank()) repository.getAllContacts() else repository.searchContacts(q)
            val notes = if (q.isBlank()) repository.getAllNotes() else repository.searchNotes(q)
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
}

class MainViewModelFactory(private val repository: CallNotesRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
