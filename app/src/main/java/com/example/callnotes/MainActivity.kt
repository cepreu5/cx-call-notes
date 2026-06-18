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
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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
                var showFabMenu by remember { mutableStateOf(false) }
                val defaultAppBg = MaterialTheme.colorScheme.background
                val parsedAppBg = remember(state.appBgColor, defaultAppBg) {
                    if (state.appBgColor == "default") defaultAppBg else parseColor(state.appBgColor, Color(0xFFF5F5F5))
                }
                val context = androidx.compose.ui.platform.LocalContext.current
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
                                            .background(Color(0xFFFF9800), CircleShape)
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Call,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("CX Call Notes", fontWeight = FontWeight.Bold)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = parsedAppBg),
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
                    },
                    floatingActionButton = {
                        val density = LocalDensity.current
                        val fabSizeDp = 56.dp
                        val fabSizePx = with(density) { fabSizeDp.toPx() }
                        val menuIconSizeDp = 36.dp
                        val menuIconSizePx = with(density) { menuIconSizeDp.toPx() }
                        val menuRadiusPx = with(density) { 80.dp.toPx() }
                        val iconHalfPx = menuIconSizePx / 2f
                        val fabHalfPx = fabSizePx / 2f
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val parentWidthPx = constraints.maxWidth.toFloat()
                            val parentHeightPx = constraints.maxHeight.toFloat()
                            val maxX = (parentWidthPx - fabSizePx).coerceAtLeast(0f)
                            val maxY = (parentHeightPx - fabSizePx).coerceAtLeast(0f)
                            val initialX = if (state.fabX < 0) maxX else state.fabX.toFloat().coerceIn(0f, maxX)
                            val initialY = if (state.fabY < 0) maxY else state.fabY.toFloat().coerceIn(0f, maxY)
                            var fabXState by remember(parentWidthPx, parentHeightPx) { mutableFloatStateOf(initialX) }
                            var fabYState by remember(parentWidthPx, parentHeightPx) { mutableFloatStateOf(initialY) }
                            val fabCenterX = fabXState + fabHalfPx
                            val fabCenterY = fabYState + fabHalfPx
                            val isRight = fabCenterX >= parentWidthPx / 2f
                            val isBottom = fabCenterY >= parentHeightPx / 2f
                            val inwardX = if (isRight) -1f else 1f
                            val inwardY = if (isBottom) -1f else 1f
                            val r = menuRadiusPx
                            val topMargin = fabCenterY
                            val bottomMargin = parentHeightPx - fabCenterY
                            val neededClearance = r + menuIconSizePx
                            val isNearTopEdge = topMargin < neededClearance
                            val isNearBottomEdge = bottomMargin < neededClearance
                            val useCornerArc = isNearTopEdge || isNearBottomEdge
                            val outer = inwardX
                            val sign = inwardY
                            data class IconSlot(val dx: Float, val dy: Float)
                            val slots: List<IconSlot> = when {
                                useCornerArc -> {
                                    val c30 = 0.866f
                                    val s30 = 0.5f
                                    val c60 = 0.5f
                                    val s60 = 0.866f
                                    listOf(
                                        IconSlot(dx = outer * r, dy = 0f),
                                        IconSlot(dx = outer * (r * c60), dy = sign * (r * s60)),
                                        IconSlot(dx = outer * (r * s60), dy = sign * (r * c60)),
                                        IconSlot(dx = 0f, dy = sign * r)
                                    )
                                }
                                else -> {
                                    val s90 = 1f
                                    val c90 = 0f
                                    val s120 = 0.866f
                                    val c120 = 0.5f
                                    val s150 = 0.5f
                                    val c150 = 0.866f
                                    val s180 = 0f
                                    val c180 = 1f
                                    listOf(
                                        IconSlot(dx = outer * (r * c90), dy = sign * (r * s90)),
                                        IconSlot(dx = outer * (r * c120), dy = sign * (r * s120)),
                                        IconSlot(dx = outer * (r * c150), dy = sign * (r * s150)),
                                        IconSlot(dx = outer * (r * c180), dy = sign * (r * s180))
                                    )
                                }
                            }
                            val slot0 = slots[0]
                            val slot1 = slots[1]
                            val slot2 = slots[2]
                            val slot3 = slots[3]
                            if (showFabMenu) {
                                Box(
                                    modifier = Modifier
                                        .size(menuIconSizeDp)
                                        .offset {
                                            IntOffset(
                                                (fabXState + (fabSizePx - menuIconSizePx) / 2f + slot0.dx).toInt(),
                                                (fabYState + (fabSizePx - menuIconSizePx) / 2f + slot0.dy).toInt()
                                            )
                                        }
                                        .background(Color(0xFFE0E0E0), CircleShape)
                                        .clickable {
                                            showFabMenu = false
                                            viewModel.selectTab(0)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = "Контакти", modifier = Modifier.size(20.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .size(menuIconSizeDp)
                                        .offset {
                                            IntOffset(
                                                (fabXState + (fabSizePx - menuIconSizePx) / 2f + slot1.dx).toInt(),
                                                (fabYState + (fabSizePx - menuIconSizePx) / 2f + slot1.dy).toInt()
                                            )
                                        }
                                        .background(Color(0xFFE0E0E0), CircleShape)
                                        .clickable {
                                            showFabMenu = false
                                            viewModel.selectTab(1)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Бележки", modifier = Modifier.size(20.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .size(menuIconSizeDp)
                                        .offset {
                                            IntOffset(
                                                (fabXState + (fabSizePx - menuIconSizePx) / 2f + slot2.dx).toInt(),
                                                (fabYState + (fabSizePx - menuIconSizePx) / 2f + slot2.dy).toInt()
                                            )
                                        }
                                        .background(Color(0xFFE0E0E0), CircleShape)
                                        .clickable {
                                            showFabMenu = false
                                            showSettings = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = "Настройки", modifier = Modifier.size(20.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .size(menuIconSizeDp)
                                        .offset {
                                            IntOffset(
                                                (fabXState + (fabSizePx - menuIconSizePx) / 2f + slot3.dx).toInt(),
                                                (fabYState + (fabSizePx - menuIconSizePx) / 2f + slot3.dy).toInt()
                                            )
                                        }
                                        .background(Color(0xFFE0E0E0), CircleShape)
                                        .clickable {
                                            showFabMenu = false
                                            val intent = Intent(this@MainActivity, PostCallNoteActivity::class.java)
                                            startActivity(intent)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Добави бележка", modifier = Modifier.size(20.dp))
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(fabXState.toInt(), fabYState.toInt()) }
                                    .size(fabSizeDp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape)
                                    .pointerInput(showFabMenu) {
                                        if (!showFabMenu) {
                                            detectDragGestures(
                                                onDragEnd = {
                                                    viewModel.saveFabPosition(fabXState.toInt(), fabYState.toInt())
                                                }
                                            ) { change, dragAmount ->
                                                change.consume()
                                                fabXState = (fabXState + dragAmount.x).coerceIn(0f, maxX)
                                                fabYState = (fabYState + dragAmount.y).coerceIn(0f, maxY)
                                            }
                                        }
                                    }
                                    .combinedClickable(
                                        onClick = { showFabMenu = !showFabMenu },
                                        onLongClick = {
                                            (context as? android.app.Activity)?.moveTaskToBack(true)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Меню", tint = Color.White)
                            }
                        }
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
                                    putExtra(PostCallNoteActivity.EXTRA_NOTE_ID, note.id)
                                }
                                startActivity(intent)
                            }
                        )
                        if (showSettings) {
                            SettingsDialog(
                                currentAppBg = state.appBgColor,
                                currentContactsBg = state.contactsBgColor,
                                currentNotesBg = state.notesBgColor,
                                currentFontColor = state.fontColor,
                                currentFormBgColor = state.formBgColor,
                                currentThemePrimary = state.themePrimary,
                                currentThemeSecondary = state.themeSecondary,
                                currentThemeTertiary = state.themeTertiary,
                                currentTags = state.tags,
                                onDismiss = { showSettings = false },
                                onSave = { appBg, contactsBg, notesBg, fontColor, formBg, themePrimary, themeSecondary, themeTertiary, tags ->
                                    viewModel.saveSettings(appBg, contactsBg, notesBg, fontColor, formBg, themePrimary, themeSecondary, themeTertiary, tags)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                    color = MaterialTheme.colorScheme.secondary,
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
                Text(
                    text = contact.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.combinedClickable(
                        onClick = { onEdit(contact) },
                        onLongClick = { onSelectSearch(contact.note) }
                    )
                )
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
    val presets = listOf(
        "#C39BD3" to Color(0xFFC39BD3),
        "#F9E79F" to Color(0xFFF9E79F),
        "#ABE188" to Color(0xFFABE188),
        "#F7A8B8" to Color(0xFFF7A8B8),
        "#F5B67A" to Color(0xFFF5B67A),
    )
    var showPicker by remember { mutableStateOf(false) }
    val currentSelectedBg = remember(selectedColor) {
        if (selectedColor == "default") Color(0xFF6ED3CF) else parseColor(selectedColor, Color.Gray)
    }
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(currentSelectedBg, RoundedCornerShape(4.dp))
                    .border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(4.dp)
                    )
            )
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
    var hexInput by remember { mutableStateOf(initialColor) }
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
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = {
                        hexInput = it
                        if (it.length == 7 && it.startsWith("#")) {
                            try {
                                val p = android.graphics.Color.parseColor(it)
                                r = android.graphics.Color.red(p)
                                g = android.graphics.Color.green(p)
                                b = android.graphics.Color.blue(p)
                            } catch (_: java.lang.Exception) {}
                        }
                    },
                    label = { Text("HEX код") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = r.toString(),
                    onValueChange = {
                        val v = it.toIntOrNull() ?: 0
                        r = v.coerceIn(0, 255)
                        hexInput = String.format("#%02X%02X%02X", r, g, b)
                    },
                    label = { Text("Червено (0-255)") },
                    singleLine = true
                )
                Slider(value = r.toFloat(), onValueChange = {
                    r = it.toInt()
                    hexInput = String.format("#%02X%02X%02X", r, g, b)
                }, valueRange = 0f..255f)
                OutlinedTextField(
                    value = g.toString(),
                    onValueChange = {
                        val v = it.toIntOrNull() ?: 0
                        g = v.coerceIn(0, 255)
                        hexInput = String.format("#%02X%02X%02X", r, g, b)
                    },
                    label = { Text("Зелено (0-255)") },
                    singleLine = true
                )
                Slider(value = g.toFloat(), onValueChange = {
                    g = it.toInt()
                    hexInput = String.format("#%02X%02X%02X", r, g, b)
                }, valueRange = 0f..255f)
                OutlinedTextField(
                    value = b.toString(),
                    onValueChange = {
                        val v = it.toIntOrNull() ?: 0
                        b = v.coerceIn(0, 255)
                        hexInput = String.format("#%02X%02X%02X", r, g, b)
                    },
                    label = { Text("Синьо (0-255)") },
                    singleLine = true
                )
                Slider(value = b.toFloat(), onValueChange = {
                    b = it.toInt()
                    hexInput = String.format("#%02X%02X%02X", r, g, b)
                }, valueRange = 0f..255f)
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
    currentFontColor: String,
    currentFormBgColor: String,
    currentThemePrimary: String,
    currentThemeSecondary: String,
    currentThemeTertiary: String,
    currentTags: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String, String, List<String>) -> Unit
) {
    var appBg by remember { mutableStateOf(currentAppBg) }
    var contactsBg by remember { mutableStateOf(currentContactsBg) }
    var notesBg by remember { mutableStateOf(currentNotesBg) }
    var fontColor by remember { mutableStateOf(currentFontColor) }
    var formBgColor by remember { mutableStateOf(currentFormBgColor) }
    var themePrimary by remember { mutableStateOf(currentThemePrimary) }
    var themeSecondary by remember { mutableStateOf(currentThemeSecondary) }
    var themeTertiary by remember { mutableStateOf(currentThemeTertiary) }
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
                ColorSelectorRow(
                    label = "Фон на форми и настройки:",
                    selectedColor = formBgColor,
                    onColorSelected = { formBgColor = it }
                )
                ColorSelectorRow(
                    label = "Цвят на шрифт на форми:",
                    selectedColor = fontColor,
                    onColorSelected = { fontColor = it }
                )
                ColorSelectorRow(
                    label = "Основен цвят на темата (Primary):",
                    selectedColor = themePrimary,
                    onColorSelected = { themePrimary = it }
                )
                ColorSelectorRow(
                    label = "Вторичен цвят на темата (Secondary):",
                    selectedColor = themeSecondary,
                    onColorSelected = { themeSecondary = it }
                )
                ColorSelectorRow(
                    label = "Третичен цвят на темата (Tertiary):",
                    selectedColor = themeTertiary,
                    onColorSelected = { themeTertiary = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Етикети (до 20, разделени със запетая):", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("Списък етикети") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val tagsList = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(20)
                onSave(appBg, contactsBg, notesBg, fontColor, formBgColor, themePrimary, themeSecondary, themeTertiary, tagsList)
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
