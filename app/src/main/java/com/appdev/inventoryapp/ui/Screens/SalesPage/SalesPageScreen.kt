package com.appdev.inventoryapp.ui.Screens.SalesPage


import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.appdev.inventoryapp.domain.model.InventoryItem
import com.appdev.inventoryapp.ui.Screens.Inventory.CategoryDropdown
import com.appdev.inventoryapp.ui.Screens.Inventory.SortDropdown

@Composable
fun SalesPageScreen(
    viewModel: SalesPageViewModel = hiltViewModel(),
    navigateToProductDetail: (InventoryItem) -> Unit,
    navigateToCartSummary: () -> Unit,
    onBackPressed: () -> Unit

) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.handleEvent(SalesPageEvent.LoadInventory)
    }
    Log.d("CHKINF","${state.cartItems}")
    SalesPageScreenContent(
        state = state,
        onEvent = viewModel::handleEvent,
        navigateToCartSummary = navigateToCartSummary,
        navigateToProductDetail = navigateToProductDetail,onBackPressed

    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesPageScreenContent(
    state: SalesPageState,
    onEvent: (SalesPageEvent) -> Unit,
    navigateToCartSummary: () -> Unit,
    navigateToProductDetail: (InventoryItem) -> Unit,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales Entry") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if(state.cartItems.isNotEmpty()){
                CartSummaryButton(
                    cartItemCount = state.cartItems.size,
                    onClick = navigateToCartSummary
                )
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
                    EmptySalesEntryMessage()
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        SearchBarWithQrScanner(
                            query = state.searchQuery,
                            onQueryChange = { onEvent(SalesPageEvent.SearchQueryChanged(it)) },
                            onQrScanClick = { onEvent(SalesPageEvent.ToggleQrScanner) }
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Sort order dropdown
                            SortDropdown(
                                currentSortOrder = state.currentSortOrder,
                                isExpanded = state.isSortMenuExpanded,
                                onExpandChange = {
                                    onEvent(SalesPageEvent.ToggleSortMenu(it))
                                },
                                onSortSelected = {
                                    onEvent(SalesPageEvent.UpdateSortOrder(it))
                                },
                                modifier = Modifier.weight(1f)
                            )

                            // Category dropdown
                            if (state.categoryIdToNameMap.isNotEmpty()) {
                                CategoryDropdown(
                                    selectedCategoryName = state.selectedCategoryName,
                                    categoryIdToNameMap = state.categoryIdToNameMap,
                                    isExpanded = state.isCategoryMenuExpanded,
                                    onExpandChange = {
                                        onEvent(SalesPageEvent.ToggleCategoryMenu(it))
                                    },
                                    onCategorySelected = {
                                        onEvent(SalesPageEvent.FilterByCategory(it))
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Display no matches message or item list
                        if (state.filteredItems.isEmpty() && (state.searchQuery.isNotEmpty() || state.selectedCategoryName != null)) {
                            NoMatchesFoundMessage(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        } else {
                            SalesItemsList(
                                context = context,
                                items = state.filteredItems, // Make sure this is state.filteredItems, not state.inventoryItems
                                onItemClick = { navigateToProductDetail(it) },
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
                        TextButton(onClick = { onEvent(SalesPageEvent.DismissError) }) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.onError)
                }
            }

            // Show success message when item added to cart
//            if (state.showSuccessMessage) {
//                Snackbar(
//                    modifier = Modifier
//                        .padding(16.dp)
//                        .align(Alignment.BottomCenter),
//                    action = {
//                        TextButton(onClick = { onEvent(SalesPageEvent.DismissSuccessMessage) }) {
//                            Text("Dismiss", color = MaterialTheme.colorScheme.onPrimary)
//                        }
//                    },
//                    containerColor = MaterialTheme.colorScheme.primary
//                ) {
//                    Text(
//                        text = "Item added to cart",
//                        color = MaterialTheme.colorScheme.onPrimary
//                    )
//                }
//            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarWithQrScanner(
    query: String,
    onQueryChange: (String) -> Unit,
    onQrScanClick: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search products") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search"
            )
        },
        trailingIcon = {
            Row {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
                IconButton(onClick = onQrScanClick) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan QR Code"
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
fun SalesItemsList(
    context: Context,
    items: List<InventoryItem>,
    onItemClick: (InventoryItem) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            SalesItemCard(
                item = item,
                context = context,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesItemCard(
    item: InventoryItem,
    context: Context,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(140.dp),
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

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Price: $${item.selling_price}",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = "Inventory Icon",
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Qty: ${item.quantity}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
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
                    contentDescription = "Product Image",
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
fun CartSummaryButton(
    cartItemCount: Int,
    onClick: () -> Unit
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        icon = {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = "Cart"
            )
        },
        text = {
            Text("Cart ($cartItemCount)")
        }
    )
}

@Composable
fun EmptySalesEntryMessage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Store,
            contentDescription = "Empty Inventory",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No products available",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add products to your inventory first",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun NoMatchesFoundMessage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
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
                text = "No matching products found",
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
}


