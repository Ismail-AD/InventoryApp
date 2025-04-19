package com.appdev.inventoryapp.ui.Screens.SalesPage

import com.appdev.inventoryapp.Utils.SortOrder
import com.appdev.inventoryapp.domain.model.InventoryItem
import com.appdev.inventoryapp.domain.model.SaleRecordItem
import com.appdev.inventoryapp.domain.model.SalesRecord
import com.appdev.inventoryapp.Utils.DateRangeFilter
import java.util.Date

// Step 2: Update SalesPageState to include user permissions
data class SalesPageState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val inventoryItems: List<InventoryItem> = emptyList(),
    val filteredItems: List<InventoryItem> = emptyList(),
    val searchQuery: String = "",
    val currentSortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val selectedCategory: String? = null,
    val categories: List<String> = emptyList(),
    val isSortMenuExpanded: Boolean = false,
    val isCategoryMenuExpanded: Boolean = false,
    val quantitySold: String = "",
    val discount: String = "",
    val isPercentageDiscount: Boolean = true,
    val cartItems: List<SaleRecordItem> = emptyList(),
    val successMessage: String? = null,
    val showSuccessMessage: Boolean = false,
    val isQrScannerActive: Boolean = false,
    val showConfirmationDialog: Boolean = false,
    val itemQuantityMap: Map<Long, Int> = emptyMap(),

    // Sales history related state
    val salesRecords: List<SalesRecord> = emptyList(),
    val filteredRecords: List<SalesRecord> = emptyList(),
    val selectedSalesRecord: SalesRecord? = null,
    val showDetailModal: Boolean = false,
    val selectedStatus: String? = null,
    val isStatusMenuExpanded: Boolean = false,
    val dateRangeFilter: DateRangeFilter = DateRangeFilter.ALL,
    val isDateRangeMenuExpanded: Boolean = false,
    val startDate: Date? = null,
    val endDate: Date? = null,
    val tempStartDate: Date? = null,
    val tempEndDate: Date? = null,
    val showStartDatePicker: Boolean = false,
    val showEndDatePicker: Boolean = false,

    // Undo sale functionality
    val userPermissions: List<String> = emptyList(),
    val showUndoConfirmationDialog: Boolean = false,
    val saleToUndo: SalesRecord? = null,
    val isUndoLoading: Boolean = false,
)

