package com.appdev.inventoryapp.ui.Screens.Reports

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdev.inventoryapp.domain.model.SalesRecord
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.PieChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.DotProperties
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.IndicatorCount
import ir.ehsannarmani.compose_charts.models.IndicatorPosition
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line
import ir.ehsannarmani.compose_charts.models.Pie
import ir.ehsannarmani.compose_charts.models.PopupProperties
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun ReportsScreen(
    viewModel: ReportViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.handleEvent(ReportEvent.LoadReports)
    }

    ReportsScreenContent(
        state = state,
        onEvent = viewModel::handleEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreenContent(
    state: ReportState,
    onEvent: (ReportEvent) -> Unit
) {
    Scaffold(
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
                    EmptyReportsMessage()
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    ) {
                        // Filters section
                        FiltersSection(
                            state = state,
                            onEvent = onEvent
                        )

                        // Reports content in scrollable container
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // KPI Cards
                            item {
                                KpiCardsRow(state = state)
                            }

                            // Sales Trend Chart
                            item {
                                SalesTrendCard(state = state)
                            }

                            // Category Breakdown Chart
                            item {
                                CategoryBreakdownCard(state = state)
                            }

                            // Top Products
                            item {
                                TopProductsCard(state = state)
                                Spacer(Modifier.height(20.dp))
                            }
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
                        TextButton(onClick = { onEvent(ReportEvent.DismissError) }) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.onError)
                }
            }

            // Date Range Picker Dialog
            if (state.isDateRangePickerVisible) {
                DateRangePickerDialog(
                    initialStartDate = state.startDate,
                    initialEndDate = state.endDate,
                    onDismiss = { onEvent(ReportEvent.HideDateRangePicker) },
                    onConfirm = { startDate, endDate ->
                        onEvent(ReportEvent.UpdateDateRange(startDate, endDate))
                    }
                )
            }
        }
    }
}


// Now modify the FiltersSection function to use the individual date pickers
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersSection(
    state: ReportState,
    onEvent: (ReportEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = "Filters",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Date Range Button - Updated to trigger start date picker directly
            OutlinedButton(
                onClick = { onEvent(ReportEvent.ShowStartDatePicker) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Date Range"
                )
                Spacer(modifier = Modifier.width(4.dp))
                val dateFormat = DateTimeFormatter.ofPattern("MMM d")
                Text(
                    text = "${state.startDate.format(dateFormat)} - ${
                        state.endDate.format(
                            dateFormat
                        )
                    }",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Second row of filters - Fix for Salesperson dropdown
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Salesperson Filter
            if (state.salespeople.isNotEmpty()) {
                Box(modifier = Modifier.weight(1f)) {
                    ExposedDropdownMenuBox(
                        expanded = state.isSalespersonFilterExpanded,
                        onExpandedChange = { onEvent(ReportEvent.ToggleSalespersonFilterMenu) }
                    ) {
                        OutlinedTextField(
                            value = state.selectedSalesperson ?: "All Sales Staff",
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = state.isSalespersonFilterExpanded)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Salesperson"
                                )
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
                            expanded = state.isSalespersonFilterExpanded,
                            onDismissRequest = { onEvent(ReportEvent.ToggleSalespersonFilterMenu) }
                        ) {
                            // Add "All Sales Staff" option
                            DropdownMenuItem(
                                text = { Text("All Sales Staff") },
                                onClick = {
                                    onEvent(ReportEvent.FilterBySalesperson(null))
                                }
                            )

                            // Add salesperson options
                            state.salespeople.forEach { person ->
                                DropdownMenuItem(
                                    text = { Text(person) },
                                    onClick = {
                                        onEvent(ReportEvent.FilterBySalesperson(person))
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Category Filter - Enhanced with proper menu structure
            if (state.categories.isNotEmpty()) {
                Box(modifier = Modifier.weight(1f)) {
                    ExposedDropdownMenuBox(
                        expanded = state.isCategoryFilterExpanded,
                        onExpandedChange = { onEvent(ReportEvent.ToggleCategoryFilterMenu) }
                    ) {
                        OutlinedTextField(
                            value = state.selectedCategory ?: "All Categories",
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = state.isCategoryFilterExpanded)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = "Category"
                                )
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
                            expanded = state.isCategoryFilterExpanded,
                            onDismissRequest = { onEvent(ReportEvent.ToggleCategoryFilterMenu) }
                        ) {
                            // Add "All Categories" option
                            DropdownMenuItem(
                                text = { Text("All Categories") },
                                onClick = {
                                    onEvent(ReportEvent.FilterByCategory(null))
                                }
                            )

                            // Add category options
                            state.categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        onEvent(ReportEvent.FilterByCategory(category))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Show start date picker dialog
    if (state.showStartDatePicker) {
        CustomDatePickerDialog(
            title = "Select Start Date",
            initialDate = state.startDate,
            onDateSelected = { date ->
                onEvent(ReportEvent.TempStartDateSelected(date))
                onEvent(ReportEvent.HideStartDatePicker)
                onEvent(ReportEvent.ShowEndDatePicker)
            },
            onDismiss = {
                onEvent(ReportEvent.HideStartDatePicker)
                onEvent(ReportEvent.CancelCustomDateRange)
            }
        )
    }

    // Show end date picker dialog
    if (state.showEndDatePicker) {
        CustomDatePickerDialog(
            title = "Select End Date",
            initialDate = state.endDate,
            minDate = state.tempStartDate, // Ensure end date is after start date
            onDateSelected = { date ->
                onEvent(ReportEvent.TempEndDateSelected(date))
                onEvent(ReportEvent.HideEndDatePicker)
                // Apply the date range if both dates are selected
                if (state.tempStartDate != null) {
                    onEvent(ReportEvent.UpdateDateRange(state.tempStartDate, date))
                }
            },
            onDismiss = {
                onEvent(ReportEvent.HideEndDatePicker)
                onEvent(ReportEvent.CancelCustomDateRange)
            }
        )
    }
}

// Add a new CustomDatePickerDialog composable function for individual date selection
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePickerDialog(
    title: String,
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    minDate: LocalDate? = null
) {
    val initialMillis = initialDate.toEpochDay() * 24 * 60 * 60 * 1000

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
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

                // DatePicker with reduced height
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
                            datePickerState.selectedDateMillis?.let { millis ->
                                val date = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                                onDateSelected(date)
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
fun KpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title row with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Value with better overflow handling
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun KpiCardsRow(state: ReportState) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Total Revenue KPI Card
        KpiCard(
            title = "Total Revenue",
            value = currencyFormatter.format(state.totalRevenue),
            icon = Icons.Default.AttachMoney,
            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .fillMaxWidth()
        )

        // Total Profit KPI Card
        KpiCard(
            title = "Total Profit",
            value = currencyFormatter.format(state.totalProfit),
            icon = Icons.Default.TrendingUp,
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )

        // Number of Sales KPI Card
        KpiCard(
            title = "Sales Count",
            value = state.numberOfSales.toString(),
            icon = Icons.Default.Receipt,
            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )
    }

}

@Composable
fun SalesTrendCard(state: ReportState) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Sales Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.salesTrend.isEmpty()) {
                Text(
                    text = "No sales data available for the selected period",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(vertical = 32.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .padding(vertical = 8.dp)
                ) {
                    // Use the prepared chart data from state
                    if (state.salesTrendChartData.isNotEmpty()) {
                        LineChart(
                            labelProperties = LabelProperties(
                                enabled = true,
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp
                                )
                            ),
                            indicatorProperties = HorizontalIndicatorProperties(
                                enabled = true,
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            ),
                            labelHelperProperties = LabelHelperProperties(
                                enabled = true,
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 14.sp
                                )
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 7.dp, vertical = 8.dp),
                            data = state.salesTrendChartData,
                            animationMode = AnimationMode.Together(delayBuilder = { it * 500L }),
                        )
                    }
                }
            }
        }
    }
}

// Inside CategoryBreakdownCard function, replace the empty else block
@Composable
fun CategoryBreakdownCard(state: ReportState) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Category Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.categoryBreakdown.isEmpty()) {
                Text(
                    text = "No category data available for the selected period",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(vertical = 32.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                // Add Ehsan Narmani Pie Chart
                Box(
                    modifier = Modifier
                        .fillMaxWidth() .wrapContentHeight()
                        .padding(vertical = 8.dp)
                ) {
                    // Generate category colors
                    val categoryColorMap = remember(state.categoryBreakdown.keys) {
                        // Generate a map of category to color
                        val categories = state.categoryBreakdown.keys.toList()
                        val colors = generateDistinctColorsImproved(categories.size)
                        categories.zip(colors).toMap()
                    }

                    // Format the data for the pie chart
                    val pieData = state.categoryBreakdown.entries.map { entry ->
                        Pie(
                            label = entry.key,
                            data = entry.value,
                            color = categoryColorMap[entry.key] ?: Color.Gray,
                            selectedColor = (categoryColorMap[entry.key]
                                ?: Color.Gray).copy(alpha = 0.8f)
                        )
                    }

                    // Keep track of selected state
                    var data by remember {
                        mutableStateOf(pieData)
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Pie chart
                        PieChart(
                            modifier = Modifier.size(200.dp),
                            data = data,
                            onPieClick = { clickedPie ->
                                val pieIndex = data.indexOf(clickedPie)
                                data = data.mapIndexed { mapIndex, pie ->
                                    pie.copy(selected = pieIndex == mapIndex)
                                }
                            },
                            selectedScale = 1.1f,
                            scaleAnimEnterSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            colorAnimEnterSpec = tween(300),
                            colorAnimExitSpec = tween(300),
                            scaleAnimExitSpec = tween(300),
                            spaceDegreeAnimExitSpec = tween(300),
                            style = Pie.Style.Fill
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        val format = NumberFormat.getCurrencyInstance(Locale.US)

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            data.forEach { pie ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(
                                                color = if (pie.selected) pie.selectedColor else pie.color,
                                                shape = RoundedCornerShape(2.dp)
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    pie.label?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Text(
                                        text = format.format(pie.data),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopProductsCard(state: ReportState) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Top Selling Products",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.topProducts.isEmpty()) {
                Text(
                    text = "No sales data available for the selected period",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(vertical = 32.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Product",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "Units Sold",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(80.dp)
                        )
                    }

                    Divider()

                    // Product Items
                    state.topProducts.forEachIndexed { index, (productName, quantity) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rank
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(24.dp)
                            )

                            // Product Name
                            Text(
                                text = productName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )

                            // Quantity
                            Text(
                                text = quantity.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(80.dp)
                            )
                        }

                        if (index < state.topProducts.size - 1) {
                            Divider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyReportsMessage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Assessment,
            contentDescription = "No Reports",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Sales Data Available",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add some sales to view reports and analytics",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    initialStartDate: LocalDate,
    initialEndDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit
) {
    var startDate by remember { mutableStateOf(initialStartDate) }
    var endDate by remember { mutableStateOf(initialEndDate) }
    var isStartDateSelection by remember { mutableStateOf(true) }

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
                        text = if (isStartDateSelection) "Select Start Date" else "Select End Date",
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

                // Date Selection Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Start Date Button
                    OutlinedButton(
                        onClick = { isStartDateSelection = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isStartDateSelection)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isStartDateSelection)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "From",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = startDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // End Date Button
                    OutlinedButton(
                        onClick = { isStartDateSelection = false },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (!isStartDateSelection)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (!isStartDateSelection)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "To",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = endDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actual DatePicker
                if (isStartDateSelection) {
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = startDate.toEpochDay() * 24 * 60 * 60 * 1000
                    )

                    DatePicker(
                        state = datePickerState,
                        modifier = Modifier
                            .height(350.dp)
                            .align(Alignment.CenterHorizontally),
                        title = null,
                        headline = null,
                        showModeToggle = false
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val date = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                                startDate = date

                                // If start date is after end date, adjust end date
                                if (startDate.isAfter(endDate)) {
                                    endDate = startDate
                                }

                                isStartDateSelection = false
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Next")
                    }
                } else {
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = endDate.toEpochDay() * 24 * 60 * 60 * 1000,
                        selectableDates = object : SelectableDates {
                            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                                val date =
                                    LocalDate.ofEpochDay(utcTimeMillis / (24 * 60 * 60 * 1000))
                                return !date.isBefore(startDate)
                            }
                        }
                    )

                    DatePicker(
                        state = datePickerState,
                        modifier = Modifier
                            .height(350.dp)
                            .align(Alignment.CenterHorizontally),
                        title = null,
                        headline = null,
                        showModeToggle = false
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isStartDateSelection = true }) {
                            Text("Back")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val date = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                                    endDate = date
                                    onConfirm(startDate, endDate)
                                }
                            }
                        ) {
                            Text("Apply")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Generates a list of visually distinct colors based on HSL color space with improved distribution.
 * This function ensures good color separation regardless of the number of categories.
 *
 * @param count Number of distinct colors to generate
 * @return List of distinct Color objects
 */
fun generateDistinctColorsImproved(count: Int): List<Color> {
    // If we only need a few colors, use a predefined palette for optimal readability
    val predefinedColors = listOf(
        Color(0xFF1F77B4), // Blue
        Color(0xFFFF7F0E), // Orange
        Color(0xFF2CA02C), // Green
        Color(0xFFD62728), // Red
        Color(0xFF9467BD), // Purple
        Color(0xFF8C564B), // Brown
        Color(0xFFE377C2), // Pink
        Color(0xFF7F7F7F), // Gray
        Color(0xFFBCBD22), // Olive
        Color(0xFF17BECF)  // Teal
    )

    if (count <= predefinedColors.size) {
        return predefinedColors.take(count)
    }

    val colors = mutableListOf<Color>()

    // Use golden ratio conjugate to ensure maximum hue distance
    val goldenRatioConjugate = 0.618033988749895
    var h = kotlin.random.Random.nextDouble(
        0.0,
        1.0
    ) // Random start value but deterministic per session

    // We'll vary both saturation and lightness to get more distinct colors
    // when we need a large number of them
    val saturationRange = if (count > 20) {
        listOf(0.65f, 0.8f)
    } else {
        listOf(0.7f)
    }

    val lightnessRange = if (count > 30) {
        listOf(0.45f, 0.6f)
    } else {
        listOf(0.55f)
    }

    // We'll cycle through combinations of saturation and lightness
    var saturationIndex = 0
    var lightnessIndex = 0

    repeat(count) {
        h += goldenRatioConjugate
        h %= 1.0

        val s = saturationRange[saturationIndex]
        val l = lightnessRange[lightnessIndex]

        // Cycle through saturation and lightness values
        saturationIndex = (saturationIndex + 1) % saturationRange.size
        if (saturationIndex == 0) {
            lightnessIndex = (lightnessIndex + 1) % lightnessRange.size
        }

        // Convert HSL to RGB
        val color = hslToColor(h.toFloat(), s, l)
        colors.add(color)
    }

    return colors
}

/**
 * Converts HSL (Hue, Saturation, Lightness) values to RGB Color.
 *
 * @param h Hue [0..1]
 * @param s Saturation [0..1]
 * @param l Lightness [0..1]
 * @return Color object
 */
fun hslToColor(h: Float, s: Float, l: Float): Color {
    val q = if (l < 0.5f) l * (1 + s) else l + s - l * s
    val p = 2 * l - q

    val r = hueToRgb(p, q, h + 1 / 3f)
    val g = hueToRgb(p, q, h)
    val b = hueToRgb(p, q, h - 1 / 3f)

    return Color(r, g, b, 1.0f)
}

/**
 * Helper function for HSL to RGB conversion.
 */
fun hueToRgb(p: Float, q: Float, t: Float): Float {
    var tAdjusted = t
    if (tAdjusted < 0) tAdjusted += 1f
    if (tAdjusted > 1) tAdjusted -= 1f

    return when {
        tAdjusted < 1 / 6f -> p + (q - p) * 6 * tAdjusted
        tAdjusted < 1 / 2f -> q
        tAdjusted < 2 / 3f -> p + (q - p) * (2 / 3f - tAdjusted) * 6
        else -> p
    }
}

fun isSystemInDarkTheme(context: Context): Boolean {
    val currentNightMode =
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return currentNightMode == Configuration.UI_MODE_NIGHT_YES
}