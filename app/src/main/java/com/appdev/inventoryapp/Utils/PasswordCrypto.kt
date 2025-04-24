package com.appdev.inventoryapp.Utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Utility class for securely encrypting and decrypting passwords using Android KeyStore
 */
class PasswordCrypto {
    companion object {
        private const val TRANSFORMATION = "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}"
        private const val KEY_SIZE = 256
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val GCM_TAG_LENGTH = 128
        private const val KEY_ALIAS = "InventoryAppPasswordKey"

        /**
         * Encrypts a password string
         * @param password The plain text password to encrypt
         * @return A data class containing the encrypted password and IV for decryption
         */
        fun encryptPassword(password: String): EncryptedData {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)

            // Create or retrieve the key
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE)
                    .build()

                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            }

            // Get the key and create cipher
            val key = keyStore.getKey(KEY_ALIAS, null) as SecretKey
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)

            // Generate a random ID for this encryption
            val keyId = UUID.randomUUID().toString()

            // Encrypt the password
            val encryptedBytes = cipher.doFinal(password.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv

            return EncryptedData(
                keyId = keyId,
                encryptedPassword = Base64.encodeToString(encryptedBytes, Base64.DEFAULT),
                iv = Base64.encodeToString(iv, Base64.DEFAULT)
            )
        }

        /**
         * Decrypts an encrypted password
         * @param encryptedData The encrypted password data
         * @return The decrypted plain text password
         */
        fun decryptPassword(encryptedData: EncryptedData): String {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)

            val key = keyStore.getKey(KEY_ALIAS, null) as SecretKey
            val cipher = Cipher.getInstance(TRANSFORMATION)

            val ivBytes = Base64.decode(encryptedData.iv, Base64.DEFAULT)
            val encryptedBytes = Base64.decode(encryptedData.encryptedPassword, Base64.DEFAULT)

            val spec = GCMParameterSpec(GCM_TAG_LENGTH, ivBytes)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        }
    }

    /**
     * Data class representing encrypted password data
     */
    data class EncryptedData(
        val keyId: String,
        val encryptedPassword: String,
        val iv: String
    )
}