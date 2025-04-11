package com.appdev.inventoryapp.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Category(
    @SerialName("id") val id: Long = 0L,
    @SerialName("categoryName") val categoryName: String,
    @SerialName("shop_id") val shop_id: String,
)