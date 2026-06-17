package com.example.callnotes.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.callnotes.CallNotesApp
import com.example.callnotes.theme.CallNotesTheme

class PostCallNoteActivity : ComponentActivity() {
    private val viewModel: PostCallNoteViewModel by viewModels {
        PostCallNoteViewModelFactory((application as CallNotesApp).container.repository, this)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val systemHandle = intent.getParcelableExtra<android.net.Uri>(android.telecom.TelecomManager.EXTRA_HANDLE)
        val phone = systemHandle?.schemeSpecificPart ?: intent.getStringExtra(EXTRA_PHONE) ?: ""
        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1).takeIf { it >= 0 }
        viewModel.init(phone, sessionId)
        setContent {
            CallNotesTheme {
                val state by viewModel.uiState.collectAsState()
                LaunchedEffect(state.saved) {
                    if (state.saved) finish()
                }
                PostCallNoteScreen(
                    state = state,
                    onPhoneChange = viewModel::updatePhoneNumber,
                    onCallerNameChange = viewModel::updateCallerName,
                    onNoteTextChange = viewModel::updateNoteText,
                    onTagToggle = viewModel::toggleTag,
                    onSave = viewModel::save,
                    onDismiss = { finish() }
                )
            }
        }
    }
    companion object {
        const val EXTRA_PHONE = "extra_phone"
        const val EXTRA_SESSION_ID = "extra_session_id"
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PostCallNoteScreen(
    state: PostCallNoteUiState,
    onPhoneChange: (String) -> Unit,
    onCallerNameChange: (String) -> Unit,
    onNoteTextChange: (String) -> Unit,
    onTagToggle: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (state.isEditMode) "✏ Редактиране" else "📞 Нова бележка",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (state.isEditMode) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = state.phoneNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            } else {
                OutlinedTextField(
                    value = state.phoneNumber,
                    onValueChange = onPhoneChange,
                    label = { Text("Телефонен номер") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.callerName,
                onValueChange = onCallerNameChange,
                label = { Text("Име на повикващия") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.noteText,
                onValueChange = onNoteTextChange,
                label = { Text("Кратък текст") },
                minLines = 3,
                trailingIcon = {
                    if (state.noteText.isNotEmpty()) {
                        IconButton(onClick = { onNoteTextChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Изчисти")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            if (state.availableTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Етикети:",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.availableTags.forEach { tag ->
                        val selected = state.selectedTags.contains(tag)
                        FilterChip(
                            selected = selected,
                            onClick = { onTagToggle(tag) },
                            label = { Text(tag) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Отказ", maxLines = 1, softWrap = false)
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Запази", maxLines = 1, softWrap = false)
                }
            }
        }
    }
}
