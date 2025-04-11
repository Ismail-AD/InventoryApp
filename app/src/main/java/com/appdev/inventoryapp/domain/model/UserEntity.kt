package com.appdev.inventoryapp.domain.model

import com.appdev.inventoryapp.Utils.Permission
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserEntity(
    @SerialName("id") var id: String = "",
    @SerialName("shop_id") var shop_id: String = "",
    @SerialName("permissions") val permissions: List<String>? = null,
    @SerialName("username") var username: String = "",
    @SerialName("isActive") var isActive: Boolean = true,
    @SerialName("shopName") var shopName: String = "",
    @SerialName("email") var email: String = "",
    @SerialName("role") var role: String = ""
)