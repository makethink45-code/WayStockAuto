package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.*
import com.example.data.local.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        InventoryFolder::class,
        InventoryItem::class,
        UnitEntity::class,
        BucketItem::class,
        SearchHistoryEntity::class,
        BroadcastAlert::class
    ],
    version = 1,
    exportSchema = false
)
abstract class InventoryDatabase : RoomDatabase() {

    abstract fun inventoryDao(): InventoryDao
    abstract fun bucketDao(): BucketDao
    abstract fun unitDao(): UnitDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun broadcastDao(): BroadcastDao

    companion object {
        @Volatile
        private var INSTANCE: InventoryDatabase? = null

        fun getDatabase(context: Context): InventoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InventoryDatabase::class.java,
                    "waystock_inventory_db"
                )
                .addCallback(DatabaseCallback())
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDefaultData(database)
                    }
                }
            }

            private suspend fun populateDefaultData(db: InventoryDatabase) {
                val unitDao = db.unitDao()
                val inventoryDao = db.inventoryDao()
                val broadcastDao = db.broadcastDao()

                // Default units
                listOf("Piece", "Box", "Packet", "Bunch", "KG", "Litre", "Dozen", "Carton").forEach {
                    unitDao.insertUnit(UnitEntity(unitName = it))
                }

                // Default Categories & Folders
                val snacksFolderId = inventoryDao.insertFolder(
                    InventoryFolder(parentId = null, name = "Snacks & Munchies", prefixInOrders = true)
                )
                val beveragesFolderId = inventoryDao.insertFolder(
                    InventoryFolder(parentId = null, name = "Beverages & Drinks", prefixInOrders = true)
                )
                val stationeryFolderId = inventoryDao.insertFolder(
                    InventoryFolder(parentId = null, name = "Office & Stationery", prefixInOrders = true)
                )
                val hardwareFolderId = inventoryDao.insertFolder(
                    InventoryFolder(parentId = null, name = "Hardware & Tools", prefixInOrders = true)
                )

                // Sub-folders and Items
                val chipsFolderId = inventoryDao.insertFolder(
                    InventoryFolder(parentId = snacksFolderId, name = "Potato Chips", prefixInOrders = true)
                )
                inventoryDao.insertItem(InventoryItem(folderId = chipsFolderId, name = "Lays Classic Salted", defaultUnit = "Packet", stockQuantity = 50))
                inventoryDao.insertItem(InventoryItem(folderId = chipsFolderId, name = "Lays Cream & Onion", defaultUnit = "Packet", stockQuantity = 45))
                inventoryDao.insertItem(InventoryItem(folderId = chipsFolderId, name = "Kurkure Masala Munch", defaultUnit = "Packet", stockQuantity = 60))

                val coldDrinksFolderId = inventoryDao.insertFolder(
                    InventoryFolder(parentId = beveragesFolderId, name = "Cold Drinks 2L", prefixInOrders = true)
                )
                inventoryDao.insertItem(InventoryItem(folderId = coldDrinksFolderId, name = "Coca Cola 2L", defaultUnit = "Bottle", stockQuantity = 30))
                inventoryDao.insertItem(InventoryItem(folderId = coldDrinksFolderId, name = "Sprite 2L", defaultUnit = "Bottle", stockQuantity = 25))
                inventoryDao.insertItem(InventoryItem(folderId = coldDrinksFolderId, name = "Real Mango Juice Box", defaultUnit = "Box", stockQuantity = 15))

                inventoryDao.insertItem(InventoryItem(folderId = stationeryFolderId, name = "A4 Printing Paper Rim", defaultUnit = "Box", stockQuantity = 10))
                inventoryDao.insertItem(InventoryItem(folderId = stationeryFolderId, name = "Gel Pen Blue (Pack of 10)", defaultUnit = "Packet", stockQuantity = 20))

                inventoryDao.insertItem(InventoryItem(folderId = hardwareFolderId, name = "HDMI Cable 1.5m", defaultUnit = "Piece", stockQuantity = 15))
                inventoryDao.insertItem(InventoryItem(folderId = hardwareFolderId, name = "USB-C Multiport Adapter", defaultUnit = "Box", stockQuantity = 8))

                // Default Broadcast Alert
                broadcastDao.insertBroadcast(
                    BroadcastAlert(
                        title = "Welcome to WayStock Master!",
                        message = "Your offline-first digital stock system is live. Long press items for bulk action or search 'admin.html' for Admin Cloud Gateway."
                    )
                )
            }
        }
    }
}
