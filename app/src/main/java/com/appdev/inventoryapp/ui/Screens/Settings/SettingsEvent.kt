package com.appdev.inventoryapp.ui.Screens.Settings

sealed class SettingsEvent {
    data class ShopNameChanged(val name: String) : SettingsEvent()
    data class UserNameChanged(val name: String) : SettingsEvent()
    object ToggleLowStockNotification : SettingsEvent()
    object RequestNotificationPermission : SettingsEvent()
    object OpenShopNameDialog : SettingsEvent()
    object CloseShopNameDialog : SettingsEvent()
    object OpenUserNameDialog : SettingsEvent()
    object CloseUserNameDialog : SettingsEvent()
    object UpdateShopName : SettingsEvent()
    object UpdateUserName : SettingsEvent()
    object DismissError : SettingsEvent()
    object DismissSuccessMessage : SettingsEvent()
    object ShowNotificationGuide : SettingsEvent()
    object DismissNotificationGuide : SettingsEvent()
    object ShowLogoutConfirmDialog : SettingsEvent()
    object HideLogoutConfirmDialog : SettingsEvent()
    object Logout : SettingsEvent()
}