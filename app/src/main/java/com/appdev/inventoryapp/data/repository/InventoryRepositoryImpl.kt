package com.appdev.inventoryapp.data.repository

import android.net.Uri
import android.util.Log
import com.appdev.inventoryapp.Utils.NotificationUtils
import com.appdev.inventoryapp.Utils.ResultState
import com.appdev.inventoryapp.domain.model.Category
import com.appdev.inventoryapp.domain.model.InventoryItem
import com.appdev.inventoryapp.domain.model.SaleRecordItem
import com.appdev.inventoryapp.domain.model.SalesRecord
import com.appdev.inventoryapp.domain.repository.InventoryRepository
import com.google.gson.Gson
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

class InventoryRepositoryImpl @Inject constructor(
    val supabase: SupabaseClient,
) : InventoryRepository {

    private val itemImageBucketId = "itemimages"
    private val itemImageFolderPath = "public/17dr9sb_2"


    override fun checkForDuplicateItem(name: String, sku: String, shopId: String, excludeItemId: Long?): Flow<ResultState<Pair<Boolean, String>>> = flow {
        try {
            emit(ResultState.Loading)

            // Query the inventory table for items with the same name or SKU in the given shop
            // If excludeItemId is provided, exclude that item from the check (for updates)
            val query = supabase
                .from("inventory")
                .select {
                    filter {
                        eq("shop_id", shopId)
                        or {
                            eq("name", name)
                            eq("sku", sku)
                        }
                        // If we're updating an existing item, exclude it from the duplicate check
                        if (excludeItemId != null) {
                            neq("id", excludeItemId)
                        }
                    }
                }

            val duplicateItems = query.decodeList<InventoryItem>()

            // Determine duplicate type
            val duplicateType = when {
                duplicateItems.any { it.name.equals(name, ignoreCase = true) } &&
                        duplicateItems.any { it.sku.equals(sku, ignoreCase = true) } -> "both name and SKU"
                duplicateItems.any { it.name.equals(name, ignoreCase = true) } -> "name"
                duplicateItems.any { it.sku.equals(sku, ignoreCase = true) } -> "SKU"
                else -> ""
            }

            val isDuplicate = duplicateItems.isNotEmpty()
            emit(ResultState.Success(Pair(isDuplicate, duplicateType)))
        } catch (e: Exception) {
            Log.e("DUPLICATE_CHECK", "Error checking for duplicates: ${e.localizedMessage}")
            emit(ResultState.Failure(e))
        }
    }


    // Step 6: Implement undoSalesRecord in InventoryRepositoryImpl
    override fun undoSalesRecord(salesRecord: SalesRecord): Flow<ResultState<String>> = flow {
        try {
            emit(ResultState.Loading)

            // 1. Update the status of the sale to "Undone"
            supabase.from("sales").update(
                {
                    set("status", "Reversed")
                    set("lastUpdated", System.currentTimeMillis().toString())
                }
            ) {
                filter {
                    eq("id", salesRecord.id)
                }
            }

            // 2. Restore inventory items for each product in the sale
            for (saleItem in salesRecord.salesRecordItem) {
                // Get current inventory item
                val inventoryItems = supabase
                    .from("inventory")
                    .select {
                        filter {
                            eq("id", saleItem.productId)
                        }
                    }
                    .decodeList<InventoryItem>()

                if (inventoryItems.isNotEmpty()) {
                    val currentItem = inventoryItems.first()

                    // Calculate new quantity (add back the sold quantity)
                    val newQuantity = currentItem.quantity + saleItem.quantity

                    // Update inventory item
                    supabase.from("inventory").update(
                        {
                            set("quantity", newQuantity)
                            set("lastUpdated", System.currentTimeMillis().toString())
                        }
                    ) {
                        filter {
                            eq("id", saleItem.productId)
                        }
                    }
                }
            }

            emit(ResultState.Success("Sale successfully undone"))
        } catch (e: Exception) {
            Log.e("UNDO_SALE", "Error undoing sale: ${e.localizedMessage}")
            emit(ResultState.Failure(e))
        }
    }


    override fun getSalesRecords(shopId: String): Flow<ResultState<List<SalesRecord>>> = flow {
        try {
            emit(ResultState.Loading)

            // Instead of decoding to Map<String, Any>, decode directly to SalesRecord
            val salesRecords = supabase
                .from("sales")
                .select {
                    filter {
                        eq("shop_id", shopId)
                    }
                }
                .decodeList<SalesRecord>()

            emit(ResultState.Success(salesRecords))
        } catch (e: Exception) {
            Log.e("SALES_RECORD", "Error fetching sales records: ${e.localizedMessage}")
            emit(ResultState.Failure(e))
        }
    }


    override fun updateInventoryItems(
        salesRecord: SalesRecord,
        mapOfProductData: Map<Long, Int>
    ): Flow<ResultState<String>> = flow {
        try {
            emit(ResultState.Loading)

            // No need for manual JSON conversion, Supabase serializer will handle it
            supabase.from("sales").insert(salesRecord)

            getCurrentUserId()?.let { uid ->
                // Process each item in the sales record
                for (saleItem in salesRecord.salesRecordItem) {
                    // Update the inventory item - only updating quantity and discount fields
                    val quantityAvailable = mapOfProductData[saleItem.productId] ?: 0
                    val newQuantity = abs(saleItem.quantity - quantityAvailable)

                    supabase.from("inventory").update(
                        {
                            // Decrease the available quantity by the sold quantity
                            set("quantity", newQuantity)
                            // Update the lastUpdated timestamp
                            set("lastUpdated", System.currentTimeMillis().toString())
                        }
                    ) {
                        filter {
                            eq("id", saleItem.productId)
                            eq("creator_id", uid)
                        }
                    }
                }

                emit(ResultState.Success("Sales record saved successfully"))

            } ?: emit(ResultState.Failure(Exception("User not authenticated")))

        } catch (e: Exception) {
            Log.d("CHJAZ","${e.localizedMessage}")
            emit(ResultState.Failure(e))
        }
    }


    override fun addCategory(
        category: Category
    ): Flow<ResultState<Category>> = flow {
        try {
            emit(ResultState.Loading)
            val categoryAdded = supabase
                .from("categories")
                .insert(category) {
                    // This tells PostgreSQL to return the inserted row
                    select()
                }
                .decodeSingle<Category>()
            emit(ResultState.Success(categoryAdded))
        } catch (e: Exception) {
            emit(ResultState.Failure(e))
        }

    }

    override fun fetchCategories(
        shopId: String
    ): Flow<ResultState<List<Category>>> = flow {
        try {
            emit(ResultState.Loading)

            // Fetch categories for the specific shop
            val categories = supabase
                .from("categories")
                .select {
                    filter {
                        eq("shop_id", shopId)
                    }
                }
                .decodeList<Category>()

            emit(ResultState.Success(categories))
        } catch (e: Exception) {
            emit(ResultState.Failure(e))
        }
    }


    override fun updateCategory(category: Category): Flow<ResultState<String>> = flow {
        try {
            emit(ResultState.Loading)

            supabase.from("categories").update({
                set("categoryName", category.categoryName)
            }) {
                filter {
                    eq("id", category.id)
                    eq("shop_id", category.shop_id)
                }
            }

            emit(ResultState.Success("Category updated successfully"))
        } catch (e: Exception) {
            emit(ResultState.Failure(e))
        }
    }

    override fun deleteCategory(id: Long, shopId: String): Flow<ResultState<Boolean>> = flow {
        try {
            emit(ResultState.Loading)

            supabase.from("categories").delete {
                filter {
                    eq("id", id)
                    eq("shop_id", shopId)
                }
            }

            emit(ResultState.Success(true))
        } catch (e: Exception) {
            emit(ResultState.Failure(e))
        }
    }


    override fun getAllInventoryItems(shopId: String?): Flow<ResultState<List<InventoryItem>>> =
        flow {
            try {
                emit(ResultState.Loading)
                if (shopId != null) {
                    val items = supabase
                        .from("inventory")
                        .select {
                            filter {
                                eq("shop_id", shopId)
                            }
                        }
                        .decodeList<InventoryItem>()
                    emit(ResultState.Success(items))
                }
            } catch (e: Exception) {
                e.message?.let { Log.d("CHAKZ", it) }
                emit(ResultState.Failure(e))
            }
        }

    override fun getCurrentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }

    override suspend fun imageUploading(
        imageUri: Uri,
        imageBytes: ByteArray,
        folderPath: String,
        bucketId: String
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val extension = when {
                    // Try to get from Uri
                    imageUri.lastPathSegment?.contains(".") == true ->
                        imageUri.lastPathSegment?.substringAfterLast('.')?.lowercase()
                    // Fallback to detecting from bytes
                    else -> when {
                        imageBytes.size >= 2 && imageBytes[0] == 0xFF.toByte() && imageBytes[1] == 0xD8.toByte() -> "jpg"
                        imageBytes.size >= 8 && String(
                            imageBytes.take(8).toByteArray()
                        ) == "PNG\r\n\u001a\n" -> "png"

                        else -> "jpg" // Default fallback
                    }
                } ?: "jpg"

                // Validate extension
                val safeExtension = when (extension.lowercase()) {
                    "jpg", "jpeg", "png", "gif" -> extension
                    else -> "jpg"
                }
                val fileName = "${UUID.randomUUID()}.$safeExtension"
                val fullPath = "$folderPath/$fileName"
                val bucket = supabase.storage.from(bucketId)
                bucket.upload(
                    path = fullPath,
                    data = imageBytes
                ) {
                    upsert = false
                }
                bucket.publicUrl(fullPath)
            } catch (e: Exception) {
                throw Exception("Failed to upload image: ${e.message}")
            }
        }
    }

    override fun addInventoryItem(
        item: InventoryItem, imageByteArrays: List<ByteArray?>,
        imageUris: List<Uri>
    ): Flow<ResultState<String>> = flow {
        getCurrentUserId()?.let { uid ->
            try {
                emit(ResultState.Loading)
                val imageUrls = mutableListOf<String>()

                for (i in imageByteArrays.indices) {
                    val byteArray = imageByteArrays[i]
                    val uri = imageUris.getOrNull(i)
                    if (byteArray != null && uri != null) {
                        val imageUrl = imageUploading(
                            uri,
                            byteArray,
                            itemImageFolderPath,
                            itemImageBucketId
                        )
                        imageUrls.add(imageUrl)
                    }
                }

                val itemToAdd = item.copy(
                    creator_id = uid,
                    lastUpdated = System.currentTimeMillis().toString(),
                    imageUrls = imageUrls
                )



                supabase
                    .from("inventory")
                    .insert(itemToAdd)
                emit(ResultState.Success("Product added successfully"))
            } catch (e: Exception) {
                emit(ResultState.Failure(e))
            }
        }
    }

    override fun updateInventoryItem(
        item: InventoryItem,
        imageByteArrays: List<ByteArray?>,
        imageUris: List<Uri>
    ): Flow<ResultState<String>> = flow {
        getCurrentUserId()?.let { uid ->
            emit(ResultState.Loading)
            try {
                val imageUrls = mutableListOf<String>()

                for (i in imageByteArrays.indices) {
                    val byteArray = imageByteArrays[i]
                    val uri = imageUris.getOrNull(i)
                    if (byteArray != null && uri != null) {
                        val imageUrl = imageUploading(
                            uri,
                            byteArray,
                            itemImageFolderPath,
                            itemImageBucketId
                        )
                        imageUrls.add(imageUrl)
                    }
                }

                if (imageUrls.isEmpty()) {
                    imageUrls.addAll(item.imageUrls)
                }

                supabase.from("inventory").update(
                    {
                        set("creator_id", uid)
                        set("shop_id", item.shop_id)
                        set("name", item.name)
                        set("quantity", item.quantity)
                        set("cost_price", item.cost_price)
                        set("selling_price", item.selling_price)
                        set("imageUrls", imageUrls)
                        set("lastUpdated", System.currentTimeMillis().toString())
                        set("category_id", item.category_id)
                        set("sku", item.sku)
                        set("taxes", item.taxes)
                        set("discount", item.discountAmount)
                        set("discountType", item.isPercentageDiscount)
                    }
                ) {
                    filter {
                        eq("id", item.id)
                        eq("creator_id", uid)
                    }
                }

                emit(ResultState.Success("Inventory item updated successfully"))
            } catch (e: Exception) {
                emit(ResultState.Failure(e))
            }
        }
    }


    override fun deleteInventoryItem(
        id: Long,
        shop_id: String,
        creator_id: String
    ): Flow<ResultState<Boolean>> = flow {
        try {
            emit(ResultState.Loading)
            supabase
                .from("inventory")
                .delete {
                    filter {
                        eq("id", id)
                        eq("creator_id", creator_id)
                        eq("shop_id", shop_id)
                    }
                }
            emit(ResultState.Success(true))
        } catch (e: Exception) {
            emit(ResultState.Failure(e))
        }
    }


}