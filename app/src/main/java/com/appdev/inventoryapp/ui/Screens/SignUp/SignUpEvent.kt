package com.appdev.inventoryapp.ui.Screens.SignUp

sealed class SignupEvent {
    data class EmailChanged(val email: String) : SignupEvent()
    data class PasswordChanged(val password: String) : SignupEvent()
    data class ConfirmPasswordChanged(val confirmPassword: String) : SignupEvent()
    data class ShopNameChanged(val shopName: String) : SignupEvent()
    data class UserNameChanged(val userName: String) : SignupEvent()
    data object TogglePasswordVisibility : SignupEvent()
    data object ToggleConfirmPasswordVisibility : SignupEvent()
    data object SignupClicked : SignupEvent()
    data object SaveProfile : SignupEvent()
    data object NavigateToLogin : SignupEvent()
    data object DismissError : SignupEvent()
}