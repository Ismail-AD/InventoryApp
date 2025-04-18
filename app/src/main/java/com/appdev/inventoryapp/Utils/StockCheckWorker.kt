package com.appdev.inventoryapp.Utils

import android.content.Context
import androidx.work.*
import com.appdev.inventoryapp.Utils.NotificationUtils
import com.appdev.inventoryapp.domain.model.InventoryItem
import com.appdev.inventoryapp.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class StockCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val repository: InventoryRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val shopId = inputData.getString("shop_id") ?: return Result.failure()

            val inventoryStateFlow = repository.getAllInventoryItems(shopId)
            val inventoryResult = inventoryStateFlow.firstOrNull()

            val lowStockItems = mutableListOf<InventoryItem>()

            inventoryResult?.let { result ->
                if (result is ResultState.Success) {
                    val items = result.data

                    items.forEach { item ->
                        if (item.quantity <= NotificationUtils.LOW_STOCK_THRESHOLD) {
                            lowStockItems.add(item)
                            NotificationUtils.showLowStockNotification(applicationContext, item)
                        }
                    }

                    // If multiple items are low, show a summary notification
                    if (lowStockItems.size > 1) {
                        NotificationUtils.showSummaryNotification(applicationContext, lowStockItems.size)
                    }
                }
            }

            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "stock_check_worker"

        // Check if the worker is already scheduled
        fun isWorkScheduled(context: Context): Boolean {
            val workManager = WorkManager.getInstance(context)
            val workInfos = workManager.getWorkInfosForUniqueWork(WORK_NAME).get()

            if (workInfos.isNotEmpty()) {
                for (workInfo in workInfos) {
                    val state = workInfo.state
                    if (state == WorkInfo.State.RUNNING ||
                        state == WorkInfo.State.ENQUEUED ||
                        state == WorkInfo.State.BLOCKED) {
                        return true
                    }
                }
            }
            return false
        }

        fun schedulePeriodicWork(context: Context, shopId: String) {
            // First check if work is already scheduled
            if (isWorkScheduled(context)) {
                // Work is already scheduled, no need to schedule again
                return
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = Data.Builder()
                .putString("shop_id", shopId)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<StockCheckWorker>(
                repeatInterval = 6,  // Check every 6 hours
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setInputData(inputData)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,  // Changed from REPLACE to KEEP
                    workRequest
                )
        }
    }
}