package com.appdev.inventoryapp.ui.Screens.SalesEntry

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdev.inventoryapp.domain.model.InventoryItem
import com.appdev.inventoryapp.ui.Screens.SalesPage.SalesPageViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdev.inventoryapp.ui.Screens.DisplayInventory.InventoryEvent
import com.appdev.inventoryapp.ui.Screens.InventoryManagemnt.TitledOutlinedTextField
import com.appdev.inventoryapp.ui.Screens.SalesPage.SalesPageEvent
import com.appdev.inventoryapp.ui.Screens.SalesPage.SalesPageState


@Composable
fun SalesEntryScreen(
    product: InventoryItem,
    viewModel: SalesPageViewModel = hiltViewModel(),
    onBackPressed: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ProductSalesEntryScreen(
        product,
        state = state,
        onEvent = viewModel::handleEvent,
        onBackPressed = onBackPressed
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductSalesEntryScreen(
    product: InventoryItem,
    state: SalesPageState,
    onEvent: (SalesPageEvent) -> Unit,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(key1 = state.showSuccessMessage) {
        if (state.showSuccessMessage) {
            Toast.makeText(context, "Item added to cart successfully", Toast.LENGTH_SHORT).show()
            onEvent(SalesPageEvent.DismissSuccessMessage)
            onBackPressed()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add to Sale") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Quantity Field
                    TitledOutlinedTextField(
                        title = "Quantity Sold",
                        value = state.quantitySold,
                        onValueChange = {
                            onEvent(SalesPageEvent.QuantitySoldChanged(it))
                        },
                        placeholder = "0",
                        singleLine = true,
                        isNumber = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Discount Field
                    TitledOutlinedTextField(
                        title = "Discount",
                        value = state.discount,
                        onValueChange = {
                            onEvent(SalesPageEvent.DiscountChanged(it))
                        },
                        placeholder = "0",
                        singleLine = true,
                        isNumber = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Discount Type Switcher
                    Text(
                        text = "Discount Type:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = state.isPercentageDiscount,
                                onClick = {
                                    onEvent(SalesPageEvent.SetDiscountType(true))
                                }
                            )
                            Text(
                                text = "Percentage (%)",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = !state.isPercentageDiscount,
                                onClick = {
                                    onEvent(SalesPageEvent.SetDiscountType(false))
                                }
                            )
                            Text(
                                text = "Flat ($)",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }

                // Product Details Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Product Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Product Name
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Product Details
                        DetailRow("SKU", product.sku)
                        DetailRow("Category", product.category)
                        DetailRow("Available Quantity", "${product.quantity}")
                        DetailRow("Selling Price", "$${product.selling_price}")
                        DetailRow("Cost Price", "$${product.cost_price}")
                        DetailRow("Taxes", "$${product.taxes}")

                        // Calculated total
//                    val quantity = state.quantitySold.toIntOrNull() ?: 0
//                    val discount = state.discount.toFloatOrNull() ?: 0.0f
//                    val unitPrice = product.selling_price.toFloat()
//                    val totalBeforeDiscount = (unitPrice * quantity)
//
//                    val discountAmount = if (state.isPercentageDiscount) {
//                        totalBeforeDiscount * (discount / 100f)
//                    } else {
//                        discount
//                    }
//
//                    val finalPrice = totalBeforeDiscount - discountAmount

//                    Divider(
//                        modifier = Modifier.padding(vertical = 16.dp),
//                        thickness = 1.dp
//                    )
//
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween
//                    ) {
//                        Text(
//                            text = "Total Price",
//                            style = MaterialTheme.typography.titleMedium,
//                            fontWeight = FontWeight.Bold
//                        )
//                        Text(
//                            text = "$${String.format("%.2f", finalPrice)}",
//                            style = MaterialTheme.typography.titleMedium,
//                            fontWeight = FontWeight.Bold,
//                            color = MaterialTheme.colorScheme.primary
//                        )
//                    }
                    }
                }

                // Add to Cart Button
                Button(
                    onClick = {
                        onEvent(
                            SalesPageEvent.AddToCart(
                                product
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = (state.quantitySold.trim()
                        .isNotEmpty() && state.quantitySold.toInt() > 0) ||
                            (state.discount.trim().isNotEmpty() && state.discount.toFloat() > 0f)
                ) {
                    Text(
                        text = "Add to Cart",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

            }
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

        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
    Divider(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
        thickness = 0.5.dp
    )
}