package com.appdev.inventoryapp.ui.Screens.SalesPage


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdev.inventoryapp.Utils.DateRangeFilter
import com.appdev.inventoryapp.Utils.ResultState
import com.appdev.inventoryapp.Utils.SessionManagement
import com.appdev.inventoryapp.Utils.SortOrder
import com.appdev.inventoryapp.domain.model.InventoryItem
import com.appdev.inventoryapp.domain.model.SaleRecordItem
import com.appdev.inventoryapp.domain.model.SalesRecord
import com.appdev.inventoryapp.domain.repository.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SalesPageViewModel @Inject constructor(
    private val repository: InventoryRepository,
    val sessionManagement: SessionManagement
) : ViewModel() {
    private val _state = MutableStateFlow(SalesPageState(isLoading = true))
    val state: StateFlow<SalesPageState> = _state.asStateFlow()

    init {
        loadInventory(sessionManagement.getShopId())
        loadSalesHistory(sessionManagement.getShopId())
        fetchCategories()
    }

    fun handleEvent(event: SalesPageEvent) {
        when (event) {
            is SalesPageEvent.ShowStartDatePicker -> {
                _state.update { it.copy(showStartDatePicker = true) }
            }

            is SalesPageEvent.HideStartDatePicker -> {
                _state.update { it.copy(showStartDatePicker = false) }
            }

            is SalesPageEvent.ShowEndDatePicker -> {
                _state.update { it.copy(showEndDatePicker = true) }
            }

            is SalesPageEvent.HideEndDatePicker -> {
                _state.update { it.copy(showEndDatePicker = false) }
            }

            is SalesPageEvent.TempStartDateSelected -> {
                _state.update { it.copy(tempStartDate = event.date) }
            }

            is SalesPageEvent.TempEndDateSelected -> {
                _state.update { it.copy(tempEndDate = event.date) }
            }

            is SalesPageEvent.ApplyCustomDateRange -> {
                val currentState = _state.value
                if (currentState.tempStartDate != null && currentState.tempEndDate != null) {
                    setCustomDateRange(currentState.tempStartDate!!, currentState.tempEndDate!!)
                }
            }

            is SalesPageEvent.CancelCustomDateRange -> {
                // If we're cancelling during date selection, revert to All Time
                if (_state.value.startDate == null || _state.value.endDate == null) {
                    _state.update {
                        it.copy(
                            dateRangeFilter = DateRangeFilter.ALL,
                            tempStartDate = null,
                            tempEndDate = null
                        )
                    }
                    applySalesHistoryFilters()
                }
            }
            // Original SalesPage events
            is SalesPageEvent.RefreshInventory -> loadInventory(sessionManagement.getShopId())
            is SalesPageEvent.SearchQueryChanged -> updateSearchQuery(event.query)
            is SalesPageEvent.DismissError -> dismissError()
            is SalesPageEvent.LoadInventory -> loadInventory(sessionManagement.getShopId())
            is SalesPageEvent.UpdateSortOrder -> {
                updateSortOrder(event.sortOrder)
                _state.update { it.copy(isSortMenuExpanded = false) }
            }

            is SalesPageEvent.FilterByCategory -> {
                filterByCategory(event.category)
                _state.update { it.copy(isCategoryMenuExpanded = false) }
            }

            is SalesPageEvent.FetchCategories -> fetchCategories()
            is SalesPageEvent.ToggleSortMenu -> _state.update { it.copy(isSortMenuExpanded = event.isExpanded) }
            is SalesPageEvent.ToggleCategoryMenu -> _state.update { it.copy(isCategoryMenuExpanded = event.isExpanded) }
            is SalesPageEvent.AddToCart -> {
                if (state.value.successMessage != null) {
                    _state.update {
                        it.copy(successMessage = null)
                    }
                }
                val quantitySold = state.value.quantitySold.toIntOrNull() ?: 0
                val availableQuantity =
                    _state.value.inventoryItems.find { it.id == event.item.id }?.quantity ?: 0

                if (quantitySold > availableQuantity) {
                    _state.update {
                        it.copy(
                            errorMessage = "Error: Quantity sold cannot exceed available quantity",
                            showSuccessMessage = false
                        )
                    }
                } else {
                    val item = SaleRecordItem(
                        quantity = quantitySold,
                        discountAmount = state.value.discount.toFloatOrNull() ?: 0.0f,
                        isPercentageDiscount = state.value.isPercentageDiscount,
                        productName = event.item.name,
                        sku = event.item.sku,
                        productId = event.item.id,
                        selling_price = event.item.selling_price,
                        category = event.item.category
                    )
                    val currentMap = state.value.itemQuantityMap
                    val updatedMap = currentMap.toMutableMap().apply {
                        this[event.item.id] = availableQuantity
                    }
                    _state.update {
                        it.copy(itemQuantityMap = updatedMap)
                    }
                    addItemToCart(item)
                }
            }

            is SalesPageEvent.ToggleQrScanner -> _state.update { it.copy(isQrScannerActive = !it.isQrScannerActive) }
            is SalesPageEvent.DismissSuccessMessage -> _state.update { it.copy(showSuccessMessage = false) }
            is SalesPageEvent.QuantitySoldChanged -> _state.update { it.copy(quantitySold = event.quantity) }
            is SalesPageEvent.DiscountChanged -> _state.update { it.copy(discount = event.discount) }
            is SalesPageEvent.SetDiscountType -> _state.update { it.copy(isPercentageDiscount = event.isPercentage) }
            is SalesPageEvent.RemoveCartItem -> removeCartItem(event.itemId)
            is SalesPageEvent.ConfirmSaleAfterDialog -> {
                _state.update { it.copy(showConfirmationDialog = false) }
                if (sessionManagement.getShopId() != null && sessionManagement.getUserId() != null && sessionManagement.getUserName() != null) {
                    val record = SalesRecord(
                        id = UUID.randomUUID().toString(),
                        shop_id = sessionManagement.getShopId()!!,
                        creator_id = sessionManagement.getUserId()!!,
                        lastUpdated = System.currentTimeMillis().toString(),
                        salesRecordItem = state.value.cartItems,
                        status = "Completed",
                        creator_name = sessionManagement.getUserName()!!
                    )
                    saveSaleToRepository(record)
                }
            }

            is SalesPageEvent.ShowConfirmationDialog -> _state.update {
                it.copy(
                    showConfirmationDialog = true
                )
            }

            is SalesPageEvent.DismissConfirmationDialog -> _state.update {
                it.copy(
                    showConfirmationDialog = false
                )
            }

            is SalesPageEvent.ConfirmSale -> _state.update { it.copy(showConfirmationDialog = true) }
            is SalesPageEvent.ClearCart -> _state.update { it.copy(cartItems = emptyList()) }

            // Added SalesHistory events
            is SalesPageEvent.RefreshSalesHistory -> loadSalesHistory(sessionManagement.getShopId())
            is SalesPageEvent.LoadSalesHistory -> loadSalesHistory(sessionManagement.getShopId())
            is SalesPageEvent.ShowSaleDetail -> {
                _state.update {
                    it.copy(
                        selectedSalesRecord = event.salesRecord,
                        showDetailModal = true
                    )
                }
            }

            is SalesPageEvent.HideSaleDetail -> _state.update { it.copy(showDetailModal = false) }
            is SalesPageEvent.FilterByStatus -> {
                filterByStatus(event.status)
                _state.update { it.copy(isStatusMenuExpanded = false) }
            }

            is SalesPageEvent.FilterByDateRange -> {
                filterByDateRange(event.rangeFilter)
                _state.update { it.copy(isDateRangeMenuExpanded = false) }
            }

            is SalesPageEvent.SetCustomDateRange -> {
                setCustomDateRange(event.startDate, event.endDate)
                _state.update { it.copy(isDateRangeMenuExpanded = false) }
            }

            is SalesPageEvent.ToggleStatusMenu -> _state.update { it.copy(isStatusMenuExpanded = event.isExpanded) }
            is SalesPageEvent.ToggleDateRangeMenu -> _state.update { it.copy(isDateRangeMenuExpanded = event.isExpanded) }
        }
    }

    // Original SalesPage methods
    private fun removeCartItem(itemId: Long) {
        val updatedItems = _state.value.cartItems.filter { it.productId != itemId }
        _state.update { it.copy(cartItems = updatedItems) }
    }

    private fun saveSaleToRepository(salesRecord: SalesRecord) {
        if (salesRecord.salesRecordItem.isEmpty()) {
            _state.update {
                it.copy(errorMessage = "Cart is empty. Please add items before confirming sale.")
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            repository.updateInventoryItems(
                salesRecord,
                mapOfProductData = state.value.itemQuantityMap
            ).collect { result ->
                when (result) {
                    is ResultState.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }

                    is ResultState.Success -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                successMessage = result.data,
                                cartItems = emptyList() // Clear cart after successful sale
                            )
                        }
                        // Refresh inventory list after successful sale
                        loadInventory(sessionManagement.getShopId())
                        // Also refresh sales history after successful sale
                        loadSalesHistory(sessionManagement.getShopId())
                    }

                    is ResultState.Failure -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Sale failed: ${result.message.localizedMessage}"
                            )
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun addItemToCart(item: SaleRecordItem) {
        val currentCartItems = _state.value.cartItems.toMutableList()
        val existingItemIndex = currentCartItems.indexOfFirst { it.productId == item.productId }

        if (existingItemIndex != -1) {
            // Item already exists in cart, replace it
            currentCartItems[existingItemIndex] = item
        } else {
            // Add new item to cart
            currentCartItems.add(item)
        }

        Log.d("CHKINF", "ITEM: ${item}")
        Log.d("CHKINF", "VM: ${state.value.cartItems}")

        _state.update {
            it.copy(
                cartItems = currentCartItems,
                showSuccessMessage = true,
                // Reset sales entry fields
                quantitySold = "",
                discount = "",
                isPercentageDiscount = true
            )
        }
        Log.d("CHKINF", "Cart after update: ${state.value.cartItems}")
    }

    private fun fetchCategories() {
        val shopId = sessionManagement.getShopId()
        if (shopId != null) {
            viewModelScope.launch {
                repository.fetchCategories(shopId).collect { result ->
                    when (result) {
                        is ResultState.Loading -> {
                            _state.update { it.copy(isLoading = true) }
                        }

                        is ResultState.Success -> {
                            _state.update {
                                it.copy(
                                    categories = result.data.map { category -> category.categoryName },
                                    isLoading = false
                                )
                            }
                        }

                        is ResultState.Failure -> {
                            _state.update {
                                it.copy(
                                    errorMessage = result.message.localizedMessage
                                        ?: "Failed to fetch categories",
                                    isLoading = false
                                )
                            }
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    private fun updateSortOrder(sortOrder: SortOrder) {
        _state.update { it.copy(currentSortOrder = sortOrder) }
        applyFilters()
        // Also apply filters for sales history
        applySalesHistoryFilters()
    }

    private fun filterByCategory(category: String?) {
        _state.update { it.copy(selectedCategory = category) }
        applyFilters()
        // Also apply filters for sales history
        applySalesHistoryFilters()
    }

    private fun loadInventory(shopId: String?) {
        viewModelScope.launch {
            repository.getAllInventoryItems(shopId).collect { result ->
                when (result) {
                    is ResultState.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }

                    is ResultState.Success -> {
                        _state.update {
                            it.copy(
                                inventoryItems = result.data,
                                filteredItems = result.data,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                        applyFilters()
                    }

                    is ResultState.Failure -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Failed to load inventory: ${result.message.localizedMessage}"
                            )
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun applyFilters() {
        val currentState = _state.value
        var filteredItems = currentState.inventoryItems

        // Apply search query filter
        if (currentState.searchQuery.isNotEmpty()) {
            filteredItems = filteredItems.filter { item ->
                item.name.contains(currentState.searchQuery, ignoreCase = true)
            }
        }

        // Apply category filter
        if (currentState.selectedCategory != null) {
            filteredItems = filteredItems.filter { item ->
                item.category == currentState.selectedCategory
            }
        }

        // Apply sorting
        filteredItems = when (currentState.currentSortOrder) {
            SortOrder.NEWEST_FIRST -> filteredItems.sortedByDescending { it.lastUpdated }
            SortOrder.OLDEST_FIRST -> filteredItems.sortedBy { it.lastUpdated }
            SortOrder.QUANTITY_LOW_TO_HIGH -> filteredItems.sortedBy { it.quantity }
            SortOrder.QUANTITY_HIGH_TO_LOW -> filteredItems.sortedByDescending { it.quantity }
            SortOrder.PRICE_LOW_TO_HIGH -> filteredItems.sortedBy { it.selling_price }
            SortOrder.PRICE_HIGH_TO_LOW -> filteredItems.sortedByDescending { it.selling_price }
        }

        // Update the state with the filtered items
        _state.update { it.copy(filteredItems = filteredItems) }
    }

    private fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
        // Apply filters immediately when search query changes
        applyFilters()
        // Also apply filters for sales history
        applySalesHistoryFilters()
    }

    // Added SalesHistory methods
    private fun loadSalesHistory(shopId: String?) {
        viewModelScope.launch {
            if (shopId == null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Shop ID is null"
                    )
                }
                return@launch
            }

            _state.update { it.copy(isLoading = true) }

            repository.getSalesRecords(shopId).collect { result ->
                when (result) {
                    is ResultState.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }

                    is ResultState.Success -> {
                        _state.update {
                            it.copy(
                                salesRecords = result.data,
                                filteredRecords = result.data,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                        applySalesHistoryFilters()
                    }

                    is ResultState.Failure -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Failed to load sales history: ${result.message.localizedMessage}"
                            )
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun filterByStatus(status: String?) {
        _state.update { it.copy(selectedStatus = status) }
        applySalesHistoryFilters()
    }

    private fun filterByDateRange(rangeFilter: DateRangeFilter) {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time

        when (rangeFilter) {
            DateRangeFilter.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.time

                _state.update {
                    it.copy(
                        startDate = startDate,
                        endDate = endDate,
                        dateRangeFilter = rangeFilter
                    )
                }
            }

            DateRangeFilter.WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.time

                _state.update {
                    it.copy(
                        startDate = startDate,
                        endDate = endDate,
                        dateRangeFilter = rangeFilter
                    )
                }
            }

            DateRangeFilter.MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.time

                _state.update {
                    it.copy(
                        startDate = startDate,
                        endDate = endDate,
                        dateRangeFilter = rangeFilter
                    )
                }
            }

            DateRangeFilter.YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.time

                _state.update {
                    it.copy(
                        startDate = startDate,
                        endDate = endDate,
                        dateRangeFilter = rangeFilter
                    )
                }
            }

            DateRangeFilter.ALL -> {
                _state.update {
                    it.copy(
                        startDate = null,
                        endDate = null,
                        dateRangeFilter = rangeFilter
                    )
                }
            }

            DateRangeFilter.CUSTOM -> {
                // Do nothing here, as custom dates are set via SetCustomDateRange event
                _state.update { it.copy(dateRangeFilter = rangeFilter) }
            }
        }

        applySalesHistoryFilters()
    }

    // In SalesPageViewModel.kt, in the setCustomDateRange function:
    private fun setCustomDateRange(startDate: Date, endDate: Date) {
        // Adjust end date to the end of the day
        val adjustedEndDate = Calendar.getInstance().apply {
            time = endDate
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time

        _state.update {
            it.copy(
                startDate = startDate,
                endDate = adjustedEndDate,
                dateRangeFilter = DateRangeFilter.CUSTOM
            )
        }
        applySalesHistoryFilters()
    }

    private fun applySalesHistoryFilters() {
        val currentState = _state.value
        var filteredRecords = currentState.salesRecords

        if (currentState.startDate != null && currentState.endDate != null) {
            filteredRecords = filteredRecords.filter { record ->
                val recordTimestamp = record.lastUpdated.toLong()
                val recordDate = Date(recordTimestamp)

                // Fix: Use after OR equal for start date, before OR equal for end date
                (recordDate.after(currentState.startDate) || recordDate.time == currentState.startDate.time) &&
                        (recordDate.before(currentState.endDate) || recordDate.time == currentState.endDate.time)
            }
        }

        // Apply search query filter
        if (currentState.searchQuery.isNotEmpty()) {
            filteredRecords = filteredRecords.filter { record ->
                record.salesRecordItem.any { item ->
                    item.productName.contains(currentState.searchQuery, ignoreCase = true) ||
                            item.sku.contains(currentState.searchQuery, ignoreCase = true)
                }
            }
        }

        // Apply category filter
        if (currentState.selectedCategory != null) {
            filteredRecords = filteredRecords.filter { record ->
                record.salesRecordItem.any { item ->
                    item.category == currentState.selectedCategory
                }
            }
        }

        // Apply status filter
        if (currentState.selectedStatus != null) {
            filteredRecords = filteredRecords.filter { record ->
                record.status == currentState.selectedStatus
            }
        }

        // Apply date range filter
        if (currentState.startDate != null && currentState.endDate != null) {
            filteredRecords = filteredRecords.filter { record ->
                val recordTimestamp = record.lastUpdated.toLong()
                val recordDate = Date(recordTimestamp)
                recordDate.after(currentState.startDate) && recordDate.before(currentState.endDate)
            }
        }

        // Apply sorting
        filteredRecords = when (currentState.currentSortOrder) {
            SortOrder.NEWEST_FIRST -> filteredRecords.sortedByDescending { it.lastUpdated }
            SortOrder.OLDEST_FIRST -> filteredRecords.sortedBy { it.lastUpdated }
            else -> filteredRecords.sortedByDescending { it.lastUpdated }
        }

        // Update the state with filtered records
        _state.update { it.copy(filteredRecords = filteredRecords) }
    }
}