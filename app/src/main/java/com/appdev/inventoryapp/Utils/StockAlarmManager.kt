package com.appdev.inventoryapp.Utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.appdev.inventoryapp.receiver.StockCheckReceiver
import java.util.concurrent.TimeUnit

object StockAlarmManager {
    private const val TAG = "StockAlarmManager"
    private const val REQUEST_CODE = 1001
    private const val INTERVAL_SIX_HOURS = 6 * 60 * 60 * 1000L // 6 hours in milliseconds

    fun scheduleAlarm(
        context: Context,
        shopId: String,
        cancelExisting: Boolean = true,
        initialDelay: Long = INTERVAL_SIX_HOURS
    ) {
        Log.d(TAG, "Scheduling stock check alarm for shop: $shopId")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, StockCheckReceiver::class.java).apply {
            putExtra("shop_id", shopId)
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            pendingIntentFlags
        )
        if (cancelExisting) {
            alarmManager.cancel(pendingIntent)
        }
        val triggerAtMillis = System.currentTimeMillis() + initialDelay

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            else -> {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }

        val delayInHours = initialDelay / (60 * 60 * 1000)
        Log.d(TAG, "Stock check alarm scheduled to trigger in $delayInHours hours")
    }

    fun scheduleStockCheck(context: Context, shopId: String) {
        scheduleAlarm(context, shopId, true)
    }

    fun scheduleNextAlarm(context: Context, shopId: String) {
        scheduleAlarm(context, shopId, false)
    }

    fun cancelStockCheck(context: Context) {

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, StockCheckReceiver::class.java)

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            pendingIntentFlags
        )

        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Stock check alarm canceled")
    }

    fun isAlarmSet(context: Context): Boolean {
        val intent = Intent(context, StockCheckReceiver::class.java)

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            pendingIntentFlags
        )

        return pendingIntent != null
    }
}
