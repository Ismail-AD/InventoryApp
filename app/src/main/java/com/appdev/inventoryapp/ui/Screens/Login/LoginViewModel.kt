package com.appdev.inventoryapp.ui.Screens.Login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdev.inventoryapp.Utils.ResultState
import com.appdev.inventoryapp.Utils.SessionManagement
import com.appdev.inventoryapp.domain.model.UserEntity
import com.appdev.inventoryapp.domain.repository.LoginRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginRepository: LoginRepository,
    private val sessionManagement: SessionManagement
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()


    private fun saveUserSession(session: UserSession, userEntity: UserEntity) {
        sessionManagement.saveSession(
            accessToken = session.accessToken,
            refreshToken = session.refreshToken,
            expiresAt = session.expiresAt.epochSeconds,
            userId = session.user?.id ?: "",
            userEmail = session.user?.email ?: "",
        )
        sessionManagement.saveBasicInfo(
            userName = userEntity.username,
            userRole = userEntity.role
        )
        sessionManagement.saveShopId(userEntity.shop_id, userEntity.shopName)
    }

    fun handleEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> {
                _state.update {
                    it.copy(
                        email = event.email,
                        isEmailError = false,
                        errorMessage = null
                    )
                }
            }

            is LoginEvent.PasswordChanged -> {
                _state.update {
                    it.copy(
                        password = event.password,
                        isPasswordError = false,
                        errorMessage = null
                    )
                }
            }

            is LoginEvent.LoginClicked -> {
                login()
            }

            is LoginEvent.NavigateToRegister -> {
                // Navigation logic to register screen
            }

            is LoginEvent.NavigateToForgotPassword -> {
                // Navigation logic to forgot password screen
            }

            is LoginEvent.DismissError -> {
                _state.update { it.copy(errorMessage = null) }
            }

            is LoginEvent.TogglePasswordVisibility -> {
                _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }
        }
    }

    private fun login() {
        val currentState = _state.value

        // Validate inputs
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

        // Show loading
        _state.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            loginRepository.login(currentState.email, currentState.password)
                .collect { result ->
                    when (result) {
                        is ResultState.Success -> {
                            result.data?.let { session ->
                                // Store session temporarily
                                _state.update { it.copy(userSession = session) }
                                // Now fetch user details before completing login
                                fetchUserDetails(session)
                            } ?: _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "Session data is null"
                                )
                            }
                        }

                        is ResultState.Failure -> _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.message.localizedMessage
                            )
                        }

                        is ResultState.Loading -> _state.update { it.copy(isLoading = true) }
                    }
                }
        }
    }


    private fun fetchUserDetails(session: UserSession) {
        viewModelScope.launch {
            loginRepository.fetchUserInfo().collect { result ->
                _state.update {
                    when (result) {
                        is ResultState.Success -> {
                            // Save user details and complete login
                            saveUserSession(session, result.data)
                            it.copy(
                                isLoading = false,
                                loginSuccess = true,
                                userEntity = result.data
                            )
                        }
                        is ResultState.Failure -> it.copy(
                            isLoading = false,
                            errorMessage = result.message.localizedMessage ?: "Failed to fetch user details"
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