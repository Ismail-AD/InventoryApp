package com.appdev.inventoryapp.ui.Screens.SignUp

import com.appdev.inventoryapp.Utils.UserRole
import io.github.jan.supabase.auth.user.UserSession

data class SignupState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val shopName: String = "",
    val userRole: String = "Admin",
    val userName: String = "",
    val isPasswordVisible: Boolean = false,
    val dataSaved: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isEmailError: Boolean = false,
    val isUsernameError: Boolean = false,
    val isPasswordError: Boolean = false,
    val isConfirmPasswordError: Boolean = false,
    val isShopNameError: Boolean = false,
    val signupSuccess: Boolean = false,
    val userSession: UserSession? = null,
    )