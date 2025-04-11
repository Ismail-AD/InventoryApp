package com.appdev.inventoryapp.domain.model

data class AuditLogEntry(
    val id: String,
    val userId: String,
    val action: String,
    val performedBy: String,
    val timestamp: Long
)