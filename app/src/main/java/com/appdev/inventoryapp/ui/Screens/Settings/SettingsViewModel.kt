package com.appdev.inventoryapp.ui.Screens.Settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdev.inventoryapp.Utils.NotificationPreferenceManager
import com.appdev.inventoryapp.Utils.ResultState
import com.appdev.inventoryapp.Utils.SessionManagement
import com.appdev.inventoryapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManagement: SessionManagement,
    private val notificationPreferenceManager: NotificationPreferenceManager,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()


    init {
        loadUserDetails()
        loadNotificationPreferences()
    }

    private fun loadUserDetails() {
        _state.update { state ->
            state.copy(
                shopName = sessionManagement.getShopName() ?: "",
                userName = sessionManagement.getUserName() ?: "",
                email = sessionManagement.getUserEmail() ?: "",
                userId = sessionManagement.getUserId() ?: "",
                shopId = sessionManagement.getShopId() ?: "",
                isLoading = false
            )
        }


    }

    private fun loadNotificationPreferences() {
        val isLowStockEnabled = notificationPreferenceManager.isLowStockNotificationEnabled()
        _state.update { it.copy(isLowStockNotificationEnabled = isLowStockEnabled) }
    }
    fun showNotificationGuide() {
        _state.update { it.copy(showNotificationGuide = true) }
    }

    fun dismissNotificationGuide() {
        _state.update { it.copy(showNotificationGuide = false) }
    }

    fun handleEvent(event: SettingsEvent) {
        when (event) {

            is SettingsEvent.ShopNameChanged -> {
                _state.update {
                    it.copy(
                        newShopName = event.name,
                        shopNameError = validateShopName(event.name)
                    )
                }
            }

            is SettingsEvent.UserNameChanged -> {
                _state.update {
                    it.copy(
                        newUserName = event.name,
                        userNameError = validateUserName(event.name)
                    )
                }
            }

            is SettingsEvent.ToggleLowStockNotification -> {
                toggleLowStockNotification()
            }

            is SettingsEvent.RequestNotificationPermission -> {
                // This would be handled in the UI layer to request permission
            }

            is SettingsEvent.OpenShopNameDialog -> {
                _state.update { it.copy(isEditingShopName = true, newShopName = it.shopName) }
            }

            is SettingsEvent.CloseShopNameDialog -> {
                _state.update { it.copy(isEditingShopName = false, shopNameError = null) }
            }

            is SettingsEvent.OpenUserNameDialog -> {
                _state.update { it.copy(isEditingUserName = true, newUserName = it.userName) }
            }

            is SettingsEvent.CloseUserNameDialog -> {
                _state.update { it.copy(isEditingUserName = false, userNameError = null) }
            }

            is SettingsEvent.UpdateShopName -> {
                updateShopName()
            }

            is SettingsEvent.UpdateUserName -> {
                updateUserName(state.value.newUserName)
            }

            is SettingsEvent.DismissError -> {
                _state.update { it.copy(errorMessage = null) }
            }

            is SettingsEvent.DismissSuccessMessage -> {
                _state.update { it.copy(showSuccessMessage = false) }
            }

            SettingsEvent.DismissNotificationGuide -> {
                dismissNotificationGuide()
            }
            SettingsEvent.ShowNotificationGuide -> {
                showNotificationGuide()
            }
            SettingsEvent.ShowLogoutConfirmDialog -> {
                _state.update { it.copy(showLogoutConfirmDialog = true) }
            }

            SettingsEvent.HideLogoutConfirmDialog -> {
                _state.update { it.copy(showLogoutConfirmDialog = false) }
            }

            SettingsEvent.Logout -> {
                performLogout()
            }
        }
    }

    private fun performLogout() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            userRepository.logout().collectLatest { result ->
                when (result) {
                    is ResultState.Success -> {
                        // Clear all user session data
                        sessionManagement.clearSession()

                        // Clear notification preferences if needed
                        notificationPreferenceManager.setLowStockNotificationEnabled(false)

                        // Update state to show a success message before navigation
                        _state.update {
                            it.copy(
                                showSuccessMessage = true,
                                successMessage = "Logged out successfully",
                                showLogoutConfirmDialog = false,
                                isLoading = false,
                                isLogoutConfirmed = true
                            )
                        }
                    }

                    is ResultState.Failure -> {
                        _state.update {
                            it.copy(
                                errorMessage = "Logout failed: ${result.message.localizedMessage}",
                                showLogoutConfirmDialog = false,
                                isLoading = false
                            )
                        }
                    }

                    is ResultState.Loading -> {
                        // Already showing loading state
                    }
                }
            }
        }
    }


    private fun validateUserName(userName: String): String? {
        return when {
            userName.isEmpty() -> "User name cannot be empty"
            userName.length > 50 -> "User name must be 50 characters or less"
            else -> null
        }
    }

    private fun toggleLowStockNotification() {
        if (_state.value.hasNotificationPermission) {
            val newValue = !_state.value.isLowStockNotificationEnabled
            notificationPreferenceManager.setLowStockNotificationEnabled(newValue)
            _state.update { it.copy(isLowStockNotificationEnabled = newValue) }
        }
    }

    private fun updateShopName() {
        val newShopName = _state.value.newShopName.trim()
        val validationError = validateShopName(newShopName)

        if (validationError != null) {
            _state.update { it.copy(shopNameError = validationError) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Check if shop name is unique
            // This would call your newly added repository method
            userRepository.checkShopNameExists(newShopName).collectLatest { result ->
                when (result) {
                    is ResultState.Success -> {
                        val exists = result.data
                        if (exists && newShopName != _state.value.shopName) {
                            _state.update {
                                it.copy(
                                    shopNameError = "Shop name already exists",
                                    isLoading = false
                                )
                            }
                        } else {
                            // Shop name is unique or unchanged, proceed with update
                            updateShopNameInDatabase(newShopName)
                        }
                    }

                    is ResultState.Failure -> {
                        _state.update {
                            it.copy(
                                errorMessage = "Error checking shop name: ${result.message.localizedMessage}",
                                isLoading = false
                            )
                        }
                    }

                    is ResultState.Loading -> {
                        // Already showing loading state
                    }
                }
            }
        }
    }

    private suspend fun updateShopNameInDatabase(newShopName: String) {
        userRepository.updateShopName(_state.value.userId, newShopName).collectLatest { result ->
            when (result) {
                is ResultState.Success -> {
                    // Update local session data
                    sessionManagement.getShopId()
                        ?.let { sessionManagement.saveShopId(it, newShopName) }

                    _state.update {
                        it.copy(
                            shopName = newShopName,
                            isEditingShopName = false,
                            isLoading = false,
                            showSuccessMessage = true,
                            successMessage = "Shop name updated successfully"
                        )
                    }
                }

                is ResultState.Failure -> {
                    _state.update {
                        it.copy(
                            errorMessage = "Failed to update shop name: ${result.message.localizedMessage}",
                            isLoading = false
                        )
                    }
                }

                is ResultState.Loading -> {
                    // Already showing loading state
                }
            }
        }
    }

    private fun updateUserName(newUserName: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            sessionManagement.getUserId()?.let { userId ->

                userRepository.updateUserName(userId = userId, newUserName)
                    .collectLatest { result ->
                        when (result) {
                            is ResultState.Success -> {
                                sessionManagement.saveBasicInfo(
                                    userName = newUserName,
                                    userRole = sessionManagement.getUserRole() ?: "View Only"
                                )

                                _state.update {
                                    it.copy(
                                        userName = newUserName,
                                        isEditingUserName = false,
                                        isLoading = false,
                                        showSuccessMessage = true,
                                        successMessage = "User name updated successfully"
                                    )
                                }
                            }

                            is ResultState.Failure -> {
                                _state.update {
                                    it.copy(
                                        errorMessage = "Failed to update user name: ${result.message.localizedMessage}",
                                        isLoading = false
                                    )
                                }
                            }

                            is ResultState.Loading -> {
                                // Already showing loading state
                            }
                        }
                    }
            }
        }
    }

    private fun validateShopName(shopName: String): String? {
        return when {
            shopName.isEmpty() -> "Shop name cannot be empty"
            shopName.length > 20 -> "Shop name must be 20 characters or less"
            shopName.contains(" ") -> "Shop name cannot contain spaces"
            !shopName.matches(Regex("^[a-zA-Z0-9]*$")) -> "Shop name cannot contain special characters"
            else -> null
        }
    }

    fun updateNotificationPermissionStatus(hasPermission: Boolean) {
        _state.update { it.copy(hasNotificationPermission = hasPermission) }
    }
}