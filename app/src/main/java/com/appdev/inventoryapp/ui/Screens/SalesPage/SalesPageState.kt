package com.appdev.inventoryapp.ui.Screens.SalesPage

import com.appdev.inventoryapp.Utils.SortOrder
import com.appdev.inventoryapp.domain.model.InventoryItem
import com.appdev.inventoryapp.domain.model.SaleRecordItem
import com.appdev.inventoryapp.domain.model.SalesRecord
import com.appdev.inventoryapp.Utils.DateRangeFilter
import java.util.Date

data class SalesPageState(
    // Original SalesPage state properties
    val inventoryItems: List<InventoryItem> = emptyList(),
    val filteredItems: List<InventoryItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSortMenuExpanded: Boolean = false,
    val isCategoryMenuExpanded: Boolean = false,
    val currentSortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val selectedCategory: String? = null,
    val categories: List<String> = emptyList(),
    val cartItems: List<SaleRecordItem> = emptyList(),
    val isQrScannerActive: Boolean = false,
    val showSuccessMessage: Boolean = false,
    val quantitySold: String = "",
    val discount: String = "",
    val isPercentageDiscount: Boolean = true,
    val itemQuantityMap: Map<Long, Int> = emptyMap(),
    val showConfirmationDialog: Boolean = false,
    val successMessage: String? = null,

    // Added SalesHistory state properties
    val salesRecords: List<SalesRecord> = emptyList(),
    val filteredRecords: List<SalesRecord> = emptyList(),
    val selectedStatus: String? = null,
    val startDate: Date? = null,
    val endDate: Date? = null,
    val dateRangeFilter: DateRangeFilter = DateRangeFilter.ALL,
    val isStatusMenuExpanded: Boolean = false,
    val isDateRangeMenuExpanded: Boolean = false,
    val selectedSalesRecord: SalesRecord? = null,
    val showDetailModal: Boolean = false,

    val showStartDatePicker: Boolean = false,
    val showEndDatePicker: Boolean = false,
    val tempStartDate: Date? = null,  // For storing temporary selection before confirming
    val tempEndDate: Date? = null     // For storing temporary selection before confirming
)

