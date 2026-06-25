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

data class RecentCall(
    val number: String,
    val name: String,
    val type: Int,
    val date: Long
)

data class PostCallNoteUiState(
    val phoneNumber: String = "",
    val callerName: String = "",
    val noteText: String = "",
    val noteId: Long? = null,
    val selectedTags: Set<String> = emptySet(),
    val availableTags: List<String> = emptyList(),
    val saved: Boolean = false,
    val isEditMode: Boolean = false,
    val isNewContact: Boolean = false,
    val callDirection: String? = null,
    val recentCalls: List<RecentCall> = emptyList()
) {
    companion object {
        const val PREFIX_INCOMING = "+ "
        const val PREFIX_OUTGOING = "- "

        fun stripDirectionPrefix(text: String): String {
            return when {
                text.startsWith(PREFIX_INCOMING) -> text.removePrefix(PREFIX_INCOMING)
                text.startsWith(PREFIX_OUTGOING) -> text.removePrefix(PREFIX_OUTGOING)
                else -> text
            }
        }

        fun getDirectionPrefix(direction: String?): String? {
            return when (direction) {
                "incoming" -> PREFIX_INCOMING
                "outgoing" -> PREFIX_OUTGOING
                else -> null
            }
        }
    }
}

class PostCallNoteViewModel(
    private val repository: CallNotesRepository,
    private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(PostCallNoteUiState())
    val uiState: StateFlow<PostCallNoteUiState> = _uiState
    private val prefs = context.getSharedPreferences("cx_call_notes_prefs", Context.MODE_PRIVATE)
    fun init(phone: String, noteId: Long? = null, callDirection: String? = null) {
        val tagsStr = prefs.getString("tags_list", "Важно,Клиент,Агенция,Строител,Лични") ?: "Важно, Клиент, Агенция, Строител, Лични"
        val availableTags = tagsStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
        _uiState.value = PostCallNoteUiState(
            phoneNumber = phone,
            noteId = noteId,
            availableTags = availableTags,
            callDirection = callDirection
        )
        if (noteId != null) {
            viewModelScope.launch {
                val note = repository.findNote(noteId)
                if (note != null) {
                    _uiState.value = _uiState.value.copy(
                        phoneNumber = note.phoneNumber,
                        callerName = note.callerName ?: "",
                        noteText = PostCallNoteUiState.stripDirectionPrefix(note.noteText),
                        callDirection = when {
                            note.noteText.startsWith(PostCallNoteUiState.PREFIX_INCOMING) -> "incoming"
                            note.noteText.startsWith(PostCallNoteUiState.PREFIX_OUTGOING) -> "outgoing"
                            else -> null
                        },
                        isEditMode = true
                    )
                    val contact = repository.findContact(note.phoneNumber)
                    if (contact != null) {
                        val selected = contact.tags?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
                        _uiState.value = _uiState.value.copy(selectedTags = selected)
                    }
                }
            }
        } else if (phone.isNotBlank()) {
            viewModelScope.launch {
                val contact = repository.findContact(phone)
                if (contact != null) {
                    val selected = contact.tags?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
                    _uiState.value = _uiState.value.copy(
                        callerName = contact.displayName,
                        noteText = PostCallNoteUiState.stripDirectionPrefix(contact.note ?: ""),
                        callDirection = callDirection ?: when {
                            contact.note?.startsWith(PostCallNoteUiState.PREFIX_INCOMING) == true -> "incoming"
                            contact.note?.startsWith(PostCallNoteUiState.PREFIX_OUTGOING) == true -> "outgoing"
                            else -> null
                        },
                        selectedTags = selected,
                        isEditMode = true
                    )
                } else {
                    val systemName = getNameFromPhoneContacts(phone) ?: ""
                    if (systemName.isNotBlank()) {
                        _uiState.value = _uiState.value.copy(callerName = systemName)
                    }
                    _uiState.value = _uiState.value.copy(isNewContact = true)
                }
            }
        }
    }
    fun loadRecentCalls() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALL_LOG) != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        val list = mutableListOf<RecentCall>()
        val cursor = context.contentResolver.query(
            android.provider.CallLog.Calls.CONTENT_URI,
            arrayOf(
                android.provider.CallLog.Calls.NUMBER,
                android.provider.CallLog.Calls.CACHED_NAME,
                android.provider.CallLog.Calls.TYPE,
                android.provider.CallLog.Calls.DATE
            ),
            null,
            null,
            "${android.provider.CallLog.Calls.DATE} DESC"
        )
        cursor?.use { c ->
            val numIdx = c.getColumnIndex(android.provider.CallLog.Calls.NUMBER)
            val nameIdx = c.getColumnIndex(android.provider.CallLog.Calls.CACHED_NAME)
            val typeIdx = c.getColumnIndex(android.provider.CallLog.Calls.TYPE)
            val dateIdx = c.getColumnIndex(android.provider.CallLog.Calls.DATE)
            var count = 0
            while (c.moveToNext() && count < 5) {
                val number = c.getString(numIdx) ?: ""
                val name = c.getString(nameIdx) ?: ""
                val type = c.getInt(typeIdx)
                val date = c.getLong(dateIdx)
                list.add(RecentCall(number, name, type, date))
                count++
            }
        }
        _uiState.value = _uiState.value.copy(recentCalls = list)
    }
    fun selectRecentCall(call: RecentCall) {
        val direction = when (call.type) {
            1 -> "incoming"
            2 -> "outgoing"
            3 -> "incoming"
            else -> null
        }
        _uiState.value = _uiState.value.copy(
            phoneNumber = call.number,
            callerName = call.name.ifBlank { getNameFromPhoneContacts(call.number) ?: "" },
            isNewContact = true,
            isEditMode = false,
            noteText = "",
            selectedTags = emptySet(),
            callDirection = direction
        )
        viewModelScope.launch {
            val contact = repository.findContact(call.number)
            if (contact != null) {
                val selected = contact.tags?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
                _uiState.value = _uiState.value.copy(
                    callerName = contact.displayName,
                    noteText = PostCallNoteUiState.stripDirectionPrefix(contact.note ?: ""),
                    selectedTags = selected,
                    isEditMode = true,
                    isNewContact = false,
                    callDirection = direction
                )
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
            val prefix = PostCallNoteUiState.getDirectionPrefix(s.callDirection) ?: ""
            val storedText = prefix + s.noteText
            repository.saveNote(normPhone, s.callerName.ifBlank { null }, storedText)
            if (s.callerName.isNotBlank()) {
                val tagsStr = s.selectedTags.joinToString(",")
                repository.saveContact(
                    ContactEntity(
                        phoneNumber = normPhone,
                        displayName = s.callerName,
                        note = storedText,
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
            val prefix = PostCallNoteUiState.getDirectionPrefix(s.callDirection) ?: ""
            val storedText = prefix + s.noteText
            if (s.noteId != null) {
                val existing = repository.findNote(s.noteId)
                if (existing != null) {
                    repository.updateNoteEntity(
                        existing.copy(
                            phoneNumber = normPhone,
                            callerName = s.callerName.ifBlank { null },
                            noteText = storedText,
                            updatedAt = System.currentTimeMillis()
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
                        note = storedText,
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
