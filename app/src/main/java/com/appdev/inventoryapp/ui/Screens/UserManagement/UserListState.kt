package com.appdev.inventoryapp.ui.Screens.UserManagement

import com.appdev.inventoryapp.Utils.Permission
import com.appdev.inventoryapp.Utils.UserRole
import com.appdev.inventoryapp.domain.model.UserEntity

data class UsersListState(
    val users: List<UserEntity> = emptyList(),
    val filteredUsers: List<UserEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val roleFilter: String? = null,
    val rolesList: List<String> = listOf("Edit User", "View Only"),
    val statusFilter: Boolean? = null,
    val showAddUserDialog: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val userToDelete: UserEntity? = null,
    val canManageUsers: Boolean = false,
    val isStatusDropdownExpanded: Boolean = false,
    val isRoleDropdownExpanded: Boolean = false,
    val selectedRole: String? = null,
    val selectedPermissions: List<Permission> = emptyList(),
    val username: String = "",
    val email: String = "",
    val emailError: String? = null,
    val showPermissions: Boolean = false,
    val isAddUserButtonLoading: Boolean = false,
    val isDeleteLoading: Boolean = false,  // Add this new state property
)
