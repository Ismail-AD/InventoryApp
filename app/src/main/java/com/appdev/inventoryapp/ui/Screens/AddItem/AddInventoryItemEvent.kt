package com.appdev.inventoryapp.ui.Screens.AddItem

import android.net.Uri

sealed class AddInventoryItemEvent {
    data class NameChanged(val name: String) : AddInventoryItemEvent()
    data class QuantityChanged(val quantity: String) : AddInventoryItemEvent()
    data class CostPriceChanged(val price: String) : AddInventoryItemEvent()
    data class SellingPriceChanged(val price: String) : AddInventoryItemEvent()
    data class ImageSelected(val uri: Uri, val index: Int) : AddInventoryItemEvent()
    data class RemoveImage(val index: Int) : AddInventoryItemEvent()
    data class SubmitItem(val listOfImageByteArrays: List<ByteArray?>) : AddInventoryItemEvent()
    data class CategoryChanged(val category: String) : AddInventoryItemEvent()
    data class SKUChanged(val sku: String) : AddInventoryItemEvent()
    data class TaxesChanged(val taxes: String) : AddInventoryItemEvent()
    data object ShowConfirmationModal : AddInventoryItemEvent()
    data object DismissConfirmationModal : AddInventoryItemEvent()
    data object ConfirmSubmit : AddInventoryItemEvent()
    data class SetDontShowConfirmationAgain(val dontShow: Boolean) : AddInventoryItemEvent()
    data object DismissError : AddInventoryItemEvent()
    data object NavigateBack : AddInventoryItemEvent()



    data object FetchCategories : AddInventoryItemEvent()
    data class CategoriesFetchFailed(val errorMessage: String) : AddInventoryItemEvent()
    data object ToggleCategoryDropdown : AddInventoryItemEvent()
    data object ShowNewCategoryDialog : AddInventoryItemEvent()
    data object DismissNewCategoryDialog : AddInventoryItemEvent()
    data class NewCategoryNameChanged(val name: String) : AddInventoryItemEvent()
    data object SaveNewCategory : AddInventoryItemEvent()
    data class NewCategoryAdded(val category: String) : AddInventoryItemEvent()
    data class NewCategoryAddFailed(val errorMessage: String) : AddInventoryItemEvent()

}