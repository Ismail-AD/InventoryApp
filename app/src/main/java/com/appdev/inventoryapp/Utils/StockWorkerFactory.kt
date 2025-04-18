package com.appdev.inventoryapp.di

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.appdev.inventoryapp.Utils.StockCheckWorker
import com.appdev.inventoryapp.domain.repository.InventoryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockWorkerFactory @Inject constructor(
    private val repository: InventoryRepository
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when(workerClassName) {
            StockCheckWorker::class.java.name ->
                StockCheckWorker(appContext, workerParameters, repository)
            else ->
                null
        }
    }
}