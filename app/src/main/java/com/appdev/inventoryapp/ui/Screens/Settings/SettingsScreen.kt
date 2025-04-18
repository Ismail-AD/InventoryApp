package com.appdev.inventoryapp.ui.Screens.Settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    navigateToAuth: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Permission launcher for notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            viewModel.updateNotificationPermissionStatus(isGranted)
            if (isGranted) {
                viewModel.handleEvent(SettingsEvent.ToggleLowStockNotification)
            } else {
                viewModel.handleEvent(SettingsEvent.ShowNotificationGuide)
            }
        }
    )

    LaunchedEffect(Unit) {
        // Check notification permission on initial load
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            viewModel.updateNotificationPermissionStatus(hasPermission)
        } else {
            // For older Android versions, permission is granted at install time
            viewModel.updateNotificationPermissionStatus(true)
        }
    }

    LaunchedEffect(key1 = state.showSuccessMessage) {
        if (state.showSuccessMessage) {
            Toast.makeText(context, state.successMessage, Toast.LENGTH_SHORT).show()
            viewModel.handleEvent(SettingsEvent.DismissSuccessMessage)
        }
    }
    LaunchedEffect(key1 = state.isLogoutConfirmed) {
        if (state.isLogoutConfirmed) {
            navigateToAuth()
        }
    }


    SettingsScreenContent(
        state = state,
        onEvent = viewModel::handleEvent,
        requestNotificationPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.updateNotificationPermissionStatus(true)
                viewModel.handleEvent(SettingsEvent.ToggleLowStockNotification)
            }
        }
    )
    if (state.showNotificationGuide) {
        NotificationPermissionGuideDialog(
            onDismiss = { viewModel.handleEvent(SettingsEvent.DismissNotificationGuide) },
            onGoToSettings = {
                viewModel.handleEvent(SettingsEvent.DismissNotificationGuide)
                openAppSettings(context)
            }
        )
    }

    // Shop Name Edit Dialog
    if (state.isEditingShopName) {
        ShopNameEditDialog(
            shopName = state.newShopName,
            error = state.shopNameError,
            onShopNameChange = { viewModel.handleEvent(SettingsEvent.ShopNameChanged(it)) },
            onDismiss = { viewModel.handleEvent(SettingsEvent.CloseShopNameDialog) },
            onConfirm = { viewModel.handleEvent(SettingsEvent.UpdateShopName) },
            isLoading = state.isLoading
        )
    }

    // User Name Edit Dialog
    if (state.isEditingUserName) {
        UserNameEditDialog(
            userName = state.newUserName,
            error = state.userNameError,
            onUserNameChange = { viewModel.handleEvent(SettingsEvent.UserNameChanged(it)) },
            onDismiss = { viewModel.handleEvent(SettingsEvent.CloseUserNameDialog) },
            onConfirm = { viewModel.handleEvent(SettingsEvent.UpdateUserName) },
            isLoading = state.isLoading
        )
    }
    if (state.showLogoutConfirmDialog) {
        LogoutConfirmDialog(
            onDismiss = { viewModel.handleEvent(SettingsEvent.HideLogoutConfirmDialog) },
            onConfirm = { viewModel.handleEvent(SettingsEvent.Logout) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    requestNotificationPermission: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
            )
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
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // User Profile Section
                    Text(
                        text = "Account Information",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    // Shop Name with Edit Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Shop Name",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = state.shopName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(onClick = { onEvent(SettingsEvent.OpenShopNameDialog) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Shop Name",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Divider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        thickness = 0.5.dp
                    )

                    // User Name with Edit Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "User Name",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = state.userName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(onClick = { onEvent(SettingsEvent.OpenUserNameDialog) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit User Name",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Divider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        thickness = 0.5.dp
                    )

                    // Email (Not editable)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Email",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = state.email,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Divider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Notifications Section
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    // Low Stock Notification Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Low Stock Alerts",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Get notified when inventory is below threshold",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = state.isLowStockNotificationEnabled,
                            onCheckedChange = {
                                if (state.hasNotificationPermission) {
                                    onEvent(SettingsEvent.ToggleLowStockNotification)
                                } else {
                                    requestNotificationPermission()
                                }
                            }
                        )
                    }

                    Divider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        thickness = 0.5.dp
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    // Logout Button
                    Button(
                        onClick = { onEvent(SettingsEvent.ShowLogoutConfirmDialog) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Logout",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Add some padding at the bottom
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Error Snackbar
            state.errorMessage?.let { errorMessage ->
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { onEvent(SettingsEvent.DismissError) }) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.onError)
                }
            }
        }
    }
}

@Composable
fun ShopNameEditDialog(
    shopName: String,
    error: String?,
    onShopNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Shop Name") },
        text = {
            Column {
                OutlinedTextField(
                    value = shopName,
                    onValueChange = onShopNameChange,
                    label = { Text("Shop Name") },
                    isError = error != null,
                    supportingText = {
                        if (error != null) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text("Maximum 20 characters, no spaces or special characters")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading && error == null && shopName.isNotEmpty()
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun UserNameEditDialog(
    userName: String,
    error: String?,
    onUserNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit User Name") },
        text = {
            Column {
                OutlinedTextField(
                    value = userName,
                    onValueChange = onUserNameChange,
                    label = { Text("User Name") },
                    isError = error != null,
                    supportingText = {
                        if (error != null) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text("Enter your full name")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading && error == null && userName.isNotEmpty()
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NotificationPermissionGuideDialog(
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enable Notifications") },
        text = {
            Column {
                Text("To receive low stock alerts, please enable notifications for this app.")
                Spacer(modifier = Modifier.height(8.dp))
                Text("You can enable notifications by following these steps:")
                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text("1. Open your device Settings")
                    Text("2. Select Apps or Applications")
                    Text("3. Find and tap on Inventory App")
                    Text("4. Select Permissions")
                    Text("5. Enable Notifications")
                }
            }
        },
        confirmButton = {
            Button(onClick = onGoToSettings) {
                Text("Go to Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}

fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

@Composable
fun LogoutConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Logout") },
        text = { Text("Are you sure you want to log out from the application?") },
        confirmButton = {
            Button(
                onClick = onConfirm
            ) {
                Text("Yes, Logout")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}