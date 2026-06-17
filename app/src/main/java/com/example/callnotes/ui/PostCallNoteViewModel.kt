package com.example.callnotes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.callnotes.data.CallNotesRepository
import com.example.callnotes.data.ContactEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PostCallNoteUiState(
    val phoneNumber: String = "",
    val callerName: String = "",
    val noteText: String = "",
    val sessionId: Long? = null,
    val saved: Boolean = false
)

class PostCallNoteViewModel(private val repository: CallNotesRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(PostCallNoteUiState())
    val uiState: StateFlow<PostCallNoteUiState> = _uiState
    fun init(phone: String, sessionId: Long? = null) {
        _uiState.value = _uiState.value.copy(phoneNumber = phone, sessionId = sessionId)
        viewModelScope.launch {
            repository.findContact(phone)?.let { contact ->
                _uiState.value = _uiState.value.copy(
                    callerName = contact.displayName,
                    noteText = contact.note ?: ""
                )
            }
        }
    }
    fun updateCallerName(value: String) {
        _uiState.value = _uiState.value.copy(callerName = value)
    }
    fun updateNoteText(value: String) {
        _uiState.value = _uiState.value.copy(noteText = value)
    }
    fun save() {
        viewModelScope.launch {
            val s = _uiState.value
            val normPhone = com.example.callnotes.data.PhoneNumberNormalizer.normalize(s.phoneNumber)
            repository.saveNote(normPhone, s.callerName.ifBlank { null }, s.noteText, s.sessionId)
            if (s.callerName.isNotBlank()) {
                repository.saveContact(
                    ContactEntity(
                        phoneNumber = normPhone,
                        displayName = s.callerName,
                        note = s.noteText
                    )
                )
            }
            _uiState.value = s.copy(saved = true)
        }
    }
}

class PostCallNoteViewModelFactory(private val repository: CallNotesRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PostCallNoteViewModel::class.java)) {
            return PostCallNoteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
