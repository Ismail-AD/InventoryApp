package com.appdev.inventoryapp.domain.repository


import com.appdev.inventoryapp.Utils.PasswordCrypto
import com.appdev.inventoryapp.Utils.ResultState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Repository interface for storing and retrieving encrypted keys/passwords
 */
interface KeyRepository {
    fun storePassword(userId: String, password: String): Flow<ResultState<Boolean>>
    fun getPassword(userId: String): Flow<ResultState<String?>>
    fun verifyPassword(userId: String, inputPassword: String): Flow<ResultState<Boolean>>
    fun deletePassword(userId: String): Flow<ResultState<Boolean>>
}