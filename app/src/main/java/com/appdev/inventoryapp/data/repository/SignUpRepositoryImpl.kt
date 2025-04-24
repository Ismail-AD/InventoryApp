package com.appdev.inventoryapp.data.repository

import android.util.Log
import com.appdev.inventoryapp.Utils.ResultState
import com.appdev.inventoryapp.domain.model.UserEntity
import com.appdev.inventoryapp.domain.repository.SignUpRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SignUpRepositoryImpl @Inject constructor(
    private val supabase: io.github.jan.supabase.SupabaseClient
) : SignUpRepository {

    override suspend fun checkShopNameExists(shopName: String): Flow<ResultState<Boolean>> = flow {
        emit(ResultState.Loading)
        try {
            val existingShops = supabase.from("users")
                .select(columns = Columns.list("shopName")) {
                    filter {
                        eq("shopName", shopName)
                    }
                }


            val shopNameExists = existingShops.decodeList<UserEntity>().isNotEmpty()
            emit(ResultState.Success(shopNameExists))
        } catch (e: Exception) {
            emit(ResultState.Failure(e))
        }
    }

    override suspend fun checkUsernameExists(username: String): Flow<ResultState<Boolean>> = flow {
        emit(ResultState.Loading)
        try {
            val users = supabase.from("users")
                .select {
                    filter {
                        eq("username", username.trimEnd())
                    }
                }
                .decodeList<UserEntity>()
            Log.d("CAZQ","username: ${users}")

            // If the list is not empty, username exists
            emit(ResultState.Success(users.isNotEmpty()))
        } catch (e: Exception) {
            emit(ResultState.Failure(e))
        }
    }

    override suspend fun checkEmailExists(email: String): Flow<ResultState<Boolean>> = flow {
        emit(ResultState.Loading)
        try {
            val existingUsers = supabase.from("users")
                .select {
                    filter {
                        eq("email", email.trim().lowercase())
                    }
                }
                .decodeList<UserEntity>()
            Log.d("CAZQ","mail: ${existingUsers}")

            val emailExists = existingUsers.isNotEmpty()
            emit(ResultState.Success(emailExists))
        } catch (e: Exception) {
            emit(ResultState.Failure(e))
        }
    }

    override suspend fun signup(
        email: String,
        password: String
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
            // 1) prepare entity
            userEntity.id = currentUserId

            // 2) attempt insert – will throw if the constraint is violated
            supabase.from("users").insert(userEntity)

            // 3) success
            emit(ResultState.Success("Profile created successfully"))

        } catch (e: Exception) {

            // ── map UNIQUE‑constraint errors to friendly messages ───────────────
            val friendlyMessage = when {
                e.message?.contains("\"users_username_key\"", ignoreCase = true) == true ->
                    "Username already exists. Please choose another one."

                e.message?.contains("\"users_email_key\"", ignoreCase = true) == true ->
                    "Email is already registered. Try signing in or use a different address."

                e.message?.contains("\"users_shopname_key\"", ignoreCase = true) == true ->
                    "Shop name already exists. Please pick a unique shop name."

                else -> e.localizedMessage ?: "Unknown database error"
            }

            Log.e("SupabaseRepository", "Profile creation failed: ${e.message}", e)
            emit(ResultState.Failure(Exception(friendlyMessage)))
        }
    }



    override fun getCurrentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }
}