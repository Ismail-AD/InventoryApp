package com.appdev.inventoryapp.ui.Screens.InventoryManagemnt

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.appdev.inventoryapp.R
import com.appdev.inventoryapp.ui.Reuseables.CustomLoader
import com.appdev.inventoryapp.ui.Screens.AddItem.AddInventoryItemEvent
import com.appdev.inventoryapp.ui.Screens.AddItem.AddInventoryItemState
import com.appdev.inventoryapp.ui.Screens.DisplayInventory.InventoryEvent

@Composable
fun AddInventoryItemRoot(
    viewModel: AddInventoryItemViewModel = hiltViewModel(),
    navigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    AddInventoryItemScreen(
        state = state,
        onEvent = { event ->
            when (event) {
                is AddInventoryItemEvent.NavigateBack -> navigateBack()
                else -> viewModel.handleEvent(event)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInventoryItemScreen(
    state: AddInventoryItemState,
    onEvent: (AddInventoryItemEvent) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(key1 = state.isSuccess) {
        state.isSuccess?.let {
            onEvent(AddInventoryItemEvent.NavigateBack)
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.itemId != null) "Update Inventory Item" else "Add Inventory Item",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(AddInventoryItemEvent.NavigateBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                CustomLoader()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Product name field
                TitledOutlinedTextField(
                    title = "Item Name",
                    value = state.name,
                    onValueChange = { onEvent(AddInventoryItemEvent.NameChanged(it)) },
                    placeholder = "Enter item name",
                    singleLine = true
                )

                // Price and quantity fields
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TitledOutlinedTextField(
                        title = "Cost Price",
                        value = state.costPrice,
                        onValueChange = { onEvent(AddInventoryItemEvent.CostPriceChanged(it)) },
                        placeholder = "Enter cost price",
                        singleLine = true,
                        isNumber = true,
                        modifier = Modifier.weight(1f)
                    )

                    TitledOutlinedTextField(
                        title = "Selling Price",
                        value = state.sellingPrice,
                        onValueChange = { onEvent(AddInventoryItemEvent.SellingPriceChanged(it)) },
                        placeholder = "Enter selling price",
                        singleLine = true,
                        isNumber = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                TitledOutlinedTextField(
                    title = "Quantity",
                    value = state.quantity,
                    onValueChange = { onEvent(AddInventoryItemEvent.QuantityChanged(it)) },
                    placeholder = "Enter quantity",
                    singleLine = true,
                    isNumber = true
                )

                Column {
                    Text(
                        text = "Category",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            ExposedDropdownMenuBox(
                                expanded = state.categoryDropdownExpanded,
                                onExpandedChange = {
                                    onEvent(AddInventoryItemEvent.ToggleCategoryDropdown)
                                }
                            ) {
                                OutlinedTextField(
                                    value = state.category.ifBlank { "Select Category" },
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = Color.LightGray,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        cursorColor = MaterialTheme.colorScheme.primary,
                                    ),
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = state.categoryDropdownExpanded
                                        )
                                    },
                                    placeholder = {
                                        Text(
                                            text = "Select Category",
                                            color = Color.Gray
                                        )
                                    }
                                )

                                ExposedDropdownMenu(
                                    expanded = state.categoryDropdownExpanded,
                                    onDismissRequest = {
                                        onEvent(AddInventoryItemEvent.ToggleCategoryDropdown)
                                    }
                                ) {
                                    state.categories.forEach { category ->
                                        DropdownMenuItem(
                                            text = { Text(category) },
                                            onClick = {
                                                onEvent(
                                                    AddInventoryItemEvent.CategoryChanged(
                                                        category
                                                    )
                                                )
                                                onEvent(AddInventoryItemEvent.ToggleCategoryDropdown)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Add Category Button
                        IconButton(
                            onClick = { onEvent(AddInventoryItemEvent.ShowNewCategoryDialog) },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Category"
                            )
                        }
                    }
                }


                // SKU TextField
                TitledOutlinedTextField(
                    title = "SKU",
                    value = state.sku,
                    onValueChange = { onEvent(AddInventoryItemEvent.SKUChanged(it)) },
                    placeholder = "Enter SKU",
                    singleLine = true
                )

                // Taxes TextField
                TitledOutlinedTextField(
                    title = "Taxes (%)",
                    value = state.taxes,
                    onValueChange = { onEvent(AddInventoryItemEvent.TaxesChanged(it)) },
                    placeholder = "Enter taxes",
                    singleLine = true,
                    isNumber = true
                )


                // Image section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Product Images",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Show existing images
                        state.imageUris.forEachIndexed { index, uri ->
                            ImageSelector(
                                uri = uri,
                                index = index,
                                onImageSelect = { selectedUri, idx ->
                                    onEvent(AddInventoryItemEvent.ImageSelected(selectedUri, idx))
                                },
                                onImageRemove = { idx ->
                                    onEvent(AddInventoryItemEvent.RemoveImage(idx))
                                }
                            )
                        }

                        // Add one more image selector if we have less than 5 images
                        if (state.imageUris.size < 5) {
                            ImageSelector(
                                uri = null,
                                index = state.imageUris.size,
                                onImageSelect = { selectedUri, idx ->
                                    onEvent(AddInventoryItemEvent.ImageSelected(selectedUri, idx))
                                }
                            )
                        }
                    }
                }

                // Submit button
                Button(
                    onClick = {
                        val imageBytesList: List<ByteArray?> =
                            state.imageUris.mapNotNull { uri ->
                                try {
                                    context.contentResolver.openInputStream(uri)?.use {
                                        it.readBytes()
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            }

                        onEvent(AddInventoryItemEvent.SubmitItem(imageBytesList))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = if (state.itemId != null) "Update Item" else "Add Item",
                        modifier = Modifier.padding(vertical = 8.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (state.newCategoryDialogVisible) {
                    AlertDialog(
                        onDismissRequest = {
                            onEvent(AddInventoryItemEvent.DismissNewCategoryDialog)
                        },
                        title = { Text("Add New Category") },
                        text = {
                            OutlinedTextField(
                                value = state.newCategoryName,
                                onValueChange = {
                                    onEvent(AddInventoryItemEvent.NewCategoryNameChanged(it))
                                },
                                label = { Text("Category Name") },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = { onEvent(AddInventoryItemEvent.SaveNewCategory) },
                                enabled = state.newCategoryName.trim().isNotBlank()
                            ) {
                                Text("Save")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { onEvent(AddInventoryItemEvent.DismissNewCategoryDialog) }
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }
                if (state.showConfirmationModal) {
                    AlertDialog(
                        onDismissRequest = { onEvent(AddInventoryItemEvent.DismissConfirmationModal) },
                        title = { Text("Confirm Item Submission") },
                        text = {
                            Column {
                                Text("Are you sure you want to add/update this inventory item?")
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Checkbox(
                                        checked = state.dontShowConfirmationAgain,
                                        onCheckedChange = {
                                            onEvent(
                                                AddInventoryItemEvent.SetDontShowConfirmationAgain(
                                                    it
                                                )
                                            )
                                        }
                                    )
                                    Text("Don't show this again")
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { onEvent(AddInventoryItemEvent.ConfirmSubmit) }) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { onEvent(AddInventoryItemEvent.DismissConfirmationModal) }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

            }

            state.errorMessage?.let { errorMessage ->
                ShowSnack(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter), errorMessage
                ) {
                    onEvent(AddInventoryItemEvent.DismissError)
                }
            }
            state.isSuccess?.let { message ->
                ShowSnack(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter), message
                ) {
                    onEvent(AddInventoryItemEvent.DismissError)
                }
            }
            state.newCategoryAddedMessage?.let { message ->
                ShowSnack(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter), message
                ) {
                    onEvent(AddInventoryItemEvent.DismissError)
                }
            }

        }
    }
}

@Composable
fun ShowSnack(modifier: Modifier, message: String, action: () -> Unit) {
    Snackbar(
        modifier = modifier,
        action = {
            TextButton(onClick = {
                action()
            }) {
                Text("Dismiss", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        containerColor = MaterialTheme.colorScheme.error
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.onError)
    }
}

@Composable
fun TitledOutlinedTextField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    isNumber: Boolean = false,
    height: Int = 56
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(height.dp),
            singleLine = singleLine,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.LightGray,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            placeholder = { Text(placeholder, color = Color.Gray) },
            keyboardOptions = if (isNumber) KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number) else KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Text
            )
        )
    }
}

@Composable
fun ImageSelector(
    uri: Uri?,
    index: Int,
    onImageSelect: (Uri, Int) -> Unit,
    onImageRemove: (Int) -> Unit = {}
) {

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { selectedUri: Uri? ->
        selectedUri?.let {
            onImageSelect(it, index)
        }
    }

    Box(modifier = Modifier.size(110.dp)) {
        Card(
            onClick = { launcher.launch("image/*") },
            modifier = Modifier.fillMaxSize(),
            border = BorderStroke(1.dp, Color.LightGray),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Product Image",
                    placeholder = painterResource(R.drawable.placeholderitem),
                    error = painterResource(R.drawable.placeholderitem),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (uri != null) 0.dp else 35.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Only show remove button if there is an image
        if (uri != null) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .align(Alignment.TopEnd)
                    .clickable { onImageRemove(index) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove Image",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}