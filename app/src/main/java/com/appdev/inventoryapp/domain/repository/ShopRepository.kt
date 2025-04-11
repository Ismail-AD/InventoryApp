package com.appdev.inventoryapp.domain.repository

import com.appdev.inventoryapp.Utils.ResultState
import kotlinx.coroutines.flow.Flow

interface ShopRepository {
    suspend fun updateShopName(name: String, updatedBy: String): Flow<ResultState<String>>
}