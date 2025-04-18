package com.appdev.inventoryapp

import android.app.Application
import com.appdev.inventoryapp.Utils.NotificationUtils
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        NotificationUtils.createNotificationChannel(this)
    }
}