package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.InventoryDatabase
import com.example.data.local.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class InventoryRepository(private val context: Context) {

    private val db = InventoryDatabase.getDatabase(context)
    private val inventoryDao = db.inventoryDao()
    private val bucketDao = db.bucketDao()
    private val unitDao = db.unitDao()
    private val searchHistoryDao = db.searchHistoryDao()
    private val broadcastDao = db.broadcastDao()

    private val prefs: SharedPreferences =
        context.getSharedPreferences("waystock_prefs", Context.MODE_PRIVATE)

    // Admin PIN Management
    fun getAdminPin(): String {
        return prefs.getString("admin_pin", "1234") ?: "1234"
    }

    fun setAdminPin(pin: String) {
        prefs.edit().putString("admin_pin", pin).apply()
    }

    // Folders & Items
    fun getFoldersByParent(parentId: Long?): Flow<List<InventoryFolder>> =
        inventoryDao.getFoldersByParent(parentId)

    fun getAllFolders(): Flow<List<InventoryFolder>> = inventoryDao.getAllFolders()

    fun getItemsByFolder(folderId: Long): Flow<List<InventoryItem>> =
        inventoryDao.getItemsByFolder(folderId)

    fun getAllItems(): Flow<List<InventoryItem>> = inventoryDao.getAllItems()

    suspend fun getFolderById(id: Long): InventoryFolder? = inventoryDao.getFolderById(id)

    suspend fun insertFolder(folder: InventoryFolder): Long = inventoryDao.insertFolder(folder)

    suspend fun updateFolder(folder: InventoryFolder) = inventoryDao.updateFolder(folder)

    suspend fun deleteFolder(id: Long) = inventoryDao.deleteFolder(id)

    suspend fun insertItem(item: InventoryItem): Long = inventoryDao.insertItem(item)

    suspend fun updateItem(item: InventoryItem) = inventoryDao.updateItem(item)

    suspend fun deleteItem(id: Long) = inventoryDao.deleteItem(id)

    // Build Breadcrumb Trail (Home > Parent1 > Parent2 > Current)
    suspend fun getBreadcrumbs(currentFolderId: Long?): List<InventoryFolder> {
        val list = mutableListOf<InventoryFolder>()
        var currId = currentFolderId
        while (currId != null) {
            val folder = inventoryDao.getFolderById(currId)
            if (folder != null) {
                list.add(0, folder)
                currId = folder.parentId
            } else {
                break
            }
        }
        return list
    }

    // Get Full Path String for Item (e.g., "Home > Category > SubCategory")
    suspend fun getFolderPathString(folderId: Long): String {
        val breadcrumbs = getBreadcrumbs(folderId)
        if (breadcrumbs.isEmpty()) return "Home"
        return "Home > " + breadcrumbs.joinToString(" > ") { it.name }
    }

    // Get Root Folder Name (for grouping in order slips)
    suspend fun getRootFolderName(folderId: Long): String {
        val breadcrumbs = getBreadcrumbs(folderId)
        return if (breadcrumbs.isNotEmpty()) breadcrumbs.first().name else "General"
    }

    // Bucket / Cart Operations
    val bucketItems: Flow<List<BucketItem>> = bucketDao.getAllBucketItems()

    suspend fun addToBucket(item: InventoryItem, qty: Int, unit: String) {
        val folderPath = getFolderPathString(item.folderId)
        val rootFolder = getRootFolderName(item.folderId)
        val existing = bucketDao.getBucketItemByItemIdAndUnit(item.id, unit)
        if (existing != null) {
            val updated = existing.copy(quantity = existing.quantity + qty)
            bucketDao.updateBucketItem(updated)
        } else {
            val newItem = BucketItem(
                itemId = item.id,
                itemName = item.name,
                folderPath = folderPath,
                rootFolderName = rootFolder,
                quantity = qty,
                unit = unit
            )
            bucketDao.insertBucketItem(newItem)
        }
    }

    suspend fun updateBucketQuantity(bucketItemId: Long, newQty: Int) {
        if (newQty <= 0) {
            bucketDao.deleteBucketItem(bucketItemId)
        } else {
            val list = bucketItems.first()
            val item = list.find { it.id == bucketItemId }
            if (item != null) {
                bucketDao.updateBucketItem(item.copy(quantity = newQty))
            }
        }
    }

    suspend fun updateBucketUnit(bucketItemId: Long, newUnit: String) {
        val list = bucketItems.first()
        val item = list.find { it.id == bucketItemId }
        if (item != null) {
            bucketDao.updateBucketItem(item.copy(unit = newUnit))
        }
    }

    suspend fun removeFromBucket(bucketItemId: Long) {
        bucketDao.deleteBucketItem(bucketItemId)
    }

    suspend fun clearBucket() {
        bucketDao.clearBucket()
    }

    // Units
    val allUnits: Flow<List<UnitEntity>> = unitDao.getAllUnits()

    suspend fun addUnit(unitName: String) {
        if (unitName.isNotBlank()) {
            unitDao.insertUnit(UnitEntity(unitName = unitName.trim()))
        }
    }

    suspend fun deleteUnit(unitName: String) {
        unitDao.deleteUnit(unitName)
    }

    // Search & Voice
    val recentSearches: Flow<List<SearchHistoryEntity>> = searchHistoryDao.getRecentSearches()

    suspend fun recordSearchQuery(query: String) {
        if (query.isNotBlank()) {
            searchHistoryDao.insertSearch(SearchHistoryEntity(query = query.trim()))
        }
    }

    suspend fun clearSearchHistory() {
        searchHistoryDao.clearSearchHistory()
    }

    suspend fun performSearch(query: String): Pair<List<InventoryFolder>, List<InventoryItem>> {
        val folders = inventoryDao.searchFolders(query)
        val items = inventoryDao.searchItems(query)
        return Pair(folders, items)
    }

    // Broadcast Alerts
    val activeBroadcasts: Flow<List<BroadcastAlert>> = broadcastDao.getActiveBroadcasts()

    suspend fun createBroadcast(title: String, message: String) {
        broadcastDao.insertBroadcast(
            BroadcastAlert(title = title, message = message, timestamp = System.currentTimeMillis())
        )
    }

    suspend fun dismissBroadcast(id: Long) {
        broadcastDao.dismissBroadcast(id)
    }

    // Bulk Import Logic (e.g. text containing "Category > SubCategory > Item" or bulk list)
    suspend fun bulkImportHierarchy(rawText: String): Int {
        var addedCount = 0
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        for (line in lines) {
            val parts = line.split(">").map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.isEmpty()) continue

            var currentParentId: Long? = null
            // If the line specifies folder hierarchy e.g. "Snacks > Chips > Lays"
            for (i in 0 until parts.size - 1) {
                val folderName = parts[i]
                if (folderName.equals("Home", ignoreCase = true)) continue

                var folder = inventoryDao.getFolderByNameAndParent(folderName, currentParentId)
                if (folder == null) {
                    val newFolderId = inventoryDao.insertFolder(
                        InventoryFolder(parentId = currentParentId, name = folderName, prefixInOrders = true)
                    )
                    currentParentId = newFolderId
                    addedCount++
                } else {
                    currentParentId = folder.id
                }
            }

            // The last part is either item or folder
            val lastPart = parts.last()
            if (parts.size == 1) {
                // Standalone item under root
                val existingFolder = inventoryDao.getFolderByNameAndParent(lastPart, currentParentId)
                if (existingFolder == null) {
                    inventoryDao.insertFolder(
                        InventoryFolder(parentId = currentParentId, name = lastPart, prefixInOrders = true)
                    )
                    addedCount++
                }
            } else {
                // Item under currentParentId
                val targetFolderId = currentParentId ?: continue
                inventoryDao.insertItem(
                    InventoryItem(folderId = targetFolderId, name = lastPart, defaultUnit = "Piece", stockQuantity = 100)
                )
                addedCount++
            }
        }
        return addedCount
    }
}
