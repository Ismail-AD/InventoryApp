package com.appdev.inventoryapp.domain.repository

import com.appdev.inventoryapp.Utils.Permission
import com.appdev.inventoryapp.Utils.ResultState
import com.appdev.inventoryapp.Utils.UserRole
import com.appdev.inventoryapp.domain.model.UserEntity
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun logout(): Flow<ResultState<Boolean>>
    suspend fun getUserPermissions(userId: String): Flow<ResultState<UserEntity>>
    suspend fun getAllUsers(shopId: String, userId: String): Flow<ResultState<List<UserEntity>>>
    suspend fun createUser(
        userEntity: UserEntity,
    ): Flow<ResultState<String>>

    suspend fun updateUser(user: UserEntity): Flow<ResultState<String>>
    suspend fun deleteUser(user: String): Flow<ResultState<Boolean>>
    suspend fun activateUser(userId: String): Flow<ResultState<Boolean>>
    suspend fun deactivateUser(userId: String): Flow<ResultState<Boolean>>
    suspend fun updateUserRole(userId: String, role: String): Flow<ResultState<Boolean>>
    suspend fun updateUserPermissions(
        userId: String,
        permissions: List<Permission>
    ): Flow<ResultState<Boolean>>
    suspend fun checkEmailExists(email: String): Flow<ResultState<Boolean>>
    suspend fun updateShopName(userId: String, shopName: String): Flow<ResultState<Boolean>>
    suspend fun updateUserName(userId: String, userName: String): Flow<ResultState<Boolean>>
    suspend fun checkShopNameExists(shopName: String): Flow<ResultState<Boolean>>
}
