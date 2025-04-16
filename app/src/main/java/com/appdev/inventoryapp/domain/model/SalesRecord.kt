package com.appdev.inventoryapp.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SalesRecord(
    @SerialName("id") val id: String = "",
    @SerialName("shop_id") val shop_id: String,
    @SerialName("creator_id") val creator_id: String,
    @SerialName("creator_name") val creator_name: String,
    @SerialName("lastUpdated") val lastUpdated: String,
    @SerialName("status") val status: String,
    @SerialName("itemsList") val salesRecordItem: List<SaleRecordItem> = emptyList()
)