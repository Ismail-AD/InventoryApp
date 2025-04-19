package com.appdev.inventoryapp.ui.Screens.SignUp

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdev.inventoryapp.Utils.Permission
import com.appdev.inventoryapp.Utils.ResultState
import com.appdev.inventoryapp.Utils.SessionManagement
import com.appdev.inventoryapp.domain.model.UserEntity
import com.appdev.inventoryapp.domain.repository.SignUpRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val signupRepository: SignUpRepository,
    private val sessionManagement: SessionManagement
) : ViewModel() {

    private val _state = MutableStateFlow(SignupState())
    val state: StateFlow<SignupState> = _state.asStateFlow()


    private fun saveUserSession(session: UserSession) {
        sessionManagement.saveSession(
            accessToken = session.accessToken,
            refreshToken = session.refreshToken,
            expiresAt = session.expiresAt.epochSeconds,
            userId = session.user?.id ?: "",
            userEmail = session.user?.email ?: "",
        )
        sessionManagement.saveBasicInfo(
            userName = state.value.userName,
            userRole = state.value.userRole
        )
    }

    fun handleEvent(event: SignupEvent) {
        when (event) {
            is SignupEvent.EmailChanged -> {
                _state.update {
                    it.copy(
                        email = event.email,
                        isEmailError = false,
                        errorMessage = null
                    )
                }
            }

            is SignupEvent.PasswordChanged -> {
                _state.update {
                    it.copy(
                        password = event.password,
                        isPasswordError = false,
                        errorMessage = null
                    )
                }
            }

            is SignupEvent.SaveProfile ->{
                saveUserProfile()
            }
            is SignupEvent.ConfirmPasswordChanged -> {
                _state.update {
                    it.copy(
                        confirmPassword = event.confirmPassword,
                        isConfirmPasswordError = false,
                        errorMessage = null
                    )
                }
            }

            is SignupEvent.ShopNameChanged -> {
                _state.update {
                    it.copy(
                        shopName = event.shopName,
                        isShopNameError = false,
                        errorMessage = null
                    )
                }
            }

            is SignupEvent.SignupClicked -> {
                signup()
            }

            is SignupEvent.NavigateToLogin -> {
                // Navigation logic to login screen
            }

            is SignupEvent.DismissError -> {
                _state.update { it.copy(errorMessage = null) }
            }

            is SignupEvent.TogglePasswordVisibility -> {
                _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }

            is SignupEvent.ToggleConfirmPasswordVisibility -> {
                _state.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
            }

            is SignupEvent.UserNameChanged -> {
                _state.update {
                    it.copy(
                        userName = event.userName,
                        isUsernameError = false,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun saveUserProfile() {
        val currentState = _state.value

        if (currentState.shopName.isEmpty()) {
            _state.update { it.copy(errorMessage = "Shop name cannot be empty") }
            return
        }

        val uniqueShopId = generateUniqueShopId()

        viewModelScope.launch {
            if (currentState.userSession == null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "No user session found. Please try signing up again."
                    )
                }
                return@launch
            }
            signupRepository.insertUser(
                UserEntity(
                    shop_id = uniqueShopId,
                    shopName = currentState.shopName,
                    username = currentState.userName,
                    email = currentState.email,
                    role = currentState.userRole,
                    permissions = Permission.entries.map { it.name },
                    isActive = true
                )
            ).collect { result ->
                _state.update {
                    when (result) {
                        is ResultState.Success -> {
                            sessionManagement.saveShopId(uniqueShopId,currentState.shopName)
                            it.copy(
                                dataSaved = true,
                                isLoading = false
                            )
                        }
                        is ResultState.Failure -> it.copy(
                            isLoading = false,
                            errorMessage = result.message.localizedMessage
                        )
                        is ResultState.Loading -> it.copy(isLoading = true)
                    }
                }
            }
        }
    }

    private fun generateUniqueShopId(): String {
        return "SHOP-${System.currentTimeMillis()}-${(1000..9999).random()}"
    }

    private fun signup() {
        val currentState = _state.value

        // Validate inputs
        if (currentState.shopName.isEmpty()) {
            _state.update {
                it.copy(
                    isShopNameError = true,
                    errorMessage = "Shop name cannot be empty"
                )
            }
            return
        }

        if (currentState.email.isEmpty()) {
            _state.update { it.copy(isEmailError = true, errorMessage = "Email cannot be empty") }
            return
        }

        if (!isValidEmail(currentState.email)) {
            _state.update { it.copy(isEmailError = true, errorMessage = "Invalid email format") }
            return
        }

        if (currentState.password.isEmpty()) {
            _state.update {
                it.copy(
                    isPasswordError = true,
                    errorMessage = "Password cannot be empty"
                )
            }
            return
        }

        if (currentState.password.length < 6) {
            _state.update {
                it.copy(
                    isPasswordError = true,
                    errorMessage = "Password must be at least 6 characters"
                )
            }
            return
        }

        if (currentState.confirmPassword.isEmpty()) {
            _state.update {
                it.copy(
                    isConfirmPasswordError = true,
                    errorMessage = "Confirm password cannot be empty"
                )
            }
            return
        }

        if (currentState.password != currentState.confirmPassword) {
            _state.update {
                it.copy(
                    isConfirmPasswordError = true,
                    errorMessage = "Passwords do not match"
                )
            }
            return
        }

        // Show loading
        _state.update { it.copy(isLoading = true, errorMessage = null) }

        // First check if email already exists
        viewModelScope.launch {
            signupRepository.checkEmailExists(currentState.email.trim().lowercase()).collect { result ->
                when (result) {
                    is ResultState.Success -> {
                        if (result.data) {
                            // Email already exists
                            _state.update {
                                it.copy(
                                    isEmailError = true,
                                    errorMessage = "Email already exists. Please use a different email address.",
                                    isLoading = false
                                )
                            }
                        } else {
                            // Email doesn't exist, proceed with signup
                            proceedWithSignup(currentState)
                        }
                    }
                    is ResultState.Failure -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.message.localizedMessage
                            )
                        }
                    }
                    is ResultState.Loading -> {
                        // Already in loading state
                    }
                }
            }
        }
    }

    private fun proceedWithSignup(currentState: SignupState) {
        viewModelScope.launch {
            signupRepository.signup(
                email = currentState.email,
                password = currentState.password,
            ).collect { result ->
                _state.update {
                    when (result) {
                        is ResultState.Success -> {
                            result.data?.let { session ->
                                saveUserSession(session)
                                Log.d("SupabaseRepository", "Profile creation START FROM MODEL")
                                it.copy(signupSuccess = true, userSession = session)
                            } ?: it.copy(isLoading = false, errorMessage = "Session data is null")
                        }
                        is ResultState.Failure -> it.copy(
                            isLoading = false,
                            errorMessage = result.message.localizedMessage
                        )
                        is ResultState.Loading -> it.copy(isLoading = true)
                    }
                }
            }
        }
    }


    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}