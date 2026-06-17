package com.example.callnotes

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.callnotes.data.CallNoteEntity
import com.example.callnotes.data.ContactEntity
import com.example.callnotes.theme.CallNotesTheme
import com.example.callnotes.ui.MainUiState
import com.example.callnotes.ui.MainViewModel
import com.example.callnotes.ui.MainViewModelFactory
import com.example.callnotes.ui.PostCallNoteActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as CallNotesApp).container.repository, this)
    }
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            checkOverlayPermission()
        }
    }
    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* Role request completed */ }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionsIfNeeded()
        setContent {
            CallNotesTheme {
                val state by viewModel.state.collectAsState()
                var showSettings by remember { mutableStateOf(false) }
                val parsedAppBg = remember(state.appBgColor) { parseColor(state.appBgColor, Color(0xFFF5F5F5)) }
                Scaffold(
                    modifier = Modifier.background(parsedAppBg),
                    containerColor = parsedAppBg,
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Call,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("CX Call Notes", fontWeight = FontWeight.Bold)
                                }
                            },
                            actions = {
                                IconButton(onClick = {
                                    val intent = Intent(this@MainActivity, PostCallNoteActivity::class.java)
                                    startActivity(intent)
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "Добави")
                                }
                                IconButton(onClick = { showSettings = true }) {
                                    Icon(Icons.Default.Settings, contentDescription = "Настройки")
                                }
                            }
                        )
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        MainScreen(
                            state = state,
                            onTabSelected = viewModel::selectTab,
                            onRefresh = viewModel::load,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onDeleteContact = viewModel::deleteContact,
                            onDeleteNote = viewModel::deleteNote,
                            onLoadMoreContacts = viewModel::loadMoreContacts,
                            onLoadMoreNotes = viewModel::loadMoreNotes,
                            onEditContact = { contact ->
                                val intent = Intent(this@MainActivity, PostCallNoteActivity::class.java).apply {
                                    putExtra(PostCallNoteActivity.EXTRA_PHONE, contact.phoneNumber)
                                }
                                startActivity(intent)
                            },
                            onEditNote = { note ->
                                val intent = Intent(this@MainActivity, PostCallNoteActivity::class.java).apply {
                                    putExtra(PostCallNoteActivity.EXTRA_PHONE, note.phoneNumber)
                                }
                                startActivity(intent)
                            }
                        )
                        if (showSettings) {
                            SettingsDialog(
                                currentAppBg = state.appBgColor,
                                currentContactsBg = state.contactsBgColor,
                                currentNotesBg = state.notesBgColor,
                                currentTags = state.tags,
                                onDismiss = { showSettings = false },
                                onSave = { appBg, contactsBg, notesBg, tags ->
                                    viewModel.saveSettings(appBg, contactsBg, notesBg, tags)
                                    showSettings = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        viewModel.load()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        ) {
            checkOverlayPermission()
        }
    }
    private fun requestPermissionsIfNeeded() {
        val perms = arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG)
        val needed = perms.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        } else {
            checkOverlayPermission()
        }
    }
    private fun checkOverlayPermission() {
        if (!android.provider.Settings.canDrawOverlays(this)) {
            val intent = Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            requestCallScreeningRole()
        }
    }
    private fun requestCallScreeningRole() {
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
            !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        ) {
            roleRequestLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
        }
    }
}

fun parseColor(hex: String, default: Color): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    default
}

@Composable
fun MainScreen(
    state: MainUiState,
    onTabSelected: (Int) -> Unit,
    onRefresh: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onDeleteContact: (ContactEntity) -> Unit,
    onDeleteNote: (CallNoteEntity) -> Unit,
    onLoadMoreContacts: () -> Unit,
    onLoadMoreNotes: () -> Unit,
    onEditContact: (ContactEntity) -> Unit,
    onEditNote: (CallNoteEntity) -> Unit
) {
    Column {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Търсене...") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Изчисти")
                    }
                }
            }
        )
        TabRow(
            selectedTabIndex = state.selectedTab,
            containerColor = Color.Transparent
        ) {
            Tab(
                selected = state.selectedTab == 0,
                onClick = { onTabSelected(0) },
                text = { Text("Контакти") },
                icon = { Icon(Icons.Default.Person, contentDescription = null) }
            )
            Tab(
                selected = state.selectedTab == 1,
                onClick = { onTabSelected(1) },
                text = { Text("Бележки") },
                icon = { Icon(Icons.Default.Edit, contentDescription = null) }
            )
        }
        when (state.selectedTab) {
            0 -> ContactsList(
                contacts = state.contacts,
                limit = state.contactsLimit,
                contactsBgColor = state.contactsBgColor,
                onSelectSearch = onSearchQueryChange,
                onDelete = onDeleteContact,
                onLoadMore = onLoadMoreContacts,
                onEdit = onEditContact
            )
            1 -> NotesList(
                notes = state.notes,
                limit = state.notesLimit,
                notesBgColor = state.notesBgColor,
                onSelectSearch = onSearchQueryChange,
                onDelete = onDeleteNote,
                onLoadMore = onLoadMoreNotes,
                onEdit = onEditNote
            )
        }
    }
}

@Composable
fun ContactsList(
    contacts: List<ContactEntity>,
    limit: Int,
    contactsBgColor: String,
    onSelectSearch: (String) -> Unit,
    onDelete: (ContactEntity) -> Unit,
    onLoadMore: () -> Unit,
    onEdit: (ContactEntity) -> Unit
) {
    if (contacts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Няма контакти",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    } else {
        val visibleContacts = remember(contacts, limit) { contacts.take(limit) }
        val hasMore = remember(contacts, limit) { contacts.size > limit }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(visibleContacts, key = { it.id }) { contact ->
                SwipeToDeleteWrapper(onDelete = { onDelete(contact) }) {
                    ContactCard(contact, contactsBgColor, onSelectSearch, onEdit)
                }
            }
            if (hasMore) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        Button(onClick = onLoadMore) {
                            Text("Покажи още")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ContactCard(
    contact: ContactEntity,
    contactsBgColor: String,
    onSelectSearch: (String) -> Unit,
    onEdit: (ContactEntity) -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = remember(contact.updatedAt) { sdf.format(Date(contact.updatedAt)) }
    val cardBg = remember(contactsBgColor) { parseColor(contactsBgColor, Color(0xFFFFFFFF)) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit(contact) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.combinedClickable(
                        onClick = { onEdit(contact) },
                        onLongClick = { onSelectSearch(contact.displayName) }
                    )
                )
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.combinedClickable(
                        onClick = { onEdit(contact) },
                        onLongClick = { onSelectSearch(contact.phoneNumber) }
                    )
                )
            }
            if (!contact.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                WordSelectableText(contact.note, onSelectSearch)
            }
            if (!contact.tags.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    contact.tags.split(",").filter { it.isNotBlank() }.forEach { tag ->
                        SuggestionChip(
                            onClick = { onSelectSearch(tag) },
                            label = { Text(tag, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                val formattedOnlyDate = remember(contact.updatedAt) { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(contact.updatedAt)) }
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = { onSelectSearch(formattedOnlyDate) }
                    )
                )
            }
        }
    }
}

@Composable
fun NotesList(
    notes: List<CallNoteEntity>,
    limit: Int,
    notesBgColor: String,
    onSelectSearch: (String) -> Unit,
    onDelete: (CallNoteEntity) -> Unit,
    onLoadMore: () -> Unit,
    onEdit: (CallNoteEntity) -> Unit
) {
    if (notes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Няма бележки",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    } else {
        val visibleNotes = remember(notes, limit) { notes.take(limit) }
        val hasMore = remember(notes, limit) { notes.size > limit }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(visibleNotes, key = { it.id }) { note ->
                SwipeToDeleteWrapper(onDelete = { onDelete(note) }) {
                    NoteCard(note, notesBgColor, onSelectSearch, onEdit)
                }
            }
            if (hasMore) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        Button(onClick = onLoadMore) {
                            Text("Покажи още")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: CallNoteEntity,
    notesBgColor: String,
    onSelectSearch: (String) -> Unit,
    onEdit: (CallNoteEntity) -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = remember(note.createdAt) { sdf.format(Date(note.createdAt)) }
    val cardBg = remember(notesBgColor) { parseColor(notesBgColor, Color(0xFFE8F5E9)) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit(note) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = note.callerName ?: "Непознат",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.combinedClickable(
                        onClick = { onEdit(note) },
                        onLongClick = { onSelectSearch(note.callerName ?: "Непознат") }
                    )
                )
                Text(
                    text = note.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.combinedClickable(
                        onClick = { onEdit(note) },
                        onLongClick = { onSelectSearch(note.phoneNumber) }
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            WordSelectableText(note.noteText, onSelectSearch)
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                val formattedOnlyDate = remember(note.createdAt) { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(note.createdAt)) }
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = { onSelectSearch(formattedOnlyDate) }
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun WordSelectableText(text: String, onSelectWord: (String) -> Unit) {
    val words = remember(text) { text.split(Regex("(?<=\\b)|(?=\\b)|\\s+")).filter { it.isNotBlank() } }
    FlowRow(modifier = Modifier.fillMaxWidth()) {
        words.forEach { word ->
            val cleanWord = word.trim().replace(Regex("[.,!?;:]"), "")
            Text(
                text = word + " ",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { if (cleanWord.isNotEmpty()) onSelectWord(cleanWord) }
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteWrapper(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.StartToEnd) {
                onDelete()
                true
            } else {
                false
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Изтрий", tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
        content = { content() }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    currentAppBg: String,
    currentContactsBg: String,
    currentNotesBg: String,
    currentTags: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, List<String>) -> Unit
) {
    var appBg by remember { mutableStateOf(currentAppBg) }
    var contactsBg by remember { mutableStateOf(currentContactsBg) }
    var notesBg by remember { mutableStateOf(currentNotesBg) }
    var tagsInput by remember { mutableStateOf(currentTags.joinToString(",")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Настройки") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Цветове (Hex или име):", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = appBg,
                    onValueChange = { appBg = it },
                    label = { Text("Фон на приложението") }
                )
                OutlinedTextField(
                    value = contactsBg,
                    onValueChange = { contactsBg = it },
                    label = { Text("Фон на панели Контакти") }
                )
                OutlinedTextField(
                    value = notesBg,
                    onValueChange = { notesBg = it },
                    label = { Text("Фон на панели Бележки") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Етикети (до 10, разделени със запетая):", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("Списък етикети") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val tagsList = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(10)
                onSave(appBg, contactsBg, notesBg, tagsList)
            }) {
                Text("Запази")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отказ")
            }
        }
    )
}
