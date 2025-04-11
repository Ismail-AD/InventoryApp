package com.appdev.inventoryapp.domain.repository

import com.appdev.inventoryapp.Utils.ResultState
import com.appdev.inventoryapp.domain.model.UserEntity
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.Flow

interface LoginRepository {
    suspend fun login(email: String, password: String): Flow<ResultState<UserSession?>>
    suspend fun fetchUserInfo(): Flow<ResultState<UserEntity>>
    fun getCurrentUserId(): String?
}