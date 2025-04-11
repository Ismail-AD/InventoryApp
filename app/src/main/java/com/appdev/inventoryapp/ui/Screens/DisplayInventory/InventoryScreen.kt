package com.appdev.inventoryapp.ui.Screens.Inventory

import android.content.Context
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.appdev.inventoryapp.R
import com.appdev.inventoryapp.Utils.SortOrder
import com.appdev.inventoryapp.domain.model.InventoryItem
import com.appdev.inventoryapp.ui.Reuseables.DeleteConfirmationDialog
import com.appdev.inventoryapp.ui.Screens.DisplayInventory.InventoryEvent
import com.appdev.inventoryapp.ui.Screens.DisplayInventory.InventoryState
import com.appdev.inventoryapp.ui.Screens.DisplayInventory.InventoryViewModel
import kotlin.math.absoluteValue

@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel = hiltViewModel(),
    navigateToAddItem: () -> Unit,
    navigateToItemDetail: (InventoryItem) -> Unit,
    navigateToUpdateItem: (InventoryItem) -> Unit,
) {
//    LaunchedEffect(Unit) {
//        viewModel.handleEvent(InventoryEvent.LoadInventory)
//    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    InventoryScreenContent(
        state = state,
        onEvent = viewModel::handleEvent,
        navigateToAddItem = navigateToAddItem,
        navigateToItemDetail = navigateToItemDetail,
        navigateToUpdateItem = navigateToUpdateItem
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreenContent(
    state: InventoryState,
    onEvent: (InventoryEvent) -> Unit,
    navigateToAddItem: () -> Unit,
    navigateToItemDetail: (InventoryItem) -> Unit,
    navigateToUpdateItem: (InventoryItem) -> Unit,
) {
    val context = LocalContext.current
    Scaffold(
        floatingActionButton = {
            if (state.inventoryItems.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { navigateToAddItem() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Item",
                        tint = MaterialTheme.colorScheme.surface
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
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                state.inventoryItems.isEmpty() && !state.isLoading -> {
                    EmptyInventoryMessage(onAddNewItem = navigateToAddItem)
                }


                else -> {
                    DeleteConfirmationDialog(
                        title = "Delete Product",
                        show = state.showDeleteConfirmation,
                        itemName = state.itemToDelete?.name ?: "",
                        onConfirm = { onEvent(InventoryEvent.ConfirmDelete) },
                        onDismiss = { onEvent(InventoryEvent.HideDeleteConfirmation) }
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        SearchBar(
                            query = state.searchQuery,
                            onQueryChange = { onEvent(InventoryEvent.SearchQueryChanged(it)) }
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Sort order dropdown with fixed layout
                            SortDropdown(
                                currentSortOrder = state.currentSortOrder,
                                isExpanded = state.isSortMenuExpanded,
                                onExpandChange = {
                                    onEvent(InventoryEvent.ToggleSortMenu(it))
                                },
                                onSortSelected = {
                                    onEvent(InventoryEvent.UpdateSortOrder(it))
                                },
                                modifier = Modifier.weight(1f)
                            )

                            // Category dropdown with fixed layout
                            if (state.categories.isNotEmpty()) {
                                CategoryDropdown(
                                    selectedCategory = state.selectedCategory,
                                    categories = state.categories,
                                    isExpanded = state.isCategoryMenuExpanded,
                                    onExpandChange = {
                                        onEvent(InventoryEvent.ToggleCategoryMenu(it))
                                    },
                                    onCategorySelected = {
                                        onEvent(InventoryEvent.FilterByCategory(it))
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }


                        Spacer(modifier = Modifier.height(16.dp))

                        // Display no matches message or item list based on filtered items
                        if (state.filteredItems.isEmpty() && (state.searchQuery.isNotEmpty() || state.selectedCategory != null)) {
                            // Simple "No matches found" message
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
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
                                        text = "No matching items found",
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Try adjusting your search or category filter",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            // Display normal list when there are items to show
                            InventoryItemsList(
                                context = context,
                                items = state.filteredItems,
                                onItemClick = { navigateToItemDetail(it) },
                                onUpdate = { navigateToUpdateItem(it) },
                                onDelete = { onEvent(InventoryEvent.ShowDeleteConfirmation(it)) }
                            )
                        }
                    }
                }
            }

            // Show error message if any
            state.errorMessage?.let { errorMessage ->
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { onEvent(InventoryEvent.DismissError) }) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.onError)
                }
            }
            if (state.showDeleteSuccessMessage) {
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { onEvent(InventoryEvent.DismissDeleteSuccess) }) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "Product deleted successfully",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search inventory items") },
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
fun InventoryItemsList(
    context: Context,
    items: List<InventoryItem>,
    onItemClick: (InventoryItem) -> Unit,
    onUpdate: (InventoryItem) -> Unit = {},
    onDelete: (InventoryItem) -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            InventoryItemCard(
                item = item, context = context,
                onUpdate = { onUpdate(item) },
                onDelete = { onDelete(item) },
                onClick = { onItemClick(item) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryItemCard(
    item: InventoryItem,
    context: Context,
    modifier: Modifier = Modifier,
    onUpdate: (InventoryItem) -> Unit = {},
    onDelete: (InventoryItem) -> Unit = {},
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(140.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(10.dp)
                    )
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .padding(start = 93.dp)
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = item.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "Cost: $${item.cost_price}",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Sell: $${item.selling_price}",
                        style = MaterialTheme.typography.bodyMedium,
                    )


                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Store,
                            contentDescription = "Shop Name Icon",
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Qty: " + item.quantity.toString(),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 5.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.End
                ) {
                    Card(
                        onClick = {
                            onUpdate(item)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xff016A5F))
                    ) {
                        Box(modifier = Modifier.padding(8.dp)) {
                            Icon(
                                painter = painterResource(id = R.drawable.edit_text_new_),
                                contentDescription = "",
                                tint = Color.White,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }
                    Card(
                        onClick = {
                            onDelete(item)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xff016A5F))
                    ) {
                        Box(modifier = Modifier.padding(8.dp)) {
                            Icon(
                                painter = painterResource(id = R.drawable.delete),
                                contentDescription = "",
                                tint = Color.White,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }
                }
            }
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .size(110.dp)
                    .offset(x = (-18).dp)
                    .align(Alignment.CenterStart)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.imageUrls.firstOrNull())
                        .crossfade(true)
                        .build(),
                    contentDescription = "Inventory Item Image",
                    placeholder = painterResource(R.drawable.placeholderitem),
                    error = painterResource(R.drawable.placeholderitem),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
fun EmptyInventoryMessage(onAddNewItem: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Inventory2,
            contentDescription = "Empty Inventory",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your inventory is empty",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add items to start managing your inventory",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAddNewItem,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add New Item")
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortDropdown(
    currentSortOrder: SortOrder,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onSortSelected: (SortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    val sortLabel = when (currentSortOrder) {
        SortOrder.NEWEST_FIRST -> "Newest First"
        SortOrder.OLDEST_FIRST -> "Oldest First"
        SortOrder.QUANTITY_LOW_TO_HIGH -> "Qty: Low to High"
        SortOrder.QUANTITY_HIGH_TO_LOW -> "Qty: High to Low"
        SortOrder.PRICE_LOW_TO_HIGH -> "Price: Low to High"
        SortOrder.PRICE_HIGH_TO_LOW -> "Price: High to Low"
    }

    Box(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = isExpanded,
            onExpandedChange = { onExpandChange(it) }
        ) {
            OutlinedTextField(
                value = sortLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Sort By") },
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
                onDismissRequest = { onExpandChange(false) }
            ) {
                SortOrder.entries.forEach { sortOrder ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                when (sortOrder) {
                                    SortOrder.NEWEST_FIRST -> "Newest First"
                                    SortOrder.OLDEST_FIRST -> "Oldest First"
                                    SortOrder.QUANTITY_LOW_TO_HIGH -> "Quantity: Low to High"
                                    SortOrder.QUANTITY_HIGH_TO_LOW -> "Quantity: High to Low"
                                    SortOrder.PRICE_LOW_TO_HIGH -> "Price: Low to High"
                                    SortOrder.PRICE_HIGH_TO_LOW -> "Price: High to Low"
                                }
                            )
                        },
                        onClick = {
                            onSortSelected(sortOrder)
                            onExpandChange(false)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    selectedCategory: String?,
    categories: List<String>,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = isExpanded,
            onExpandedChange = { onExpandChange(it) }
        ) {
            OutlinedTextField(
                value = selectedCategory ?: "All Categories",
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
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
                onDismissRequest = { onExpandChange(false) }
            ) {
                // Add "All Categories" option
                DropdownMenuItem(
                    text = { Text("All Categories") },
                    onClick = {
                        onCategorySelected(null)
                        onExpandChange(false)
                    }
                )

                // Dynamically add category options
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            onCategorySelected(category)
                            onExpandChange(false)
                        }
                    )
                }
            }
        }
    }
}