package com.appdev.inventoryapp.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KeyEntry(
    @SerialName("id")
    val userId: String,

    @SerialName("key_id")
    val keyId: String,

    @SerialName("encrypted_data")
    val encryptedData: String,

    @SerialName("iv")
    val iv: String
)