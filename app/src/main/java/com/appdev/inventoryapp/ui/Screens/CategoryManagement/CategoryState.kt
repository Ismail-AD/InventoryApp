package com.appdev.inventoryapp.ui.Screens.Categories

import com.appdev.inventoryapp.domain.model.Category

data class CategoryState(
    val categories: List<Category> = emptyList(),
    val shopId:String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showSuccessMessage: Boolean = false,
    val successMessage: String = "",
    val isEditingCategory: Boolean = false,
    val categoryToEdit: Category? = null,
    val newCategoryName: String = "",
    val categoryNameError: String? = null,
    val showDeleteConfirmDialog: Boolean = false,
    val categoryToDelete: Category? = null,
    val showDeleteErrorDialog: Boolean = false,
    val deleteErrorMessage: String = "",
    val isAddingCategory: Boolean = false  // New field for add dialog
)