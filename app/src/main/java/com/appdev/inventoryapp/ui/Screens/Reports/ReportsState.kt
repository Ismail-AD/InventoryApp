package com.appdev.inventoryapp.ui.Screens.Reports

import com.appdev.inventoryapp.domain.model.InventoryItem
import com.appdev.inventoryapp.domain.model.SalesRecord
import ir.ehsannarmani.compose_charts.models.Line
import java.time.LocalDate

data class ReportState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val inventoryItems: List<InventoryItem> = emptyList(),
    val salesRecords: List<SalesRecord> = emptyList(),
    val filteredSalesRecords: List<SalesRecord> = emptyList(),
    val categories: List<String> = emptyList(),
    val salespeople: List<String> = emptyList(),
    val startDate: LocalDate = LocalDate.now().minusMonths(1),
    val endDate: LocalDate = LocalDate.now(),
    val selectedCategory: String? = null,
    val selectedSalesperson: String? = null,
    val totalRevenue: Double = 0.0,
    val totalProfit: Double = 0.0,
    val numberOfSales: Int = 0,
    val categoryBreakdown: Map<String, Double> = emptyMap(),
    val salesTrend: Map<String, Double> = emptyMap(),
    val topProducts: List<Pair<String, Int>> = emptyList(),
    val isDateRangePickerVisible: Boolean = false,
    val isCategoryFilterExpanded: Boolean = false,
    val isSalespersonFilterExpanded: Boolean = false,
    val showStartDatePicker: Boolean = false,
    val showEndDatePicker: Boolean = false,
    val tempStartDate: LocalDate? = null,
    val tempEndDate: LocalDate? = null,
    // Added for line chart data
    val salesTrendSortedEntries: List<Map.Entry<String, Double>> = emptyList(),
    val salesTrendValues: List<Double> = emptyList(),
    val salesTrendLine: Line? = null,
    val salesTrendChartData: List<Line> = emptyList(),
    // Added for category ID mapping
    val categoryIdToNameMap: Map<Long, String> = emptyMap(),
    val categoryNameToIdMap: Map<String, Long> = emptyMap()
)