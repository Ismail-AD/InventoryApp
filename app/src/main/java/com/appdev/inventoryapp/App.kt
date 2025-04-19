package com.appdev.inventoryapp

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import com.appdev.inventoryapp.Utils.NotificationUtils
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application(){

    override fun onCreate() {
        super.onCreate()

        NotificationUtils.createNotificationChannel(this)

    }

}