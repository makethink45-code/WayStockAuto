package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.InventoryFolder
import com.example.data.local.entities.InventoryItem
import com.example.ui.components.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.InventoryViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: InventoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: InventoryViewModel) {
    val currentFolderId by viewModel.currentFolderId.collectAsState()
    val breadcrumbs by viewModel.breadcrumbs.collectAsState()
    val folders by viewModel.currentFolders.collectAsState()
    val items by viewModel.currentItems.collectAsState()
    val bucketItems by viewModel.bucketItems.collectAsState()
    val allUnits by viewModel.allUnits.collectAsState()
    val activeBroadcasts by viewModel.activeBroadcasts.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    val isAdminMode by viewModel.isAdminMode.collectAsState()
    val selectionModeActive by viewModel.selectionModeActive.collectAsState()
    val selectedFolderIds by viewModel.selectedFolderIds.collectAsState()
    val selectedItemIds by viewModel.selectedItemIds.collectAsState()

    val userMessage by viewModel.userMessage.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    // Dialog & Overlay State
    var showBucketSheet by remember { mutableStateOf(false) }
    var showOrderSlipDialog by remember { mutableStateOf(false) }
    var showAdminPinModal by remember { mutableStateOf(false) }
    var showSearchOverlay by remember { mutableStateOf(false) }
    var showBulkImportModal by remember { mutableStateOf(false) }
    var showBroadcastModal by remember { mutableStateOf(false) }
    var showChangePinModal by remember { mutableStateOf(false) }
    var showUnitsModal by remember { mutableStateOf(false) }
    var showQRShareModal by remember { mutableStateOf(false) }

    // Add Folder / Item Dialog
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var newFolderPrefix by remember { mutableStateOf(true) }
    var newItemName by remember { mutableStateOf("") }
    var newItemUnit by remember { mutableStateOf("Piece") }
    var newItemQtyText by remember { mutableStateOf("100") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📦", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "WayStock Master",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = syncStatus,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    if (isAdminMode) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = "ADMIN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { showSearchOverlay = true },
                        modifier = Modifier.testTag("top_search_btn")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            // Main Bottom Navigation Bar
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { viewModel.navigateToFolder(null) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Inventory", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { showSearchOverlay = true },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Search", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = isAdminMode,
                    onClick = {
                        if (isAdminMode) {
                            showBulkImportModal = true
                        } else {
                            showAdminPinModal = true
                        }
                    },
                    icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin") },
                    label = { Text(if (isAdminMode) "Admin Tools" else "Admin", fontSize = 10.sp) },
                    modifier = Modifier.testTag("admin_nav_item")
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { showQRShareModal = true },
                    icon = { Icon(Icons.Default.QrCode, contentDescription = "Share") },
                    label = { Text("Share", fontSize = 10.sp) }
                )
            }
        },
        floatingActionButton = {
            if (isAdminMode) {
                Column(horizontalAlignment = Alignment.End) {
                    SmallFloatingActionButton(
                        onClick = { showAddFolderDialog = true },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .testTag("add_folder_fab")
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "Add Folder")
                    }
                    if (currentFolderId != null) {
                        FloatingActionButton(
                            onClick = { showAddItemDialog = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("add_item_fab")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Item")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
        ) {
            // Broadcast Alert Banner
            activeBroadcasts.firstOrNull()?.let { alert ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(alert.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(alert.message, fontSize = 11.sp)
                        }
                        IconButton(
                            onClick = { viewModel.dismissBroadcast(alert.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Dynamic Breadcrumb Trail Bar
            BreadcrumbBar(
                breadcrumbs = breadcrumbs,
                onBreadcrumbClick = { folderId -> viewModel.navigateToFolder(folderId) },
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Selection Mode Bar (Bulk Actions)
            if (selectionModeActive) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedFolderIds.size + selectedItemIds.size} Selected",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Row {
                            TextButton(onClick = { viewModel.deleteSelectedItemsAndFolders() }) {
                                Text("Delete Selected", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = { viewModel.clearSelection() }) {
                                Text("Cancel", color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }

            // Folder and Item Content List
            Box(modifier = Modifier.weight(1f)) {
                if (folders.isEmpty() && items.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (currentFolderId == null) "No Root Categories" else "This folder is empty",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            if (isAdminMode) {
                                Text(
                                    text = "Tap + or use Bulk Import to add stock items",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp)
                    ) {
                        // Render Sub-Folders
                        items(folders, key = { "folder_${it.id}" }) { folder ->
                            FolderCard(
                                folder = folder,
                                isAdminMode = isAdminMode,
                                isSelected = selectedFolderIds.contains(folder.id),
                                selectionModeActive = selectionModeActive,
                                onClick = {
                                    if (selectionModeActive) {
                                        viewModel.toggleFolderSelection(folder.id)
                                    } else {
                                        viewModel.navigateToFolder(folder.id)
                                    }
                                },
                                onLongClick = { viewModel.toggleFolderSelection(folder.id) },
                                onPrefixToggle = { viewModel.toggleFolderPrefix(folder) },
                                onDeleteFolder = { viewModel.deleteFolder(folder.id) }
                            )
                        }

                        // Render Stock Items inside Folder
                        items(items, key = { "item_${it.id}" }) { itemObj ->
                            ItemCard(
                                item = itemObj,
                                isAdminMode = isAdminMode,
                                isSelected = selectedItemIds.contains(itemObj.id),
                                selectionModeActive = selectionModeActive,
                                onLongClick = { viewModel.toggleItemSelection(itemObj.id) },
                                onAddToBucket = { qty -> viewModel.addItemToBucket(itemObj, qty) },
                                onDeleteItem = { viewModel.deleteItem(itemObj.id) }
                            )
                        }
                    }
                }
            }

            // Floating "My Bucket" Order Bar (#21005D Dark Purple theme)
            if (bucketItems.isNotEmpty()) {
                Surface(
                    onClick = { showBucketSheet = true },
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF21005D),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .testTag("floating_bucket_bar")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.padding(end = 12.dp)) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "Bucket",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Surface(
                                    color = Color(0xFFE8DEF8),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-4).dp)
                                ) {
                                    Text(
                                        text = "${bucketItems.sumOf { it.quantity }}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF21005D),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Column {
                                Text("My Bucket", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                Text("Generate Order Slip", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Components
    if (showBucketSheet) {
        BucketBottomSheet(
            bucketItems = bucketItems,
            allUnits = allUnits,
            onUpdateQuantity = { id, q -> viewModel.updateBucketQuantity(id, q) },
            onUpdateUnit = { id, u -> viewModel.updateBucketUnit(id, u) },
            onRemoveItem = { id -> viewModel.removeBucketItem(id) },
            onClearBucket = { viewModel.flushBucket() },
            onGenerateOrderSlip = {
                showBucketSheet = false
                showOrderSlipDialog = true
            },
            onAddCustomUnit = { unitName -> viewModel.addGlobalUnit(unitName) },
            onDismiss = { showBucketSheet = false }
        )
    }

    if (showOrderSlipDialog) {
        OrderSlipDialog(
            bucketItems = bucketItems,
            onFlushBucket = { viewModel.flushBucket() },
            onDismiss = { showOrderSlipDialog = false }
        )
    }

    if (showAdminPinModal) {
        AdminPinModal(
            onConfirmPin = { pin -> viewModel.checkAdminPin(pin) },
            onDismiss = { showAdminPinModal = false }
        )
    }

    if (showSearchOverlay) {
        SearchAndVoiceOverlay(
            query = searchQuery,
            onQueryChanged = { viewModel.onSearchQueryChanged(it) },
            searchResults = searchResults,
            recentSearches = recentSearches,
            onFolderClick = { folderId -> viewModel.navigateToFolder(folderId) },
            onItemClick = { item -> viewModel.addItemToBucket(item) },
            onVoiceResult = { voiceText -> viewModel.processVoiceCommand(voiceText) },
            onOpenAdminPinModal = { showAdminPinModal = true },
            onDismiss = { showSearchOverlay = false }
        )
    }

    if (showBulkImportModal) {
        BulkImportModal(
            onExecuteImport = { rawText -> viewModel.executeBulkImport(rawText) },
            onDismiss = { showBulkImportModal = false }
        )
    }

    if (showBroadcastModal) {
        BroadcastNotificationModal(
            onPublish = { title, msg -> viewModel.createBroadcast(title, msg) },
            onDismiss = { showBroadcastModal = false }
        )
    }

    if (showChangePinModal) {
        ChangePinModal(
            onUpdatePin = { newPin -> viewModel.updateAdminPin(newPin) },
            onDismiss = { showChangePinModal = false }
        )
    }

    if (showUnitsModal) {
        UnitsManagerModal(
            allUnits = allUnits,
            onAddUnit = { viewModel.addGlobalUnit(it) },
            onDeleteUnit = { viewModel.deleteGlobalUnit(it) },
            onDismiss = { showUnitsModal = false }
        )
    }

    if (showQRShareModal) {
        QRShareDialog(onDismiss = { showQRShareModal = false })
    }

    // Add Folder Dialog
    if (showAddFolderDialog) {
        AlertDialog(
            onDismissRequest = { showAddFolderDialog = false },
            title = { Text("Create New Folder") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Folder Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = newFolderPrefix,
                            onCheckedChange = { newFolderPrefix = it }
                        )
                        Text("Prefix folder name in order slips", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.createFolder(newFolderName, newFolderPrefix)
                            newFolderName = ""
                            showAddFolderDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Item Dialog
    if (showAddItemDialog) {
        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("Add New Stock Item") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        label = { Text("Item Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newItemUnit,
                        onValueChange = { newItemUnit = it },
                        label = { Text("Default Unit (e.g. Box, Piece, Packet)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newItemQtyText,
                        onValueChange = { newItemQtyText = it },
                        label = { Text("Initial Stock Quantity") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newItemName.isNotBlank()) {
                            val qty = newItemQtyText.toIntOrNull() ?: 100
                            viewModel.createItem(newItemName, newItemUnit, qty)
                            newItemName = ""
                            showAddItemDialog = false
                        }
                    }
                ) {
                    Text("Add Item")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
