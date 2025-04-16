package com.appdev.inventoryapp.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class InventoryItem(
    @SerialName("id") val id: Long = 0L,
    @SerialName("name") val name: String,
    @SerialName("quantity") val quantity: Int,
    @SerialName("cost_price") val cost_price: Double,
    @SerialName("selling_price") val selling_price: Double,
    @SerialName("lastUpdated") val lastUpdated: String,
    @SerialName("imageUrls") var imageUrls: List<String> = emptyList(),
    @SerialName("shop_id") val shop_id: String,
    @SerialName("creator_id") val creator_id: String,
    @SerialName("categoryName") val category: String = "",
    @SerialName("sku") val sku: String = "",
    @SerialName("taxes") val taxes: Double = 0.0,
    @SerialName("discount") val discountAmount: Float = 0.0f,
    @SerialName("discountType") val isPercentageDiscount: Boolean = true
) : Parcelable