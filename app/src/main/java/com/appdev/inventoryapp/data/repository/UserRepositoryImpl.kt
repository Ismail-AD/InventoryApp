package com.appdev.inventoryapp.data.repository

import android.util.Log
import com.appdev.inventoryapp.Utils.Permission
import com.appdev.inventoryapp.Utils.ResultState
import com.appdev.inventoryapp.Utils.UserRole
import com.appdev.inventoryapp.domain.model.UserEntity
import com.appdev.inventoryapp.domain.model.UserPermissions
import com.appdev.inventoryapp.domain.repository.UserRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
) : UserRepository {

    companion object {
        private const val TAG = "UserRepositoryImpl"
        private const val USERS_TABLE = "users"
    }


    override suspend fun updatePassword(email: String,currentPassword: String, newPassword: String): Flow<ResultState<Unit>> = flow {
        emit(ResultState.Loading)
        try {
            try {
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = currentPassword
                }

                supabase.auth.updateUser {
                    password = newPassword
                }

                emit(ResultState.Success(Unit))
            } catch (e: Exception) {
                emit(ResultState.Failure(Exception("Current password is incorrect")))
            }
        } catch (e: Exception) {
            emit(ResultState.Failure(e))
        }
    }

    override suspend fun getShopIdByUserId(userId: String): Flow<ResultState<String>> = flow {
        emit(ResultState.Loading)
        try {
            val user = supabase.from(USERS_TABLE)
                .select(Columns.list("shop_id")) {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<UserEntity>()

            val shopId = user.shop_id ?: ""
            emit(ResultState.Success(shopId))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch shop ID for user: ${e.message}", e)
            emit(ResultState.Failure(e))
        }
    }


    override fun getCurrentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }
    override fun logout(): Flow<ResultState<Boolean>> = flow {
        emit(ResultState.Loading)
        try {
            // Sign out from Supabase
            // SignOutScope.GLOBAL will invalidate all session tokens for this user
            supabase.auth.signOut()
            emit(ResultState.Success(true))
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

    override suspend fun getUserById(userId: String): Flow<ResultState<UserEntity>> = flow {
        emit(ResultState.Loading)
        try {
            val user = supabase.from(USERS_TABLE)
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<UserEntity>()

            emit(ResultState.Success(user))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch user details: ${e.message}", e)
            emit(ResultState.Failure(e))
        }
    }

    override suspend fun updateUserName(
        userId: String,
        userName: String
    ): Flow<ResultState<Boolean>> = flow {
        emit(ResultState.Loading)
        try {
            supabase.from(USERS_TABLE)
                .update({
                    set("username", userName)
                }) {
                    filter {
                        eq("id", userId)
                    }
                }
            emit(ResultState.Success(true))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update user name: ${e.message}", e)
            emit(ResultState.Failure(e))
        }
    }

    override suspend fun updateShopName(
        userId: String,
        shopName: String
    ): Flow<ResultState<Boolean>> = flow {
        emit(ResultState.Loading)
        try {
            supabase.from(USERS_TABLE)
                .update({
                    set("shopName", shopName)
                }) {
                    filter {
                        eq("id", userId)
                    }
                }
            emit(ResultState.Success(true))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update shop name: ${e.message}", e)
            emit(ResultState.Failure(e))
        }
    }

    override suspend fun checkShopNameExists(shopName: String): Flow<ResultState<Boolean>> = flow {
        emit(ResultState.Loading)
        try {
            val existingShops = supabase.from(USERS_TABLE)
                .select {
                    filter {
                        eq("shopName", shopName.trim())
                    }
                }
                .decodeList<UserEntity>()

            val shopNameExists = existingShops.isNotEmpty()
            emit(ResultState.Success(shopNameExists))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check shop name existence: ${e.message}", e)
            emit(ResultState.Failure(e))
        }
    }

    override suspend fun checkEmailExists(email: String): Flow<ResultState<Boolean>> = flow {
        emit(ResultState.Loading)
        try {
            val existingUsers = supabase.from(USERS_TABLE)
                .select {
                    filter {
                        eq("email", email.trim().lowercase())
                    }
                }
                .decodeList<UserEntity>()

            val emailExists = existingUsers.isNotEmpty()
            emit(ResultState.Success(emailExists))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check email existence: ${e.message}", e)
            emit(ResultState.Failure(e))
        }
    }

    override suspend fun getAllUsers(
        shopId: String,
        userId: String
    ): Flow<ResultState<List<UserEntity>>> = flow {
        emit(ResultState.Loading)
        try {
            val users = supabase.from(USERS_TABLE)
                .select {
                    filter {
                        eq("shop_id", shopId)
                        neq("id", userId)
                    }
                }
                .decodeList<UserEntity>()

            emit(ResultState.Success(users))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch users: ${e.message}", e)
            emit(ResultState.Failure(e))
        }
    }

    override suspend fun createUser(userEntity: UserEntity): Flow<ResultState<String>> = flow {
        emit(ResultState.Loading)
        try {
            supabase.from(USERS_TABLE)
                .insert(userEntity)
            emit(ResultState.Success("User Created"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create user: ${e.message}", e)
            emit(ResultState.Failure(e))
        }
    }

    override suspend fun updateUser(user: UserEntity): Flow<ResultState<String>> = flow {
        emit(ResultState.Loading)
        try {
            supabase.from(USERS_TABLE)
                .update(user) {
                    filter {
                        eq("id", user.id)
                    }
                }

            emit(ResultState.Success("User Updated"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update user: ${e.message}", e)
            emit(ResultState.Failure(e))
        }
    }

    override suspend fun deleteUser(user: String): Flow<ResultState<Boolean>> = flow {
        emit(ResultState.Loading)
        try {
            supabase.from(USERS_TABLE)
                .delete {
                    filter {
                        eq("id", user)
                    }
                }

            emit(ResultState.Success(true))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete user: ${e.message}", e)
            emit(ResultState.Failure(e))
        }
    }

    override suspend fun activateUser(userId: String): Flow<ResultState<Boolean>> = flow {
        emit(ResultState.Loading)
        try {
            supabase.from(USERS_TABLE)
                .update({
                    set("isActive", true)
                }) {
                    filter {
                        eq("id", userId)
                    }
                }

            emit(ResultState.Success(true))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to activate user: ${e.message}", e)
            emit(ResultState.Failure(e))
        }
    }

    override suspend fun deactivateUser(userId: String): Flow<ResultState<Boolean>> = flow {
        emit(ResultState.Loading)
        try {
            supabase.from(USERS_TABLE)
                .update({
                    set("isActive", false)
                }) {
                    filter {
                        eq("id", userId)
                    }
                }

            emit(ResultState.Success(true))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deactivate user: ${e.message}", e)
            emit(ResultState.Failure(e))
        }
    }

    override suspend fun updateUserRole(
        userId: String,
        role: String
    ): Flow<ResultState<Boolean>> = flow {
        emit(ResultState.Loading)
        try {
            supabase.from(USERS_TABLE)
                .update({
                    set("role", role)
                }) {
                    filter {
                        eq("id", userId)
                    }
                }

            emit(ResultState.Success(true))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update user role: ${e.message}", e)
            emit(ResultState.Failure(e))
        }
    }

    override suspend fun updateUserPermissions(
        userId: String,
        permissions: List<Permission>
    ): Flow<ResultState<Boolean>> = flow {
        emit(ResultState.Loading)
        try {
            supabase.from(USERS_TABLE)
                .update({
                    set("permissions", permissions)
                }) {
                    filter {
                        eq("id", userId)
                    }
                }

            emit(ResultState.Success(true))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update user permissions: ${e.message}", e)
            emit(ResultState.Failure(e))
        }
    }

    override suspend fun getUserPermissions(userId: String): Flow<ResultState<UserEntity>> = flow {
        emit(ResultState.Loading)
        try {
            val response = supabase.from(USERS_TABLE)
                .select(Columns.list("permissions")) {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<UserEntity>()
            emit(ResultState.Success(response))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user permissions: ${e.message}", e)
            emit(ResultState.Failure(e))
        }
    }

}