package com.appdev.inventoryapp.ui.Screens.UserManagement

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdev.inventoryapp.Utils.Permission
import com.appdev.inventoryapp.Utils.ResultState
import com.appdev.inventoryapp.Utils.SessionManagement
import com.appdev.inventoryapp.domain.model.UserEntity
import com.appdev.inventoryapp.domain.repository.UserRepository
import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class UsersListViewModel @Inject constructor(
    private val userRepository: UserRepository,
    val sessionManagement: SessionManagement
) : ViewModel() {

    private val _state = MutableStateFlow(UsersListState())
    val state: StateFlow<UsersListState> = _state.asStateFlow()

    private var currentUserPermissions: List<Permission> = emptyList()

    init {
        viewModelScope.launch {
            handleEvent(UsersListEvent.RefreshUsers)
        }
    }

    private fun canManageUsers(): Boolean {
        return currentUserPermissions.contains(Permission.MANAGE_USERS)
    }

    fun handleEvent(event: UsersListEvent) {
//        if (!canManageUsers() && event !is UsersListEvent.RefreshUsers) {
//            return
//        }

        viewModelScope.launch {
            when (event) {
                is UsersListEvent.SearchUsers -> {
                    _state.update { it.copy(searchQuery = event.query) }
                    applyFilters()
                }

                is UsersListEvent.FilterByRole -> {
                    _state.update { it.copy(roleFilter = event.role) }
                    applyFilters()
                }

                is UsersListEvent.FilterByStatus -> {
                    _state.update { it.copy(statusFilter = event.isActive) }
                    applyFilters()
                }

                is UsersListEvent.UpdatePermissions -> {
                    _state.update { it.copy(selectedPermissions = event.permissions) }
                }

                is UsersListEvent.UpdateUsername -> {
                    _state.update { it.copy(username = event.username) }
                }

                is UsersListEvent.UpdateEmail -> {
                    _state.update { it.copy(email = event.email, emailError = null) }
                }

                is UsersListEvent.ToggleShowPermissions -> {
                    _state.update { it.copy(showPermissions = event.show) }
                }

                is UsersListEvent.ToggleStatusDropdown -> {
                    _state.update { it.copy(isStatusDropdownExpanded = event.isExpanded) }
                }

                is UsersListEvent.ToggleRoleDropdown -> {
                    _state.update { it.copy(isRoleDropdownExpanded = event.isExpanded) }
                }

                is UsersListEvent.SelectRole -> {
                    _state.update {
                        it.copy(
                            selectedRole = event.role,
                            showPermissions = true
                        )
                    }
                }

                is UsersListEvent.RefreshUsers -> {
                    _state.update { it.copy(isLoading = true, error = null) }
                    if (sessionManagement.getUserId() != null && sessionManagement.getShopId() != null) {
                        userRepository.getAllUsers(
                            sessionManagement.getShopId()!!,
                            sessionManagement.getUserId()!!
                        ).collect { result ->
                            when (result) {
                                is ResultState.Success -> {
                                    _state.update {
                                        it.copy(
                                            users = result.data,
                                            filteredUsers = result.data,
                                            isLoading = false
                                        )
                                    }
                                    applyFilters()
                                }

                                is ResultState.Failure -> {
                                    _state.update {
                                        it.copy(
                                            error = result.message.localizedMessage,
                                            isLoading = false
                                        )
                                    }
                                }

                                is ResultState.Loading -> {
                                    _state.update { it.copy(isLoading = true) }
                                }
                            }
                        }
                    }
                }

                is UsersListEvent.DeleteUser -> {
                    _state.update { it.copy(isDeleteLoading = true) }
                    userRepository.deleteUser(event.userId).collect { result ->
                        when (result) {
                            is ResultState.Success -> {
                                _state.update { it.copy(
                                    isDeleteLoading = false,
                                    showDeleteConfirmation = false,
                                    userToDelete = null
                                ) }
                                handleEvent(UsersListEvent.RefreshUsers)
                            }

                            is ResultState.Failure -> {
                                _state.update { it.copy(
                                    error = result.message.localizedMessage,
                                    isDeleteLoading = false
                                ) }
                            }

                            is ResultState.Loading -> {
                                // Loading state is already set above
                            }
                        }
                    }
                }

                is UsersListEvent.ActivateUser -> {
                    userRepository.activateUser(event.userId).collect { result ->
                        when (result) {
                            is ResultState.Success -> {
                                handleEvent(UsersListEvent.RefreshUsers)
                            }

                            is ResultState.Failure -> {
                                _state.update { it.copy(error = result.message.localizedMessage) }
                            }

                            is ResultState.Loading -> {
                                _state.update { it.copy(isLoading = true) }
                            }
                        }
                    }
                }

                is UsersListEvent.DeactivateUser -> {
                    userRepository.deactivateUser(event.userId).collect { result ->
                        when (result) {
                            is ResultState.Success -> {
                                handleEvent(UsersListEvent.RefreshUsers)
                            }

                            is ResultState.Failure -> {
                                _state.update { it.copy(error = result.message.localizedMessage) }
                            }

                            is ResultState.Loading -> {
                                _state.update { it.copy(isLoading = true) }
                            }
                        }
                    }
                }

                is UsersListEvent.NavigateToUserDetail -> {
                    // Navigation will be handled by the UI
                }

                is UsersListEvent.ShowAddUserDialog -> {
                    _state.update {
                        it.copy(
                            showAddUserDialog = true,
                            username = "",
                            email = "",
                            selectedRole = null,
                            selectedPermissions = emptyList(),
                            emailError = null
                        )
                    }
                }

                is UsersListEvent.HideAddUserDialog -> {
                    _state.update { it.copy(showAddUserDialog = false) }
                }

                is UsersListEvent.ShowDeleteConfirmation -> {
                    _state.update {
                        it.copy(
                            showDeleteConfirmation = true,
                            userToDelete = event.user
                        )
                    }
                }

                is UsersListEvent.HideDeleteConfirmation -> {
                    _state.update {
                        it.copy(
                            showDeleteConfirmation = false,
                            userToDelete = null
                        )
                    }
                }

                is UsersListEvent.AddUser -> {
                    _state.update { it.copy(emailError = null, isAddUserButtonLoading = true) }

                    // Check if email exists in the database
                    userRepository.checkEmailExists(event.email).collect { result ->
                        when (result) {
                            is ResultState.Success -> {
                                if (result.data) {
                                    // Email already exists
                                    _state.update {
                                        it.copy(
                                            emailError = "Email already exists. Please use a different email address.",
                                            isAddUserButtonLoading = false
                                        )
                                    }
                                } else {
                                    // Email doesn't exist, proceed with user creation
                                    sessionManagement.getShopId()?.let { shopId ->
                                        sessionManagement.getShopName()?.let { shopName ->
                                            val newUser = UserEntity(
                                                shop_id = shopId,
                                                username = event.username,
                                                shopName = shopName,
                                                email = event.email.trim().lowercase(),
                                                role = event.role,
                                                permissions = event.permissions.map { it.name },
                                                isActive = true,
                                                id = NanoIdUtils.randomNanoId()
                                            )

                                            // Call repository method to create user
                                            userRepository.createUser(newUser).collect { createResult ->
                                                when (createResult) {
                                                    is ResultState.Success -> {
                                                        _state.update { it.copy(showAddUserDialog = false,isAddUserButtonLoading = false) }
                                                        handleEvent(UsersListEvent.RefreshUsers)
                                                    }
                                                    is ResultState.Failure -> {
                                                        _state.update { it.copy(error = createResult.message.localizedMessage, isAddUserButtonLoading = false) }
                                                    }
                                                    is ResultState.Loading -> {
                                                        // Already in loading state
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            is ResultState.Failure -> {
                                _state.update { it.copy(error = result.message.localizedMessage, isAddUserButtonLoading = false) }
                            }
                            is ResultState.Loading -> {
                                // Already in loading state
                            }
                        }
                    }
                }


            }
        }
    }

    private fun checkDuplicateEmail(email: String): Boolean {
        return _state.value.users.any {
            it.email.equals(email, ignoreCase = true)
        }
    }

    private fun applyFilters() {
        val currentState = _state.value
        var filtered = currentState.users

        // Apply search query
        if (currentState.searchQuery.isNotEmpty()) {
            filtered = filtered.filter { user ->
                user.username.contains(currentState.searchQuery, ignoreCase = true) ||
                        user.email.contains(currentState.searchQuery, ignoreCase = true)
            }
        }

        // Apply role filter
        currentState.roleFilter?.let { role ->
            filtered = filtered.filter { it.role == role }
        }

        // Apply status filter
        currentState.statusFilter?.let { isActive ->
            filtered = filtered.filter { it.isActive == isActive }
        }

        _state.update { it.copy(filteredUsers = filtered) }
    }
}

