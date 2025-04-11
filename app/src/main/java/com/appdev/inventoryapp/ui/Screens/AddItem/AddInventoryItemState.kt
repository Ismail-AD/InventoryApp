package com.appdev.inventoryapp.ui.Screens.AddItem

import android.net.Uri
import androidx.compose.runtime.Stable
import kotlinx.serialization.SerialName


@Stable
data class AddInventoryItemState(
    val name: String = "",
    val quantity: String = "",
    val costPrice: String = "",
    val sellingPrice: String = "",
    val imageUris: List<Uri> = listOf(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: String? = null,

    // New fields
    val category: String = "",
    val categories: List<String> = emptyList(),
    val categoryDropdownExpanded: Boolean = false,
    val newCategoryDialogVisible: Boolean = false,
    val newCategoryName: String = "",
    val categoriesLoadError: String? = null, // Track category loading errors
    val categoriesLoading: Boolean = false, // Track category loading state
    val newCategoryAddError: String? = null, // Track new category addition errors
    val newCategoryAddedMessage: String? = null, // Track new category addition errors


    val sku: String = "",
    val taxes: String = "",
    val showConfirmationModal: Boolean = false,
    val dontShowConfirmationAgain: Boolean = false,
    val listOfImageByteArrays: List<ByteArray?> = emptyList(),
    // For updating an existing item
    val itemId: Long? = null,
    val shop_id: String ="",
    val creator_id: String="",
    val initialImageUrls: List<String> = emptyList()
)