package com.appdev.inventoryapp.ui.Screens.UserManagement

import com.appdev.inventoryapp.Utils.Permission
import com.appdev.inventoryapp.Utils.UserRole
import com.appdev.inventoryapp.domain.model.UserEntity

sealed class UsersListEvent {
    data class SearchUsers(val query: String) : UsersListEvent()
    data class FilterByRole(val role: String?) : UsersListEvent()
    data class FilterByStatus(val isActive: Boolean?) : UsersListEvent()
    data object RefreshUsers : UsersListEvent()
    data class DeleteUser(val userId: String) : UsersListEvent()
    data class ActivateUser(val userId: String) : UsersListEvent()
    data class DeactivateUser(val userId: String) : UsersListEvent()
    data class NavigateToUserDetail(val userId: String?) : UsersListEvent()

    data object ShowAddUserDialog : UsersListEvent()
    data object HideAddUserDialog : UsersListEvent()
    data class ShowDeleteConfirmation(val user: UserEntity) : UsersListEvent()
    data object HideDeleteConfirmation : UsersListEvent()

    data class AddUser(
        val username: String,
        val email: String,
        val password: String, // Added password parameter
        val role: String,
        val permissions: List<Permission>
    ) : UsersListEvent()

    // UI State Management
    data class ToggleStatusDropdown(val isExpanded: Boolean) : UsersListEvent()
    data class ToggleRoleDropdown(val isExpanded: Boolean) : UsersListEvent()
    data class SelectRole(val role: String) : UsersListEvent()
    data class UpdatePermissions(val permissions: List<Permission>) : UsersListEvent()
    data class UpdateUsername(val username: String) : UsersListEvent()
    data class UpdateEmail(val email: String) : UsersListEvent()
    data class ToggleShowPermissions(val show: Boolean) : UsersListEvent()

    data class ShowEditUserDialog(val user: UserEntity) : UsersListEvent()
    data object HideEditUserDialog : UsersListEvent()
    data class UpdateEditUsername(val username: String) : UsersListEvent()
    data class UpdateEditRole(val role: String) : UsersListEvent()
    data class UpdateEditPermissions(val permissions: List<Permission>) : UsersListEvent()
    data object UpdateUser : UsersListEvent()
    data class ToggleEditRoleDropdown(val isExpanded: Boolean) : UsersListEvent()

    data class ShowUserAuditLog(val user: UserEntity) : UsersListEvent()
    data object ShowLogsDialog : UsersListEvent()
    data object HideLogsDialog : UsersListEvent()
    data object FetchAllLogs : UsersListEvent()

    data class UpdatePassword(val password: String) : UsersListEvent() // Added password update event
    data object TogglePasswordVisibility : UsersListEvent() // Added password visibility toggle
}
