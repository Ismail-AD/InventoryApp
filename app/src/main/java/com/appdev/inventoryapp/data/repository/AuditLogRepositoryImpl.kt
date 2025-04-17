package com.appdev.inventoryapp.data.repository

import android.util.Log
import com.appdev.inventoryapp.Utils.ResultState
import com.appdev.inventoryapp.domain.model.AuditLogEntry
import com.appdev.inventoryapp.domain.repository.AuditLogRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class AuditLogRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : AuditLogRepository {

    override fun createAuditLog(entry: AuditLogEntry): Flow<ResultState<String>> = flow {
        try {
            emit(ResultState.Loading)

            supabase
                .from("audit_logs")
                .insert(entry)

            emit(ResultState.Success("Audit log created successfully"))
        } catch (e: Exception) {
            Log.e("AUDIT_LOGS", "Error creating audit log: ${e.localizedMessage}")
            emit(ResultState.Failure(e))
        }
    }


    override fun getAuditLogsForUser(shopId: String): Flow<ResultState<List<AuditLogEntry>>> = flow {
        try {
            emit(ResultState.Loading)

            val auditLogs = supabase
                .from("audit_logs")
                .select {
                    filter {
                        eq("shop_id", shopId)
                    }
                    order("timestamp", order = Order.ASCENDING)
                }
                .decodeList<AuditLogEntry>()

            emit(ResultState.Success(auditLogs))
        } catch (e: Exception) {
            Log.e("AUDIT_LOGS", "Error fetching user audit logs: ${e.localizedMessage}")
            emit(ResultState.Failure(e))
        }
    }
}