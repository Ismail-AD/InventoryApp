package com.appdev.inventoryapp.Utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.appdev.inventoryapp.MainActivity
import com.appdev.inventoryapp.R
import com.appdev.inventoryapp.domain.model.InventoryItem

object NotificationUtils {
    const val LOW_STOCK_THRESHOLD = 5
    private const val CHANNEL_ID = "low_stock_channel"
    private const val CHANNEL_NAME = "Low Stock Alerts"
    private const val CHANNEL_DESCRIPTION = "Notifications for low stock inventory items"
    private const val NOTIFICATION_ID_BASE = 1000
    private const val SUMMARY_NOTIFICATION_ID = 9999

    /**
     * Create notification channel for Android O and above
     */
     fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Create an intent to open the inventory screen
     */
    private fun createInventoryIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Add any extras to navigate to the inventory screen
            putExtra("OPEN_INVENTORY", true)
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getActivity(
            context,
            0,
            intent,
            pendingIntentFlags
        )
    }

    /**
     * Show notification for a single low stock item
     */
    fun showLowStockNotification(context: Context, item: InventoryItem) {
        createNotificationChannel(context)

        val pendingIntent = createInventoryIntent(context)

        val notificationId = NOTIFICATION_ID_BASE + item.id.hashCode()
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.available)
            .setContentTitle("Low Stock Alert")
            .setContentText("${item.name} is running low (${item.quantity} remaining)")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Notification disappears when tapped
            .setGroup("low_stock_group")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    /**
     * Show a summary notification when multiple items are low on stock
     */
    fun showSummaryNotification(context: Context, itemCount: Int) {
        createNotificationChannel(context)

        val pendingIntent = createInventoryIntent(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.available)
            .setContentTitle("Low Stock Alert")
            .setContentText("$itemCount items are running low on stock")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup("low_stock_group")
            .setGroupSummary(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(SUMMARY_NOTIFICATION_ID, builder.build())
    }
}