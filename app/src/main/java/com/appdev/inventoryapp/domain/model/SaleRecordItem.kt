package com.appdev.inventoryapp.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SaleRecordItem(
    @SerialName("sku") val sku: String = "",
    @SerialName("id") val productId: Long = 0L,
    @SerialName("selling_price") val selling_price: Double,
    @SerialName("quantitySold") val quantity: Int,
    @SerialName("discount") val discountAmount: Float = 0.0f,
    @SerialName("discountType") val isPercentageDiscount: Boolean
)