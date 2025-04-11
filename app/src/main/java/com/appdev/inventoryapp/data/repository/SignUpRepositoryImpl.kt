package com.appdev.inventoryapp.data.repository

import android.util.Log
import com.appdev.inventoryapp.Utils.ResultState
import com.appdev.inventoryapp.domain.model.UserEntity
import com.appdev.inventoryapp.domain.repository.SignUpRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SignUpRepositoryImpl @Inject constructor(
    private val supabase: io.github.jan.supabase.SupabaseClient
) : SignUpRepository {

    override suspend fun signup(
        email: String,
        password: String,
        shopName: String
    ): Flow<ResultState<UserSession?>> = flow {
        emit(ResultState.Loading)
        try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            val session = supabase.auth.currentSessionOrNull()
            if (session != null) {
                emit(ResultState.Success(session))
            } else {
                emit(ResultState.Failure(Exception("Failed to create session after signup")))
            }
        } catch (e: Exception) {
            emit(ResultState.Failure(e))
        }
    }

    override fun insertUser(userEntity: UserEntity): Flow<ResultState<String>> = flow {
        val currentUserId = getCurrentUserId()

        if (currentUserId == null) {
            emit(ResultState.Failure(Exception("User not authenticated")))
            return@flow
        }

        emit(ResultState.Loading)
        try {
            userEntity.id = currentUserId
            supabase.from("users").insert(userEntity)
            emit(ResultState.Success("Profile created successfully"))
        } catch (e: Exception) {
            Log.e("SupabaseRepository", "Profile creation failed: ${e.message}", e)
            emit(ResultState.Failure(e))
        }
    }

    override fun getCurrentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }
}