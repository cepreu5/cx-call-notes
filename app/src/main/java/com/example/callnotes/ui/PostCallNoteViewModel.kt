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
    val noteId: Long? = null,
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
    fun init(phone: String, noteId: Long? = null) {
        val tagsStr = prefs.getString("tags_list", "Клиент,Важно,Партньор,Доставчик,Лично") ?: "Клиент,Важно,Партньор,Доставчик,Лично"
        val availableTags = tagsStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
        var finalPhone = phone
        var finalName = ""
        if (finalPhone.isBlank() && noteId == null) {
            finalPhone = prefs.getString("last_call_phone", "") ?: ""
            finalName = prefs.getString("last_call_name", "") ?: ""
        }
        _uiState.value = PostCallNoteUiState(
            phoneNumber = finalPhone,
            callerName = finalName,
            noteId = noteId,
            availableTags = availableTags
        )
        if (noteId != null) {
            viewModelScope.launch {
                val note = repository.findNote(noteId)
                if (note != null) {
                    _uiState.value = _uiState.value.copy(
                        phoneNumber = note.phoneNumber,
                        callerName = note.callerName ?: "",
                        noteText = note.noteText,
                        isEditMode = true
                    )
                    val contact = repository.findContact(note.phoneNumber)
                    if (contact != null) {
                        val selected = contact.tags?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
                        _uiState.value = _uiState.value.copy(selectedTags = selected)
                    }
                }
            }
        } else if (finalPhone.isNotBlank()) {
            viewModelScope.launch {
                val contact = repository.findContact(finalPhone)
                if (contact != null) {
                    val selected = contact.tags?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
                    _uiState.value = _uiState.value.copy(
                        callerName = contact.displayName,
                        noteText = contact.note ?: "",
                        selectedTags = selected,
                        isEditMode = true
                    )
                } else {
                    val systemName = getNameFromPhoneContacts(finalPhone) ?: ""
                    if (systemName.isNotBlank()) {
                        _uiState.value = _uiState.value.copy(callerName = systemName)
                    }
                }
            }
        }
    }
    private fun getNameFromPhoneContacts(phoneNumber: String): String? {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return null
        val uri = android.net.Uri.withAppendedPath(android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI, android.net.Uri.encode(phoneNumber))
        val projection = arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME)
        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getString(0)
                }
            }
        } catch (_: Exception) {}
        return null
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
            repository.saveNote(normPhone, s.callerName.ifBlank { null }, s.noteText)
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
    fun updateNote() {
        viewModelScope.launch {
            val s = _uiState.value
            val normPhone = com.example.callnotes.data.PhoneNumberNormalizer.normalize(s.phoneNumber)
            if (s.noteId != null) {
                val existing = repository.findNote(s.noteId)
                if (existing != null) {
                    repository.updateNoteEntity(
                        existing.copy(
                            phoneNumber = normPhone,
                            callerName = s.callerName.ifBlank { null },
                            noteText = s.noteText
                        )
                    )
                }
            }
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
