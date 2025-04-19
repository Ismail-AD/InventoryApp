package com.appdev.inventoryapp.ui.Screens.DisplayInventory


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdev.inventoryapp.Utils.ResultState
import com.appdev.inventoryapp.Utils.SessionManagement
import com.appdev.inventoryapp.Utils.SortOrder
import com.appdev.inventoryapp.domain.model.InventoryItem
import com.appdev.inventoryapp.domain.repository.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repository: InventoryRepository,
    val sessionManagement: SessionManagement
) : ViewModel() {
    private val _state = MutableStateFlow(InventoryState(isLoading = true))
    val state: StateFlow<InventoryState> = _state.asStateFlow()

    init {
//        loadInventory(sessionManagement.getShopId())
        fetchCategories()
    }
    fun handleEvent(event: InventoryEvent) {
        when (event) {
            is InventoryEvent.RefreshInventory -> loadInventory(sessionManagement.getShopId())
            is InventoryEvent.SearchQueryChanged -> updateSearchQuery(event.query)
            is InventoryEvent.DeleteItem -> deleteItem(event.item)
            is InventoryEvent.DismissError -> dismissError()
            is InventoryEvent.LoadInventory -> {
                loadInventory(sessionManagement.getShopId())
            }
            is InventoryEvent.UpdateSortOrder -> {
                updateSortOrder(event.sortOrder)
                _state.update { it.copy(isSortMenuExpanded = false) }
            }
            is InventoryEvent.FilterByCategory -> {
                filterByCategory(event.category)
                _state.update { it.copy(isCategoryMenuExpanded = false) }
            }
            is InventoryEvent.FetchCategories -> fetchCategories()
            is InventoryEvent.ToggleSortMenu -> {
                _state.update { it.copy(isSortMenuExpanded = event.isExpanded) }
            }
            is InventoryEvent.ToggleCategoryMenu -> {
                _state.update { it.copy(isCategoryMenuExpanded = event.isExpanded) }
            }
            is InventoryEvent.ShowDeleteConfirmation -> {
                _state.update { it.copy(
                    showDeleteConfirmation = true,
                    itemToDelete = event.item
                ) }
            }
            is InventoryEvent.HideDeleteConfirmation -> {
                _state.update { it.copy(
                    showDeleteConfirmation = false,
                    itemToDelete = null
                ) }
            }
            is InventoryEvent.ConfirmDelete -> {
                _state.value.itemToDelete?.let { item ->
                    deleteItem(item)
                }
                _state.update { it.copy(
                    showDeleteConfirmation = false,
                    itemToDelete = null
                ) }
            }
            is InventoryEvent.DismissDeleteSuccess -> {
                _state.update { it.copy(showDeleteSuccessMessage = false) }
            }

            else -> {}
        }
    }

    private fun fetchCategories() {
        val shopId = sessionManagement.getShopId()
        if (shopId != null) {
            viewModelScope.launch {
                repository.fetchCategories(shopId)
                    .collect { result ->
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

    private fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    private fun updateSortOrder(sortOrder: SortOrder) {
        _state.update { it.copy(currentSortOrder = sortOrder) }
        applyFilters()
    }

    private fun filterByCategory(category: String?) {
        _state.update { it.copy(selectedCategory = category) }
        applyFilters()
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


    private fun deleteItem(item: InventoryItem) {
        viewModelScope.launch {
            repository.deleteInventoryItem(item.id, item.shop_id, item.creator_id)
                .collect { result ->
                    when (result) {
                        is ResultState.Loading -> {
                            _state.update { it.copy(isLoading = true) }
                        }
                        is ResultState.Success -> {
                            // Refresh the inventory after successful deletion
                            loadInventory(sessionManagement.getShopId())
                            // Show success message
                            _state.update { it.copy(
                                showDeleteSuccessMessage = true,
                                isLoading = false
                            ) }
                        }
                        is ResultState.Failure -> {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "Failed to delete item: ${result.message.localizedMessage}"
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
                item.name.contains(currentState.searchQuery, ignoreCase = true) ||
                        item.sku.contains(currentState.searchQuery, ignoreCase = true)
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

        _state.update { it.copy(filteredItems = filteredItems) }
    }

}