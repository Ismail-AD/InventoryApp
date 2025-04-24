package com.appdev.inventoryapp.Utils


import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManagement @Inject constructor(private val sharedPreferences: SharedPreferences) {

    private val KEY_ACCESS_TOKEN = "access_token"
    private val KEY_REFRESH_TOKEN = "refresh_token"
    private val KEY_EXPIRES_AT = "expires_at"
    private val KEY_USER_ID = "user_id"
    private val KEY_USER_EMAIL = "user_email"
    private val KEY_USER_NAME = "user_name"
    private val KEY_USER_ROLE = "user_type"
    private val KEY_SHOP_ID = "shop_id"
    private val KEY_SHOP_NAME = "shop_name"
    private val EXPIRY_MARGIN = 300

    fun saveSession(
        accessToken: String,
        refreshToken: String,
        expiresAt: Long,
        userId: String,
        userEmail: String,
    ) {
        sharedPreferences.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putLong(KEY_EXPIRES_AT, expiresAt)
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, userEmail)
            apply()
        }
    }

    fun saveBasicInfo(
        userName: String,
        userRole: String
    ) {
        sharedPreferences.edit().apply {
            putString(KEY_USER_NAME, userName)
            putString(KEY_USER_ROLE, userRole)
            apply()
        }
    }


    fun saveShopId(shopId: String,shopName:String) {
        sharedPreferences.edit().apply {
            putString(KEY_SHOP_ID, shopId)
            putString(KEY_SHOP_NAME, shopName)
            apply()
        }
        Log.d("CHJUKA","saved")
    }

    fun saveShop_Id(shopId: String) {
        sharedPreferences.edit().apply {
            putString(KEY_SHOP_ID, shopId)
            apply()
        }
    }

    fun getShopId(): String? = sharedPreferences.getString(KEY_SHOP_ID, null)
    fun getShopName(): String? = sharedPreferences.getString(KEY_SHOP_NAME, null)

    fun getAccessToken(): String? = sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshToken(): String? = sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    fun getUserId(): String? = sharedPreferences.getString(KEY_USER_ID, null)
    fun getUserRole(): String? = sharedPreferences.getString(KEY_USER_ROLE, null)
    fun getUserEmail(): String? = sharedPreferences.getString(KEY_USER_EMAIL, null)
    fun getUserName(): String? = sharedPreferences.getString(KEY_USER_NAME, null)

    fun isSessionValid(): Boolean {
        val expiresAt = sharedPreferences.getLong(KEY_EXPIRES_AT, 0)
        val currentTime = System.currentTimeMillis() / 1000
        return currentTime < (expiresAt - EXPIRY_MARGIN)
    }


    fun clearSession() {
        sharedPreferences.edit().clear().commit()   // blocks until written
    }
}
