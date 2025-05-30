package com.appdev.inventoryapp.core.di

import android.content.Context
import android.content.SharedPreferences
import com.appdev.inventoryapp.Utils.NotificationPreferenceManager
import com.appdev.inventoryapp.Utils.SessionManagement
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("user_session_prefs", Context.MODE_PRIVATE)
    }


    @Provides
    @Singleton
    fun provideSessionManagement(sharedPreferences: SharedPreferences): SessionManagement {
        return SessionManagement(sharedPreferences)
    }

    @Provides
    @Singleton
    @Named("notification_preferences")
    fun provideNotificationSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences(
            "notification_preferences",
            Context.MODE_PRIVATE
        )
    }

    @Provides
    @Singleton
    fun provideNotificationManagement(sharedPreferences: SharedPreferences): NotificationPreferenceManager {
        return NotificationPreferenceManager(sharedPreferences)
    }

}