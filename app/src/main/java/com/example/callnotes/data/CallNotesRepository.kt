package com.example.callnotes.data

import kotlinx.coroutines.flow.Flow

class CallNotesRepository(private val db: AppDatabase) {
    suspend fun findContact(phone: String) = db.contactDao().findByPhone(phone)
    suspend fun saveContact(contact: ContactEntity) = db.contactDao().upsert(contact)
    suspend fun getAllContacts() = db.contactDao().getAll()
    fun getAllContactsFlow(): Flow<List<ContactEntity>> = db.contactDao().getAllFlow()
    suspend fun getAllNotes() = db.callNoteDao().getAll()
    suspend fun findNote(id: Long) = db.callNoteDao().findById(id)
    suspend fun searchContacts(query: String) = db.contactDao().search(query)
    suspend fun searchNotes(query: String) = db.callNoteDao().search(query)
    suspend fun createSession(phone: String, known: ContactEntity?) =
        db.callSessionDao().insert(
            CallSessionEntity(
                phoneNumber = phone,
                callType = 1,
                screenedKnown = known != null,
                knownContactId = known?.id
            )
        )
    suspend fun endSession(sessionId: Long) {
        db.callSessionDao().markEnded(sessionId, System.currentTimeMillis(), state = 0)
    }
    suspend fun saveNote(
        phone: String,
        callerName: String?,
        noteText: String,
        sessionId: Long? = null
    ) = db.callNoteDao().insert(
        CallNoteEntity(
            phoneNumber = phone,
            callerName = callerName,
            noteText = noteText,
            callSessionId = sessionId
        )
    )
    suspend fun updateNoteEntity(note: CallNoteEntity) = db.callNoteDao().update(note)
    suspend fun deleteContact(contact: ContactEntity) = db.contactDao().delete(contact)
    suspend fun deleteNote(note: CallNoteEntity) = db.callNoteDao().delete(note)
}
