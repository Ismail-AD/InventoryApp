package com.appdev.inventoryapp.domain.repository

import android.net.Uri
import com.appdev.inventoryapp.Utils.ResultState
import com.appdev.inventoryapp.domain.model.Category
import com.appdev.inventoryapp.domain.model.InventoryItem
import com.appdev.inventoryapp.domain.model.SalesRecord
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {
    fun getSalesRecords(shopId: String): Flow<ResultState<List<SalesRecord>>>
    fun updateInventoryItems(
        salesRecord: SalesRecord,
        mapOfProductData: Map<Long, Int>
    ): Flow<ResultState<String>>

    fun getAllInventoryItems(shopId: String?): Flow<ResultState<List<InventoryItem>>>
    fun addInventoryItem(
        item: InventoryItem, imageByteArrays: List<ByteArray?>,
        imageUris: List<Uri>
    ): Flow<ResultState<String>>

    fun updateInventoryItem(
        item: InventoryItem, imageByteArrays: List<ByteArray?>,
        imageUris: List<Uri>
    ): Flow<ResultState<String>>

    fun deleteInventoryItem(
        id: Long,
        shop_id: String,
        creator_id: String
    ): Flow<ResultState<Boolean>>

    fun getCurrentUserId(): String?
    suspend fun imageUploading(
        imageUri: Uri,
        imageBytes: ByteArray,
        folderPath: String,
        bucketId: String
    ): String

    fun addCategory(category: Category): Flow<ResultState<String>>
    fun fetchCategories(shopId: String): Flow<ResultState<List<Category>>>




}