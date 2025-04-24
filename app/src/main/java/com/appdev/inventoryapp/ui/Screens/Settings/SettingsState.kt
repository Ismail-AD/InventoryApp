package com.appdev.inventoryapp.ui.Screens.Settings


data class SettingsState(
    val shopName: String = "",
    val userRole: String = "",
    val userName: String = "",
    val email: String = "",
    val userId: String = "",
    val shopId: String = "",
    val isLowStockNotificationEnabled: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val isEditingShopName: Boolean = false,
    val newShopName: String = "",
    val shopNameError: String? = null,
    val isEditingUserName: Boolean = false,
    val newUserName: String = "",
    val userNameError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showSuccessMessage: Boolean = false,
    val successMessage: String = "",
    val showNotificationGuide: Boolean = false,
    val showLogoutConfirmDialog: Boolean = false,
    val isLogoutConfirmed: Boolean = false,
    val isEditingPassword: Boolean = false,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val passwordError: String? = null
)