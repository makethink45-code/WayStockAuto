package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.InventoryFolder
import com.example.data.local.entities.InventoryItem
import com.example.data.local.entities.SearchHistoryEntity

@Composable
fun SearchAndVoiceOverlay(
    query: String,
    onQueryChanged: (String) -> Unit,
    searchResults: Pair<List<InventoryFolder>, List<InventoryItem>>?,
    recentSearches: List<SearchHistoryEntity>,
    onFolderClick: (Long) -> Unit,
    onItemClick: (InventoryItem) -> Unit,
    onVoiceResult: (String) -> Unit,
    onOpenAdminPinModal: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // SpeechRecognizer Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                onVoiceResult(spokenText)
            }
        }
    }

    fun launchVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say 'Open [folder]' or 'Add [qty] [item] to cart'")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            // Speech recognition not available on device
        }
    }

    LaunchedEffect(query) {
        if (query.equals("admin.html", ignoreCase = true)) {
            onOpenAdminPinModal()
            onDismiss()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_overlay"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Search Input Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChanged,
                    placeholder = { Text("Search folders, items, or 'admin.html'...") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    trailingIcon = {
                        Row {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { onQueryChanged("") }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                            IconButton(
                                onClick = { launchVoiceRecognition() },
                                modifier = Modifier.testTag("voice_search_mic_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Assistant",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_input_field")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Results or Recent History
            if (query.isBlank()) {
                // Show Recent Searches History
                Text(
                    text = "⏱️ RECENT SEARCHES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                if (recentSearches.isEmpty()) {
                    Text(
                        text = "No recent searches yet",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    LazyColumn {
                        items(recentSearches) { history ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onQueryChanged(history.query) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = history.query,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            } else {
                // Display Filtered Search Results
                val folders = searchResults?.first ?: emptyList()
                val items = searchResults?.second ?: emptyList()

                if (folders.isEmpty() && items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching items or folders found",
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (folders.isNotEmpty()) {
                            item {
                                Text(
                                    text = "FOLDERS (${folders.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(folders) { folder ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onFolderClick(folder.id)
                                            onDismiss()
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(folder.name, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (items.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "ITEMS (${items.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(items) { itemObj ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onItemClick(itemObj)
                                            onDismiss()
                                        },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Inventory2,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(itemObj.name, fontWeight = FontWeight.Bold)
                                                Text("Unit: ${itemObj.defaultUnit}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                            }
                                        }
                                        IconButton(onClick = { onItemClick(itemObj) }) {
                                            Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = "Add")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
