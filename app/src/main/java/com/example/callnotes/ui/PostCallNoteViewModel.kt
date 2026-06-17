package com.example.callnotes.ui

import android.content.Context
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
    val selectedTags: Set<String> = emptySet(),
    val availableTags: List<String> = emptyList(),
    val saved: Boolean = false,
    val isEditMode: Boolean = false
)

class PostCallNoteViewModel(
    private val repository: CallNotesRepository,
    private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(PostCallNoteUiState())
    val uiState: StateFlow<PostCallNoteUiState> = _uiState
    private val prefs = context.getSharedPreferences("cx_call_notes_prefs", Context.MODE_PRIVATE)
    fun init(phone: String, sessionId: Long? = null) {
        val tagsStr = prefs.getString("tags_list", "Клиент,Важно,Партньор,Доставчик,Лично") ?: "Клиент,Важно,Партньор,Доставчик,Лично"
        val availableTags = tagsStr.split(",").filter { it.isNotBlank() }
        _uiState.value = _uiState.value.copy(
            phoneNumber = phone,
            sessionId = sessionId,
            availableTags = availableTags
        )
        if (phone.isNotBlank()) {
            viewModelScope.launch {
                repository.findContact(phone)?.let { contact ->
                    val selected = contact.tags?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
                    _uiState.value = _uiState.value.copy(
                        callerName = contact.displayName,
                        noteText = contact.note ?: "",
                        selectedTags = selected,
                        isEditMode = true
                    )
                }
            }
        }
    }
    fun updatePhoneNumber(value: String) {
        _uiState.value = _uiState.value.copy(phoneNumber = value)
    }
    fun updateCallerName(value: String) {
        _uiState.value = _uiState.value.copy(callerName = value)
    }
    fun updateNoteText(value: String) {
        _uiState.value = _uiState.value.copy(noteText = value)
    }
    fun toggleTag(tag: String) {
        val current = _uiState.value.selectedTags.toMutableSet()
        if (current.contains(tag)) {
            current.remove(tag)
        } else {
            current.add(tag)
        }
        _uiState.value = _uiState.value.copy(selectedTags = current)
    }
    fun save() {
        viewModelScope.launch {
            val s = _uiState.value
            val normPhone = com.example.callnotes.data.PhoneNumberNormalizer.normalize(s.phoneNumber)
            repository.saveNote(normPhone, s.callerName.ifBlank { null }, s.noteText, s.sessionId)
            if (s.callerName.isNotBlank()) {
                val tagsStr = s.selectedTags.joinToString(",")
                repository.saveContact(
                    ContactEntity(
                        phoneNumber = normPhone,
                        displayName = s.callerName,
                        note = s.noteText,
                        tags = tagsStr
                    )
                )
            }
            _uiState.value = s.copy(saved = true)
        }
    }
}

class PostCallNoteViewModelFactory(
    private val repository: CallNotesRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PostCallNoteViewModel::class.java)) {
            return PostCallNoteViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
