package com.appdev.inventoryapp.ui.Screens.ProductDetails

import com.appdev.inventoryapp.domain.model.InventoryItem

sealed class ProductDetailEvent {
    data class EditItem(val updatedItem: InventoryItem) : ProductDetailEvent()
}
