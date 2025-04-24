package com.appdev.inventoryapp.ui.Screens.Categories

import com.appdev.inventoryapp.domain.model.Category

sealed class CategoryEvent {
    object LoadCategories : CategoryEvent()
    data class CategoryNameChanged(val name: String) : CategoryEvent()
    data class OpenEditCategoryDialog(val category: Category) : CategoryEvent()
    object CloseEditCategoryDialog : CategoryEvent()
    object UpdateCategory : CategoryEvent()
    object DismissError : CategoryEvent()
    object DismissSuccessMessage : CategoryEvent()
    data class OpenDeleteConfirmDialog(val category: Category) : CategoryEvent()
    object CloseDeleteConfirmDialog : CategoryEvent()
    object DeleteCategory : CategoryEvent()
    object DismissDeleteErrorDialog : CategoryEvent()
    object OpenAddCategoryDialog : CategoryEvent()
    object CloseAddCategoryDialog : CategoryEvent()
    object AddCategory : CategoryEvent()
}