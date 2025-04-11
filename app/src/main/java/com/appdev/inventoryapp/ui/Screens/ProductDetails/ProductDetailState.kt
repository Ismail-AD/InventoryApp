package com.appdev.inventoryapp.ui.Screens.ProductDetails

import com.appdev.inventoryapp.domain.model.InventoryItem

data class ProductDetailState(
    val item: InventoryItem? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)