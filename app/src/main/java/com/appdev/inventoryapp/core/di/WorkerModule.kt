package com.appdev.inventoryapp.core.di

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import com.appdev.inventoryapp.di.StockWorkerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {

    @Singleton
    @Provides
    fun provideWorkManagerConfiguration(
        workerFactory: StockWorkerFactory
    ): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }

    @Singleton
    @Provides
    fun provideWorkManager(
        @ApplicationContext context: Context,
        configuration: Configuration
    ): WorkManager {
        WorkManager.initialize(context, configuration)
        return WorkManager.getInstance(context)
    }
}