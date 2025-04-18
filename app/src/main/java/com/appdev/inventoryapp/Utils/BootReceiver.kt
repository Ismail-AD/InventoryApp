package com.appdev.inventoryapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.appdev.inventoryapp.Utils.NotificationPreferenceManager
import com.appdev.inventoryapp.Utils.SessionManagement
import com.appdev.inventoryapp.Utils.StockCheckWorker
import com.appdev.inventoryapp.domain.repository.InventoryRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var sessionManagement: SessionManagement

    @Inject
    lateinit var notificationPreferenceManager: NotificationPreferenceManager
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && sessionManagement.getShopId() != null
            && notificationPreferenceManager.isLowStockNotificationEnabled()
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                // Reschedule the worker
                StockCheckWorker.schedulePeriodicWork(context, sessionManagement.getShopId()!!)
            }
        }
    }
}