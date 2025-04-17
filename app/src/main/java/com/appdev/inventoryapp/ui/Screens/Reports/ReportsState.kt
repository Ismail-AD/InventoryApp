package com.appdev.inventoryapp.ui.Screens.Reports

import com.appdev.inventoryapp.domain.model.InventoryItem
import com.appdev.inventoryapp.domain.model.SalesRecord
import java.time.LocalDate

data class ReportState(
    val isLoading: Boolean = false,
    val salesRecords: List<SalesRecord> = emptyList(),
    val filteredSalesRecords: List<SalesRecord> = emptyList(),
    val inventoryItems: List<InventoryItem> = emptyList(),
    val errorMessage: String? = null,

    // KPI data
    val totalRevenue: Double = 0.0,
    val totalProfit: Double = 0.0,
    val numberOfSales: Int = 0,

    // Filter params
    val selectedCategory: String? = null,
    val selectedSalesperson: String? = null,
    val startDate: LocalDate = LocalDate.now().minusMonths(1), // Default one month ago
    val endDate: LocalDate = LocalDate.now(),                  // Default today


    // Add these new properties for individual date pickers
    val showStartDatePicker: Boolean = false,
    val showEndDatePicker: Boolean = false,
    val tempStartDate: LocalDate? = null,
    val tempEndDate: LocalDate? = null,

    // Filter UI states
    val isDateRangePickerVisible: Boolean = false,
    val isCategoryFilterExpanded: Boolean = false,
    val isSalespersonFilterExpanded: Boolean = false,

    // Chart data
    val categoryBreakdown: Map<String, Double> = emptyMap(),
    val salesTrend: Map<String, Double> = emptyMap(),
    val topProducts: List<Pair<String, Int>> = emptyList(),
    val categories: List<String> = emptyList(),
    val salespeople: List<String> = emptyList()
)