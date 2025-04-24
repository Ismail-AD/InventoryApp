package com.appdev.inventoryapp.navigation

sealed class Routes(val route: String) {
    // Authentication
    data object Login : Routes("login_screen")
    data object Register : Routes("register_screen")
    data object ForgotPassword : Routes("forgot_password_screen")

    // Main Screens
    data object Home : Routes("home_dashboard_screen")

    // Inventory Management
    data object InventoryManagement : Routes("inventory_management_screen")
    data object AddProduct : Routes("add_product_screen")
    data object EditProduct : Routes("edit_product_screen")
    data object ProductDetail : Routes("product_detail_screen")

    // Sales
    data object SalesEntry : Routes("sales_entry_screen")
    data object SalesHistory : Routes("sales_history_screen")
    data object ProductSearch : Routes("product_search_screen")
    data object CartSummary : Routes("cart_summary_screen")
    data object CategoryManagement : Routes("categories_screen")

    // Reports and Analytics
    data object ReportsAndAnalytics : Routes("reports_and_analytics_screen")
    data object RevenueReport : Routes("revenue_report_screen")
    data object InventoryReport : Routes("inventory_report_screen")
    data object SalesReport : Routes("sales_report_screen")

    // User Management
    data object UserManagement : Routes("user_management_screen")
    data object AddUser : Routes("add_user_screen")
    data object EditUser : Routes("edit_user_screen")
    data object UserPermissions : Routes("user_permissions_screen")

    // Settings
    data object Settings : Routes("settings_screen")
    data object StorageSettings : Routes("storage_settings_screen")
    data object AppSettings : Routes("app_settings_screen")
    data object PrinterSettings : Routes("printer_settings_screen")

    // Additional Screens
    data object Notifications : Routes("notifications_screen")
    data object Profile : Routes("profile_screen")
    data object Backup : Routes("backup_screen")
}