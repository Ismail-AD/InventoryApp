package com.appdev.inventoryapp.ui.Screens.Categories

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.appdev.inventoryapp.R
import com.appdev.inventoryapp.domain.model.Category
import com.appdev.inventoryapp.ui.Screens.Settings.SettingsEvent

@Composable
fun CategoryListScreen(
    viewModel: CategoryViewModel = hiltViewModel(),
    navigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Launch the side effect when successful message is shown
    LaunchedEffect(state.showSuccessMessage) {
        if (state.showSuccessMessage) {
            Toast.makeText(context, state.successMessage, Toast.LENGTH_SHORT).show()
            viewModel.handleEvent(CategoryEvent.DismissSuccessMessage)
        }
    }

    CategoryScreenContent(
        state = state,
        onEvent = viewModel::handleEvent,
        navigateBack = { navigateBack() }
    )

    // Show edit dialog if needed
    if (state.isEditingCategory) {
        CategoryEditDialog(
            categoryName = state.newCategoryName,
            error = state.categoryNameError,
            onCategoryNameChange = { viewModel.handleEvent(CategoryEvent.CategoryNameChanged(it)) },
            onDismiss = { viewModel.handleEvent(CategoryEvent.CloseEditCategoryDialog) },
            onConfirm = { viewModel.handleEvent(CategoryEvent.UpdateCategory) },
            isLoading = state.isLoading
        )
    }

    if (state.isAddingCategory) {
        CategoryAddDialog(
            categoryName = state.newCategoryName,
            error = state.categoryNameError,
            onCategoryNameChange = { viewModel.handleEvent(CategoryEvent.CategoryNameChanged(it)) },
            onDismiss = { viewModel.handleEvent(CategoryEvent.CloseAddCategoryDialog) },
            onConfirm = { viewModel.handleEvent(CategoryEvent.AddCategory) },
            isLoading = state.isLoading
        )
    }

    // Show delete confirmation dialog if needed
    if (state.showDeleteConfirmDialog) {
        DeleteCategoryConfirmDialog(
            categoryName = state.categoryToDelete?.categoryName ?: "",
            onDismiss = { viewModel.handleEvent(CategoryEvent.CloseDeleteConfirmDialog) },
            onConfirm = { viewModel.handleEvent(CategoryEvent.DeleteCategory) }
        )
    }

    // Show delete error dialog if needed
    if (state.showDeleteErrorDialog) {
        DeleteCategoryErrorDialog(
            message = state.deleteErrorMessage,
            onDismiss = { viewModel.handleEvent(CategoryEvent.DismissDeleteErrorDialog) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreenContent(
    state: CategoryState,
    onEvent: (CategoryEvent) -> Unit,
    navigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
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
            } else if (state.categories.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No categories found",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { /* Add new category */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xff016A5F)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Category")
                    }
                }
            } else {
                // Categories list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(state.categories) { category ->
                        CategoryItem(
                            category = category,
                            onUpdate = { onEvent(CategoryEvent.OpenEditCategoryDialog(it)) },
                            onDelete = { onEvent(CategoryEvent.OpenDeleteConfirmDialog(it)) }
                        )
                    }
                    // Add some space at the bottom
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }

                // FAB for adding a new category
                FloatingActionButton(
                    onClick = { onEvent(CategoryEvent.OpenAddCategoryDialog) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = Color(0xff016A5F),
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Category"
                    )
                }
            }

            // Error Snackbar
            state.errorMessage?.let { errorMessage ->
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { onEvent(CategoryEvent.DismissError) }) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.onError)
                }
            }

            // Success Snackbar
            if (state.showSuccessMessage) {
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { onEvent(CategoryEvent.DismissSuccessMessage) }) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                    containerColor = Color(0xff016A5F)
                ) {
                    Text(text = state.successMessage, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun CategoryItem(
    category: Category,
    onUpdate: (Category) -> Unit,
    onDelete: (Category) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Category name
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = category.categoryName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            }

            // Action buttons
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Update button
                Card(
                    onClick = { onUpdate(category) },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xff016A5F))
                ) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        Icon(
                            painter = painterResource(id = R.drawable.edit_text_new_),
                            contentDescription = "Edit",
                            tint = Color.White,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Delete button
                Card(
                    onClick = { onDelete(category) },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xff016A5F))
                ) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        Icon(
                            painter = painterResource(id = R.drawable.delete),
                            contentDescription = "Delete",
                            tint = Color.White,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryEditDialog(
    categoryName: String,
    error: String?,
    onCategoryNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Category") },
        text = {
            Column {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = onCategoryNameChange,
                    label = { Text("Category Name") },
                    isError = error != null,
                    supportingText = {
                        if (error != null) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error
                            )
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
                enabled = !isLoading && error == null && categoryName.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xff016A5F)
                )
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
fun DeleteCategoryConfirmDialog(
    categoryName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Delete") },
        text = {
            Text("Are you sure you want to delete the category '$categoryName'? This action cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
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
fun DeleteCategoryErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cannot Delete Category") },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xff016A5F)
                )
            ) {
                Text("OK")
            }
        }
    )
}

@Composable
fun CategoryAddDialog(
    categoryName: String,
    error: String?,
    onCategoryNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Category") },
        text = {
            Column {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = onCategoryNameChange,
                    label = { Text("Category Name") },
                    isError = error != null,
                    supportingText = {
                        if (error != null) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error
                            )
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
                enabled = !isLoading && error == null && categoryName.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xff016A5F)
                )
            ) {
                Text("Add Category")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}