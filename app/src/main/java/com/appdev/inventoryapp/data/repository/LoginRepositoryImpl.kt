package com.appdev.inventoryapp.data.repository

import com.appdev.inventoryapp.Utils.ResultState
import com.appdev.inventoryapp.domain.model.UserEntity
import com.appdev.inventoryapp.domain.repository.LoginRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class LoginRepositoryImpl @Inject constructor(val supabase: SupabaseClient,
) : LoginRepository {
    override suspend fun login(email: String, password: String): Flow<ResultState<UserSession?>> = flow {
        try {
            emit(ResultState.Loading)
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            emit(ResultState.Success(supabase.auth.currentSessionOrNull()))
        } catch (e: Exception) {
            emit(ResultState.Failure(e))
        }
    }

    override suspend fun fetchUserInfo(): Flow<ResultState<UserEntity>> = flow {
        getCurrentUserId()?.let { uid ->
            emit(ResultState.Loading)
            try {
                val user = supabase.from("users").select {
                    filter {
                        eq("id", uid)
                    }
                }.decodeSingle<UserEntity>()
                emit(ResultState.Success(user))
            } catch (e: Exception) {
                emit(ResultState.Failure(e))
            }
        }
    }
    override fun getCurrentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }
}