package com.appdev.inventoryapp.Utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "app_preferences"
        private const val KEY_SKIP_INVENTORY_CONFIRMATION = "skip_inventory_confirmation"
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun shouldSkipInventoryConfirmation(): Boolean {
        return sharedPreferences.getBoolean(KEY_SKIP_INVENTORY_CONFIRMATION, false)
    }

    fun setSkipInventoryConfirmation(skip: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SKIP_INVENTORY_CONFIRMATION, skip).apply()
    }
}