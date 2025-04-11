package com.appdev.inventoryapp.ui.Screens.Login

import com.appdev.inventoryapp.domain.model.UserEntity
import io.github.jan.supabase.auth.user.UserSession

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isEmailError: Boolean = false,
    val isPasswordError: Boolean = false,
    val loginSuccess: Boolean = false,
    val userSession: UserSession? = null,
    val userName: String = "",
    val userRole: String = "AdminUser",
    val userEntity: UserEntity? = null
    )