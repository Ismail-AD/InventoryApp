package com.appdev.inventoryapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.appdev.inventoryapp.Utils.NotificationUtils
import com.appdev.inventoryapp.Utils.ResultState
import com.appdev.inventoryapp.Utils.StockAlarmManager
import com.appdev.inventoryapp.domain.model.InventoryItem
import com.appdev.inventoryapp.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

@AndroidEntryPoint
class StockCheckReceiver : BroadcastReceiver() {
    private val TAG = "StockCheckReceiver"
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    @Inject
    lateinit var repository: InventoryRepository

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Stock check alarm received")

        val shopId = intent.getStringExtra("shop_id")
        if (shopId == null) {
            Log.e(TAG, "No shop ID provided in intent")
            return
        }

        scope.launch {
            try {
                checkInventoryLevels(context, shopId)

                // Schedule the next alarm
                StockAlarmManager.scheduleNextAlarm(context, shopId)
            } catch (e: Exception) {
                Log.e(TAG, "Error checking inventory levels", e)
            }
        }

    }

    private suspend fun checkInventoryLevels(context: Context, shopId: String) {
        Log.d(TAG, "Checking inventory levels for shop: $shopId")

        try {
            // Don't use firstOrNull directly on the repository flow
            repository.getAllInventoryItems(shopId).collect { result ->
                when (result) {
                    is ResultState.Success -> {
                        val items = result.data
                        val lowStockItems = mutableListOf<InventoryItem>()

                        items.forEach { item ->
                            if (item.quantity < NotificationUtils.LOW_STOCK_THRESHOLD) {
                                lowStockItems.add(item)
                                Log.d(TAG, "Low stock item found: ${item.name}, quantity: ${item.quantity}")
                                NotificationUtils.showLowStockNotification(context, item)
                            }
                        }

                        if (lowStockItems.size > 1) {
                            Log.d(TAG, "Multiple low stock items found: ${lowStockItems.size}")
                            NotificationUtils.showSummaryNotification(context, lowStockItems.size)
                        }
                    }
                    is ResultState.Failure -> {
                        Log.e(TAG, "Failed to get inventory items: ${result.message}")
                    }
                    is ResultState.Loading -> {
                        // Just log this state
                        Log.d(TAG, "Loading inventory items...")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in inventory flow collection", e)
        }
    }
}