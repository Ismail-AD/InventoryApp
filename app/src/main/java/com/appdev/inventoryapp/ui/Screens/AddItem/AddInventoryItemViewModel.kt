package com.appdev.inventoryapp.ui.Screens.InventoryManagemnt

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdev.inventoryapp.Utils.AppPreferencesManager
import com.appdev.inventoryapp.Utils.ResultState
import com.appdev.inventoryapp.Utils.SessionManagement
import com.appdev.inventoryapp.domain.model.Category
import com.appdev.inventoryapp.domain.model.InventoryItem
import com.appdev.inventoryapp.domain.repository.InventoryRepository
import com.appdev.inventoryapp.ui.Screens.AddItem.AddInventoryItemEvent
import com.appdev.inventoryapp.ui.Screens.AddItem.AddInventoryItemState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddInventoryItemViewModel @Inject constructor(
    private val repository: InventoryRepository,
    val sessionManagement: SessionManagement,
    val appPreferencesManager: AppPreferencesManager
) : ViewModel() {


    private val _state = MutableStateFlow(AddInventoryItemState())
    val state: StateFlow<AddInventoryItemState> = _state.asStateFlow()

    init {
        fetchCategories()
        _state.update {
            it.copy(dontShowConfirmationAgain = appPreferencesManager.shouldSkipInventoryConfirmation())
        }
    }

    fun initializeWithProduct(product: InventoryItem) {
        _state.update {
            it.copy(
                itemId = product.id,
                shop_id = product.shop_id,
                creator_id = product.creator_id,
                name = product.name,
                quantity = product.quantity.toString(),
                costPrice = product.cost_price.toString(),
                sellingPrice = product.selling_price.toString(),
                category = product.category,
                sku = product.sku,
                taxes = product.taxes.toString(),
                initialImageUrls = product.imageUrls,
                imageUris = product.imageUrls.map { url -> Uri.parse(url) }
            )
        }
    }

    fun handleEvent(event: AddInventoryItemEvent) {
        when (event) {
            is AddInventoryItemEvent.NameChanged -> {
                _state.update { it.copy(name = event.name) }
            }

            is AddInventoryItemEvent.QuantityChanged -> {
                if (event.quantity.isEmpty() || event.quantity.all { it.isDigit() }) {
                    _state.update { it.copy(quantity = event.quantity) }
                }
            }

            is AddInventoryItemEvent.CostPriceChanged -> {
                if (event.price.isEmpty() || event.price.matches(Regex("^\\d*\\.?\\d*$"))) {
                    _state.update { it.copy(costPrice = event.price) }
                }
            }

            is AddInventoryItemEvent.SellingPriceChanged -> {
                if (event.price.isEmpty() || event.price.matches(Regex("^\\d*\\.?\\d*$"))) {
                    _state.update { it.copy(sellingPrice = event.price) }
                }
            }

            is AddInventoryItemEvent.ImageSelected -> {
                val currentUris = _state.value.imageUris.toMutableList()
                if (event.index < currentUris.size) {
                    currentUris[event.index] = event.uri
                } else {
                    currentUris.add(event.uri)
                }
                _state.update { it.copy(imageUris = currentUris) }
            }

            is AddInventoryItemEvent.CategoryChanged -> {
                _state.update { it.copy(category = event.category) }
            }

            is AddInventoryItemEvent.FetchCategories -> fetchCategories()


            is AddInventoryItemEvent.CategoriesFetchFailed -> {
                _state.update {
                    it.copy(
                        categories = emptyList(),
                        categoriesLoading = false,
                        categoriesLoadError = event.errorMessage
                    )
                }
            }

            is AddInventoryItemEvent.SaveNewCategory -> saveNewCategory()

            is AddInventoryItemEvent.NewCategoryAdded -> {
                _state.update {
                    it.copy(
                        categories = it.categories + event.category,
                        category = event.category,
                        newCategoryDialogVisible = false,
                        newCategoryName = "",
                        newCategoryAddError = null
                    )
                }
            }

            is AddInventoryItemEvent.NewCategoryAddFailed -> {
                _state.update {
                    it.copy(
                        newCategoryAddError = event.errorMessage
                    )
                }
            }

            is AddInventoryItemEvent.SKUChanged -> {
                _state.update { it.copy(sku = event.sku) }
            }

            is AddInventoryItemEvent.TaxesChanged -> {
                if (event.taxes.isEmpty() || event.taxes.matches(Regex("^\\d*\\.?\\d*$"))) {
                    _state.update { it.copy(taxes = event.taxes) }
                }
            }

            is AddInventoryItemEvent.ShowConfirmationModal -> {
                _state.update { it.copy(showConfirmationModal = true) }
            }

            is AddInventoryItemEvent.DismissConfirmationModal -> {
                _state.update { it.copy(showConfirmationModal = false) }
            }

            is AddInventoryItemEvent.ConfirmSubmit -> {
                _state.update { it.copy(showConfirmationModal = false) }
                submitItem(state.value.listOfImageByteArrays)
            }


            is AddInventoryItemEvent.RemoveImage -> {
                val currentUris = _state.value.imageUris.toMutableList()


                // Remove the image at the specified index
                if (event.index < currentUris.size) {
                    currentUris.removeAt(event.index)
                    _state.update { it.copy(imageUris = currentUris) }
                }

            }

            is AddInventoryItemEvent.SubmitItem -> {
                _state.update { it.copy(listOfImageByteArrays = event.listOfImageByteArrays) }

                // Validate before showing confirmation or submitting
                val validationError = validateItemSubmission()
                if (validationError != null) {
                    // Show error message if validation fails
                    _state.update { it.copy(errorMessage = validationError) }
                } else if (appPreferencesManager.shouldSkipInventoryConfirmation()) {
                    submitItem(event.listOfImageByteArrays)
                } else {
                    _state.update { it.copy(showConfirmationModal = true) }
                }
            }

            is AddInventoryItemEvent.DismissError -> {
                _state.update {
                    it.copy(
                        errorMessage = null,
                        isSuccess = null,
                        newCategoryAddedMessage = null
                    )
                }
            }

            is AddInventoryItemEvent.ToggleCategoryDropdown -> {
                if (_state.value.categories.isEmpty()) {
                    _state.update { it.copy(errorMessage = "Please add a category first!") }
                } else {
                    _state.update {
                        it.copy(categoryDropdownExpanded = !it.categoryDropdownExpanded)
                    }
                }
            }

            is AddInventoryItemEvent.ShowNewCategoryDialog -> {
                _state.update {
                    it.copy(
                        newCategoryDialogVisible = true,
                        newCategoryName = ""
                    )
                }
            }

            is AddInventoryItemEvent.DismissNewCategoryDialog -> {
                _state.update {
                    it.copy(
                        newCategoryDialogVisible = false,
                        newCategoryName = ""
                    )
                }
            }

            is AddInventoryItemEvent.NewCategoryNameChanged -> {
                _state.update {
                    it.copy(newCategoryName = event.name)
                }
            }

            is AddInventoryItemEvent.SetDontShowConfirmationAgain -> {
                _state.update { it.copy(dontShowConfirmationAgain = event.dontShow) }
                appPreferencesManager.setSkipInventoryConfirmation(event.dontShow)
            }

            else -> {

            }
        }
    }

    private fun saveNewCategory() {
        val newCategoryName = state.value.newCategoryName
        val shopId = sessionManagement.getShopId()

        if (newCategoryName.isNotBlank() && shopId != null) {
            // Check if category already exists
            val isDuplicate =
                state.value.categories.any { it.equals(newCategoryName, ignoreCase = true) }

            if (!isDuplicate) {
                // Create new category object
                val newCategory = Category(
                    categoryName = newCategoryName,
                    shop_id = shopId
                )

                viewModelScope.launch {
                    repository.addCategory(newCategory)
                        .collect { result ->
                            when (result) {
                                is ResultState.Loading -> {
                                    _state.update {
                                        it.copy(isLoading = true)
                                    }
                                }

                                is ResultState.Success -> {
                                    handleEvent(
                                        AddInventoryItemEvent.NewCategoryAdded(
                                            newCategoryName
                                        )
                                    )
                                    _state.update {
                                        it.copy(
                                            newCategoryDialogVisible = false,
                                            newCategoryName = "",
                                            newCategoryAddedMessage = "Category Created",
                                            isLoading = false,
                                        )
                                    }
                                }

                                is ResultState.Failure -> {
                                    _state.update {
                                        it.copy(
                                            errorMessage = result.message.localizedMessage
                                                ?: "Failed to add category",
                                            newCategoryDialogVisible = false,
                                            isLoading = false
                                        )
                                    }
                                }
                            }
                        }
                }
            } else {
                // Handle duplicate category
                _state.update {
                    it.copy(
                        errorMessage = "Category already exists",
                        newCategoryDialogVisible = false
                    )
                }
            }
        } else {
            // Handle invalid input
            _state.update {
                it.copy(
                    errorMessage = "Category name cannot be empty",
                    newCategoryDialogVisible = false
                )
            }
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
                        }
                    }
            }
        }
    }

    private fun validateItemSubmission(): String? {
        val currentState = _state.value

        return when {
            currentState.name.isBlank() -> "Please enter a product name"
            currentState.quantity.isBlank() -> "Please enter quantity"
            currentState.costPrice.isBlank() -> "Please enter cost price"
            currentState.sellingPrice.isBlank() -> "Please enter selling price"
            currentState.category.isBlank() -> "Please select a category"
            currentState.sku.isBlank() -> "Please enter SKU"
            currentState.taxes.isBlank() -> "Please enter taxes"
            _state.value.imageUris.isEmpty() -> "Please add at least one image"
            else -> null
        }
    }


    private fun submitItem(listOfImageByteArrays: List<ByteArray?>) {
        val currentState = _state.value

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }


            if (currentState.itemId != null) {
                // Update existing item
                val updatedItem = InventoryItem(
                    id = currentState.itemId,
                    shop_id = currentState.shop_id, // Replace with actual shop ID
                    creator_id = currentState.creator_id, // Replace with actual user ID
                    name = currentState.name,
                    quantity = currentState.quantity.toInt(),
                    cost_price = currentState.costPrice.toDouble(),
                    selling_price = currentState.sellingPrice.toDouble(),
                    lastUpdated = System.currentTimeMillis().toString(),
                    imageUrls = currentState.initialImageUrls,
                    category = currentState.category,
                    sku = currentState.sku,
                    taxes = if (currentState.taxes.isNotEmpty()) currentState.taxes.toDoubleOrNull()
                        ?: 0.0 else 0.0
                )

                updateInventoryItem(updatedItem, listOfImageByteArrays, currentState.imageUris)
            } else {
                // Create new item
                val newItem = InventoryItem(
                    shop_id = sessionManagement.getShopId() ?: "", // Replace with actual shop ID
                    creator_id = sessionManagement.getUserId() ?: "", // Replace with actual user ID
                    name = currentState.name,
                    quantity = currentState.quantity.toInt(),
                    cost_price = currentState.costPrice.toDouble(),
                    selling_price = currentState.sellingPrice.toDouble(),
                    lastUpdated = System.currentTimeMillis().toString(),
                    imageUrls = emptyList(),
                    category = currentState.category,
                    sku = currentState.sku,
                    taxes = if (currentState.taxes.isNotEmpty()) currentState.taxes.toDoubleOrNull()
                        ?: 0.0 else 0.0
                )

                addInventoryItem(newItem, listOfImageByteArrays, currentState.imageUris)
            }
        }
    }

    private fun addInventoryItem(
        item: InventoryItem, imageByteArrays: List<ByteArray?>,
        imageUris: List<Uri>
    ) {
        viewModelScope.launch {
            repository.addInventoryItem(item, imageByteArrays, imageUris).collect { result ->
                when (result) {
                    is ResultState.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }

                    is ResultState.Success -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isSuccess = "Product Created",
                                errorMessage = null
                            )
                        }
                    }

                    is ResultState.Failure -> {
                        _state.update {
                            it.copy(
                                errorMessage = "Failed to add item: ${result.message.localizedMessage}",
                                isLoading = false
                            )
                        }
                    }
                }
            }
        }
    }

    private fun updateInventoryItem(
        item: InventoryItem, imageByteArrays: List<ByteArray?>,
        imageUris: List<Uri>
    ) {
        viewModelScope.launch {
            repository.updateInventoryItem(item, imageByteArrays, imageUris).collect { result ->
                when (result) {
                    is ResultState.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }

                    is ResultState.Success -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isSuccess = "Product Updated",
                                errorMessage = null
                            )
                        }
                    }

                    is ResultState.Failure -> {
                        _state.update {
                            it.copy(
                                errorMessage = "Failed to update item: ${result.message.localizedMessage}",
                                isLoading = false
                            )
                        }
                    }
                }
            }
        }
    }
}