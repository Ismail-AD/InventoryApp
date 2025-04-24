package com.appdev.inventoryapp.data.repository

import android.util.Log
import com.appdev.inventoryapp.Utils.PasswordCrypto
import com.appdev.inventoryapp.Utils.ResultState
import com.appdev.inventoryapp.domain.model.KeyEntry
import com.appdev.inventoryapp.domain.repository.KeyRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : KeyRepository {

    /* -------------------------------------------------- store -------------------------------------------------- */

    override fun storePassword(userId: String, password: String): Flow<ResultState<Boolean>> = flow {
        emit(ResultState.Loading)

        val enc = PasswordCrypto.encryptPassword(password)

        val keyEntry = KeyEntry(
            userId = userId,
            keyId = enc.keyId,
            encryptedData = enc.encryptedPassword,
            iv = enc.iv
        )

        supabase.from("keys").upsert(keyEntry)   // insert or replace

        emit(ResultState.Success(true))
    }.catch { e ->
        Log.e("KeyRepository", "storePassword failed", e)
        emit(ResultState.Failure(e))
    }

    /* -------------------------------------------------- fetch -------------------------------------------------- */

    override fun getPassword(userId: String): Flow<ResultState<String?>> = flow {
        emit(ResultState.Loading)

        val row = supabase.from("keys")
            .select {
                filter { eq("id", userId) }
                limit(1)
            }
            .decodeSingleOrNull<KeyEntry>()            // null when no row

        if (row == null) {
            emit(ResultState.Success(null))
            return@flow
        }

        val enc = PasswordCrypto.EncryptedData(
            keyId = row.keyId,
            encryptedPassword = row.encryptedData,
            iv = row.iv
        )

        emit(ResultState.Success(PasswordCrypto.decryptPassword(enc)))
    }.catch { e ->
        Log.e("KeyRepository", "getPassword failed", e)
        emit(ResultState.Failure(e))
    }

    /* -------------------------------------------------- verify ------------------------------------------------- */

    override fun verifyPassword(userId: String, inputPassword: String): Flow<ResultState<Boolean>> =
        flow {

            emit(ResultState.Loading)

            try {
                getPassword(userId).collect { result ->
                    when (result) {
                        is ResultState.Success -> {
                            val storedPassword = result.data
                            if (storedPassword != null) {
                                // Compare the input password with stored password
                                emit(ResultState.Success(inputPassword == storedPassword))
                            } else {
                                emit(ResultState.Success(false))
                            }
                        }
                        is ResultState.Failure -> {
                            emit(result)
                        }
                        else -> {}
                    }
                }

            } catch (e: Exception) {
                emit(ResultState.Failure(e))
            }
        }

    /* -------------------------------------------------- delete ------------------------------------------------- */

    override fun deletePassword(userId: String): Flow<ResultState<Boolean>> = flow {
        emit(ResultState.Loading)

        supabase.from("keys")
            .delete {
                filter { eq("id", userId) }
            }

        emit(ResultState.Success(true))
    }.catch { e ->
        Log.e("KeyRepository", "deletePassword failed", e)
        emit(ResultState.Failure(e))
    }
}
