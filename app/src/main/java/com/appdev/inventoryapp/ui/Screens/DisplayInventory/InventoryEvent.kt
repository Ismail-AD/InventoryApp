package com.appdev.inventoryapp.ui.Screens.DisplayInventory

import com.appdev.inventoryapp.Utils.SortOrder
import com.appdev.inventoryapp.domain.model.InventoryItem

sealed class InventoryEvent {
    data object RefreshInventory : InventoryEvent()
    data class SearchQueryChanged(val query: String) : InventoryEvent()
    data class DeleteItem(val item: InventoryItem) : InventoryEvent()
    data object DismissError : InventoryEvent()
    data object LoadInventory : InventoryEvent()
    data class UpdateSortOrder(val sortOrder: SortOrder) : InventoryEvent()
    data class FilterByCategory(val categoryId: Long) : InventoryEvent()
    data object FetchCategories : InventoryEvent()
    data class ToggleSortMenu(val isExpanded: Boolean) : InventoryEvent()
    data class ToggleCategoryMenu(val isExpanded: Boolean) : InventoryEvent()

    data class ShowDeleteConfirmation(val item: InventoryItem) : InventoryEvent()
    data object HideDeleteConfirmation : InventoryEvent()
    data object ConfirmDelete : InventoryEvent()
    data object DismissDeleteSuccess : InventoryEvent()
}