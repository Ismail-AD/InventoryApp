package com.appdev.inventoryapp.domain.repository

import com.appdev.inventoryapp.Utils.ResultState
import com.appdev.inventoryapp.domain.model.AuditLogEntry
import kotlinx.coroutines.flow.Flow

interface AuditLogRepository {
    suspend fun getUserAuditLogs(): Flow<ResultState<List<AuditLogEntry>>>
    suspend fun createAuditLog(entry: AuditLogEntry): Flow<ResultState<AuditLogEntry>>
    suspend fun undoAction(logEntryId: String): Flow<ResultState<Boolean>>
}
