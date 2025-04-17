package com.appdev.inventoryapp.domain.repository

import com.appdev.inventoryapp.Utils.ResultState
import com.appdev.inventoryapp.domain.model.AuditLogEntry
import kotlinx.coroutines.flow.Flow

interface AuditLogRepository {
    fun getAuditLogsForUser(shopId: String): Flow<ResultState<List<AuditLogEntry>>>
    fun createAuditLog(entry: AuditLogEntry): Flow<ResultState<String>>
}
