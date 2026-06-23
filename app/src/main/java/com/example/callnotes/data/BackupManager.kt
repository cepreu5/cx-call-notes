package com.example.callnotes.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

object BackupManager {

    data class BackupData(
        val contacts: List<ContactEntity>,
        val notes: List<CallNoteEntity>,
        val backupTime: Long = System.currentTimeMillis()
    )

    suspend fun exportFull(context: Context, uri: Uri, repository: CallNotesRepository): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val contacts = repository.getAllContacts()
                val notes = repository.getAllNotes()

                val json = JSONObject().apply {
                    put("backupTime", System.currentTimeMillis())
                    put("contacts", contactsToJson(contacts))
                    put("notes", notesToJson(notes))
                }

                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toString(2).toByteArray())
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    suspend fun exportIncremental(context: Context, uri: Uri, repository: CallNotesRepository, lastBackupDate: Long): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val contacts = repository.getAllContacts().filter { it.updatedAt > lastBackupDate }
                val notes = repository.getAllNotes().filter { it.updatedAt > lastBackupDate }

                val json = JSONObject().apply {
                    put("backupTime", System.currentTimeMillis())
                    put("incremental", true)
                    put("sinceDate", lastBackupDate)
                    put("contacts", contactsToJson(contacts))
                    put("notes", notesToJson(notes))
                }

                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toString(2).toByteArray())
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    suspend fun exportSettingsOnly(context: Context, uri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("backupTime", System.currentTimeMillis())
                    put("settingsOnly", true)
                    put("settings", readSettings(context))
                }

                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toString(2).toByteArray())
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    suspend fun importFull(context: Context, uri: Uri, repository: CallNotesRepository): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val json = readJsonFromUri(context, uri) ?: return@withContext false
                val contacts = jsonToContacts(json.getJSONArray("contacts"))
                val notes = jsonToNotes(json.getJSONArray("notes"))

                repository.replaceAll(contacts, notes)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    suspend fun importIncremental(context: Context, uri: Uri, repository: CallNotesRepository): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val json = readJsonFromUri(context, uri) ?: return@withContext false
                val contacts = jsonToContacts(json.getJSONArray("contacts"))
                val notes = jsonToNotes(json.getJSONArray("notes"))

                repository.merge(contacts, notes)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    suspend fun importSettingsOnly(context: Context, uri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val json = readJsonFromUri(context, uri) ?: return@withContext false
                val settings = json.optJSONObject("settings") ?: return@withContext false
                writeSettings(context, settings)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    private fun contactsToJson(contacts: List<ContactEntity>): JSONArray {
        val arr = JSONArray()
        contacts.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("phoneNumber", c.phoneNumber)
                put("displayName", c.displayName)
                putOpt("company", c.company)
                putOpt("note", c.note)
                putOpt("tags", c.tags)
                put("createdAt", c.createdAt)
                put("updatedAt", c.updatedAt)
            })
        }
        return arr
    }

    private fun notesToJson(notes: List<CallNoteEntity>): JSONArray {
        val arr = JSONArray()
        notes.forEach { n ->
            arr.put(JSONObject().apply {
                put("id", n.id)
                put("phoneNumber", n.phoneNumber)
                putOpt("callerName", n.callerName)
                put("noteText", n.noteText)
                put("createdAt", n.createdAt)
                put("updatedAt", n.updatedAt)
            })
        }
        return arr
    }

    private fun jsonToContacts(arr: JSONArray): List<ContactEntity> {
        val list = mutableListOf<ContactEntity>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(ContactEntity(
                id = obj.optLong("id", 0),
                phoneNumber = obj.getString("phoneNumber"),
                displayName = obj.getString("displayName"),
                company = obj.optString("company", null),
                note = obj.optString("note", null),
                tags = obj.optString("tags", null),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
            ))
        }
        return list
    }

    private fun jsonToNotes(arr: JSONArray): List<CallNoteEntity> {
        val list = mutableListOf<CallNoteEntity>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(CallNoteEntity(
                id = obj.optLong("id", 0),
                phoneNumber = obj.getString("phoneNumber"),
                callerName = obj.optString("callerName", null),
                noteText = obj.getString("noteText"),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
            ))
        }
        return list
    }

    private fun readJsonFromUri(context: Context, uri: Uri): JSONObject? {
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            val reader = BufferedReader(InputStreamReader(stream))
            val text = reader.readText()
            JSONObject(text)
        }
    }

    private val excludedKeys = setOf("fab_x", "fab_y", "fab_hidden", "fab_transparency", "backup_uri")

    private fun readSettings(context: Context): JSONObject {
        val prefs = context.getSharedPreferences("cx_call_notes_prefs", Context.MODE_PRIVATE)
        return JSONObject().apply {
            for ((key, _) in prefs.all) {
                if (key in excludedKeys) continue
                when (val value = prefs.all[key]) {
                    is String -> put(key, value)
                    is Int -> put(key, value)
                    is Long -> put(key, value)
                    is Float -> put(key, value)
                    is Boolean -> put(key, value)
                }
            }
        }
    }

    private fun writeSettings(context: Context, settings: JSONObject) {
        val prefs = context.getSharedPreferences("cx_call_notes_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for (key in settings.keys()) {
            when (val value = settings.get(key)) {
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Boolean -> editor.putBoolean(key, value)
            }
        }
        editor.apply()
    }
}
