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
import com.appdev.inventoryapp.domain.repository.KeyRepository
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
    private val keyRepository: KeyRepository,
    private val auditLogRepository: AuditLogRepository,
    val sessionManagement: SessionManagement
) : ViewModel() {

    private val _state = MutableStateFlow(UsersListState())
    val state: StateFlow<UsersListState> = _state.asStateFlow()


    init {
        viewModelScope.launch {
            loadUserPermissions()
            handleEvent(UsersListEvent.RefreshUsers)
        }
    }

    fun getUserRole(): String {
        return sessionManagement.getUserRole() ?: "View Only"
    }

    fun canManageUsers(): Boolean {
        return state.value.userPermissions.contains(Permission.MANAGE_USERS.name)
    }

    private fun loadUserPermissions() {
        viewModelScope.launch {
            val myId = userRepository.getCurrentUserId() ?: sessionManagement.getUserId()
            if (myId != null) {
                userRepository.getUserById(myId).collect { result ->
                    when (result) {
                        is ResultState.Loading -> {
                            _state.update { it.copy(isLoading = true) }
                        }

                        is ResultState.Success -> {
                            Log.d("CADS", "${result.data}")
                            _state.update {
                                it.copy(
                                    userPermissions = result.data.permissions ?: emptyList(),
                                    error = null
                                )
                            }
                        }

                        is ResultState.Failure -> {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Failed to get Permissions: ${result.message.localizedMessage}"
                                )
                            }
                        }

                        else -> {}
                    }

                }
            }
        }
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

    private fun validatePassword(password: String): String? {
        if (password.length < 6) {
            return "Password must be at least 6 characters long"
        }
        return null // Password is valid
    }

    // Function to create user and then store the password securely
    private fun createUserAndStorePassword(newUser: UserEntity, password: String) {
        viewModelScope.launch {
            // First create the user
            userRepository.createUser(newUser).collect { createResult ->
                when (createResult) {
                    is ResultState.Success -> {
                        // Now store the password in the keys table
                        keyRepository.storePassword(newUser.id, password)
                            .collect { passwordResult ->
                                when (passwordResult) {
                                    is ResultState.Success -> {
                                        // Create audit log entry for new user creation
                                        val details = listOf(
                                            "Role: ${newUser.role}",
                                            "Permissions: ${newUser.permissions?.joinToString(", ") ?: "none"}"
                                        )

                                        createAuditLog(
                                            AuditActionType.USER_CREATED,
                                            newUser,
                                            details
                                        )

                                        _state.update {
                                            it.copy(
                                                showAddUserDialog = false,
                                                isAddUserButtonLoading = false,
                                                password = "" // Clear password after successful creation
                                            )
                                        }
                                        handleEvent(UsersListEvent.RefreshUsers)
                                    }

                                    is ResultState.Failure -> {
                                        _state.update {
                                            it.copy(
                                                error = "User created but failed to store password: ${passwordResult.message.localizedMessage}",
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

    fun handleEvent(event: UsersListEvent) {
//        if (!canManageUsers()) {
//            return
//        }

        viewModelScope.launch {
            when (event) {

                // Add password-related events
                is UsersListEvent.UpdatePassword -> {
                    // Validate password complexity
                    val passwordError = validatePassword(event.password)
                    _state.update {
                        it.copy(
                            password = event.password,
                            passwordError = passwordError
                        )
                    }
                }

                is UsersListEvent.TogglePasswordVisibility -> {
                    _state.update { it.copy(showPassword = !it.showPassword) }
                }

                // Update AddUser event to handle passwords
                is UsersListEvent.AddUser -> {
                    _state.update {
                        it.copy(
                            emailError = null,
                            usernameError = null,
                            passwordError = null,
                            isAddUserButtonLoading = true
                        )
                    }

                    // Validate password
                    val passwordError = validatePassword(event.password)
                    if (passwordError != null) {
                        _state.update {
                            it.copy(
                                passwordError = passwordError,
                                isAddUserButtonLoading = false
                            )
                        }
                        return@launch
                    }

                    // Check if username exists
                    val usernameResult = checkUsernameExists(event.username)
                    if (usernameResult is ResultState.Success && usernameResult.data) {
                        _state.update {
                            it.copy(
                                usernameError = "Username already exists. Please choose a different username.",
                                isAddUserButtonLoading = false
                            )
                        }
                        return@launch
                    }

                    // Username doesn't exist, now check email
                    val emailResult = checkEmailExists(event.email)
                    if (emailResult is ResultState.Success && emailResult.data) {
                        _state.update {
                            it.copy(
                                emailError = "Email already exists. Please use a different email address.",
                                isAddUserButtonLoading = false
                            )
                        }
                        return@launch
                    }

                    // Both username and email are unique, proceed with user creation
                    val shopId = sessionManagement.getShopId()
                    val shopName = sessionManagement.getShopName()

                    if (shopId != null && shopName != null) {
                        // Generate userId first
                        val userId = NanoIdUtils.randomNanoId()

                        val newUser = UserEntity(
                            id = userId,
                            shop_id = shopId,
                            username = event.username,
                            shopName = shopName,
                            email = event.email.trim().lowercase(),
                            role = event.role,
                            permissions = event.permissions.map { it.name },
                            isActive = true
                        )

                        // Create user first, then store password
                        createUserAndStorePassword(newUser, event.password)
                    } else {
                        _state.update {
                            it.copy(
                                error = "Shop information is missing",
                                isAddUserButtonLoading = false
                            )
                        }
                    }
                }

                // Also update DeleteUser to delete the password
                is UsersListEvent.DeleteUser -> {
                    // Find the user in the state to include in audit log
                    val userToDelete = _state.value.userToDelete

                    _state.update { it.copy(isDeleteLoading = true) }

                    // Delete the user
                    userRepository.deleteUser(event.userId).collect { result ->
                        when (result) {
                            is ResultState.Success -> {
                                // Also delete the password from keys table
                                keyRepository.deletePassword(event.userId).collect { _ ->
                                    // We don't need to handle this result specifically,
                                    // just continue with the flow
                                }

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
                        _state.update { it.copy(
                            isUpdateUserLoading = true,
                            usernameError = null,
                            emailError = null
                        ) }

                        // Check if username exists (except for current user)
                        val usernameResult = checkUsernameExists(currentState.editUsername, userToEdit.id)
                        if (usernameResult is ResultState.Success && usernameResult.data) {
                            _state.update {
                                it.copy(
                                    usernameError = "Username already exists. Please choose a different username.",
                                    isUpdateUserLoading = false
                                )
                            }
                            return@launch
                        }

                        // Username check passed, proceed with update
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

                        updateUserAndCreateAuditLog(updatedUser, changeDetails)
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

//                is UsersListEvent.DeleteUser -> {
//                    // Find the user in the state to include in audit log
//                    val userToDelete = _state.value.userToDelete
//
//                    _state.update { it.copy(isDeleteLoading = true) }
//                    userRepository.deleteUser(event.userId).collect { result ->
//                        when (result) {
//                            is ResultState.Success -> {
//                                // Create audit log entry for deletion
//                                userToDelete?.let {
//                                    createAuditLog(
//                                        AuditActionType.USER_DELETED,
//                                        it
//                                    )
//                                }
//
//                                _state.update {
//                                    it.copy(
//                                        isDeleteLoading = false,
//                                        showDeleteConfirmation = false,
//                                        userToDelete = null
//                                    )
//                                }
//                                handleEvent(UsersListEvent.RefreshUsers)
//                            }
//
//                            is ResultState.Failure -> {
//                                _state.update {
//                                    it.copy(
//                                        error = result.message.localizedMessage,
//                                        isDeleteLoading = false
//                                    )
//                                }
//                            }
//
//                            is ResultState.Loading -> {
//                                // Loading state is already set above
//                            }
//                        }
//                    }
//                }
//
//
//
//                // Now let's also refactor the AddUser event handler to use these functions
//                is UsersListEvent.AddUser -> {
//                    _state.update {
//                        it.copy(
//                            emailError = null,
//                            usernameError = null,
//                            isAddUserButtonLoading = true
//                        )
//                    }
//
//                    // Check if username exists
//                    val usernameResult = checkUsernameExists(event.username)
//                    if (usernameResult is ResultState.Success && usernameResult.data) {
//                        _state.update {
//                            it.copy(
//                                usernameError = "Username already exists. Please choose a different username.",
//                                isAddUserButtonLoading = false
//                            )
//                        }
//                        return@launch
//                    }
//
//                    // Username doesn't exist, now check email
//                    val emailResult = checkEmailExists(event.email)
//                    if (emailResult is ResultState.Success && emailResult.data) {
//                        _state.update {
//                            it.copy(
//                                emailError = "Email already exists. Please use a different email address.",
//                                isAddUserButtonLoading = false
//                            )
//                        }
//                        return@launch
//                    }
//
//                    // Both username and email are unique, proceed with user creation
//                    val shopId = sessionManagement.getShopId()
//                    val shopName = sessionManagement.getShopName()
//
//                    if (shopId != null && shopName != null) {
//                        val newUser = UserEntity(
//                            shop_id = shopId,
//                            username = event.username,
//                            shopName = shopName,
//                            email = event.email.trim().lowercase(),
//                            role = event.role,
//                            permissions = event.permissions.map { it.name },
//                            isActive = true,
//                            id = NanoIdUtils.randomNanoId()
//                        )
//
//                        createUserAndAuditLog(newUser)
//                    } else {
//                        _state.update {
//                            it.copy(
//                                error = "Shop information is missing",
//                                isAddUserButtonLoading = false
//                            )
//                        }
//                    }
//                }

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
    private fun createUserAndAuditLog(newUser: UserEntity) {
        viewModelScope.launch {
            userRepository.createUser(newUser).collect { createResult ->
                when (createResult) {
                    is ResultState.Success -> {
                        // Create audit log entry for new user creation
                        val details = listOf(
                            "Role: ${newUser.role}",
                            "Permissions: ${newUser.permissions?.joinToString(", ") ?: "none"}"
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
    private fun updateUserAndCreateAuditLog(updatedUser: UserEntity, changeDetails: List<String>) {
        viewModelScope.launch {
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


    // First, let's create a separate function for checking username and email existence

    private suspend fun checkUsernameExists(username: String, userId: String? = null): ResultState<Boolean> {
        return try {
            var exists = false
            userRepository.checkUsernameExists(username).collect { result ->
                when (result) {
                    is ResultState.Success -> {
                        // If we're updating a user, the username is allowed to be the same as the current user's
                        if (userId != null) {
                            // Get the current user with this ID to compare usernames
                            var currentUsername = ""
                            userRepository.getUserById(userId).collect { userResult ->
                                if (userResult is ResultState.Success) {
                                    currentUsername = userResult.data.username
                                }
                            }
                            // Username exists but it's the current user's username, so it's okay
                            exists = result.data && username != currentUsername
                        } else {
                            // For new users, any existing username is a conflict
                            exists = result.data
                        }
                    }
                    is ResultState.Failure -> throw result.message
                    else -> {}
                }
            }
            ResultState.Success(exists)
        } catch (e: Exception) {
            ResultState.Failure(e)
        }
    }

    private suspend fun checkEmailExists(email: String, userId: String? = null): ResultState<Boolean> {
        return try {
            var exists = false
            userRepository.checkEmailExists(email).collect { result ->
                when (result) {
                    is ResultState.Success -> {
                        // If we're updating a user, the email is allowed to be the same as the current user's
                        if (userId != null) {
                            // Get the current user with this ID to compare emails
                            var currentEmail = ""
                            userRepository.getUserById(userId).collect { userResult ->
                                if (userResult is ResultState.Success) {
                                    currentEmail = userResult.data.email
                                }
                            }
                            // Email exists but it's the current user's email, so it's okay
                            exists = result.data && email.lowercase() != currentEmail.lowercase()
                        } else {
                            // For new users, any existing email is a conflict
                            exists = result.data
                        }
                    }
                    is ResultState.Failure -> throw result.message
                    else -> {}
                }
            }
            ResultState.Success(exists)
        } catch (e: Exception) {
            ResultState.Failure(e)
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

        // Find AM/PM pattern to correctly position "performed by name"
        val amPmRegex = "(\\d+:\\d+ [AP]M)".toRegex()
        val matchResult = amPmRegex.find(firstPartWithUsername)

        return if (matchResult != null) {
            val amPmEndIndex = matchResult.range.last + 1

            // Check if there's a period after AM/PM
            val periodAfterTime = firstPartWithUsername.indexOf(".", amPmEndIndex)

            if (periodAfterTime != -1) {
                // Insert "performed by name" after AM/PM but before the period
                firstPartWithUsername.substring(0, amPmEndIndex) +
                        " performed by $performedByName" +
                        firstPartWithUsername.substring(amPmEndIndex)
            } else {
                // No period found after the time, just append at the end of the string
                "$firstPartWithUsername performed by $performedByName"
            }
        } else {
            // Fallback if AM/PM pattern not found
            // Try to find a timestamp pattern or just append at the end
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
                            " performed by $performedByName" +
                            firstPartWithUsername.substring(breakpointIndex)
                } else {
                    // No obvious breakpoint, just append at the end
                    "$firstPartWithUsername performed by $performedByName"
                }
            } else {
                // No "at" found, just append at the end
                "$firstPartWithUsername performed by $performedByName"
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

