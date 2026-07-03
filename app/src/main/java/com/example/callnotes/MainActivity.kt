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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.callnotes.data.CallNoteEntity
import com.example.callnotes.data.ContactEntity
import com.example.callnotes.theme.CallNotesTheme
import com.example.callnotes.theme.ColorConstants
import com.example.callnotes.ui.MainUiState
import com.example.callnotes.ui.MainViewModel
import com.example.callnotes.ui.MainViewModelFactory
import com.example.callnotes.ui.PostCallNoteViewModel
import com.example.callnotes.ui.PostCallNoteViewModelFactory
import com.example.callnotes.ui.PostCallNoteScreen
import com.example.callnotes.ui.RecentCall
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as CallNotesApp).container.repository, this)
    }
    private val noteViewModel: PostCallNoteViewModel by viewModels {
        PostCallNoteViewModelFactory((application as CallNotesApp).container.repository, this)
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
            val state by viewModel.state.collectAsState()
            CallNotesTheme(
                themePrimary = state.themePrimary,
                themeSecondary = state.themeSecondary,
                themeTertiary = state.themeTertiary
            ) {
                var showSettings by remember { mutableStateOf(false) }
                var showFabMenu by remember { mutableStateOf(false) }
                var showPostCallNote by remember { mutableStateOf(false) }
                var shouldMinimize by remember { mutableStateOf(false) }
                var fromCall by remember { mutableStateOf(false) }
                var callConfirmPhone by remember { mutableStateOf<Pair<String, String>?>(null) }
                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()
                val noteState by noteViewModel.uiState.collectAsState()
                val prefs = remember { getSharedPreferences("cx_call_notes_prefs", Context.MODE_PRIVATE) }
                val formBg = remember { prefs.getString("form_bg_color", "default") ?: "default" }
                val fontCol = remember { prefs.getString("font_color", "default") ?: "default" }
                val backupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
                ) { uri ->
                    if (uri != null) {
                        coroutineScope.launch {
                            val success = com.example.callnotes.data.BackupManager.exportFull(
                                this@MainActivity, uri, (application as CallNotesApp).container.repository
                            )
                            if (success) {
                                prefs.edit()
                                    .putLong("last_backup_date", System.currentTimeMillis())
                                    .putString("backup_uri", uri.toString())
                                    .apply()
                                viewModel.loadSettings()
                                snackbarHostState.showSnackbar("Архивът е записан")
                            } else {
                                snackbarHostState.showSnackbar("Грешка при запис")
                            }
                        }
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Записването е отменено")
                        }
                    }
                }
                val restoreLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) {
                        coroutineScope.launch {
                            val success = com.example.callnotes.data.BackupManager.importFull(
                                this@MainActivity, uri, (application as CallNotesApp).container.repository
                            )
                            if (success) {
                                viewModel.load()
                                snackbarHostState.showSnackbar("Данните са възстановени")
                            } else {
                                snackbarHostState.showSnackbar("Грешка при възстановяване")
                            }
                        }
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Възстановяването е отменено")
                        }
                    }
                }
                val settingsBackupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
                ) { uri ->
                    if (uri != null) {
                        coroutineScope.launch {
                            val success = com.example.callnotes.data.BackupManager.exportSettingsOnly(
                                this@MainActivity, uri
                            )
                            if (success) {
                                snackbarHostState.showSnackbar("Настройките са записани")
                            } else {
                                snackbarHostState.showSnackbar("Грешка при запис")
                            }
                        }
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Записването е отменено")
                        }
                    }
                }
                val settingsRestoreLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) {
                        coroutineScope.launch {
                            val success = com.example.callnotes.data.BackupManager.importSettingsOnly(
                                this@MainActivity, uri
                            )
                            if (success) {
                                viewModel.loadSettings()
                                snackbarHostState.showSnackbar("Настройките са възстановени")
                            } else {
                                snackbarHostState.showSnackbar("Грешка при възстановяване")
                            }
                        }
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Възстановяването е отменено")
                        }
                    }
                }
                LaunchedEffect(shouldMinimize) {
                    if (shouldMinimize) {
                        shouldMinimize = false
                        this@MainActivity.moveTaskToBack(true)
                    }
                }
                val defaultAppBg = MaterialTheme.colorScheme.background
                val parsedAppBg = remember(state.appBgColor, defaultAppBg) {
                    if (state.appBgColor == "default") defaultAppBg else parseColor(state.appBgColor, Color(0xFFF5F5F5))
                }
                val context = androidx.compose.ui.platform.LocalContext.current
                var showBackupReminder by remember { mutableStateOf(false) }
                LaunchedEffect(state.backupFrequency, state.lastBackupDate) {
                    if (state.backupFrequency > 0 && state.lastBackupDate > 0L) {
                        val elapsed = System.currentTimeMillis() - state.lastBackupDate
                        val threshold = state.backupFrequency * 24L * 60 * 60 * 1000
                        if (elapsed > threshold) showBackupReminder = true
                    }
                }
                if (showBackupReminder) {
                    AlertDialog(
                        onDismissRequest = { showBackupReminder = false },
                        title = { Text("Напомняне за бекъп") },
                        text = { Text("Последният бекъп е преди повече от ${state.backupFrequency} дни. Искате ли да направите бекъп сега?") },
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
                                        viewModel.loadSettings()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ColorConstants.ButtonBackground,
                                        contentColor = ColorConstants.ButtonFontColor
                                    )
                                ) { Text("Отложи") }
                                Button(
                                    onClick = { showBackupReminder = false },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ColorConstants.ButtonBackground,
                                        contentColor = ColorConstants.ButtonFontColor
                                    )
                                ) { Text("Отказ") }
                            }
                        }
                    )
                }
                Scaffold(
                    modifier = Modifier.background(parsedAppBg),
                    containerColor = parsedAppBg,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.foundation.Image(
                                            painter = androidx.compose.ui.res.painterResource(R.drawable.ic_launcher_foreground),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "CX Call Notes",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = parsedAppBg),
                            actions = {
                                IconButton(onClick = {
                                    fromCall = false
                                    noteViewModel.init("")
                                    showPostCallNote = true
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "Добави", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { showSettings = true }) {
                                    Icon(Icons.Default.Settings, contentDescription = "Настройки", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                    },
                ) { padding ->
                    val density = LocalDensity.current
                    val fabSizeDp = 45.dp
                    val fabSizePx = with(density) { fabSizeDp.toPx() }
                    val menuIconSizeDp = 32.dp
                    val menuIconSizePx = with(density) { menuIconSizeDp.toPx() }
                    val menuRadiusPx = with(density) { 72.dp.toPx() }
                    val fabHalfPx = fabSizePx / 2f
                    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
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
                                fromCall = false
                                noteViewModel.init(contact.phoneNumber)
                                showPostCallNote = true
                            },
                            onEditNote = { note ->
                                fromCall = false
                                noteViewModel.init("", note.id)
                                showPostCallNote = true
                            },
                            onLongCall = { phone, name ->
                                callConfirmPhone = phone to name
                            },
                            onCallDirectionFilter = viewModel::toggleCallDirectionFilter
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
                                currentFabTransparency = state.fabTransparency,
                                currentFabHidden = state.fabHidden,
                                currentBackupFrequency = state.backupFrequency,
                                currentLastBackupDate = state.lastBackupDate,
                                onDismiss = { showSettings = false },
                                onSave = { appBg, contactsBg, notesBg, fontColor, formBg, themePrimary, themeSecondary, themeTertiary, tags ->
                                    val needsRestart = fontColor != state.fontColor ||
                                        formBg != state.formBgColor ||
                                        themePrimary != state.themePrimary ||
                                        themeSecondary != state.themeSecondary ||
                                        themeTertiary != state.themeTertiary
                                    showSettings = false
                                    viewModel.saveSettings(appBg, contactsBg, notesBg, fontColor, formBg, themePrimary, themeSecondary, themeTertiary, tags)
                                    if (needsRestart) {
                                        //coroutineScope.launch {
                                        //  snackbarHostState.showSnackbar("Промените изискват рестарт на приложението", duration = SnackbarDuration.Long)
                                        //}
                                    }
                                },
                            onFabTransparencyChange = { viewModel.saveFabTransparency(it) },
                            onFabHiddenChange = { viewModel.saveFabHidden(it) },
                            onBackupFrequencyChange = { viewModel.saveBackupFrequency(it) },
                            onBackupClick = {
                                showSettings = false
                                backupLauncher.launch("cx-call-notes-backup.json")
                            },
                            onRestoreClick = { showSettings = false; restoreLauncher.launch(arrayOf("application/json")) },
                            onSettingsBackupClick = { showSettings = false; settingsBackupLauncher.launch("cx-call-notes-settings.json") },
                            onSettingsRestoreClick = { showSettings = false; settingsRestoreLauncher.launch(arrayOf("application/json")) },
                            onReset = {
                                val ctx = this@MainActivity
                                ctx.getSharedPreferences("cx_call_notes_prefs", Context.MODE_PRIVATE).edit().clear().commit()
                                val restartIntent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
                                ctx.startActivity(restartIntent)
                                ctx.finish()
                                Runtime.getRuntime().exit(0)
                            }
                            )
                        }
                        if (callConfirmPhone != null) {
                            val (phone, name) = callConfirmPhone!!
                            AlertDialog(
                                onDismissRequest = { callConfirmPhone = null },
                                title = { Text("Обаждане") },
                                text = {
                                    Text(if (name.isNotBlank()) "Обади се на $name ($phone)?" else "Обади се на $phone?")
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            callConfirmPhone = null
                                            val intent = Intent(Intent.ACTION_CALL, android.net.Uri.parse("tel:$phone"))
                                            startActivity(intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ColorConstants.ButtonBackground,
                                            contentColor = ColorConstants.ButtonFontColor
                                        )
                                    ) { Text("Да") }
                                },
                                dismissButton = {
                                    Button(
                                        onClick = { callConfirmPhone = null },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ColorConstants.ButtonBackground,
                                            contentColor = ColorConstants.ButtonFontColor
                                        )
                                    ) { Text("Отказ") }
                                }
                            )
                        }
                        if (showPostCallNote) {
                            LaunchedEffect(noteState.saved) {
                                if (noteState.saved) {
                                    showPostCallNote = false
                                    viewModel.load()
                                }
                            }
                            PostCallNoteScreen(
                                state = noteState,
                                formBgColor = formBg,
                                fontColor = fontCol,
                                onPhoneChange = noteViewModel::updatePhoneNumber,
                                onCallerNameChange = noteViewModel::updateCallerName,
                                onNoteTextChange = noteViewModel::updateNoteText,
                                onTagToggle = noteViewModel::toggleTag,
                                onSave = noteViewModel::save,
                                onUpdate = noteViewModel::updateNote,
                                onLoadRecentCalls = noteViewModel::loadRecentCalls,
                                onSelectRecentCall = noteViewModel::selectRecentCall,
                                onDismiss = {
                                    showPostCallNote = false
                                    viewModel.load()
                                    if (fromCall) shouldMinimize = true
                                }
                            )
                        }
                    }
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        if (!state.fabHidden) {
                        val screenW = constraints.maxWidth.toFloat()
                        val screenH = constraints.maxHeight.toFloat()
                        val maxX = (screenW - fabSizePx).coerceAtLeast(0f)
                        val maxY = (screenH - fabSizePx).coerceAtLeast(0f)
                        val defaultX = maxX
                        val defaultY = maxY / 2f
                        val initialX = if (state.fabX < 0) defaultX else state.fabX.toFloat().coerceIn(0f, maxX)
                        val initialY = if (state.fabY < 0) defaultY else state.fabY.toFloat().coerceIn(0f, maxY)
                        var fabXState by remember { mutableFloatStateOf(initialX) }
                        var fabYState by remember { mutableFloatStateOf(initialY) }
                        val fabAlpha = state.fabTransparency / 100f
                        val fabCenterX = fabXState + fabHalfPx
                        val fabCenterY = fabYState + fabHalfPx
                        val inwardX = if (fabCenterX >= screenW / 2f) -1f else 1f
                        val inwardY = if (fabCenterY >= screenH / 2f) -1f else 1f
                        val r = menuRadiusPx
                        val neededClearance = r + menuIconSizePx
                        val isNearTop = fabCenterY < neededClearance
                        val isNearBottom = (screenH - fabCenterY) < neededClearance
                        val isNearLeft = fabCenterX < neededClearance
                        val isNearRight = (screenW - fabCenterX) < neededClearance
                        data class IconSlot(val dx: Float, val dy: Float)
                        fun semicircleSlots(centerAngleRad: Double): List<IconSlot> {
                            return (1..4).map { i ->
                                val angle = centerAngleRad - 5.0 * Math.PI / 8.0 + (Math.PI / 4.0) * i
                                IconSlot((r * Math.cos(angle)).toFloat(), (r * Math.sin(angle)).toFloat())
                            }
                        }
                        fun cornerSlots(dirX: Float, dirY: Float): List<IconSlot> {
                            val c60 = 0.5f; val s60 = 0.866f
                            return listOf(
                                IconSlot(dx = dirX * r, dy = 0f),
                                IconSlot(dx = dirX * (r * c60), dy = dirY * (r * s60)),
                                IconSlot(dx = dirX * (r * s60), dy = dirY * (r * c60)),
                                IconSlot(dx = 0f, dy = dirY * r)
                            )
                        }
                        val allSlots: List<IconSlot> = when {
                            (isNearLeft || isNearRight) && (isNearTop || isNearBottom) -> {
                                val cx = if (isNearRight) -1f else 1f
                                val cy = if (isNearBottom) -1f else 1f
                                cornerSlots(cx, cy)
                            }
                            isNearLeft || isNearRight -> {
                                val centerAngle = if (isNearRight) Math.PI else 0.0
                                semicircleSlots(centerAngle)
                            }
                            isNearTop || isNearBottom -> {
                                val centerAngle = if (isNearBottom) -Math.PI / 2 else Math.PI / 2
                                semicircleSlots(centerAngle)
                            }
                            else -> cornerSlots(inwardX, inwardY)
                        }
                        if (showFabMenu) {
                            allSlots.forEachIndexed { index, slot ->
                                val action: () -> Unit = when (index) {
                                    0 -> { { showFabMenu = false; viewModel.selectTab(0) } }
                                    1 -> { { showFabMenu = false; viewModel.selectTab(1) } }
                                    2 -> { { showFabMenu = false; fromCall = false; noteViewModel.init(""); showPostCallNote = true } }
                                    3 -> { { showFabMenu = false; showSettings = true } }
                                    else -> { { showFabMenu = false } }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(menuIconSizeDp)
                                        .offset {
                                            IntOffset(
                                                (fabXState + (fabSizePx - menuIconSizePx) / 2f + slot.dx).toInt(),
                                                (fabYState + (fabSizePx - menuIconSizePx) / 2f + slot.dy).toInt()
                                            )
                                        }
                                        .shadow(2.dp, CircleShape)
                                        .background(Color.White, CircleShape)
                                        .border(0.5.dp, Color.LightGray, CircleShape)
                                        .clickable { action() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (index) {
                        0 -> Icon(Icons.Default.Person, contentDescription = "Контакти", modifier = Modifier.size(22.dp), tint = Color.DarkGray)
                        1 -> Icon(Icons.AutoMirrored.Default.Note, contentDescription = "Бележки", modifier = Modifier.size(22.dp), tint = Color.DarkGray)
                        2 -> Icon(Icons.Default.Add, contentDescription = "Добави бележка", modifier = Modifier.size(22.dp), tint = Color.DarkGray)
                        3 -> Icon(Icons.Default.Settings, contentDescription = "Настройки", modifier = Modifier.size(22.dp), tint = Color.DarkGray)
                                    }
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(fabXState.toInt(), fabYState.toInt()) }
                                .size(fabSizeDp)
                                .background(Color.White.copy(alpha = fabAlpha), CircleShape)
                                .border(1.dp, Color.DarkGray.copy(alpha = 0.4f * fabAlpha), CircleShape)
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
                                    onLongClick = { (context as? android.app.Activity)?.moveTaskToBack(true) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Меню", tint = Color.DarkGray.copy(alpha = fabAlpha), modifier = Modifier.size(20.dp))
                        }
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
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        ) {
            checkOverlayPermission()
        }
    }
    private fun requestPermissionsIfNeeded() {
        val perms = if (android.os.Build.VERSION.SDK_INT >= 33) {
            arrayOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CALL_PHONE
            )
        }
        val needed = perms.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) {
            val prefs = getSharedPreferences("cx_call_notes_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .remove("theme_primary").remove("theme_secondary").remove("theme_tertiary")
                .remove("app_bg_color").remove("contacts_bg_color").remove("notes_bg_color")
                .remove("font_color").remove("form_bg_color")
                .commit()
            permissionLauncher.launch(needed.toTypedArray())
        } else {
            checkOverlayPermission()
        }
    }
    private fun checkOverlayPermission() {
        if (!android.provider.Settings.canDrawOverlays(this)) {
            try {
                val intent = Intent(
                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } catch (_: Exception) {}
        } else {
            requestCallScreeningRole()
        }
    }
    private fun requestCallScreeningRole() {
        try {
            val roleManager = getSystemService(RoleManager::class.java) ?: return
            if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
            ) {
                roleRequestLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
            }
        } catch (_: Exception) {}
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
    onEditNote: (CallNoteEntity) -> Unit,
    onLongCall: (String, String) -> Unit,
    onCallDirectionFilter: (String?) -> Unit
) {
    val contactsListState = rememberLazyListState()
    val notesListState = rememberLazyListState()
    LaunchedEffect(state.contacts.size) {
        if (state.contacts.isNotEmpty()) {
            contactsListState.scrollToItem(0)
        }
    }
    LaunchedEffect(state.notes.size) {
        if (state.notes.isNotEmpty()) {
            notesListState.scrollToItem(0)
        }
    }
    Column {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Търсене...", color = MaterialTheme.colorScheme.primary) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.primary,
                unfocusedTextColor = MaterialTheme.colorScheme.primary
            ),
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Изчисти", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        )
        TabRow(
            selectedTabIndex = state.selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
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
                icon = { Icon(Icons.AutoMirrored.Default.Note, contentDescription = null) }
            )
        }
        if (state.callDirectionFilter != null) {
            val label = if (state.callDirectionFilter == "incoming") "Входящи обаждания" else "Изходящи обаждания"
            val icon = if (state.callDirectionFilter == "incoming") Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.CallMade
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onCallDirectionFilter(null) },
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Clear, contentDescription = "Изчисти филтър", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                }
            }
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
                onEdit = onEditContact,
                onLongCall = onLongCall,
                onCallDirectionFilter = onCallDirectionFilter,
                listState = contactsListState
            )
            1 -> NotesList(
                notes = state.notes,
                limit = state.notesLimit,
                appBgColor = state.appBgColor,
                notesBgColor = state.notesBgColor,
                onSelectSearch = onSearchQueryChange,
                onDelete = onDeleteNote,
                onLoadMore = onLoadMoreNotes,
                onEdit = onEditNote,
                onLongCall = onLongCall,
                onCallDirectionFilter = onCallDirectionFilter,
                listState = notesListState
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
    onEdit: (ContactEntity) -> Unit,
    onLongCall: (String, String) -> Unit,
    onCallDirectionFilter: (String?) -> Unit,
    listState: LazyListState
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
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(visibleContacts, key = { it.id }) { contact ->
                SwipeToDeleteWrapper(appBgColor = appBgColor, onDelete = { onDelete(contact) }) {
                    ContactCard(contact, contactsBgColor, onSelectSearch, onEdit, onLongCall, onCallDirectionFilter)
                }
            }
            if (hasMore) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = onLoadMore,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorConstants.ButtonBackground,
                                contentColor = ColorConstants.ButtonFontColor
                            )
                        ) {
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
    onEdit: (ContactEntity) -> Unit,
    onLongCall: (String, String) -> Unit,
    onCallDirectionFilter: (String?) -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = remember(contact.updatedAt) { sdf.format(Date(contact.updatedAt)) }
    val defaultCardBg = MaterialTheme.colorScheme.surfaceContainerLow
    val cardBg = remember(contactsBgColor) { parseColor(contactsBgColor, defaultCardBg) }
    val tagsList = remember(contact.tags) { contact.tags?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList() }
    val callDirection = remember(contact.note) {
        when {
            contact.note?.startsWith(com.example.callnotes.ui.PostCallNoteUiState.PREFIX_INCOMING) == true -> "incoming"
            contact.note?.startsWith(com.example.callnotes.ui.PostCallNoteUiState.PREFIX_OUTGOING) == true -> "outgoing"
            else -> null
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .combinedClickable(
                    onClick = { onEdit(contact) },
                    onLongClick = { onLongCall(contact.phoneNumber, contact.displayName) }
                )
                .padding(12.dp)
        ) {
            val nameParts = remember(contact.displayName) { splitNameForFirstLine(contact.displayName) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (callDirection != null) {
                    val dirIcon = if (callDirection == "incoming") Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.CallMade
                    // val dirColor = if (callDirection == "incoming") Color(0xFF4CAF50) else Color(0xFF2196F3)
                    Icon(
                        dirIcon,
                        contentDescription = if (callDirection == "incoming") "Входящо" else "Изходящо",
                        // tint = dirColor,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(top = 2.dp)
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { onCallDirectionFilter(callDirection) }
                            )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = nameParts.first,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .combinedClickable(
                            onClick = { onEdit(contact) },
                            onLongClick = { onSelectSearch(contact.displayName) }
                        )
                )
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.combinedClickable(
                        onClick = { onEdit(contact) },
                        onLongClick = { onSelectSearch(contact.phoneNumber) }
                    )
                )
            }
            if (nameParts.second.isNotBlank()) {
                Text(
                    text = nameParts.second,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onEdit(contact) },
                            onLongClick = { onSelectSearch(contact.displayName) }
                        )
                )
            }
            if (!contact.note.isNullOrBlank()) {
                val displayNote = com.example.callnotes.ui.PostCallNoteUiState.stripDirectionPrefix(contact.note)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = displayNote,
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.combinedClickable(
                        onClick = { onEdit(contact) },
                        onLongClick = { onSelectSearch(displayNote) }
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
                    color = MaterialTheme.colorScheme.secondary,
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
    onEdit: (CallNoteEntity) -> Unit,
    onLongCall: (String, String) -> Unit,
    onCallDirectionFilter: (String?) -> Unit,
    listState: LazyListState
) {
    if (notes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.AutoMirrored.Default.Note,
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
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(visibleNotes, key = { it.id }) { note ->
                SwipeToDeleteWrapper(appBgColor = appBgColor, onDelete = { onDelete(note) }) {
                    NoteCard(note, notesBgColor, onSelectSearch, onEdit, onLongCall, onCallDirectionFilter)
                }
            }
            if (hasMore) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = onLoadMore,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorConstants.ButtonBackground,
                                contentColor = ColorConstants.ButtonFontColor
                            )
                        ) {
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
    onEdit: (CallNoteEntity) -> Unit,
    onLongCall: (String, String) -> Unit,
    onCallDirectionFilter: (String?) -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = remember(note.createdAt) { sdf.format(Date(note.createdAt)) }
    val defaultCardBg = MaterialTheme.colorScheme.tertiaryContainer
    val cardBg = remember(notesBgColor) { parseColor(notesBgColor, defaultCardBg) }
    val callDirection = remember(note.noteText) {
        when {
            note.noteText.startsWith(com.example.callnotes.ui.PostCallNoteUiState.PREFIX_INCOMING) -> "incoming"
            note.noteText.startsWith(com.example.callnotes.ui.PostCallNoteUiState.PREFIX_OUTGOING) -> "outgoing"
            else -> null
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .combinedClickable(
                    onClick = { onEdit(note) },
                    onLongClick = { onLongCall(note.phoneNumber, note.callerName ?: "") }
                )
                .padding(12.dp)
        ) {
            val nameParts = remember(note.callerName) { splitNameForFirstLine(note.callerName ?: "Непознат") }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (callDirection != null) {
                    val dirIcon = if (callDirection == "incoming") Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.CallMade
                    // val dirColor = if (callDirection == "incoming") Color(0xFF4CAF50) else Color(0xFF2196F3)
                    Icon(
                        dirIcon,
                        contentDescription = if (callDirection == "incoming") "Входящо" else "Изходящо",
                        // tint = dirColor,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(top = 2.dp)
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { onCallDirectionFilter(callDirection) }
                            )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = nameParts.first,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .combinedClickable(
                            onClick = { onEdit(note) },
                            onLongClick = { onSelectSearch(note.callerName ?: "Непознат") }
                        )
                )
                Text(
                    text = note.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.combinedClickable(
                        onClick = { onEdit(note) },
                        onLongClick = { onSelectSearch(note.phoneNumber) }
                    )
                )
            }
            if (nameParts.second.isNotBlank()) {
                Text(
                    text = nameParts.second,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onEdit(note) },
                            onLongClick = { onSelectSearch(note.callerName ?: "Непознат") }
                        )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            WordSelectableText(com.example.callnotes.ui.PostCallNoteUiState.stripDirectionPrefix(note.noteText), onSelectSearch)
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                val formattedOnlyDate = remember(note.createdAt) { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(note.createdAt)) }
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
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
    val words = remember(text) { text.split(Regex("\\s+")).filter { it.isNotBlank() } }
    FlowRow(modifier = Modifier.fillMaxWidth()) {
        words.forEach { word ->
            val cleanWord = word.trim().replace(Regex("[.,!?;:]"), "")
            Text(
                text = word + " ",
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                color = MaterialTheme.colorScheme.tertiary,
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
            .background(ColorConstants.TagChipBackground, RoundedCornerShape(6.dp))
            .border(1.dp, ColorConstants.TagChipBorder, RoundedCornerShape(6.dp))
            .combinedClickable(
                onClick = { onSelectSearch(tag) },
                onLongClick = { onSelectSearch(tag) }
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = ColorConstants.TagChipText
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteWrapper(appBgColor: String, onDelete: () -> Unit, content: @Composable () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenWidthPx = with(density) { androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val dismissStateRef = remember { arrayOfNulls<androidx.compose.material3.SwipeToDismissBoxState>(1) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.StartToEnd) {
                val offset = try { dismissStateRef[0]?.requireOffset() ?: 0f } catch (_: Exception) { 0f }
                if (offset >= screenWidthPx * 0.5f) {
                    showDeleteConfirm = true
                }
                false
            } else {
                false
            }
        },
        positionalThreshold = { distance -> distance * 0.5f }
    )
    dismissStateRef[0] = dismissState
    val parsedAppBg = remember(appBgColor) {
        if (appBgColor == "default") Color.Transparent else parseColor(appBgColor, Color.Transparent)
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Изтриване") },
            text = { Text("Сигурни ли сте?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorConstants.ButtonBackground,
                        contentColor = ColorConstants.ButtonFontColor
                    )
                ) { Text("Изтрий") }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorConstants.ButtonBackground,
                        contentColor = ColorConstants.ButtonFontColor
                    )
                ) { Text("Отказ") }
            }
        )
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
    defaultColor: Color,
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
        if (selectedColor == "default") defaultColor else parseColor(selectedColor, defaultColor)
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
            val hexDefault = String.format("#%02X%02X%02X",
                (currentSelectedBg.red * 255).toInt(),
                (currentSelectedBg.green * 255).toInt(),
                (currentSelectedBg.blue * 255).toInt()
            )
            CustomColorPickerDialog(
                initialColor = if (selectedColor.startsWith("#")) selectedColor else hexDefault,
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
            Button(
                onClick = { onColorSelected(hexString) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorConstants.ButtonBackground,
                    contentColor = ColorConstants.ButtonFontColor
                )
            ) {
                Text("Избери")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorConstants.ButtonBackground,
                    contentColor = ColorConstants.ButtonFontColor
                )
            ) {
                Text("Отказ")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
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
    currentFabTransparency: Int,
    currentFabHidden: Boolean,
    currentBackupFrequency: Int,
    currentLastBackupDate: Long,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String, String, List<String>) -> Unit,
    onReset: () -> Unit,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onSettingsBackupClick: () -> Unit,
    onSettingsRestoreClick: () -> Unit,
    onFabTransparencyChange: (Int) -> Unit,
    onFabHiddenChange: (Boolean) -> Unit,
    onBackupFrequencyChange: (Int) -> Unit
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
    var fabTransparency by remember { mutableFloatStateOf(currentFabTransparency.toFloat()) }
    var fabHidden by remember { mutableStateOf(currentFabHidden) }
    var backupFrequency by remember { mutableIntStateOf(currentBackupFrequency) }
    val defaultSettingsBg = MaterialTheme.colorScheme.background
    val defaultSettingsFont = MaterialTheme.colorScheme.onBackground
    val settingsBg = remember(currentFormBgColor, defaultSettingsBg) {
        if (currentFormBgColor == "default") defaultSettingsBg else parseColor(currentFormBgColor, defaultSettingsBg)
    }
    val settingsFont = remember(currentFontColor, defaultSettingsFont) {
        if (currentFontColor == "default") defaultSettingsFont else parseColor(currentFontColor, defaultSettingsFont)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = settingsBg,
        titleContentColor = settingsFont,
        textContentColor = settingsFont,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Настройки")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val colorSettings = listOf(
                    "Основен фон" to appBg,
                    "Основен шрифт" to themePrimary,
                    "Фон Контакти" to contactsBg,
                    "Шрифт Контакти" to themeSecondary,
                    "Фон Бележки" to notesBg,
                    "Шрифт Бележки" to themeTertiary,
                    "Фон на форма" to formBgColor,
                    "Шрифт на форма" to fontColor
                )
                val colorDefaults = listOf(
                    ColorConstants.Background, ColorConstants.Primary,
                    ColorConstants.SurfaceContainerLow, ColorConstants.Secondary,
                    ColorConstants.Background, ColorConstants.Tertiary,
                    ColorConstants.Background, ColorConstants.contrastOn(ColorConstants.Background)
                )
                var selectedSetting by remember { mutableIntStateOf(0) }
                var pickerR by remember { mutableIntStateOf(20) }
                var pickerG by remember { mutableIntStateOf(20) }
                var pickerB by remember { mutableIntStateOf(20) }
                var pickerHex by remember { mutableStateOf("#141414") }
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("Етикети (до 20)", color = settingsFont) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = settingsFont, unfocusedTextColor = settingsFont, focusedLabelColor = settingsFont, unfocusedLabelColor = settingsFont)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = !fabHidden,
                        onCheckedChange = {
                            fabHidden = !it
                            onFabHiddenChange(!it)
                        }
                    )
                    Text("Меню бутон", color = settingsFont)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("${fabTransparency.toInt()}%", color = settingsFont, modifier = Modifier.width(60.dp))
                    Slider(
                        value = fabTransparency,
                        onValueChange = { fabTransparency = it },
                        onValueChangeFinished = { onFabTransparencyChange(fabTransparency.toInt()) },
                        valueRange = 0f..100f,
                        modifier = Modifier.weight(1f).height(24.dp),
                        enabled = !fabHidden
                    )
                }
                LaunchedEffect(selectedSetting) {
                    val hex = colorSettings[selectedSetting].second
                    val parsed = try {
                        if (hex == "default") colorDefaults[selectedSetting]
                        else Color(android.graphics.Color.parseColor(hex))
                    } catch (_: Exception) { colorDefaults[selectedSetting] }
                    pickerR = (parsed.red * 255).toInt()
                    pickerG = (parsed.green * 255).toInt()
                    pickerB = (parsed.blue * 255).toInt()
                    pickerHex = String.format("#%02X%02X%02X", pickerR, pickerG, pickerB)
                }
                fun applyColor() {
                    val hex = pickerHex
                    when (selectedSetting) {
                        0 -> appBg = hex
                        1 -> themePrimary = hex
                        2 -> contactsBg = hex
                        3 -> themeSecondary = hex
                        4 -> notesBg = hex
                        5 -> themeTertiary = hex
                        6 -> formBgColor = hex
                        7 -> fontColor = hex
                    }
                }
                val previewBg = try {
                    val bgHex = if (selectedSetting % 2 == 0) {
                        colorSettings[selectedSetting].second
                    } else {
                        colorSettings[selectedSetting - 1].second
                    }
                    if (bgHex == "default") colorDefaults[selectedSetting - (selectedSetting % 2)] else Color(android.graphics.Color.parseColor(bgHex))
                } catch (_: Exception) { colorDefaults[selectedSetting - (selectedSetting % 2)] }
                val previewFont = try {
                    val fontIdx = if (selectedSetting % 2 == 1) selectedSetting else selectedSetting + 1
                    val fontHex = colorSettings[fontIdx].second
                    if (fontHex == "default") colorDefaults[fontIdx] else Color(android.graphics.Color.parseColor(fontHex))
                } catch (_: Exception) { colorDefaults[selectedSetting + 1.coerceAtMost(1)] }
                HorizontalDivider(color = settingsFont)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = previewBg)
                ) {
                    Text(
                        text = "Примерен текст",
                        modifier = Modifier.padding(12.dp),
                        color = previewFont,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                var dropdownExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = colorSettings[selectedSetting].first,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Цветове") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = settingsFont, unfocusedTextColor = settingsFont)
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        colorSettings.forEachIndexed { index, (name, _) ->
                            DropdownMenuItem(
                                text = { Text(name, color = settingsFont) },
                                onClick = {
                                    selectedSetting = index
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                val settingsPresets = listOf(
                    "#F7A8B8", "#F9E79F", "#ABE188", "#F5B67A",
                    "#C39BD3", "#6ED3CF", "#FF9800", "#FF5722",
                    "#FFFFFF", "#F5F5F2", "#ECECE8", "#E0E0E0",
                    "#2A2A28", "#000000"
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    settingsPresets.take(7).forEach { code ->
                        val presetColor = try { Color(android.graphics.Color.parseColor(code)) } catch (_: Exception) { Color.Gray }
                        val isSelected = pickerHex.equals(code, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(presetColor, CircleShape)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) settingsFont else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable {
                                    pickerR = (presetColor.red * 255).toInt()
                                    pickerG = (presetColor.green * 255).toInt()
                                    pickerB = (presetColor.blue * 255).toInt()
                                    pickerHex = code
                                    applyColor()
                                }
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    settingsPresets.drop(7).forEach { code ->
                        val presetColor = try { Color(android.graphics.Color.parseColor(code)) } catch (_: Exception) { Color.Gray }
                        val isSelected = pickerHex.equals(code, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(presetColor, CircleShape)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) settingsFont else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable {
                                    pickerR = (presetColor.red * 255).toInt()
                                    pickerG = (presetColor.green * 255).toInt()
                                    pickerB = (presetColor.blue * 255).toInt()
                                    pickerHex = code
                                    applyColor()
                                }
                        )
                    }
                }
                OutlinedTextField(
                    value = pickerHex,
                    onValueChange = {
                        pickerHex = it
                        if (it.length == 7 && it.startsWith("#")) {
                            try {
                                val p = android.graphics.Color.parseColor(it)
                                pickerR = android.graphics.Color.red(p)
                                pickerG = android.graphics.Color.green(p)
                                pickerB = android.graphics.Color.blue(p)
                                applyColor()
                            } catch (_: Exception) {}
                        }
                    },
                    label = { Text("HEX", color = settingsFont) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = settingsFont, unfocusedTextColor = settingsFont, focusedLabelColor = settingsFont, unfocusedLabelColor = settingsFont)
                )

                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) { //@@
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        
                        val sharedThumbColor = Color(android.graphics.Color.rgb(pickerR, pickerG, pickerB))
                        
                        val updateColors = {
                            pickerHex = String.format("#%02X%02X%02X", pickerR, pickerG, pickerB)
                            applyColor()
                        }

                        // Викаме ги директно без "R", "G", "B" параметъра отпред:
                        ColorPickerRow(pickerR, { pickerR = it; updateColors() }, Color.Red, sharedThumbColor, settingsFont)
                        ColorPickerRow(pickerG, { pickerG = it; updateColors() }, Color.Green, sharedThumbColor, settingsFont)
                        ColorPickerRow(pickerB, { pickerB = it; updateColors() }, Color.Blue, sharedThumbColor, settingsFont)
                    }
                }

                // Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorConstants.ButtonBackground,
                        contentColor = ColorConstants.ButtonFontColor
                    )
                ) {
                    Text("Reset цветове")
                }
                // Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = settingsFont)
                Text("Архивиране", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = settingsFont)
                // Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onBackupClick() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorConstants.ButtonBackground,
                            contentColor = ColorConstants.ButtonFontColor
                        )
                    ) { Text("Архив") }
                    Button(
                        onClick = { onRestoreClick() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorConstants.ButtonBackground,
                            contentColor = ColorConstants.ButtonFontColor
                        )
                    ) { Text("Възст.") }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Настройки", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = settingsFont)
                // Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSettingsBackupClick() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorConstants.ButtonBackground,
                            contentColor = ColorConstants.ButtonFontColor
                        )
                    ) { Text("Архив") }
                    Button(
                        onClick = { onSettingsRestoreClick() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorConstants.ButtonBackground,
                            contentColor = ColorConstants.ButtonFontColor
                        )
                    ) { Text("Възст.") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Периодичност (дни):", modifier = Modifier.weight(1f), color = settingsFont)
                    var freqText by remember { mutableStateOf(currentBackupFrequency.toString()) }
                    OutlinedTextField(
                        value = freqText,
                        onValueChange = {
                            freqText = it.filter { c -> c.isDigit() }
                            backupFrequency = freqText.toIntOrNull() ?: 7
                        },
                        modifier = Modifier.width(60.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = settingsFont, unfocusedTextColor = settingsFont, focusedLabelColor = settingsFont, unfocusedLabelColor = settingsFont)
                    )
                }
                val lastBackupSdf = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
                val lastBackupText = remember(currentLastBackupDate) {
                    if (currentLastBackupDate > 0) {
                        "Последен архив: ${lastBackupSdf.format(Date(currentLastBackupDate))}"
                    } else "Не е правен архив"
                }
                Text(lastBackupText, style = MaterialTheme.typography.bodySmall, color = settingsFont)
                // Spacer(modifier = Modifier.height(8.dp))
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = settingsFont)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorConstants.ButtonBackground,
                        contentColor = ColorConstants.ButtonFontColor
                    )
                ) {
                    Text("Затвори")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val tagsList = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(20)
                        onFabTransparencyChange(fabTransparency.toInt())
                        onFabHiddenChange(fabHidden)
                        onBackupFrequencyChange(backupFrequency)
                        onSave(appBg, contactsBg, notesBg, fontColor, formBgColor, themePrimary, themeSecondary, themeTertiary, tagsList)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorConstants.ButtonBackground,
                        contentColor = ColorConstants.ButtonFontColor
                    )
                ) {
                    Text("Запази")
                }
                }
            }
        },
        dismissButton = {}
    )
}

private fun splitNameForFirstLine(name: String): Pair<String, String> {
    if (name.length <= MAX_FIRST_LINE_CHARS) return name to ""
    val breakIndex = name.lastIndexOf(' ', MAX_FIRST_LINE_CHARS).takeIf { it > 0 } ?: MAX_FIRST_LINE_CHARS
    return name.substring(0, breakIndex) to name.substring(breakIndex).trim()
}

private const val MAX_FIRST_LINE_CHARS = 18

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerRow(
    channelValue: Int,
    onValueChange: (Int) -> Unit,
    activeTrackColor: Color,
    thumbColor: Color,
    settingsFont: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        
        BasicTextField(
            value = channelValue.toString(),
            onValueChange = { onValueChange((it.toIntOrNull() ?: 0).coerceIn(0, 255)) },
            modifier = Modifier
                .width(60.dp) // Тъй като няма букви, можем да го свием още малко (до 60dp)
                .height(36.dp), // Фиксирана компактна височина
            textStyle = TextStyle(color = settingsFont, fontSize = 14.sp),
            singleLine = true,
            interactionSource = interactionSource
        ) { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = channelValue.toString(),
                innerTextField = innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
                interactionSource = interactionSource,
                // Премахнати label и placeholder за максимално чист дизайн
                // Контролираме отстъпите около числото: 8dp отляво, за да не лепи в рамката
                contentPadding = PaddingValues(start = 8.dp, end = 4.dp, top = 0.dp, bottom = 0.dp), 
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = true,
                        isError = false,
                        interactionSource = interactionSource,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = settingsFont,
                            unfocusedTextColor = settingsFont,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            )
        }

        // Слайдерът се разпъва в останалото пространство
        Slider(
            value = channelValue.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..255f,
            modifier = Modifier
                .weight(1f)
                .height(24.dp),
            colors = SliderDefaults.colors(
                thumbColor = thumbColor,
                activeTrackColor = activeTrackColor
            )
        )
    }
}
