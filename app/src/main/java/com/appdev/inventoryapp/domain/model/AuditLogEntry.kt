package com.appdev.inventoryapp.domain.model

import com.appdev.inventoryapp.Utils.AuditActionType
import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class AuditLogEntry(
    @SerialName("id")
    val id: String = NanoIdUtils.randomNanoId(),

    @SerialName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @SerialName("action_type")
    val actionType: String = AuditActionType.OTHER.name,

    @SerialName("shop_id")
    val shopId: String = "",

    @SerialName("performed_by_user_id")
    val performedByUserId: String = "",

    @SerialName("performed_by_username")
    val performedByUsername: String = "",

    @SerialName("target_user_id")
    val targetUserId: String = "",

    @SerialName("target_username")
    val targetUsername: String = "",

    @SerialName("description")
    val description: String = "",
)