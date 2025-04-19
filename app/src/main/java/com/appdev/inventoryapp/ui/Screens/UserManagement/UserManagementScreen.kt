package com.appdev.inventoryapp.ui.Screens.UserManagement

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdev.inventoryapp.R
import com.appdev.inventoryapp.Utils.AuditActionType
import com.appdev.inventoryapp.Utils.Permission
import com.appdev.inventoryapp.Utils.UserRole
import com.appdev.inventoryapp.domain.model.AuditLogEntry
import com.appdev.inventoryapp.domain.model.UserEntity
import com.appdev.inventoryapp.ui.Reuseables.DeleteConfirmationDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UsersManagementScreen(
    viewModel: UsersListViewModel = hiltViewModel(),
    navigateToUserDetail: (String?) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    UsersManagementScreenContent(
        viewModel.getUserRole(),
        state = state,
        onEvent = viewModel::handleEvent,
        navigateToUserDetail = navigateToUserDetail
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersManagementScreenContent(
    userRole: String,
    state: UsersListState,
    onEvent: (UsersListEvent) -> Unit,
    navigateToUserDetail: (String?) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            if (state.users.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { onEvent(UsersListEvent.ShowAddUserDialog) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Add User",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            else if (!state.userPermissions.contains(Permission.MANAGE_USERS.name)) {
                AccessDeniedMessage()
            }
            else if (state.users.isEmpty()) {
                EmptyUsersMessage(onAddUser = { onEvent(UsersListEvent.ShowAddUserDialog) })
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    if (userRole == "Admin") {
                        Button(
                            onClick = {
                                onEvent(UsersListEvent.FetchAllLogs)
                                onEvent(UsersListEvent.ShowLogsDialog)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ListAlt,
                                contentDescription = "Show Logs",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Show Logs")
                        }
                    }
                    UserSearchBar(
                        query = state.searchQuery,
                        onQueryChange = { onEvent(UsersListEvent.SearchUsers(it)) }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        RoleFilterDropdown(
                            roles=state.rolesList,
                            selectedRole = state.roleFilter,
                            onRoleSelected = { onEvent(UsersListEvent.FilterByRole(it)) },
                            isExpanded = state.isRoleDropdownExpanded,
                            onExpandedChange = { onEvent(UsersListEvent.ToggleRoleDropdown(it)) },
                            modifier = Modifier.weight(1f)
                        )

                        StatusFilterDropdown(
                            selectedStatus = state.statusFilter,
                            onStatusSelected = { onEvent(UsersListEvent.FilterByStatus(it)) },
                            isExpanded = state.isStatusDropdownExpanded,
                            onExpandedChange = { onEvent(UsersListEvent.ToggleStatusDropdown(it)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.filteredUsers.isEmpty()) {
                        NoUsersFoundMessage()
                    } else {
                        UsersList(
                            users = state.filteredUsers,
                            onUserClick = { onEvent(UsersListEvent.ShowEditUserDialog(it)) } ,
                            onActivate = { onEvent(UsersListEvent.ActivateUser(it.id)) },
                            onDeactivate = { onEvent(UsersListEvent.DeactivateUser(it.id)) },
                            onDelete = { onEvent(UsersListEvent.ShowDeleteConfirmation(it)) }
                        )
                    }
                }
            }

            // Error message
            state.error?.let { errorMessage ->
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { onEvent(UsersListEvent.RefreshUsers) }) {
                            Text("Retry", color = MaterialTheme.colorScheme.onError)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) {
                    Text(text = errorMessage)
                }
            }
        }
    }

    if (state.showAddUserDialog) {
        AddUserDialog(
            state = state,
            onEvent = onEvent,
            onDismiss = { onEvent(UsersListEvent.HideAddUserDialog) }
        )
    }

    if (state.showEditUserDialog && state.userToEdit != null) {
        EditUserDialog(
            state = state,
            onEvent = onEvent,
            onDismiss = { onEvent(UsersListEvent.HideEditUserDialog) }
        )
    }


    // Delete Confirmation Dialog
    if (state.showDeleteConfirmation && state.userToDelete != null) {
        DeleteConfirmationDialog(
            title = "Delete User",
            show = state.showDeleteConfirmation,
            itemName = state.userToDelete.username,
            isLoading = state.isDeleteLoading, // Pass the loading state
            onConfirm = {
                state.userToDelete.id.let { onEvent(UsersListEvent.DeleteUser(it)) }
            },
            onDismiss = {
                onEvent(UsersListEvent.HideDeleteConfirmation)
            }
        )
    }
    if (state.showLogsDialog) {
        LogsDialog(
            state = state,
            onDismiss = { onEvent(UsersListEvent.HideLogsDialog) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search username or email") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search"
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search"
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.LightGray,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
        )
    )
}


@Composable
fun UsersList(
    users: List<UserEntity>,
    onUserClick: (UserEntity) -> Unit,
    onActivate: (UserEntity) -> Unit,
    onDeactivate: (UserEntity) -> Unit,
    onDelete: (UserEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(users) { user ->
            UserCard(
                user = user,
                onClick = { onUserClick(user) },
                onActivate = { onActivate(user) },
                onDeactivate = { onDeactivate(user) },
                onDelete = { onDelete(user) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserCard(
    user: UserEntity,
    onClick: () -> Unit,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User Avatar or Icon
//            Card(
//                shape = RoundedCornerShape(50),
//                colors = CardDefaults.cardColors(
//                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
//                ),
//                modifier = Modifier.size(64.dp)
//            ) {
//                Box(
//                    modifier = Modifier.fillMaxSize(),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.Person,
//                        contentDescription = "User",
//                        tint = MaterialTheme.colorScheme.primary,
//                        modifier = Modifier.size(36.dp)
//                    )
//                }
//            }
//
//            Spacer(modifier = Modifier.width(16.dp))

            // User Details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = user.username,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 5.dp),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 5.dp),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Role Badge
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.surfaceTint
                    ) {
                        Text(
                            text = user.role,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Status Badge
                    Badge(
                        containerColor = if (user.isActive) Color(0xFF4CAF50) else Color(0xFFE57373),
                        contentColor = Color.White
                    ) {
                        Text(
                            text = if (user.isActive) "Active" else "Inactive",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // User Actions
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxHeight()
            ) {
                // Edit action is handled by clicking on the card

                // Activate/Deactivate button
                IconButton(
                    onClick = { if (user.isActive) onDeactivate() else onActivate() },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (user.isActive) Color(0xFFE57373) else Color(0xFF4CAF50),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (user.isActive) Icons.Default.Block else Icons.Default.CheckCircle,
                        contentDescription = if (user.isActive) "Deactivate User" else "Activate User",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Delete button
                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xff016A5F),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.delete),
                        contentDescription = "Delete User",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyUsersMessage(onAddUser: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PeopleAlt,
            contentDescription = "No Users",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No users found",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add users to manage access to your application",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAddUser,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = "Add User"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add New User")
        }
    }
}

@Composable
fun NoUsersFoundMessage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = "No Results",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No matching users found",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Try adjusting your search or filters",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AccessDeniedMessage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Access Denied",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Access Denied",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "You don't have permission to manage users",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleFilterDropdown(
    roles:List<String>,
    selectedRole: String?,
    onRoleSelected: (String?) -> Unit,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val label = selectedRole ?: "All Roles"

    Box(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = isExpanded,
            onExpandedChange = onExpandedChange
        ) {
            OutlinedTextField(
                value = label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Role") },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.LightGray,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                )
            )

            ExposedDropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                // All Roles option
                DropdownMenuItem(
                    text = { Text("All Roles") },
                    onClick = {
                        onRoleSelected(null)
                        onExpandedChange(false)
                    }
                )

                // Add role options
                roles.forEach { role ->
                    DropdownMenuItem(
                        text = { Text(role) },
                        onClick = {
                            onRoleSelected(role)
                            onExpandedChange(false)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusFilterDropdown(
    selectedStatus: Boolean?,
    onStatusSelected: (Boolean?) -> Unit,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val label = when (selectedStatus) {
        true -> "Active"
        false -> "Inactive"
        null -> "All Status"
    }

    Box(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = isExpanded,
            onExpandedChange = onExpandedChange
        ) {
            OutlinedTextField(
                value = label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Status") },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.LightGray,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                )
            )

            ExposedDropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                // All Status option
                DropdownMenuItem(
                    text = { Text("All Status") },
                    onClick = {
                        onStatusSelected(null)
                        onExpandedChange(false)
                    }
                )

                // Active/Inactive options
                DropdownMenuItem(
                    text = { Text("Active") },
                    onClick = {
                        onStatusSelected(true)
                        onExpandedChange(false)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Inactive") },
                    onClick = {
                        onStatusSelected(false)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserDialog(
    state: UsersListState,
    onEvent: (UsersListEvent) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Add New User",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Username field
                OutlinedTextField(
                    value = state.username,
                    onValueChange = { onEvent(UsersListEvent.UpdateUsername(it)) },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Email field with error display
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { onEvent(UsersListEvent.UpdateEmail(it)) },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = state.emailError != null,
                    supportingText = {
                        state.emailError?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Role selection
                Box(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = state.isRoleDropdownExpanded,
                        onExpandedChange = { onEvent(UsersListEvent.ToggleRoleDropdown(it)) }
                    ) {
                        OutlinedTextField(
                            value = state.selectedRole ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Role") },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = state.isRoleDropdownExpanded)
                            }
                        )

                        ExposedDropdownMenu(
                            expanded = state.isRoleDropdownExpanded,
                            onDismissRequest = { onEvent(UsersListEvent.ToggleRoleDropdown(false)) }
                        ) {
                            state.rolesList.forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(role) },
                                    onClick = {
                                        onEvent(UsersListEvent.SelectRole(role))
                                        onEvent(UsersListEvent.ToggleRoleDropdown(false))
                                    }
                                )
                            }
                        }
                    }
                }

                // Permissions section
                AnimatedVisibility(visible = state.showPermissions) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Permissions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // List of permissions with checkboxes
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Permission.entries.forEach { permission ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isSelected = state.selectedPermissions.contains(permission)
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { isChecked ->
                                            val updatedPermissions = if (isChecked) {
                                                state.selectedPermissions + permission
                                            } else {
                                                state.selectedPermissions - permission
                                            }
                                            onEvent(
                                                UsersListEvent.UpdatePermissions(
                                                    updatedPermissions
                                                )
                                            )
                                        }
                                    )
                                    Text(
                                        text = permission.name.replace("_", " "),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            state.selectedRole?.let { role ->
                                onEvent(
                                    UsersListEvent.AddUser(
                                        username = state.username,
                                        email = state.email,
                                        role = role,
                                        permissions = state.selectedPermissions
                                    )
                                )
                            }
                        },
                        enabled = !state.isAddUserButtonLoading &&
                                state.username.isNotBlank() &&
                                state.email.isNotBlank() &&
                                state.selectedRole != null &&
                                state.emailError == null
                    ) {
                        if (state.isAddUserButtonLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Add User")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserDialog(
    state: UsersListState,
    onEvent: (UsersListEvent) -> Unit,
    onDismiss: () -> Unit
) {
    val user = state.userToEdit ?: return

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Edit User",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Username field (editable)
                OutlinedTextField(
                    value = state.editUsername,
                    onValueChange = { onEvent(UsersListEvent.UpdateEditUsername(it)) },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Email field (disabled)
                OutlinedTextField(
                    value = user.email,
                    onValueChange = { /* Read-only */ },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Role selection (editable)
                Box(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = state.isRoleDropdownExpanded,
                        onExpandedChange = { onEvent(UsersListEvent.ToggleEditRoleDropdown(it)) }
                    ) {
                        OutlinedTextField(
                            value = state.editRole ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Role") },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = state.isRoleDropdownExpanded)
                            }
                        )

                        ExposedDropdownMenu(
                            expanded = state.isRoleDropdownExpanded,
                            onDismissRequest = { onEvent(UsersListEvent.ToggleEditRoleDropdown(false)) }
                        ) {
                            state.rolesList.forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(role) },
                                    onClick = {
                                        onEvent(UsersListEvent.UpdateEditRole(role))
                                        onEvent(UsersListEvent.ToggleEditRoleDropdown(false))
                                    }
                                )
                            }
                        }
                    }
                }

                // Status indicator (non-editable)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Status:",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(
                        containerColor = if (user.isActive) Color(0xFF4CAF50) else Color(0xFFE57373),
                        contentColor = Color.White
                    ) {
                        Text(
                            text = if (user.isActive) "Active" else "Inactive",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Permissions section (editable)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Permissions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // List of permissions with checkboxes
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp) // Fixed height for scrollable content
                ) {
                    LazyColumn {
                        items(Permission.entries) { permission ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isSelected = state.editPermissions.contains(permission)
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { isChecked ->
                                        val updatedPermissions = if (isChecked) {
                                            state.editPermissions + permission
                                        } else {
                                            state.editPermissions - permission
                                        }
                                        onEvent(UsersListEvent.UpdateEditPermissions(updatedPermissions))
                                    }
                                )
                                Text(
                                    text = permission.name.replace("_", " "),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onEvent(UsersListEvent.UpdateUser) },
                        enabled = !state.isUpdateUserLoading &&
                                state.editUsername.isNotBlank() &&
                                state.editRole != null
                    ) {
                        if (state.isUpdateUserLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Update User")
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun LogsDialog(
    state: UsersListState,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "System Logs",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                when {
                    state.isLogsLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    state.logsData.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No logs available")
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                        ) {
                            items(state.logsData) { log ->
                                LogCard(log)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun LogCard(log: AuditLogEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            // Different background color for detail items
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Display action type with icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (log.actionType) {
                        AuditActionType.USER_CREATED.name -> Icons.Default.PersonAdd
                        AuditActionType.USER_UPDATED.name -> Icons.Default.Edit
                        AuditActionType.USER_DELETED.name -> Icons.Default.Delete
                        AuditActionType.USER_ACTIVATED.name -> Icons.Default.CheckCircle
                        AuditActionType.USER_DEACTIVATED.name -> Icons.Default.Block
                        else -> Icons.Default.Info
                    },
                    contentDescription = log.actionType,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = log.actionType.replace("_", " "),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Target user info
            if(log.actionType!=AuditActionType.SALE_REVERSED.name){
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Target: ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = log.targetUsername,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Performed by info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "By: ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = log.performedByUsername,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Display timestamp in the bottom section
            Text(
                text = formatLogTimestamp(log.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Full description
            Text(
                text = log.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

fun formatLogTimestamp(timestamp: Long?): String {
    if (timestamp == null) return "N/A"
    val dateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}