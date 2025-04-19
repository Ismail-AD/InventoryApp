package com.appdev.inventoryapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.appdev.inventoryapp.Utils.NotificationPreferenceManager
import com.appdev.inventoryapp.Utils.SessionManagement
import com.appdev.inventoryapp.Utils.StockAlarmManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {
    private val TAG = "BootCompletedReceiver"
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    @Inject
    lateinit var sessionManagement: SessionManagement

    @Inject
    lateinit var notificationPreferenceManager: NotificationPreferenceManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device booted, checking if stock notifications were enabled")

            scope.launch {
                try {
                    // Get the shop ID from preferences
                    val shopId = sessionManagement.getShopId() ?: ""
                    val notificationsEnabled = notificationPreferenceManager.isLowStockNotificationEnabled()

                    Log.d(TAG, "Active shop ID: $shopId, Notifications enabled: $notificationsEnabled")

                    // If notifications were enabled and we have a valid shop ID, restart the alarm
                    if (shopId.isNotEmpty() && notificationsEnabled) {
                        Log.d(TAG, "Restarting stock check alarms after device reboot")
                        StockAlarmManager.scheduleStockCheck(context, shopId)
                    } else {
                        Log.d(TAG, "Stock notifications were not enabled before reboot")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error restarting stock check alarms", e)
                }
            }

        }
    }
}