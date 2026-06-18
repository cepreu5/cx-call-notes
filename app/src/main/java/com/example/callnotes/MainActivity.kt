package com.example.callnotes

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
                val defaultAppBg = MaterialTheme.colorScheme.background
                val parsedAppBg = remember(state.appBgColor, defaultAppBg) {
                    if (state.appBgColor == "default") defaultAppBg else parseColor(state.appBgColor, Color(0xFFF5F5F5))
                }
                Scaffold(
                    modifier = Modifier.background(parsedAppBg),
                    containerColor = parsedAppBg,
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Call,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
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
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        ) {
            checkOverlayPermission()
        }
    }
    private fun requestPermissionsIfNeeded() {
        val perms = arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS)
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
    if (hex == "default") default else Color(android.graphics.Color.parseColor(hex))
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
                appBgColor = state.appBgColor,
                contactsBgColor = state.contactsBgColor,
                onSelectSearch = onSearchQueryChange,
                onDelete = onDeleteContact,
                onLoadMore = onLoadMoreContacts,
                onEdit = onEditContact
            )
            1 -> NotesList(
                notes = state.notes,
                limit = state.notesLimit,
                appBgColor = state.appBgColor,
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
    appBgColor: String,
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
                SwipeToDeleteWrapper(appBgColor = appBgColor, onDelete = { onDelete(contact) }) {
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
    val defaultCardBg = MaterialTheme.colorScheme.surfaceContainerLow
    val cardBg = remember(contactsBgColor) { parseColor(contactsBgColor, defaultCardBg) }
    val tagsList = remember(contact.tags) { contact.tags?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList() }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit(contact) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF333333),
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
                Spacer(modifier = Modifier.height(4.dp))
                WordSelectableText(contact.note, onSelectSearch)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tagsList.forEach { tag ->
                        TagChip(tag = tag, onSelectSearch = onSelectSearch)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                val formattedOnlyDate = remember(contact.updatedAt) { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(contact.updatedAt)) }
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666),
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
    appBgColor: String,
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
                SwipeToDeleteWrapper(appBgColor = appBgColor, onDelete = { onDelete(note) }) {
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
    val defaultCardBg = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    val cardBg = remember(notesBgColor) { parseColor(notesBgColor, defaultCardBg) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit(note) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.callerName ?: "Непознат",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF333333),
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
            Spacer(modifier = Modifier.height(4.dp))
            WordSelectableText(note.noteText, onSelectSearch)
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                val formattedOnlyDate = remember(note.createdAt) { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(note.createdAt)) }
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666),
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
                color = Color(0xFF555555),
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { if (cleanWord.isNotEmpty()) onSelectWord(cleanWord) }
                )
            )
        }
    }
}

@Composable
fun TagChip(tag: String, onSelectSearch: (String) -> Unit) {
    Box(
        modifier = Modifier
            .background(Color(0xFFFFE0B2), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFFFF9800), RoundedCornerShape(6.dp))
            .clickable { onSelectSearch(tag) }
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFFE65100)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteWrapper(appBgColor: String, onDelete: () -> Unit, content: @Composable () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.StartToEnd) {
                onDelete()
                true
            } else {
                false
            }
        },
        positionalThreshold = { distance -> distance * 0.8f }
    )
    val parsedAppBg = remember(appBgColor) {
        if (appBgColor == "default") Color.Transparent else parseColor(appBgColor, Color.Transparent)
    }
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            val isSwiping = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isSwiping) MaterialTheme.colorScheme.errorContainer else parsedAppBg)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (isSwiping) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Изтрий",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        },
        content = { content() }
    )
}

@Composable
fun ColorSelectorRow(
    label: String,
    selectedColor: String,
    onColorSelected: (String) -> Unit
) {
    /*val presets = listOf(
        "default" to Color(0xFF37474F),
        "#121212" to Color(0xFF121212),
        "#1A237E" to Color(0xFF1A237E),
        "#1B5E20" to Color(0xFF1B5E20),
        "#3E2723" to Color(0xFF3E2723),
        "#004D40" to Color(0xFF004D40)
    )*/
    val presets = listOf(
        "default" to Color(0xFF6ED3CF),
        "#C39BD3" to Color(0xFFC39BD3),
        "#F9E79F" to Color(0xFFF9E79F),
        "#ABE188" to Color(0xFFABE188),
        "#F7A8B8" to Color(0xFFF7A8B8),
        "#F5B67A" to Color(0xFFF5B67A),
    )
    var showPicker by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            presets.forEach { (code, color) ->
                val isSelected = selectedColor == code
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(color, CircleShape)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(code) }
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.DarkGray, CircleShape)
                    .border(
                        width = if (selectedColor.startsWith("#") && presets.none { it.first == selectedColor }) 3.dp else 1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
                    .clickable { showPicker = true },
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        if (showPicker) {
            CustomColorPickerDialog(
                initialColor = if (selectedColor.startsWith("#")) selectedColor else "#121212",
                onDismiss = { showPicker = false },
                onColorSelected = {
                    onColorSelected(it)
                    showPicker = false
                }
            )
        }
    }
}

@Composable
fun CustomColorPickerDialog(
    initialColor: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    var r by remember { mutableStateOf(20) }
    var g by remember { mutableStateOf(20) }
    var b by remember { mutableStateOf(20) }
    LaunchedEffect(initialColor) {
        try {
            val parsed = android.graphics.Color.parseColor(initialColor)
            r = android.graphics.Color.red(parsed)
            g = android.graphics.Color.green(parsed)
            b = android.graphics.Color.blue(parsed)
        } catch (_: Exception) {}
    }
    val currentColor = Color(r, g, b)
    val hexString = String.format("#%02X%02X%02X", r, g, b)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Избор на цвят") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(currentColor, RoundedCornerShape(8.dp))
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Код: $hexString", fontWeight = FontWeight.Bold)
                Text("Червено: $r")
                Slider(value = r.toFloat(), onValueChange = { r = it.toInt() }, valueRange = 0f..255f)
                Text("Зелено: $g")
                Slider(value = g.toFloat(), onValueChange = { g = it.toInt() }, valueRange = 0f..255f)
                Text("Синьо: $b")
                Slider(value = b.toFloat(), onValueChange = { b = it.toInt() }, valueRange = 0f..255f)
            }
        },
        confirmButton = {
            Button(onClick = { onColorSelected(hexString) }) {
                Text("Избери")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отказ")
            }
        }
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
    var tagsInput by remember { mutableStateOf(currentTags.joinToString(", ")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Настройки") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ColorSelectorRow(
                    label = "Фон на приложението:",
                    selectedColor = appBg,
                    onColorSelected = { appBg = it }
                )
                ColorSelectorRow(
                    label = "Фон на панели Контакти:",
                    selectedColor = contactsBg,
                    onColorSelected = { contactsBg = it }
                )
                ColorSelectorRow(
                    label = "Фон на панели Бележки:",
                    selectedColor = notesBg,
                    onColorSelected = { notesBg = it }
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
