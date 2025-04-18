package com.appdev.inventoryapp.Utils


import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationPreferenceManager @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    fun setLowStockNotificationEnabled(enabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(KEY_LOW_STOCK_NOTIFICATION, enabled)
        }
    }

    fun isLowStockNotificationEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_LOW_STOCK_NOTIFICATION, false)
    }

    companion object {
        private const val KEY_LOW_STOCK_NOTIFICATION = "low_stock_notification"
    }
}