package com.example.callnotes.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: CallNotesRepository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = CallNotesRepository(db)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `replaceAll clears and inserts all data`() = runTest {
        repository.saveContact(ContactEntity(phoneNumber = "111", displayName = "Old"))
        repository.saveNote("111", "Old", "old note")

        val newContacts = listOf(
            ContactEntity(phoneNumber = "222", displayName = "New1"),
            ContactEntity(phoneNumber = "333", displayName = "New2")
        )
        val newNotes = listOf(
            CallNoteEntity(phoneNumber = "222", noteText = "new note")
        )
        repository.replaceAll(newContacts, newNotes)

        val contacts = repository.getAllContacts()
        val notes = repository.getAllNotes()
        assertEquals(2, contacts.size)
        assertEquals(1, notes.size)
        assertEquals("New1", contacts[0].displayName)
    }

    @Test
    fun `replaceAll is destructive`() = runTest {
        repository.saveContact(ContactEntity(phoneNumber = "111", displayName = "A"))
        repository.saveContact(ContactEntity(phoneNumber = "222", displayName = "B"))

        repository.replaceAll(emptyList(), emptyList())

        assertEquals(0, repository.getAllContacts().size)
        assertEquals(0, repository.getAllNotes().size)
    }

    @Test
    fun `merge adds new contacts`() = runTest {
        repository.saveContact(ContactEntity(phoneNumber = "111", displayName = "Old"))

        val incoming = listOf(
            ContactEntity(phoneNumber = "111", displayName = "Updated", updatedAt = System.currentTimeMillis() + 1000),
            ContactEntity(phoneNumber = "222", displayName = "New")
        )
        repository.merge(incoming, emptyList())

        val contacts = repository.getAllContacts()
        assertEquals(2, contacts.size)
    }

    @Test
    fun `merge keeps newer version of existing contact`() = runTest {
        val old = ContactEntity(phoneNumber = "111", displayName = "Old", updatedAt = 1000)
        repository.saveContact(old)

        val incoming = ContactEntity(phoneNumber = "111", displayName = "Newer", updatedAt = 2000)
        repository.merge(listOf(incoming), emptyList())

        val contacts = repository.getAllContacts()
        assertEquals(1, contacts.size)
        assertEquals("Newer", contacts[0].displayName)
    }

    @Test
    fun `merge keeps old version when incoming is older`() = runTest {
        val existing = ContactEntity(phoneNumber = "111", displayName = "Current", updatedAt = 2000)
        repository.saveContact(existing)

        val incoming = ContactEntity(phoneNumber = "111", displayName = "Stale", updatedAt = 1000)
        repository.merge(listOf(incoming), emptyList())

        val contacts = repository.getAllContacts()
        assertEquals(1, contacts.size)
        assertEquals("Current", contacts[0].displayName)
    }

    @Test
    fun `merge adds new notes`() = runTest {
        val incoming = listOf(
            CallNoteEntity(phoneNumber = "111", noteText = "note1", createdAt = 1000, updatedAt = 1000),
            CallNoteEntity(phoneNumber = "222", noteText = "note2", createdAt = 2000, updatedAt = 2000)
        )
        repository.merge(emptyList(), incoming)

        val notes = repository.getAllNotes()
        assertEquals(2, notes.size)
    }

    @Test
    fun `merge keeps newer version of existing note`() = runTest {
        val existing = CallNoteEntity(phoneNumber = "111", noteText = "old", createdAt = 1000, updatedAt = 1000)
        repository.saveNote("111", null, "old")

        val incoming = CallNoteEntity(phoneNumber = "111", noteText = "updated", createdAt = 1000, updatedAt = 2000)
        repository.merge(emptyList(), listOf(incoming))

        val notes = repository.getAllNotes()
        assertEquals(1, notes.size)
        assertEquals("updated", notes[0].noteText)
    }

    @Test
    fun `findContact returns correct contact`() = runTest {
        repository.saveContact(ContactEntity(phoneNumber = "+359888123456", displayName = "Test"))

        val found = repository.findContact("+359888123456")
        assertNotNull(found)
        assertEquals("Test", found?.displayName)
    }

    @Test
    fun `findContact returns null for unknown number`() = runTest {
        val found = repository.findContact("+359000000000")
        assertNull(found)
    }

    @Test
    fun `saveNote creates note with correct fields`() = runTest {
        val id = repository.saveNote("+359888123456", "Test Contact", "This is a note")

        val note = repository.findNote(id)
        assertNotNull(note)
        assertEquals("+359888123456", note?.phoneNumber)
        assertEquals("Test Contact", note?.callerName)
        assertEquals("This is a note", note?.noteText)
    }

    @Test
    fun `deleteContact removes contact`() = runTest {
        val contact = ContactEntity(phoneNumber = "111", displayName = "ToDelete")
        repository.saveContact(contact)

        val loaded = repository.findContact("111")!!
        repository.deleteContact(loaded)

        assertNull(repository.findContact("111"))
    }

    @Test
    fun `deleteNote removes note`() = runTest {
        val id = repository.saveNote("111", null, "to delete")
        val note = repository.findNote(id)!!
        repository.deleteNote(note)

        assertNull(repository.findNote(id))
    }
}
