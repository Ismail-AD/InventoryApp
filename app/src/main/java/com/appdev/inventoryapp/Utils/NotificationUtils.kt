package com.appdev.inventoryapp.Utils


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.appdev.inventoryapp.MainActivity
import com.appdev.inventoryapp.R
import com.appdev.inventoryapp.domain.model.InventoryItem

object NotificationUtils {
    const val CHANNEL_ID = "inventory_alerts"
    const val NOTIFICATION_GROUP = "inventory_group"
    private const val STOCK_ALERT_REQUEST_CODE = 1001

    // Stock threshold - can be moved to settings later
    const val LOW_STOCK_THRESHOLD = 10

    fun createNotificationChannel(context: Context) {
        val name = "Inventory Alerts"
        val descriptionText = "Notifications for low inventory stock"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            enableVibration(true)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun showLowStockNotification(context: Context, item: InventoryItem, notificationId: Int = item.id.toInt()) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to_inventory", true)
            putExtra("item_id", item.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, STOCK_ALERT_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.available) // Make sure you have this icon
            .setContentTitle("Low Stock Alert")
            .setContentText("${item.name} is running low (${item.quantity} left)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP)
            .build()

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(notificationId, notification)
            } catch (e: SecurityException) {
                // Handle notification permission error
            }
        }
    }

    fun showSummaryNotification(context: Context, itemCount: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to_inventory", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.available)
            .setContentTitle("Multiple Low Stock Alerts")
            .setContentText("$itemCount items are low in stock")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP)
            .setGroupSummary(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(0, notification)
            } catch (e: SecurityException) {
                // Handle notification permission error
            }
        }
    }
}