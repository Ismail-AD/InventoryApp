package com.appdev.inventoryapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CartData(
    val items: List<InventoryItem> = emptyList()
)
