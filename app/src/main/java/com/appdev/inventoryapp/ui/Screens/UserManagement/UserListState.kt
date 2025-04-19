package com.appdev.inventoryapp.ui.Screens.UserManagement

import com.appdev.inventoryapp.Utils.Permission
import com.appdev.inventoryapp.Utils.UserRole
import com.appdev.inventoryapp.domain.model.AuditLogEntry
import com.appdev.inventoryapp.domain.model.UserEntity

data class UsersListState(
    val rolesList: List<String> = listOf("Edit User", "View Only"),
    val users: List<UserEntity> = emptyList(),
    val filteredUsers: List<UserEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val roleFilter: String? = null,
    val statusFilter: Boolean? = null,
    val isRoleDropdownExpanded: Boolean = false,
    val isStatusDropdownExpanded: Boolean = false,
    val showAddUserDialog: Boolean = false,
    val showEditUserDialog: Boolean = false,
    val username: String = "",
    val email: String = "",
    val emailError: String? = null,
    val selectedRole: String? = null,
    val selectedPermissions: List<Permission> = emptyList(),
    val showPermissions: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val userToDelete: UserEntity? = null,
    val isDeleteLoading: Boolean = false,
    val isAddUserButtonLoading: Boolean = false,
    val isUpdateUserLoading: Boolean = false,
    val canManageUsers: Boolean = true,
    val userToEdit: UserEntity? = null,
    val editUsername: String = "",
    val editRole: String? = null,
    val editPermissions: List<Permission> = emptyList(),
    val auditLogs: List<AuditLogEntry> = emptyList(),
    val isAuditLogLoading: Boolean = false,
    val auditLogError: String? = null,
    val showAuditLogDialog: Boolean = false,
    val selectedUserForAuditLog: UserEntity? = null,

    val showLogsDialog: Boolean = false,
    val logsData: List<AuditLogEntry> = emptyList(),
    val isLogsLoading: Boolean = false,
    val userPermissions: List<String> = emptyList(),

    )
