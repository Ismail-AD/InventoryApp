package com.appdev.inventoryapp.ui.Screens.SalesPage

import com.appdev.inventoryapp.Utils.DateRangeFilter
import com.appdev.inventoryapp.Utils.SortOrder
import com.appdev.inventoryapp.domain.model.InventoryItem
import com.appdev.inventoryapp.domain.model.SalesRecord
import java.security.Permissions
import java.util.Date

sealed class SalesPageEvent {
    data class SearchQueryChanged(val query: String) : SalesPageEvent()
    data object DismissError : SalesPageEvent()
    data class UpdateSortOrder(val sortOrder: SortOrder) : SalesPageEvent()
    data class FilterByCategory(val category: String?) : SalesPageEvent()
    data object FetchCategories : SalesPageEvent()
    data class ToggleSortMenu(val isExpanded: Boolean) : SalesPageEvent()
    data class ToggleCategoryMenu(val isExpanded: Boolean) : SalesPageEvent()
    data class AddToCart(val item: InventoryItem) : SalesPageEvent()
    data object ToggleQrScanner : SalesPageEvent()
    data object DismissSuccessMessage : SalesPageEvent()
    data class QuantitySoldChanged(val quantity: String) : SalesPageEvent()
    data class DiscountChanged(val discount: String) : SalesPageEvent()
    data class SetDiscountType(val isPercentage: Boolean) : SalesPageEvent()
    data class RemoveCartItem(val itemId: Long) : SalesPageEvent()
    data object ConfirmSale : SalesPageEvent()
    data object ClearCart : SalesPageEvent()
    data object ShowConfirmationDialog : SalesPageEvent()
    data object DismissConfirmationDialog : SalesPageEvent()
    data object ConfirmSaleAfterDialog : SalesPageEvent()

    // Added SalesHistory events
    data object RefreshSalesHistory : SalesPageEvent()
    data object LoadSalesHistory : SalesPageEvent()
    data class ShowSaleDetail(val salesRecord: SalesRecord) : SalesPageEvent()
    data object HideSaleDetail : SalesPageEvent()
    data class FilterByStatus(val status: String?) : SalesPageEvent()
    data class FilterByDateRange(val rangeFilter: DateRangeFilter) : SalesPageEvent()
    data class SetCustomDateRange(val startDate: Date, val endDate: Date) : SalesPageEvent()
    data class ToggleStatusMenu(val isExpanded: Boolean) : SalesPageEvent()
    data class ToggleDateRangeMenu(val isExpanded: Boolean) : SalesPageEvent()

    data object ShowStartDatePicker : SalesPageEvent()
    data object HideStartDatePicker : SalesPageEvent()
    data object ShowEndDatePicker : SalesPageEvent()
    data object HideEndDatePicker : SalesPageEvent()
    data class TempStartDateSelected(val date: Date) : SalesPageEvent()
    data class TempEndDateSelected(val date: Date) : SalesPageEvent()
    data object ApplyCustomDateRange : SalesPageEvent()
    data object CancelCustomDateRange : SalesPageEvent()

    data class ShowUndoConfirmation(val salesRecord: SalesRecord) : SalesPageEvent()
    object DismissUndoConfirmation : SalesPageEvent()
    object ConfirmUndoSale : SalesPageEvent()
}