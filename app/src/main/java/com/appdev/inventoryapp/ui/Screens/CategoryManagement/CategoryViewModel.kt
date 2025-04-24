package com.appdev.inventoryapp.ui.Screens.Categories

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdev.inventoryapp.Utils.ResultState
import com.appdev.inventoryapp.Utils.SessionManagement
import com.appdev.inventoryapp.domain.model.Category
import com.appdev.inventoryapp.domain.model.InventoryItem
import com.appdev.inventoryapp.domain.repository.InventoryRepository
import com.appdev.inventoryapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val userRepository: UserRepository,
    private val sessionManagement: SessionManagement
) : ViewModel() {

    private val _state = MutableStateFlow(CategoryState())
    val state: StateFlow<CategoryState> = _state.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Try to get shopId from session
            var shopId = sessionManagement.getShopId()

            // If shopId is null, try to fetch it from the repository
            if (shopId == null) {
                val userId = userRepository.getCurrentUserId() ?: sessionManagement.getUserId()
                if (userId != null) {
                    userRepository.getShopIdByUserId(userId).collectLatest { result ->
                        when (result) {
                            is ResultState.Success -> {
                                // Save the shop ID to session and continue
                                shopId = result.data
                                sessionManagement.saveShop_Id(shopId ?: "")
                                fetchCategoriesWithShopId(shopId)
                            }

                            is ResultState.Failure -> {
                                _state.update {
                                    it.copy(
                                        errorMessage = "Failed to get shop ID: ${result.message.localizedMessage}",
                                        isLoading = false
                                    )
                                }
                            }

                            is ResultState.Loading -> {
                                // Already showing loading state
                            }
                        }
                    }
                } else {
                    _state.update {
                        it.copy(
                            errorMessage = "User ID not found. Please log in again.",
                            isLoading = false
                        )
                    }
                }
            } else {
                // We already have the shopId, proceed with fetching categories
                fetchCategoriesWithShopId(shopId)
            }
        }
    }

    // Helper function to fetch categories once we have a valid shopId
    private fun fetchCategoriesWithShopId(shopId: String?) {
        viewModelScope.launch {
            if (shopId != null) {
                inventoryRepository.fetchCategories(shopId).collectLatest { result ->
                    when (result) {
                        is ResultState.Success -> {
                            _state.update {
                                it.copy(
                                    categories = result.data,
                                    isLoading = false
                                )
                            }
                        }

                        is ResultState.Failure -> {
                            _state.update {
                                it.copy(
                                    errorMessage = "Failed to load categories: ${result.message.localizedMessage}",
                                    isLoading = false
                                )
                            }
                        }

                        is ResultState.Loading -> {
                            // Already showing loading state
                        }
                    }
                }
            } else {
                _state.update {
                    it.copy(
                        errorMessage = "Shop ID not found. Please log in again.",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun handleEvent(event: CategoryEvent) {
        when (event) {
            // Existing events
            is CategoryEvent.LoadCategories -> {
                loadCategories()
            }

            is CategoryEvent.CategoryNameChanged -> {
                _state.update {
                    it.copy(
                        newCategoryName = event.name,
                        categoryNameError = validateCategoryName(event.name)
                    )
                }
            }

            is CategoryEvent.OpenEditCategoryDialog -> {
                _state.update {
                    it.copy(
                        isEditingCategory = true,
                        categoryToEdit = event.category,
                        newCategoryName = event.category.categoryName,
                        categoryNameError = null
                    )
                }
            }

            is CategoryEvent.CloseEditCategoryDialog -> {
                _state.update {
                    it.copy(
                        isEditingCategory = false,
                        categoryToEdit = null,
                        newCategoryName = "",
                        categoryNameError = null
                    )
                }
            }

            is CategoryEvent.UpdateCategory -> {
                updateCategory()
            }

            is CategoryEvent.DismissError -> {
                _state.update { it.copy(errorMessage = null) }
            }

            is CategoryEvent.DismissSuccessMessage -> {
                _state.update { it.copy(showSuccessMessage = false) }
            }

            is CategoryEvent.OpenDeleteConfirmDialog -> {
                _state.update {
                    it.copy(
                        showDeleteConfirmDialog = true,
                        categoryToDelete = event.category
                    )
                }
            }

            is CategoryEvent.CloseDeleteConfirmDialog -> {
                _state.update {
                    it.copy(
                        showDeleteConfirmDialog = false,
                        categoryToDelete = null
                    )
                }
            }

            is CategoryEvent.DeleteCategory -> {
                checkCategoryUsageAndDelete()
            }

            is CategoryEvent.DismissDeleteErrorDialog -> {
                _state.update { it.copy(showDeleteErrorDialog = false) }
            }

            // New handlers for adding category
            is CategoryEvent.OpenAddCategoryDialog -> {
                _state.update {
                    it.copy(
                        isAddingCategory = true,
                        newCategoryName = "",
                        categoryNameError = null
                    )
                }
            }

            is CategoryEvent.CloseAddCategoryDialog -> {
                _state.update {
                    it.copy(
                        isAddingCategory = false,
                        newCategoryName = "",
                        categoryNameError = null
                    )
                }
            }

            is CategoryEvent.AddCategory -> {
                addNewCategory()
            }

            else -> {}
        }
    }

    private fun updateCategory() {
        viewModelScope.launch {
            val categoryToEdit = _state.value.categoryToEdit ?: return@launch
            val newName = _state.value.newCategoryName.trim()

            if (validateCategoryName(newName) != null) {
                return@launch
            }
            // Check for duplicate category names
            val isDuplicate = _state.value.categories.any {
                it.categoryName.equals(newName, ignoreCase = true)
            }

            if (isDuplicate) {
                _state.update {
                    it.copy(
                        categoryNameError = "Category with this name already exists",
                        isLoading = false
                    )
                }
                return@launch
            }
            _state.update { it.copy(isLoading = true) }

            val updatedCategory = categoryToEdit.copy(categoryName = newName)

            // This is a placeholder - you would need to implement updateCategory in your repository
            inventoryRepository.updateCategory(updatedCategory).collectLatest { result ->
                when (result) {
                    is ResultState.Success -> {
                        _state.update {
                            it.copy(
                                isEditingCategory = false,
                                categoryToEdit = null,
                                newCategoryName = "",
                                isLoading = false,
                                showSuccessMessage = true,
                                successMessage = "Category updated successfully"
                            )
                        }
                        loadCategories() // Reload categories to get the updated list

                    }

                    is ResultState.Failure -> {
                        _state.update {
                            it.copy(
                                errorMessage = "Failed to update category: ${result.message.localizedMessage}",
                                isLoading = false
                            )
                        }
                    }

                    is ResultState.Loading -> {
                        // Already showing loading state
                    }
                }
            }
        }
    }

    private fun checkCategoryUsageAndDelete() {
        viewModelScope.launch {
            val categoryToDelete = _state.value.categoryToDelete ?: return@launch
            val shopId = sessionManagement.getShopId() ?: return@launch

            _state.update { it.copy(isLoading = true) }

            // First, check if there are products using this category
            inventoryRepository.getAllInventoryItems(shopId).collectLatest { result ->
                when (result) {
                    is ResultState.Success -> {
                        val itemsWithCategory = result.data.filter { item ->
                            item.category_id == categoryToDelete.id
                        }

                        if (itemsWithCategory.size > 1) {
                            // Multiple products using this category, show error
                            _state.update {
                                it.copy(
                                    showDeleteConfirmDialog = false,
                                    showDeleteErrorDialog = true,
                                    deleteErrorMessage = "Cannot delete category '${categoryToDelete.categoryName}' because it is used by ${itemsWithCategory.size} products.",
                                    isLoading = false
                                )
                            }
                        } else if (itemsWithCategory.size == 1) {
                            // Single product, show different error
                            _state.update {
                                it.copy(
                                    showDeleteConfirmDialog = false,
                                    showDeleteErrorDialog = true,
                                    deleteErrorMessage = "Cannot delete category '${categoryToDelete.categoryName}' because it is used by one product. Update the product's category first.",
                                    isLoading = false
                                )
                            }
                        } else {
                            // No products using this category, proceed with deletion
                            deleteCategory(categoryToDelete)
                        }
                    }

                    is ResultState.Failure -> {
                        _state.update {
                            it.copy(
                                errorMessage = "Failed to check category usage: ${result.message.localizedMessage}",
                                showDeleteConfirmDialog = false,
                                isLoading = false
                            )
                        }
                    }

                    is ResultState.Loading -> {
                        // Already showing loading state
                    }
                }
            }
        }
    }

    private fun addNewCategory() {
        viewModelScope.launch {
            val newName = _state.value.newCategoryName.trim()

            // Basic validation
            if (validateCategoryName(newName) != null) {
                return@launch
            }

            // Check for duplicate category names
            val isDuplicate = _state.value.categories.any {
                it.categoryName.equals(newName, ignoreCase = true)
            }

            if (isDuplicate) {
                _state.update {
                    it.copy(
                        categoryNameError = "Category with this name already exists",
                        isLoading = false
                    )
                }
                return@launch
            }

            _state.update { it.copy(isLoading = true) }

            val shopId = sessionManagement.getShopId() ?: return@launch

            // Create a new category object
            val newCategory = Category(
                categoryName = newName,
                shop_id = shopId
            )

            inventoryRepository.addCategory(newCategory).collectLatest { result ->
                when (result) {
                    is ResultState.Success -> {
                        _state.update {
                            it.copy(
                                isAddingCategory = false,
                                newCategoryName = "",
                                isLoading = false,
                                showSuccessMessage = true,
                                successMessage = "Category added successfully"
                            )
                        }
                        loadCategories() // Reload categories to get the updated list
                    }

                    is ResultState.Failure -> {
                        _state.update {
                            it.copy(
                                errorMessage = "Failed to add category: ${result.message.localizedMessage}",
                                isLoading = false
                            )
                        }
                    }

                    is ResultState.Loading -> {
                        // Already showing loading state
                    }
                }
            }
        }
    }

    private suspend fun deleteCategory(category: Category) {
        inventoryRepository.deleteCategory(category.id, category.shop_id).collectLatest { result ->
            when (result) {
                is ResultState.Success -> {
                    _state.update {
                        it.copy(
                            showDeleteConfirmDialog = false,
                            categoryToDelete = null,
                            isLoading = false,
                            showSuccessMessage = true,
                            successMessage = "Category deleted successfully"
                        )
                    }
                    loadCategories() // Reload categories to get the updated list

                }

                is ResultState.Failure -> {
                    _state.update {
                        it.copy(
                            errorMessage = "Failed to delete category: ${result.message.localizedMessage}",
                            showDeleteConfirmDialog = false,
                            isLoading = false
                        )
                    }
                }

                is ResultState.Loading -> {
                    // Already showing loading state
                }
            }
        }
    }

    private fun validateCategoryName(name: String): String? {
        return when {
            name.isEmpty() -> "Category name cannot be empty"
            name.length > 20 -> "Category name must be 20 characters or less"
            else -> null
        }
    }
}