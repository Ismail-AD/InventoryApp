package com.appdev.inventoryapp.ui.Screens.UserManagement

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdev.inventoryapp.Utils.AuditActionType
import com.appdev.inventoryapp.Utils.Permission
import com.appdev.inventoryapp.Utils.ResultState
import com.appdev.inventoryapp.Utils.SessionManagement
import com.appdev.inventoryapp.Utils.UserRole
import com.appdev.inventoryapp.domain.model.AuditLogEntry
import com.appdev.inventoryapp.domain.model.UserEntity
import com.appdev.inventoryapp.domain.repository.AuditLogRepository
import com.appdev.inventoryapp.domain.repository.UserRepository
import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class UsersListViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val auditLogRepository: AuditLogRepository,
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

    fun getUserRole(): String {
        return sessionManagement.getUserRole() ?: "View Only"
    }


    private fun createAuditLog(
        actionType: AuditActionType,
        targetUser: UserEntity,
        changes: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            val formattedTime = formatTimestamp(System.currentTimeMillis())
            val performedBy = sessionManagement.getUserName() ?: ""

            // Create a comprehensive description with all details
            val description = buildString {
                append("${actionType.name} - User")
                append("(email: ${targetUser.email}, role: ${targetUser.role}) ")
                append("at $formattedTime")

                if (changes.isNotEmpty()) {
                    append(". Changes: ${changes.joinToString("; ")}")
                }
            }

            val auditEntry = AuditLogEntry(
                actionType = actionType.name,
                shopId = sessionManagement.getShopId() ?: "",
                performedByUserId = sessionManagement.getUserId() ?: "",
                performedByUsername = performedBy,
                targetUserId = targetUser.id,
                targetUsername = targetUser.username,
                description = description
            )

            auditLogRepository.createAuditLog(auditEntry).collect { result ->
                when (result) {
                    is ResultState.Failure -> {
                        Log.e(
                            "AUDIT_LOG",
                            "Failed to create audit log: ${result.message.localizedMessage}"
                        )
                    }

                    else -> {}
                }
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    fun handleEvent(event: UsersListEvent) {
//        if (!canManageUsers() && event !is UsersListEvent.RefreshUsers) {
//            return
//        }

        viewModelScope.launch {
            when (event) {

                is UsersListEvent.ShowLogsDialog -> {
                    _state.update { it.copy(showLogsDialog = true) }
                }

                is UsersListEvent.HideLogsDialog -> {
                    _state.update { it.copy(showLogsDialog = false) }
                }

                is UsersListEvent.FetchAllLogs -> {
                    _state.update { it.copy(isLogsLoading = true) }

                    val shopId = sessionManagement.getShopId() ?: ""

                    auditLogRepository.getAuditLogsForUser(shopId).collect { result ->
                        when (result) {
                            is ResultState.Success -> {
                                val processedLogs = result.data.map { logEntry ->
                                    // Update the description for each log entry
                                    val updatedDescription = updateLogString(
                                        originalString = logEntry.description,
                                        targetUserName = logEntry.targetUsername,
                                        performedByName = logEntry.performedByUsername
                                    )

                                    // Return a copy of the log entry with the updated description
                                    logEntry.copy(description = updatedDescription)
                                }

                                _state.update {
                                    it.copy(
                                        logsData = processedLogs,
                                        isLogsLoading = false
                                    )
                                }
                            }

                            is ResultState.Failure -> {
                                _state.update {
                                    it.copy(
                                        error = result.message.localizedMessage,
                                        isLogsLoading = false
                                    )
                                }
                            }

                            is ResultState.Loading -> {
                                // Already in loading state
                            }
                        }
                    }
                }

                is UsersListEvent.ShowEditUserDialog -> {
                    val permissionsList = event.user.permissions?.mapNotNull {
                        try {
                            Permission.valueOf(it)
                        } catch (e: IllegalArgumentException) {
                            null
                        }
                    } ?: emptyList()

                    _state.update {
                        it.copy(
                            showEditUserDialog = true,
                            userToEdit = event.user,
                            editUsername = event.user.username,
                            editRole = event.user.role,
                            editPermissions = permissionsList,
                            showPermissions = true
                        )
                    }
                }

                is UsersListEvent.UpdateUser -> {
                    val currentState = _state.value
                    val userToEdit = currentState.userToEdit

                    if (userToEdit != null) {
                        _state.update { it.copy(isUpdateUserLoading = true) }

                        val updatedUser = userToEdit.copy(
                            username = currentState.editUsername,
                            role = currentState.editRole ?: userToEdit.role,
                            permissions = currentState.editPermissions.map { it.name }
                        )

                        // Track all changes in a single list
                        val changeDetails = mutableListOf<String>()

                        if (updatedUser.username != userToEdit.username) {
                            changeDetails.add("Username changed from '${userToEdit.username}' to '${updatedUser.username}'")
                        }

                        if (updatedUser.role != userToEdit.role) {
                            changeDetails.add("Role changed from '${userToEdit.role}' to '${updatedUser.role}'")
                        }

                        val oldPermissions = userToEdit.permissions?.toSet() ?: emptySet()
                        val newPermissions = updatedUser.permissions?.toSet() ?: emptySet()

                        if (oldPermissions != newPermissions) {
                            val added = newPermissions.filter { it !in oldPermissions }
                            val removed = oldPermissions.filter { it !in newPermissions }

                            if (added.isNotEmpty()) {
                                changeDetails.add("Permissions added: ${added.joinToString(", ")}")
                            }

                            if (removed.isNotEmpty()) {
                                changeDetails.add("Permissions removed: ${removed.joinToString(", ")}")
                            }
                        }

                        userRepository.updateUser(updatedUser).collect { result ->
                            when (result) {
                                is ResultState.Success -> {
                                    _state.update {
                                        it.copy(
                                            isUpdateUserLoading = false,
                                            showEditUserDialog = false,
                                            userToEdit = null
                                        )
                                    }

                                    // Create a single comprehensive audit log with all changes
                                    if (changeDetails.isNotEmpty()) {
                                        createAuditLog(
                                            AuditActionType.USER_UPDATED,
                                            updatedUser,
                                            changeDetails
                                        )
                                    }

                                    handleEvent(UsersListEvent.RefreshUsers)
                                }

                                is ResultState.Failure -> {
                                    _state.update {
                                        it.copy(
                                            error = result.message.localizedMessage,
                                            isUpdateUserLoading = false
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

                is UsersListEvent.ActivateUser -> {
                    // Find the user in the state to include in audit log
                    val userToActivate = _state.value.users.find { it.id == event.userId }

                    userRepository.activateUser(event.userId).collect { result ->
                        when (result) {
                            is ResultState.Success -> {
                                // Create audit log entry for activation
                                userToActivate?.let {
                                    createAuditLog(
                                        AuditActionType.USER_ACTIVATED,
                                        it
                                    )
                                }
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
                    // Find the user in the state to include in audit log
                    val userToDeactivate = _state.value.users.find { it.id == event.userId }

                    userRepository.deactivateUser(event.userId).collect { result ->
                        when (result) {
                            is ResultState.Success -> {
                                // Create audit log entry for deactivation
                                userToDeactivate?.let {
                                    createAuditLog(
                                        AuditActionType.USER_DEACTIVATED,
                                        it
                                    )
                                }
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

                is UsersListEvent.DeleteUser -> {
                    // Find the user in the state to include in audit log
                    val userToDelete = _state.value.userToDelete

                    _state.update { it.copy(isDeleteLoading = true) }
                    userRepository.deleteUser(event.userId).collect { result ->
                        when (result) {
                            is ResultState.Success -> {
                                // Create audit log entry for deletion
                                userToDelete?.let {
                                    createAuditLog(
                                        AuditActionType.USER_DELETED,
                                        it
                                    )
                                }

                                _state.update {
                                    it.copy(
                                        isDeleteLoading = false,
                                        showDeleteConfirmation = false,
                                        userToDelete = null
                                    )
                                }
                                handleEvent(UsersListEvent.RefreshUsers)
                            }

                            is ResultState.Failure -> {
                                _state.update {
                                    it.copy(
                                        error = result.message.localizedMessage,
                                        isDeleteLoading = false
                                    )
                                }
                            }

                            is ResultState.Loading -> {
                                // Loading state is already set above
                            }
                        }
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
                                            userRepository.createUser(newUser)
                                                .collect { createResult ->
                                                    when (createResult) {
                                                        is ResultState.Success -> {
                                                            // Create audit log entry for new user creation
                                                            val details = listOf(
                                                                "Role: ${newUser.role}",
                                                                "Permissions: ${
                                                                    newUser.permissions?.joinToString(
                                                                        ", "
                                                                    ) ?: "none"
                                                                }"
                                                            )

                                                            createAuditLog(
                                                                AuditActionType.USER_CREATED,
                                                                newUser,
                                                                details
                                                            )

                                                            _state.update {
                                                                it.copy(
                                                                    showAddUserDialog = false,
                                                                    isAddUserButtonLoading = false
                                                                )
                                                            }
                                                            handleEvent(UsersListEvent.RefreshUsers)
                                                        }

                                                        is ResultState.Failure -> {
                                                            _state.update {
                                                                it.copy(
                                                                    error = createResult.message.localizedMessage,
                                                                    isAddUserButtonLoading = false
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
                                }
                            }

                            is ResultState.Failure -> {
                                _state.update {
                                    it.copy(
                                        error = result.message.localizedMessage,
                                        isAddUserButtonLoading = false
                                    )
                                }
                            }

                            is ResultState.Loading -> {
                                // Already in loading state
                            }
                        }
                    }
                }

                // Audit log related events
                is UsersListEvent.ShowUserAuditLog -> {
                    _state.update {
                        it.copy(
                            showAuditLogDialog = true,
                            selectedUserForAuditLog = event.user,
                            isAuditLogLoading = true
                        )
                    }

                    val shopId = sessionManagement.getShopId() ?: return@launch

                    auditLogRepository.getAuditLogsForUser(shopId).collect { result ->
                        when (result) {
                            is ResultState.Success -> {
                                _state.update {
                                    it.copy(
                                        auditLogs = result.data,
                                        isAuditLogLoading = false
                                    )
                                }
                            }

                            is ResultState.Failure -> {
                                _state.update {
                                    it.copy(
                                        auditLogError = result.message.localizedMessage,
                                        isAuditLogLoading = false
                                    )
                                }
                            }

                            is ResultState.Loading -> {
                                // Already in loading state
                            }
                        }
                    }
                }


                is UsersListEvent.HideEditUserDialog -> {
                    _state.update {
                        it.copy(
                            showEditUserDialog = false,
                            userToEdit = null,
                            editUsername = "",
                            editRole = null,
                            editPermissions = emptyList()
                        )
                    }
                }

                is UsersListEvent.UpdateEditUsername -> {
                    _state.update { it.copy(editUsername = event.username) }
                }

                is UsersListEvent.UpdateEditRole -> {
                    _state.update { it.copy(editRole = event.role) }
                }

                is UsersListEvent.UpdateEditPermissions -> {
                    _state.update { it.copy(editPermissions = event.permissions) }
                }

                is UsersListEvent.ToggleEditRoleDropdown -> {
                    _state.update { it.copy(isRoleDropdownExpanded = event.isExpanded) }
                }


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


            }
        }
    }



    private fun checkDuplicateEmail(email: String): Boolean {
        return _state.value.users.any {
            it.email.equals(email, ignoreCase = true)
        }
    }

    fun updateLogString(
        originalString: String,
        targetUserName: String,
        performedByName: String
    ): String {
        // Insert target username after "User" keyword and before the parenthesis
        val userKeywordIndex = originalString.indexOf("User")

        val firstPartWithUsername = if (userKeywordIndex != -1) {
            val afterUserInsertPosition = userKeywordIndex + "User".length
            originalString.substring(0, afterUserInsertPosition) +
                    " $targetUserName" +
                    originalString.substring(afterUserInsertPosition)
        } else {
            originalString
        }

        // Find the timestamp pattern to correctly position "by performedBy"
        val timePattern = "at \\w+ \\d+, \\d{4} at \\d+:\\d+ [AP]M"
        val regex = timePattern.toRegex()
        val matchResult = regex.find(firstPartWithUsername)

        return if (matchResult != null) {
            val timeEndIndex = matchResult.range.last + 1

            // Check if there's additional content (like "Changes:") after the timestamp
            val periodIndex = firstPartWithUsername.indexOf(".", timeEndIndex)

            if (periodIndex != -1) {
                // Insert "by performedBy" before the period that starts additional content
                firstPartWithUsername.substring(0, periodIndex) +
                        " by $performedByName" +
                        firstPartWithUsername.substring(periodIndex)
            } else {
                // No additional content, append "by performedBy" at the end
                "${firstPartWithUsername} by $performedByName"
            }
        } else {
            // Fallback if timestamp pattern not found: try to find just "at" near the end
            val atIndex = firstPartWithUsername.lastIndexOf("at ")

            if (atIndex != -1) {
                // Look for natural breakpoints after the timestamp
                val possibleBreakpoints = listOf(". ", ", ", " - ")
                val breakpointIndex = possibleBreakpoints.mapNotNull {
                    val idx = firstPartWithUsername.indexOf(it, atIndex)
                    if (idx != -1) idx else null
                }.minOrNull()

                if (breakpointIndex != null) {
                    firstPartWithUsername.substring(0, breakpointIndex) +
                            " by $performedByName" +
                            firstPartWithUsername.substring(breakpointIndex)
                } else {
                    // No obvious breakpoint, just append at the end
                    "$firstPartWithUsername by $performedByName"
                }
            } else {
                // No "at" found, just append at the end
                "$firstPartWithUsername by $performedByName"
            }
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

