package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_folders")
data class InventoryFolder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentId: Long? = null,
    val name: String,
    val prefixInOrders: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long,
    val name: String,
    val defaultUnit: String = "Piece",
    val stockQuantity: Int = 100,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "unit_entities")
data class UnitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unitName: String
)

@Entity(tableName = "bucket_items")
data class BucketItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val itemName: String,
    val folderPath: String,
    val rootFolderName: String,
    val quantity: Int = 1,
    val unit: String = "Piece",
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "broadcast_alerts")
data class BroadcastAlert(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
