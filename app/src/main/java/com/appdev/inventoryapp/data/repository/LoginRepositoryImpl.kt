package com.appdev.inventoryapp.data.repository

import android.util.Log
import com.appdev.inventoryapp.Utils.ResultState
import com.appdev.inventoryapp.domain.model.UserEntity
import com.appdev.inventoryapp.domain.repository.LoginRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class LoginRepositoryImpl @Inject constructor(val supabase: SupabaseClient,
) : LoginRepository {

    override suspend fun resetPassword(email: String): Flow<ResultState<Boolean>> = flow {
        emit(ResultState.Loading)
        try {
            supabase.auth.resetPasswordForEmail(email)
            emit(ResultState.Success(true))
        } catch (e: RestException) {
            emit(ResultState.Failure(e))
        } catch (e: Exception) {
            // Handle other exceptions
            emit(ResultState.Failure(e))
        }
    }

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

    override suspend fun fetchUserInfo(email: String): Flow<ResultState<UserEntity>> = flow {
        emit(ResultState.Loading)
        try {
            // First fetch the user info
            val user = supabase.from("users").select {
                filter {
                    eq("email", email)
                }
            }.decodeSingle<UserEntity>()

            Log.d("USER_INFO", "Fetched user: $user")

            // Check if we need to update the user ID
            val currentAuthId = supabase.auth.currentUserOrNull()?.id
            if (currentAuthId != null && currentAuthId != user.id) {
                Log.d("USER_UPDATE", "Updating user ID from ${user.id} to $currentAuthId")

                // Update the user record with the new auth ID
                try {
                    supabase.from("users").update({
                        set("id", currentAuthId)
                    }) {
                        filter {
                            eq("email", email)
                        }
                    }
                    val updatedUser = user.copy(id = currentAuthId)

                    Log.d("USER_UPDATE", "Updated user: $updatedUser")
                    emit(ResultState.Success(updatedUser))
                } catch (e: Exception) {
                    Log.e("USER_UPDATE", "Failed to update user ID", e)
                    emit(ResultState.Failure(e))
                }
            } else {
                // No need to update, return the user as is
                emit(ResultState.Success(user))
            }
        } catch (e: Exception) {
            Log.e("USER_FETCH", "Failed to fetch user info", e)
            emit(ResultState.Failure(e))
        }
    }

    override fun getCurrentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }
}