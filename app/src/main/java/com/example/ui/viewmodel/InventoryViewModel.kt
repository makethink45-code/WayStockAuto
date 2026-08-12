package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entities.*
import com.example.data.repository.InventoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class InventoryViewModel(application: Application) : AndroidViewModel(application) {

    val repository = InventoryRepository(application)

    // Current Folder ID (null = Root 'Home')
    val currentFolderId = MutableStateFlow<Long?>(null)

    // Breadcrumbs
    val breadcrumbs = MutableStateFlow<List<InventoryFolder>>(emptyList())

    // Admin & Selection Mode
    val isAdminMode = MutableStateFlow(false)
    val selectionModeActive = MutableStateFlow(false)
    val selectedFolderIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedItemIds = MutableStateFlow<Set<Long>>(emptySet())

    // Search
    val searchQuery = MutableStateFlow("")
    val isSearchOpen = MutableStateFlow(false)
    val searchResults = MutableStateFlow<Pair<List<InventoryFolder>, List<InventoryItem>>?>(null)
    val isVoiceListening = MutableStateFlow(false)

    // System / UI Messages
    val userMessage = MutableStateFlow<String?>(null)
    val syncStatus = MutableStateFlow("Synced • Offline Ready")

    // Reactive Lists for Current Folder
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentFolders: StateFlow<List<InventoryFolder>> = currentFolderId
        .flatMapLatest { repository.getFoldersByParent(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentItems: StateFlow<List<InventoryItem>> = currentFolderId
        .flatMapLatest { id ->
            if (id != null) repository.getItemsByFolder(id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bucketItems: StateFlow<List<BucketItem>> = repository.bucketItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUnits: StateFlow<List<UnitEntity>> = repository.allUnits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeBroadcasts: StateFlow<List<BroadcastAlert>> = repository.activeBroadcasts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSearches: StateFlow<List<SearchHistoryEntity>> = repository.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        updateBreadcrumbs()
    }

    fun navigateToFolder(folderId: Long?) {
        currentFolderId.value = folderId
        clearSelection()
        updateBreadcrumbs()
    }

    private fun updateBreadcrumbs() {
        viewModelScope.launch {
            val crumbs = repository.getBreadcrumbs(currentFolderId.value)
            breadcrumbs.value = crumbs
        }
    }

    // Admin Auth
    fun checkAdminPin(pin: String): Boolean {
        val correct = repository.getAdminPin() == pin
        if (correct) {
            isAdminMode.value = true
            userMessage.value = "Admin Cloud Gateway Unlocked!"
        } else {
            userMessage.value = "Incorrect PIN! Default is 1234"
        }
        return correct
    }

    fun exitAdminMode() {
        isAdminMode.value = false
        clearSelection()
        userMessage.value = "Logged out from Admin Gateway"
    }

    fun updateAdminPin(newPin: String) {
        if (newPin.length >= 4) {
            repository.setAdminPin(newPin)
            userMessage.value = "Admin Master PIN updated successfully!"
        } else {
            userMessage.value = "PIN must be at least 4 digits"
        }
    }

    // Folder Actions
    fun createFolder(name: String, prefixInOrders: Boolean = true) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertFolder(
                InventoryFolder(parentId = currentFolderId.value, name = name.trim(), prefixInOrders = prefixInOrders)
            )
            userMessage.value = "Folder '$name' created"
        }
    }

    fun toggleFolderPrefix(folder: InventoryFolder) {
        viewModelScope.launch {
            repository.updateFolder(folder.copy(prefixInOrders = !folder.prefixInOrders))
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            repository.deleteFolder(folderId)
            userMessage.value = "Folder deleted"
        }
    }

    // Item Actions
    fun createItem(name: String, defaultUnit: String = "Piece", stockQty: Int = 100) {
        val folderId = currentFolderId.value ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertItem(
                InventoryItem(folderId = folderId, name = name.trim(), defaultUnit = defaultUnit, stockQuantity = stockQty)
            )
            userMessage.value = "Item '$name' added"
        }
    }

    fun deleteItem(itemId: Long) {
        viewModelScope.launch {
            repository.deleteItem(itemId)
            userMessage.value = "Item deleted"
        }
    }

    // Selection Mode (Bulk Actions)
    fun toggleFolderSelection(folderId: Long) {
        val current = selectedFolderIds.value.toMutableSet()
        if (current.contains(folderId)) current.remove(folderId)
        else current.add(folderId)
        selectedFolderIds.value = current
        updateSelectionActive()
    }

    fun toggleItemSelection(itemId: Long) {
        val current = selectedItemIds.value.toMutableSet()
        if (current.contains(itemId)) current.remove(itemId)
        else current.add(itemId)
        selectedItemIds.value = current
        updateSelectionActive()
    }

    private fun updateSelectionActive() {
        selectionModeActive.value = selectedFolderIds.value.isNotEmpty() || selectedItemIds.value.isNotEmpty()
    }

    fun clearSelection() {
        selectedFolderIds.value = emptySet()
        selectedItemIds.value = emptySet()
        selectionModeActive.value = false
    }

    fun deleteSelectedItemsAndFolders() {
        viewModelScope.launch {
            selectedFolderIds.value.forEach { repository.deleteFolder(it) }
            selectedItemIds.value.forEach { repository.deleteItem(it) }
            userMessage.value = "Selected items and folders deleted"
            clearSelection()
        }
    }

    // Bucket System
    fun addItemToBucket(item: InventoryItem, qty: Int = 1, unit: String = item.defaultUnit) {
        viewModelScope.launch {
            repository.addToBucket(item, qty, unit)
            userMessage.value = "Added ${qty}x ${item.name} to Bucket"
        }
    }

    fun updateBucketQuantity(bucketItemId: Long, newQty: Int) {
        viewModelScope.launch {
            repository.updateBucketQuantity(bucketItemId, newQty)
        }
    }

    fun updateBucketUnit(bucketItemId: Long, newUnit: String) {
        viewModelScope.launch {
            repository.updateBucketUnit(bucketItemId, newUnit)
        }
    }

    fun removeBucketItem(bucketItemId: Long) {
        viewModelScope.launch {
            repository.removeFromBucket(bucketItemId)
        }
    }

    fun flushBucket() {
        viewModelScope.launch {
            repository.clearBucket()
            userMessage.value = "Bucket cleared"
        }
    }

    // Units
    fun addGlobalUnit(unitName: String) {
        viewModelScope.launch {
            repository.addUnit(unitName)
            userMessage.value = "Unit '$unitName' added"
        }
    }

    fun deleteGlobalUnit(unitName: String) {
        viewModelScope.launch {
            repository.deleteUnit(unitName)
            userMessage.value = "Unit '$unitName' deleted"
        }
    }

    // Search & Voice Commands
    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
        if (query.equals("admin.html", ignoreCase = true)) {
            // Secret admin gateway trigger
            return
        }
        if (query.isNotBlank()) {
            viewModelScope.launch {
                val results = repository.performSearch(query)
                searchResults.value = results
                repository.recordSearchQuery(query)
            }
        } else {
            searchResults.value = null
        }
    }

    fun processVoiceCommand(command: String) {
        val lower = command.lowercase().trim()
        viewModelScope.launch {
            if (lower.startsWith("open ")) {
                val folderName = lower.substringAfter("open ").trim()
                val allFolders = repository.getAllFolders().first()
                val target = allFolders.find { it.name.lowercase().contains(folderName) }
                if (target != null) {
                    navigateToFolder(target.id)
                    userMessage.value = "Voice Command: Opened '${target.name}'"
                } else {
                    userMessage.value = "Folder '$folderName' not found"
                }
            } else if (lower.startsWith("add ") && lower.contains("to cart")) {
                // e.g. "Add 5 Lays to cart" or "Add Lays to cart"
                val body = lower.substringAfter("add ").substringBefore("to cart").trim()
                val parts = body.split(" ")
                var qty = 1
                var itemNameQuery = body
                if (parts.isNotEmpty() && parts[0].toIntOrNull() != null) {
                    qty = parts[0].toInt()
                    itemNameQuery = parts.drop(1).joinToString(" ")
                }
                val allItems = repository.getAllItems().first()
                val targetItem = allItems.find { it.name.lowercase().contains(itemNameQuery.lowercase()) }
                if (targetItem != null) {
                    addItemToBucket(targetItem, qty, targetItem.defaultUnit)
                    userMessage.value = "Voice Command: Added $qty x ${targetItem.name}"
                } else {
                    userMessage.value = "Item '$itemNameQuery' not found"
                }
            } else {
                onSearchQueryChanged(command)
                userMessage.value = "Voice Search: '$command'"
            }
        }
    }

    // Bulk Import
    fun executeBulkImport(rawText: String) {
        viewModelScope.launch {
            val count = repository.bulkImportHierarchy(rawText)
            userMessage.value = "Bulk Import Complete: $count items/folders created!"
        }
    }

    // Broadcast Notifications
    fun createBroadcast(title: String, message: String) {
        viewModelScope.launch {
            repository.createBroadcast(title, message)
            userMessage.value = "Broadcast Alert published to all users!"
        }
    }

    fun dismissBroadcast(id: Long) {
        viewModelScope.launch {
            repository.dismissBroadcast(id)
        }
    }

    fun clearUserMessage() {
        userMessage.value = null
    }
}
