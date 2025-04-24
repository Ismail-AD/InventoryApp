package com.appdev.inventoryapp.ui.Screens.CartSummary

import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.appdev.inventoryapp.R
import com.appdev.inventoryapp.domain.model.InventoryItem
import com.appdev.inventoryapp.domain.model.SaleRecordItem
import com.appdev.inventoryapp.ui.Reuseables.CustomLoader
import com.appdev.inventoryapp.ui.Screens.SalesPage.SalesPageEvent
import com.appdev.inventoryapp.ui.Screens.SalesPage.SalesPageState
import com.appdev.inventoryapp.ui.Screens.SalesPage.SalesPageViewModel
import java.text.NumberFormat
import java.util.*

@Composable
fun CartSummaryScreen(
    viewModel: SalesPageViewModel = hiltViewModel(),
    navigateBack: () -> Unit,
    navigateToSalesHistory: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CartSummaryScreenContent(
        state = state,
        inventoryItems = state.cartItems,
        onEvent = viewModel::handleEvent,
        navigateBack = navigateBack,
        navigateToSalesHistory = navigateToSalesHistory
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartSummaryScreenContent(
    state: SalesPageState,
    inventoryItems: List<SaleRecordItem>,
    onEvent: (SalesPageEvent) -> Unit,
    navigateBack: () -> Unit,
    navigateToSalesHistory: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            // Navigate after showing the toast
            navigateToSalesHistory()
            // Clear the success message
            onEvent(SalesPageEvent.DismissError)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Items") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go back"
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
            when {
                state.isLoading -> {
                    CustomLoader()
                }

                inventoryItems.isEmpty() -> {
                    EmptyCartMessage(
                        onBackToShopping = navigateBack
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Items list
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(inventoryItems) { item ->
                                state.inventoryItems.find { it.id == item.productId }?.let { matchedItem ->
                                    InventoryItemCard(
                                        categoryName = state.categoryIdToNameMap[matchedItem.category_id]?:"",
                                        item = matchedItem,
                                        saleRecordItem = item,
                                        context = context,
                                        onRemove = { onEvent(SalesPageEvent.RemoveCartItem(item.productId)) }
                                    )
                                }
                            }
                        }


                        // Action button
                        Button(
                            onClick = {
                                onEvent(SalesPageEvent.ShowConfirmationDialog)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCartCheckout,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Update Inventory",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
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

            if (state.showConfirmationDialog) {
                ConfirmationDialog(
                    onDismiss = { onEvent(SalesPageEvent.DismissConfirmationDialog) },
                    onConfirm = { onEvent(SalesPageEvent.ConfirmSaleAfterDialog) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryItemCard(
    categoryName:String,
    item: InventoryItem,
    saleRecordItem: SaleRecordItem,
    context: Context,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = NumberFormat.getCurrencyInstance(Locale.US)

    // Calculate pricing details
    val originalPricePerUnit = item.selling_price
    val quantity = saleRecordItem.quantity

    // Handle discount based on percentage or fixed amount
    val discountAmount = saleRecordItem.discountAmount.toDouble()

    val priceAfterDiscountPerUnit = originalPricePerUnit - discountAmount
    val subtotalBeforeDiscount = originalPricePerUnit * quantity
    val subtotalAfterDiscount = priceAfterDiscountPerUnit * quantity

    val taxRate = item.taxes / 100  // Convert tax percentage to decimal
    val taxAmount = priceAfterDiscountPerUnit * taxRate * quantity
    val finalPrice = subtotalAfterDiscount + taxAmount

    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Top row with image and basic details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product Image - REDUCED SIZE
                Card(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(70.dp)
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
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Product Details
                Column(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Basic item details
                    Text(
                        text = "Category: $categoryName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Remove button
                Surface(
                    onClick = onRemove,
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove item",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))

            // Price breakdown details - MODIFIED TO KEEP PRICES ON ONE LINE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left column: Unit price (with discount inline)
                Column(modifier = Modifier.weight(1f)) {
                    // Single line for unit price with or without discount
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Unit: ",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (discountAmount > 0 ) {
                            Text(
                                text = formatter.format(originalPricePerUnit) + " ",
                                style = MaterialTheme.typography.bodySmall,
                                textDecoration = TextDecoration.LineThrough,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatter.format(priceAfterDiscountPerUnit),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = formatter.format(originalPricePerUnit),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Discount information on a separate line
                    if (discountAmount > 0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        val discountText = if (saleRecordItem.isPercentageDiscount) {
                            // Calculate the actual percentage from the dollar amount
                            val calculatedPercentage = ((saleRecordItem.discountAmount / item.selling_price.toFloat()) * 100).toInt()
                            "$calculatedPercentage% off"
                        } else {
                            "${formatter.format(saleRecordItem.discountAmount)} off"
                        }
                        Text(
                            text = "Discount: $discountText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right column: Quantity and subtotal
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Qty: $quantity",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Subtotal: ${formatter.format(subtotalAfterDiscount)}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Tax and final price - All on one line with proper overflow handling
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tax (${(taxRate * 100).toInt()}%): ${formatter.format(taxAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "Total: ${formatter.format(finalPrice)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(0.8f),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}



@Composable
fun CartSummarySection(
    inventoryItems: List<InventoryItem>,
    modifier: Modifier = Modifier
) {
    val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    val totalValue = inventoryItems.sumOf { it.selling_price * it.quantity }
    val totalItems = inventoryItems.sumOf { it.quantity }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Inventory Summary",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Items",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "$totalItems",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Number of Products",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${inventoryItems.size}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Value",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatter.format(totalValue),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


@Composable
fun ConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Confirm Update") },
        text = {
            Text(
                "Are you sure you want to update inventory with these items? This action cannot be undone."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Composable
fun EmptyCartMessage(
    onBackToShopping: () -> Unit
) {
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
            text = "No items in Cart",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add items to your cart to update them",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}