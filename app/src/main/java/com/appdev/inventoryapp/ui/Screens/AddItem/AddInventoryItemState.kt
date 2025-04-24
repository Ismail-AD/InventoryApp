package com.appdev.inventoryapp.ui.Screens.AddItem

import android.net.Uri
import androidx.compose.runtime.Stable
import kotlinx.serialization.SerialName


@Stable
data class AddInventoryItemState(
    val itemId: Long? = null,
    val shop_id: String = "",
    val creator_id: String = "",
    val name: String = "",
    val quantity: String = "",
    val costPrice: String = "",
    val sellingPrice: String = "",
    val categoryId: Long = -1L, // Changed from category: String = ""
    val categoryName: String = "", // Added to display category name in UI
    val sku: String = "",
    val taxes: String = "",
    val imageUris: List<Uri> = emptyList(),
    val initialImageUrls: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: String? = null,
    val categoryDropdownExpanded: Boolean = false,
    val categories: List<Pair<Long, String>> = emptyList(), // Changed to store id and name pairs
    val categoriesLoading: Boolean = false,
    val categoriesLoadError: String? = null,
    val listOfImageByteArrays: List<ByteArray?> = emptyList(),
    val newCategoryDialogVisible: Boolean = false,
    val newCategoryName: String = "",
    val newCategoryAddError: String? = null,
    val newCategoryAddedMessage: String? = null,
    val showConfirmationModal: Boolean = false,
    val dontShowConfirmationAgain: Boolean = false
)