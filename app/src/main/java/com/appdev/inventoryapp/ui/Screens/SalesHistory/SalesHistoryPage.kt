package com.appdev.inventoryapp.ui.Screens.SalesHistory

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdev.inventoryapp.Utils.DateRangeFilter
import com.appdev.inventoryapp.domain.model.SalesRecord
import com.appdev.inventoryapp.ui.Screens.Inventory.CategoryDropdown
import com.appdev.inventoryapp.ui.Screens.Inventory.SortDropdown
import com.appdev.inventoryapp.ui.Screens.SalesPage.SalesPageEvent
import com.appdev.inventoryapp.ui.Screens.SalesPage.SalesPageState
import com.appdev.inventoryapp.ui.Screens.SalesPage.SalesPageViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SalesHistoryScreen(
    viewModel: SalesPageViewModel = hiltViewModel(),
    navigateToSalesEntry: () -> Unit // Add navigation parameter
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SalesHistoryScreenContent(
        state = state,
        onEvent = viewModel::handleEvent,
        navigateToSaleDetail = { salesRecord -> viewModel.handleEvent(SalesPageEvent.ShowSaleDetail(salesRecord)) },
        navigateToSalesEntry = navigateToSalesEntry // Pass it down
    )
    if (state.showDetailModal && state.selectedSalesRecord != null) {
        SalesDetailModal(
            inventoryItems = state.inventoryItems,
            isUndoLoading = state.isUndoLoading,
            salesRecord = state.selectedSalesRecord!!,
            onDismiss = { viewModel.handleEvent(SalesPageEvent.HideSaleDetail) },
            userPermissions = state.userPermissions,
            onUndoSale = { viewModel.handleEvent(SalesPageEvent.ShowUndoConfirmation(it)) }
        )
    }

    // Show undo confirmation dialog if needed
    if (state.showUndoConfirmationDialog) {
        UndoConfirmationDialog(
            onConfirm = { viewModel.handleEvent(SalesPageEvent.ConfirmUndoSale) },
            onDismiss = { viewModel.handleEvent(SalesPageEvent.DismissUndoConfirmation) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesHistoryScreenContent(
    state: SalesPageState,
    onEvent: (SalesPageEvent) -> Unit,
    navigateToSaleDetail: (SalesRecord) -> Unit,
    navigateToSalesEntry: () -> Unit // Add parameter here
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales History") }
            )
        },
        floatingActionButton = {
            if (state.salesRecords.isNotEmpty()) {
                FloatingActionButton(
                    onClick = navigateToSalesEntry,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Sale"
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

                state.salesRecords.isEmpty() -> {
                    EmptySalesHistoryMessage(
                        onAddSaleClick = navigateToSalesEntry
                    )
                }


                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        SearchBar(
                            query = state.searchQuery,
                            onQueryChange = { onEvent(SalesPageEvent.SearchQueryChanged(it)) }
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
                            if (state.categories.isNotEmpty()) {
                                CategoryDropdown(
                                    selectedCategory = state.selectedCategory,
                                    categories = state.categories,
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

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Status dropdown
                            StatusDropdown(
                                selectedStatus = state.selectedStatus,
                                isExpanded = state.isStatusMenuExpanded,
                                onExpandChange = {
                                    onEvent(SalesPageEvent.ToggleStatusMenu(it))
                                },
                                onStatusSelected = {
                                    onEvent(SalesPageEvent.FilterByStatus(it))
                                },
                                modifier = Modifier.weight(1f)
                            )

                            DateRangeDropdownWithPicker(
                                state = state,
                                onEvent = onEvent,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (state.dateRangeFilter == DateRangeFilter.CUSTOM &&
                            state.startDate != null && state.endDate != null) {
                            CustomDateRangeSummary(
                                startDate = state.startDate,
                                endDate = state.endDate,
                                onClear = { onEvent(SalesPageEvent.FilterByDateRange(DateRangeFilter.ALL)) }
                            )
                        }
                        // Display no matches message or sales list
                        if (state.filteredRecords.isEmpty() && (state.searchQuery.isNotEmpty() ||
                                    state.selectedCategory != null ||
                                    state.selectedStatus != null ||
                                    state.dateRangeFilter != DateRangeFilter.ALL)
                        ) {
                            NoMatchesFoundMessage(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        } else {
                            SalesHistoryList(
                                context = context,
                                salesRecords = state.filteredRecords,
                                onSaleClick = { navigateToSaleDetail(it) }
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
        }
    }
}

@Composable
fun EmptySalesHistoryMessage(
    onAddSaleClick: () -> Unit // Add parameter for click handling
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ReceiptLong,
            contentDescription = "No sales",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Sales History Found",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "When you make sales, they'll appear here",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Add Sale Button
        Button(
            onClick = onAddSaleClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Create New Sale",
                fontSize = 16.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusDropdown(
    selectedStatus: String?,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onStatusSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val statuses = listOf("Completed", "Reversed")

    Box(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = isExpanded,
            onExpandedChange = onExpandChange
        ) {
            OutlinedTextField(
                value = selectedStatus ?: "All Status",
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            ExposedDropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { onExpandChange(false) }
            ) {
                DropdownMenuItem(
                    text = { Text("All Status") },
                    onClick = {
                        onStatusSelected(null)
                        onExpandChange(false)
                    }
                )

                statuses.forEach { status ->
                    DropdownMenuItem(
                        text = { Text(status) },
                        onClick = {
                            onStatusSelected(status)
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
fun DateRangeDropdownWithPicker(
    state: SalesPageState,
    onEvent: (SalesPageEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateRanges = mapOf(
        DateRangeFilter.ALL to "All Time",
        DateRangeFilter.TODAY to "Today",
        DateRangeFilter.WEEK to "This Week",
        DateRangeFilter.MONTH to "This Month",
        DateRangeFilter.YEAR to "This Year",
        DateRangeFilter.CUSTOM to "Set Range"
    )

    Box(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = state.isDateRangeMenuExpanded,
            onExpandedChange = { onEvent(SalesPageEvent.ToggleDateRangeMenu(it)) }
        ) {
            OutlinedTextField(
                value = dateRanges[state.dateRangeFilter] ?: "All Time",
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = state.isDateRangeMenuExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            ExposedDropdownMenu(
                expanded = state.isDateRangeMenuExpanded,
                onDismissRequest = { onEvent(SalesPageEvent.ToggleDateRangeMenu(false)) }
            ) {
                dateRanges.forEach { (filter, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onEvent(SalesPageEvent.FilterByDateRange(filter))
                            // If custom range is selected, show the date picker
                            if (filter == DateRangeFilter.CUSTOM) {
                                onEvent(SalesPageEvent.ShowStartDatePicker)
                            }
                        }
                    )
                }
            }
        }
    }

    // Show start date picker dialog
    if (state.showStartDatePicker) {
        CustomDatePickerDialog(
            title = "Select Start Date",
            onDateSelected = { date ->
                onEvent(SalesPageEvent.TempStartDateSelected(date))
                onEvent(SalesPageEvent.HideStartDatePicker)
                onEvent(SalesPageEvent.ShowEndDatePicker)
            },
            onDismiss = {
                onEvent(SalesPageEvent.HideStartDatePicker)
                onEvent(SalesPageEvent.CancelCustomDateRange)
            }
        )
    }

    // Show end date picker dialog
    if (state.showEndDatePicker) {
        CustomDatePickerDialog(
            title = "Select End Date",
            onDateSelected = { date ->
                onEvent(SalesPageEvent.TempEndDateSelected(date))
                onEvent(SalesPageEvent.HideEndDatePicker)
                // Apply the date range if both dates are selected
                if (state.tempStartDate != null && date != null) {
                    onEvent(SalesPageEvent.SetCustomDateRange(state.tempStartDate, date))
                }
            },
            onDismiss = {
                onEvent(SalesPageEvent.HideEndDatePicker)
                onEvent(SalesPageEvent.CancelCustomDateRange)
            },
            minDate = state.tempStartDate // Ensure end date is after start date
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePickerDialog(
    title: String,
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit,
    minDate: Date? = null
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = Calendar.getInstance().timeInMillis
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Compact DatePicker with reduced height
                DatePicker(
                    state = datePickerState,
                    modifier = Modifier
                        .height(350.dp) // Reduced height
                        .align(Alignment.CenterHorizontally),
                    title = null, // Remove title to save space
                    headline = null, // Remove headline to save space
                    showModeToggle = false // Hide mode toggle to save space
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                            // Use the actual selected date from datePickerState
                            datePickerState.selectedDateMillis?.let { dateMillis ->
                                val selectedCalendar = Calendar.getInstance()
                                selectedCalendar.timeInMillis = dateMillis
                                onDateSelected(selectedCalendar.time)
                            }
                        }
                    ) {
                        Text("Select")
                    }
                }
            }
        }
    }
}

@Composable
fun CustomDateRangeSummary(
    startDate: Date?,
    endDate: Date?,
    onClear: () -> Unit
) {
    if (startDate == null || endDate == null) return

    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Date Range",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            TextButton(onClick = onClear) {
                Text(
                    text = "Clear",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SalesHistoryList(
    context: Context,
    salesRecords: List<SalesRecord>,
    onSaleClick: (SalesRecord) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(salesRecords) { record ->
            SalesHistoryCard(
                record = record,
                onClick = { onSaleClick(record) }
            )
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
        placeholder = { Text("Search product name/SKU") },
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

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun CategoryDropdown(
//    selectedCategory: String?,
//    categories: List<String>,
//    isExpanded: Boolean,
//    onExpandChange: (Boolean) -> Unit,
//    onCategorySelected: (String?) -> Unit,
//    modifier: Modifier = Modifier
//) {
//    Box(modifier = modifier) {
//        ExposedDropdownMenuBox(
//            expanded = isExpanded,
//            onExpandedChange = onExpandChange
//        ) {
//            OutlinedTextField(
//                value = selectedCategory ?: "All Categories",
//                onValueChange = {},
//                readOnly = true,
//                singleLine = true,
//                trailingIcon = {
//                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
//                },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .menuAnchor(),
//                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
//                    focusedContainerColor = Color.Transparent,
//                    unfocusedContainerColor = Color.Transparent
//                )
//            )
//
//            ExposedDropdownMenu(
//                expanded = isExpanded,
//                onDismissRequest = { onExpandChange(false) }
//            ) {
//                DropdownMenuItem(
//                    text = { Text("All Categories") },
//                    onClick = {
//                        onCategorySelected(null)
//                        onExpandChange(false)
//                    }
//                )
//
//                categories.forEach { category ->
//                    DropdownMenuItem(
//                        text = { Text(category) },
//                        onClick = {
//                            onCategorySelected(category)
//                            onExpandChange(false)
//                        }
//                    )
//                }
//            }
//        }
//    }
//}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesHistoryCard(
    record: SalesRecord,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val date = Date(record.lastUpdated.toLong())
    val formattedDate = dateFormat.format(date)

    val totalItems = record.salesRecordItem.sumOf { it.quantity }
    val totalAmount = record.salesRecordItem.sumOf {
        it.quantity * (it.selling_price - it.discountAmount)
    }

    val currencyFormat = NumberFormat.getCurrencyInstance()

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sale #${record.id.takeLast(8)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Status indicator
                val statusColor = if (record.status == "Completed")
                    MaterialTheme.colorScheme.primary else Color.Red

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = statusColor.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = record.status,
                        fontSize = 12.sp,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Simplified content - just date, total items and amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formattedDate,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Text(
                    text = "Total: ${currencyFormat.format(totalAmount)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$totalItems items",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            // Remove detailed item information - this will now show in the modal
        }
    }
}


@Composable
fun NoMatchesFoundMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FilterAlt,
            contentDescription = "No matches",
            modifier = Modifier.size(70.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Matching Records Found",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Try adjusting your filters or search terms",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

// Step 7: Create UndoConfirmationDialog composable
@Composable
fun UndoConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Undo Sale") },
        text = {
            Text(
                "Are you sure you want to undo this sale? This will return all sold items back to inventory and mark the sale as Reversed."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Undo Sale")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}