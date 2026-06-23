package com.example.callnotes.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.callnotes.CallNotesApp
import com.example.callnotes.theme.CallNotesTheme
import com.example.callnotes.theme.ColorConstants

fun parseColor(hex: String, default: Color): Color = try {
    if (hex == "default") default else Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    default
}

class PostCallNoteActivity : ComponentActivity() {
    private val viewModel: PostCallNoteViewModel by viewModels {
        PostCallNoteViewModelFactory((application as CallNotesApp).container.repository, this)
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val phone = intent.getStringExtra(EXTRA_PHONE) ?: ""
        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1).takeIf { it >= 0 }
        val callDirection = intent.getStringExtra(EXTRA_CALL_DIRECTION)
        if (phone.isNotBlank() || noteId != null) {
            viewModel.init(phone, noteId, callDirection)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val systemHandle = intent.getParcelableExtra<android.net.Uri>(android.telecom.TelecomManager.EXTRA_HANDLE)
        val phone = systemHandle?.schemeSpecificPart ?: intent.getStringExtra(EXTRA_PHONE) ?: ""
        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1).takeIf { it >= 0 }
        val fromCall = intent.getBooleanExtra(EXTRA_FROM_CALL, false)
        val callDirection = intent.getStringExtra(EXTRA_CALL_DIRECTION)
        viewModel.init(phone, noteId, callDirection)
        val prefs = getSharedPreferences("cx_call_notes_prefs", android.content.Context.MODE_PRIVATE)
        val backupFrequency = prefs.getInt("backup_frequency_days", 7)
        val lastBackupDate = prefs.getLong("last_backup_date", 0L)
        val backupDue = (System.currentTimeMillis() - lastBackupDate) > backupFrequency * 24L * 60 * 60 * 1000
        setContent {
            val prefs = getSharedPreferences("cx_call_notes_prefs", android.content.Context.MODE_PRIVATE)
            CallNotesTheme(
                themePrimary = prefs.getString("theme_primary", "default") ?: "default",
                themeSecondary = prefs.getString("theme_secondary", "default") ?: "default",
                themeTertiary = prefs.getString("theme_tertiary", "default") ?: "default"
            ) {
                val formBg = remember { prefs.getString("form_bg_color", "default") ?: "default" }
                val fontCol = remember { prefs.getString("font_color", "default") ?: "default" }
                val state by viewModel.uiState.collectAsState()
                var showBackupReminder by remember { mutableStateOf(false) }
                val repository = (application as CallNotesApp).container.repository
                val backupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
                ) { uri ->
                    if (uri != null) {
                        lifecycleScope.launch {
                            val success = com.example.callnotes.data.BackupManager.exportFull(
                                this@PostCallNoteActivity, uri, repository
                            )
                            if (success) {
                                prefs.edit()
                                    .putLong("last_backup_date", System.currentTimeMillis())
                                    .putString("backup_uri", uri.toString())
                                    .apply()
                            }
                        }
                    }
                    if (fromCall) moveTaskToBack(true)
                    finish()
                }
                val savedBackupUri = prefs.getString("backup_uri", null)
                if (savedBackupUri != null) {
                    val savedUri = android.net.Uri.parse(savedBackupUri)
                    LaunchedEffect(Unit) {
                        lifecycleScope.launch {
                            val exists = try {
                                contentResolver.openInputStream(savedUri)?.close()
                                true
                            } catch (_: Exception) { false }
                            if (exists) {
                                val success = com.example.callnotes.data.BackupManager.exportFull(
                                    this@PostCallNoteActivity, savedUri, repository
                                )
                                if (success) {
                                    prefs.edit().putLong("last_backup_date", System.currentTimeMillis()).apply()
                                }
                                if (fromCall) moveTaskToBack(true)
                                finish()
                            } else {
                                prefs.edit().remove("backup_uri").apply()
                                backupLauncher.launch("cx-call-notes-backup.json")
                            }
                        }
                    }
                }
                LaunchedEffect(state.saved) {
                    if (state.saved) {
                        if (backupDue) showBackupReminder = true
                        else {
                            if (fromCall) moveTaskToBack(true)
                            finish()
                        }
                    }
                }
                if (showBackupReminder) {
                    AlertDialog(
                        onDismissRequest = {
                            showBackupReminder = false
                            if (fromCall) moveTaskToBack(true)
                            finish()
                        },
                        title = { Text("Напомняне за бекъп") },
                        text = { Text("Последният бекъп е преди повече от $backupFrequency дни. Искате ли да направите бекъп сега?") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showBackupReminder = false
                                    backupLauncher.launch("cx-call-notes-backup.json")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ColorConstants.ButtonBackground,
                                    contentColor = ColorConstants.ButtonFontColor
                                )
                            ) { Text("Backup сега") }
                        },
                        dismissButton = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        showBackupReminder = false
                                        prefs.edit().putLong("last_backup_date", System.currentTimeMillis()).apply()
                                        if (fromCall) moveTaskToBack(true)
                                        finish()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ColorConstants.ButtonBackground,
                                        contentColor = ColorConstants.ButtonFontColor
                                    )
                                ) { Text("Отложи") }
                                Button(
                                    onClick = {
                                        showBackupReminder = false
                                        if (fromCall) moveTaskToBack(true)
                                        finish()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ColorConstants.ButtonBackground,
                                        contentColor = ColorConstants.ButtonFontColor
                                    )
                                ) { Text("Отказ") }
                            }
                        }
                    )
                } else {
                    PostCallNoteScreen(
                        state = state,
                        formBgColor = formBg,
                        fontColor = fontCol,
                        onPhoneChange = viewModel::updatePhoneNumber,
                        onCallerNameChange = viewModel::updateCallerName,
                        onNoteTextChange = viewModel::updateNoteText,
                        onTagToggle = viewModel::toggleTag,
                        onSave = viewModel::save,
                        onUpdate = viewModel::updateNote,
                        onDismiss = {
                            if (fromCall) moveTaskToBack(true)
                            finish()
                        }
                    )
                }
            }
        }
    }
    companion object {
        const val EXTRA_PHONE = "extra_phone"
        const val EXTRA_NOTE_ID = "extra_note_id"
        const val EXTRA_FROM_CALL = "extra_from_call"
        const val EXTRA_CALL_DIRECTION = "extra_call_direction"
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PostCallNoteScreen(
    state: PostCallNoteUiState,
    formBgColor: String,
    fontColor: String,
    onPhoneChange: (String) -> Unit,
    onCallerNameChange: (String) -> Unit,
    onNoteTextChange: (String) -> Unit,
    onTagToggle: (String) -> Unit,
    onSave: () -> Unit,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    val defaultBg = MaterialTheme.colorScheme.background
    val parsedBg = remember(formBgColor, defaultBg) {
        if (formBgColor == "default") defaultBg else parseColor(formBgColor, defaultBg)
    }
    val defaultFont = MaterialTheme.colorScheme.onBackground
    val parsedFont = remember(fontColor, defaultFont) {
        if (fontColor == "default") defaultFont else parseColor(fontColor, defaultFont)
    }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = parsedBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            state.isEditMode -> "📝 Редактиране"
                            state.isNewContact -> "📝 Нов контакт"
                            else -> "📝 Нова бележка"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = parsedFont
                    )
                    IconButton(onClick = {
                        onPhoneChange("")
                        onCallerNameChange("")
                        onNoteTextChange("")
                    }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Изчисти",
                            tint = parsedFont
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.phoneNumber,
                    onValueChange = onPhoneChange,
                    label = { Text("Телефон") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = parsedFont,
                        unfocusedTextColor = parsedFont
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.callerName,
                    onValueChange = onCallerNameChange,
                    label = { Text("Име") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = parsedFont,
                        unfocusedTextColor = parsedFont
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.noteText,
                    onValueChange = onNoteTextChange,
                    label = { Text("Бележка") },
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = parsedFont,
                        unfocusedTextColor = parsedFont
                    ),
                    trailingIcon = {
                        if (state.noteText.isNotEmpty()) {
                            IconButton(onClick = { onNoteTextChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Изчисти", tint = parsedFont)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (state.availableTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Етикети:",
                        color = parsedFont,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.availableTags.forEach { tag ->
                            val selected = state.selectedTags.contains(tag)
                            FormTagChip(tag = tag, selected = selected, onClick = { onTagToggle(tag) })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorConstants.ButtonBackground,
                            contentColor = ColorConstants.ButtonFontColor
                        )
                    ) {
                        Text("Отказ", maxLines = 1, softWrap = false)
                    }
                    if (state.isEditMode) {
                        Button(
                            onClick = onUpdate,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorConstants.ButtonBackground,
                                contentColor = ColorConstants.ButtonFontColor
                            )
                        ) {
                            Text("Обнови", maxLines = 1, softWrap = false)
                        }
                    }
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorConstants.ButtonBackground,
                            contentColor = ColorConstants.ButtonFontColor
                        )
                    ) {
                        Text("Добави", maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
fun FormTagChip(tag: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (selected) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.tertiary else Color.LightGray,
                RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
            color = if (selected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.outline
        )
    }
}
