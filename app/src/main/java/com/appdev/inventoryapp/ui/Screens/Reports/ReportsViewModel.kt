package com.appdev.inventoryapp.ui.Screens.Reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdev.inventoryapp.Utils.ResultState
import com.appdev.inventoryapp.Utils.SessionManagement
import com.appdev.inventoryapp.domain.model.InventoryItem
import com.appdev.inventoryapp.domain.model.SalesRecord
import com.appdev.inventoryapp.domain.repository.InventoryRepository
import com.appdev.inventoryapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val userRepository: UserRepository,
    val sessionManagement: SessionManagement
) : ViewModel() {

    private val _state = MutableStateFlow(ReportState())
    val state: StateFlow<ReportState> = _state


    fun handleEvent(event: ReportEvent) {
        when (event) {
            is ReportEvent.ShowStartDatePicker -> {
                _state.update { it.copy(showStartDatePicker = true) }
            }

            is ReportEvent.HideStartDatePicker -> {
                _state.update { it.copy(showStartDatePicker = false) }
            }

            is ReportEvent.ShowEndDatePicker -> {
                _state.update { it.copy(showEndDatePicker = true) }
            }

            is ReportEvent.HideEndDatePicker -> {
                _state.update { it.copy(showEndDatePicker = false) }
            }

            is ReportEvent.TempStartDateSelected -> {
                _state.update { it.copy(tempStartDate = event.date) }
            }

            is ReportEvent.TempEndDateSelected -> {
                _state.update { it.copy(tempEndDate = event.date) }
            }


            is ReportEvent.CancelCustomDateRange -> {
                _state.update {
                    it.copy(
                        tempStartDate = null,
                        tempEndDate = null
                    )
                }
            }

            is ReportEvent.LoadReports -> {
                sessionManagement.getShopId()?.let { loadBaseData(it) }
            }
            is ReportEvent.DismissError -> {
                _state.update { it.copy(errorMessage = null) }
            }
            is ReportEvent.ShowDateRangePicker -> {
                _state.update { it.copy(isDateRangePickerVisible = true) }
            }
            is ReportEvent.HideDateRangePicker -> {
                _state.update { it.copy(isDateRangePickerVisible = false) }
            }

            is ReportEvent.ToggleCategoryFilterMenu -> {
                _state.update { it.copy(isCategoryFilterExpanded = !it.isCategoryFilterExpanded) }
            }

            is ReportEvent.FilterByCategory -> {
                _state.update { it.copy(
                    selectedCategory = event.category,
                    isCategoryFilterExpanded = false // Close the dropdown
                ) }
                applyFiltersAndUpdateReports()
            }

            is ReportEvent.FilterBySalesperson -> {
                _state.update { it.copy(
                    selectedSalesperson = event.salesperson,
                    isSalespersonFilterExpanded = false // Close the dropdown
                ) }
                applyFiltersAndUpdateReports()
            }

            is ReportEvent.UpdateDateRange -> {
                _state.update {
                    it.copy(
                        isDateRangePickerVisible = false, // Close the date picker
                        startDate = event.startDate,
                        endDate = event.endDate,
                        tempStartDate = null,
                        tempEndDate = null
                    )
                }
                applyFiltersAndUpdateReports()
            }

            is ReportEvent.ToggleSalespersonFilterMenu -> {
                _state.update { it.copy(isSalespersonFilterExpanded = !it.isSalespersonFilterExpanded) }
            }
            is ReportEvent.ResetFilters -> {
                _state.update {
                    it.copy(
                        startDate = LocalDate.now().minusMonths(1),
                        endDate = LocalDate.now(),
                        selectedCategory = null,
                        selectedSalesperson = null
                    )
                }
                applyFiltersAndUpdateReports()
            }
        }
    }
    private fun fetchUsers(shopId: String) {
        val userId = sessionManagement.getUserId()
        if (userId != null) {
            viewModelScope.launch {
                userRepository.getAllUsers(shopId, userId).collect { result ->
                    when (result) {
                        is ResultState.Success -> {
                            val users = result.data
                            val salesPersonList = users.map { person->person.username }
                            _state.update {
                                it.copy(
                                    salespeople = salesPersonList
                                )
                            }
                            loadAllSalesRecords(shopId)
                        }
                        is ResultState.Failure -> {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "Failed to fetch users: ${result.message.localizedMessage}"
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }


    private fun fetchCategories() {
        val shopId = sessionManagement.getShopId()
        if (shopId != null) {
            viewModelScope.launch {
                inventoryRepository.fetchCategories(shopId).collect { result ->
                    when (result) {
                        is ResultState.Loading -> {
                            _state.update { it.copy(isLoading = true) }
                        }
                        is ResultState.Success -> {
                            _state.update {
                                it.copy(
                                    categories = result.data.map { category -> category.categoryName },
                                )
                            }
                            fetchUsers(shopId)
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

    /**
     * Loads all base data needed for reports: inventory items and all sales records.
     * After loading, filters will be applied based on current state.
     */
    private fun loadBaseData(shopId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Load all inventory items
            inventoryRepository.getAllInventoryItems(shopId).collect { result ->
                when (result) {
                    is ResultState.Success -> {
                        val inventoryItems = result.data

                        // Update inventory items in state
                        _state.update { currentState ->
                            currentState.copy(
                                inventoryItems = inventoryItems,
                            )
                        }
                        fetchCategories()

                    }
                    is ResultState.Failure -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Failed to load inventory: ${result.message.localizedMessage}"
                            )
                        }
                    }
                    is ResultState.Loading -> {
                        // Already handled
                    }
                }
            }
        }
    }

    /**
     * Loads all sales records from repository and then applies filters
     */
    private fun loadAllSalesRecords(shopId: String) {
        viewModelScope.launch {
            inventoryRepository.getSalesRecords(shopId).collect { result ->
                when (result) {
                    is ResultState.Success -> {
                        val allSalesRecords = result.data

                        // Update sales records in state
                        _state.update { currentState ->
                            currentState.copy(
                                salesRecords = allSalesRecords,
                                salespeople = allSalesRecords.map { it.creator_name }.distinct()
                            )
                        }

                        // Apply filters to the loaded data
                        applyFiltersAndUpdateReports()
                    }
                    is ResultState.Failure -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Failed to load sales records: ${result.message.localizedMessage}"
                            )
                        }
                    }
                    is ResultState.Loading -> {
                        // Already handled
                    }
                }
            }
        }
    }

    /**
     * Applies all current filters to sales records and updates reports
     */
    private fun applyFiltersAndUpdateReports() {
        val currentState = _state.value

        if (currentState.salesRecords.isEmpty()) {
            _state.update {
                it.copy(
                    isLoading = false,
                    filteredSalesRecords = emptyList()
                )
            }
            return
        }

        // Apply date range filter
        val startEpochMilli = currentState.startDate.atStartOfDay()
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endEpochMilli = currentState.endDate.atStartOfDay()
            .plusDays(1).minusSeconds(1)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        var filteredRecords = currentState.salesRecords.filter {
            val timestamp = it.lastUpdated.toLong()
            timestamp in startEpochMilli..endEpochMilli
        }

        // Apply category filter if selected
        if (currentState.selectedCategory != null) {
            filteredRecords = filteredRecords.filter { record ->
                record.salesRecordItem.any { item ->
                    item.category == currentState.selectedCategory
                }
            }
        }

        // Apply salesperson filter if selected
        if (currentState.selectedSalesperson != null) {
            filteredRecords = filteredRecords.filter { record ->
                record.creator_name == currentState.selectedSalesperson
            }
        }

        // Update filtered records in state
        _state.update {
            it.copy(
                filteredSalesRecords = filteredRecords,
                isLoading = false
            )
        }

        // Process the filtered data to update reports
        processSalesData(filteredRecords, currentState.inventoryItems)
    }

    private fun processSalesData(salesRecords: List<SalesRecord>, inventoryItems: List<InventoryItem>) {
        // Calculate total revenue
        val totalRevenue = salesRecords.flatMap { it.salesRecordItem }
            .sumOf { saleItem ->
                val price = saleItem.selling_price
                val quantity = saleItem.quantity
                val discount = calculateDiscount(price, saleItem.discountAmount, saleItem.isPercentageDiscount)
                (price - discount) * quantity
            }

        // Calculate total profit
        val totalProfit = salesRecords.flatMap { it.salesRecordItem }
            .sumOf { saleItem ->
                val sellingPrice = saleItem.selling_price
                val discount = calculateDiscount(sellingPrice, saleItem.discountAmount, saleItem.isPercentageDiscount)
                val costPrice = inventoryItems.find { it.id == saleItem.productId }?.cost_price ?: 0.0
                val itemProfit = (sellingPrice - discount - costPrice) * saleItem.quantity
                maxOf(itemProfit, 0.0) // Ensure profit is never negative
            }

        // Count number of sales transactions
        val numberOfSales = salesRecords.size

        // Create category breakdown
        val categoryBreakdown = salesRecords.flatMap { it.salesRecordItem }
            .groupBy { it.category }
            .mapValues { (_, items) ->
                items.sumOf { saleItem ->
                    val price = saleItem.selling_price
                    val discount = calculateDiscount(price, saleItem.discountAmount, saleItem.isPercentageDiscount)
                    (price - discount) * saleItem.quantity
                }
            }
            .filter { it.value > 0 } // Only include categories with positive values

        // Create sales trend (by date)
        val salesTrend = salesRecords
            .groupBy { record ->
                // Group by day using lastUpdated timestamp
                val timestamp = record.lastUpdated.toLong()
                val localDate = java.time.Instant.ofEpochMilli(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                localDate.toString()
            }
            .mapValues { (_, records) ->
                records.flatMap { it.salesRecordItem }
                    .sumOf { saleItem ->
                        val price = saleItem.selling_price
                        val discount = calculateDiscount(price, saleItem.discountAmount, saleItem.isPercentageDiscount)
                        (price - discount) * saleItem.quantity
                    }
            }

        // Create top products list
        val productSales = salesRecords.flatMap { it.salesRecordItem }
            .groupBy { it.productName }
            .mapValues { (_, items) -> items.sumOf { it.quantity } }
            .toList()
            .sortedByDescending { it.second }
            .take(5) // Top 5 products

        // Update state with all calculated data
        _state.update { currentState ->
            currentState.copy(
                totalRevenue = totalRevenue,
                totalProfit = totalProfit,
                numberOfSales = numberOfSales,
                categoryBreakdown = categoryBreakdown,
                salesTrend = salesTrend,
                topProducts = productSales
            )
        }
    }

    private fun calculateDiscount(price: Double, discountAmount: Float, isPercentage: Boolean): Double {
        return if (isPercentage) {
            price * (discountAmount / 100f)
        } else {
            discountAmount.toDouble()
        }
    }
}