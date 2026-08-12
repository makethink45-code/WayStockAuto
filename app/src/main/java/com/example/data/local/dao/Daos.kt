package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_folders WHERE parentId IS :parentId ORDER BY name ASC")
    fun getFoldersByParent(parentId: Long?): Flow<List<InventoryFolder>>

    @Query("SELECT * FROM inventory_folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<InventoryFolder>>

    @Query("SELECT * FROM inventory_folders WHERE id = :id LIMIT 1")
    suspend fun getFolderById(id: Long): InventoryFolder?

    @Query("SELECT * FROM inventory_items WHERE folderId = :folderId ORDER BY name ASC")
    fun getItemsByFolder(folderId: Long): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items ORDER BY name ASC")
    fun getAllItems(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_folders WHERE parentId IS :parentId AND name = :name LIMIT 1")
    suspend fun getFolderByNameAndParent(name: String, parentId: Long?): InventoryFolder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: InventoryFolder): Long

    @Update
    suspend fun updateFolder(folder: InventoryFolder)

    @Query("DELETE FROM inventory_folders WHERE id = :id")
    suspend fun deleteFolder(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItem): Long

    @Update
    suspend fun updateItem(item: InventoryItem)

    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("SELECT * FROM inventory_folders WHERE name LIKE '%' || :query || '%'")
    suspend fun searchFolders(query: String): List<InventoryFolder>

    @Query("SELECT * FROM inventory_items WHERE name LIKE '%' || :query || '%'")
    suspend fun searchItems(query: String): List<InventoryItem>
}

@Dao
interface BucketDao {
    @Query("SELECT * FROM bucket_items ORDER BY addedAt DESC")
    fun getAllBucketItems(): Flow<List<BucketItem>>

    @Query("SELECT * FROM bucket_items WHERE itemId = :itemId AND unit = :unit LIMIT 1")
    suspend fun getBucketItemByItemIdAndUnit(itemId: Long, unit: String): BucketItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBucketItem(item: BucketItem)

    @Update
    suspend fun updateBucketItem(item: BucketItem)

    @Query("DELETE FROM bucket_items WHERE id = :id")
    suspend fun deleteBucketItem(id: Long)

    @Query("DELETE FROM bucket_items")
    suspend fun clearBucket()
}

@Dao
interface UnitDao {
    @Query("SELECT * FROM unit_entities ORDER BY unitName ASC")
    fun getAllUnits(): Flow<List<UnitEntity>>

    @Query("SELECT COUNT(*) FROM unit_entities")
    suspend fun getUnitCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: UnitEntity)

    @Query("DELETE FROM unit_entities WHERE unitName = :unitName")
    suspend fun deleteUnit(unitName: String)
}

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 10")
    fun getRecentSearches(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: SearchHistoryEntity)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()
}

@Dao
interface BroadcastDao {
    @Query("SELECT * FROM broadcast_alerts WHERE isActive = 1 ORDER BY timestamp DESC")
    fun getActiveBroadcasts(): Flow<List<BroadcastAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBroadcast(alert: BroadcastAlert)

    @Query("UPDATE broadcast_alerts SET isActive = 0 WHERE id = :id")
    suspend fun dismissBroadcast(id: Long)
}
