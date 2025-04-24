package com.appdev.inventoryapp.domain.repository

import android.net.Uri
import com.appdev.inventoryapp.Utils.ResultState
import com.appdev.inventoryapp.domain.model.UserEntity
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.Flow

interface SignUpRepository {
    suspend fun signup(
        email: String,
        password: String
    ): Flow<ResultState<UserSession?>>


    fun insertUser(
        userEntity: UserEntity,
    ): Flow<ResultState<String>>

    fun getCurrentUserId(): String?
    suspend fun checkEmailExists(email: String): Flow<ResultState<Boolean>>
    suspend fun checkUsernameExists(username: String): Flow<ResultState<Boolean>>
    suspend fun checkShopNameExists(shopName: String): Flow<ResultState<Boolean>>

}