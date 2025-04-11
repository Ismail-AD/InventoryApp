package com.appdev.inventoryapp.ui.Screens.DisplayInventory

import com.appdev.inventoryapp.Utils.SortOrder
import com.appdev.inventoryapp.domain.model.InventoryItem

data class InventoryState(
    val inventoryItems: List<InventoryItem> = emptyList(),
    val filteredItems: List<InventoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val categories: List<String> = emptyList(),
    val currentSortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val isSortMenuExpanded: Boolean = false,
    val isCategoryMenuExpanded: Boolean = false,

    val showDeleteConfirmation: Boolean = false,
    val itemToDelete: InventoryItem? = null,
    val showDeleteSuccessMessage: Boolean = false
)