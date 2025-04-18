package com.appdev.inventoryapp.ui.Screens.Main

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SupervisedUserCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.appdev.inventoryapp.domain.model.BottomNavItem
import com.appdev.inventoryapp.domain.model.InventoryItem
import com.appdev.inventoryapp.navigation.Routes
import com.appdev.inventoryapp.ui.Screens.CartSummary.CartSummaryScreen
import com.appdev.inventoryapp.ui.Screens.Inventory.InventoryScreen
import com.appdev.inventoryapp.ui.Screens.InventoryManagemnt.AddInventoryItemRoot
import com.appdev.inventoryapp.ui.Screens.InventoryManagemnt.AddInventoryItemViewModel
import com.appdev.inventoryapp.ui.Screens.ProductDetails.ProductDetailScreen
import com.appdev.inventoryapp.ui.Screens.Reports.ReportsScreen
import com.appdev.inventoryapp.ui.Screens.SalesEntry.SalesEntryScreen
import com.appdev.inventoryapp.ui.Screens.SalesHistory.SalesHistoryScreen
import com.appdev.inventoryapp.ui.Screens.SalesPage.SalesPageScreen
import com.appdev.inventoryapp.ui.Screens.SalesPage.SalesPageViewModel
import com.appdev.inventoryapp.ui.Screens.Settings.SettingsScreen
import com.appdev.inventoryapp.ui.Screens.UserManagement.UsersManagementScreen
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    val hideBottomBarRoutes = listOf(
        Routes.AddProduct.route + "/{productJson}?",
        Routes.ProductDetail.route + "/{productJson}",
        Routes.SalesEntry.route + "/{productJson}",
        Routes.CartSummary.route,
        Routes.ProductSearch.route,
    )
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // Define navigation items with icons
    val bottomNavItems = listOf(
        BottomNavItem(
            route = Routes.InventoryManagement.route,
            icon = { Icon(Icons.Default.Inventory, contentDescription = "Inventory") },
            label = "Inventory"
        ),
        BottomNavItem(
            route = Routes.SalesHistory.route,
            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Sales") },
            label = "Sales"
        ),
        BottomNavItem(
            route = Routes.ReportsAndAnalytics.route,
            icon = { Icon(Icons.Default.BarChart, contentDescription = "Reports") },
            label = "Reports"
        ),
        BottomNavItem(
            route = Routes.UserManagement.route,
            icon = { Icon(Icons.Default.SupervisedUserCircle, contentDescription = "More") },
            label = "Staff"
        ),
        BottomNavItem(
            route = Routes.Settings.route,
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = "Settings"
        )

    )

    Scaffold(
        bottomBar = {
            if (currentRoute !in hideBottomBarRoutes) {
                BottomNavigation(navController, bottomNavItems)
            }
        }
    ) { innerPadding ->

        val salesViewModel: SalesPageViewModel = hiltViewModel()
        NavHost(
            navController = navController,
            startDestination = Routes.InventoryManagement.route,
            modifier = Modifier.padding(innerPadding)
        ) {

            composable(Routes.InventoryManagement.route) {
                InventoryScreen(navigateToAddItem = {
                    navController.navigate(Routes.AddProduct.route + "/{}")
                }, navigateToItemDetail = {
                    val productJson = Uri.encode(Json.encodeToString(it))
                    navController.navigate(Routes.ProductDetail.route + "/$productJson")
                }) {
                    navController.navigate(
                        Routes.AddProduct.route + "/${
                            Uri.encode(
                                Json.encodeToString(
                                    it
                                )
                            )
                        }"
                    )
                }
            }
            composable(Routes.ProductSearch.route) {
                SalesPageScreen(viewModel = salesViewModel, navigateToProductDetail = {
                    val productJson = Uri.encode(Json.encodeToString(it))
                    navController.navigate(Routes.SalesEntry.route + "/$productJson")
                }, navigateToCartSummary = {
                    navController.navigate(Routes.CartSummary.route)

                }) {
                    navController.navigateUp()
                }
            }
            composable(
                Routes.CartSummary.route
            ) { backStackEntry ->
                CartSummaryScreen(
                    viewModel = salesViewModel,
                    navigateBack = { navController.navigateUp() },
                    navigateToSalesHistory = {
                        navController.navigate(Routes.SalesHistory.route) {
                            popUpTo(Routes.CartSummary.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.SalesHistory.route) {
                SalesHistoryScreen(viewModel = salesViewModel) {
                    navController.navigate(Routes.ProductSearch.route)
                }
            }

            composable(Routes.SalesEntry.route + "/{productJson}", arguments = listOf(
                navArgument("productJson") {
                    type = NavType.StringType
                }
            )) { backStackEntry ->


                val productJson = backStackEntry.arguments?.getString("productJson") ?: ""
                val decodedProduct = try {
                    Json.decodeFromString<InventoryItem>(Uri.decode(productJson))
                } catch (e: Exception) {
                    null
                }
                decodedProduct?.let { product ->
                    SalesEntryScreen(viewModel = salesViewModel, product = product) {
                        navController.navigateUp()
                    }
                }
            }

            composable(Routes.AddProduct.route + "/{productJson}?",
                arguments = listOf(
                    navArgument("productJson") {
                        type = NavType.StringType
                        nullable = true
                    }
                )
            ) { backStackEntry ->
                val viewModel: AddInventoryItemViewModel = hiltViewModel()
                val productJson = backStackEntry.arguments?.getString("productJson")
                val decodedProduct = productJson?.let {
                    try {
                        Json.decodeFromString<InventoryItem>(Uri.decode(productJson))
                    } catch (e: Exception) {
                        null
                    }
                }
                if (decodedProduct != null) {
                    viewModel.initializeWithProduct(decodedProduct)
                }
                AddInventoryItemRoot(
                    navigateBack = { navController.navigateUp() }
                )
            }
            composable(Routes.ReportsAndAnalytics.route) {
                ReportsScreen()
            }
            composable(Routes.UserManagement.route) {
                UsersManagementScreen { }
            }
            composable(Routes.Settings.route) {
                SettingsScreen{
                    navController.navigate(Routes.Login.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                }
            }
            composable(
                route = Routes.ProductDetail.route + "/{productJson}",
                arguments = listOf(
                    navArgument("productJson") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val productJson = backStackEntry.arguments?.getString("productJson") ?: ""
                val decodedProduct = try {
                    Json.decodeFromString<InventoryItem>(Uri.decode(productJson))
                } catch (e: Exception) {
                    null
                }

                if (decodedProduct != null) {
                    ProductDetailScreen(
                        product = decodedProduct,
                        onBackPressed = {
                            navController.navigateUp()
                        }
                    )
                }
            }
//
//            // Nested routes for the More section
//            composable("settings") {
//                SettingsScreen(navController = navController)
//            }
//            composable("user_management") {
//                UserManagementScreen(navController = navController)
//            }
//            composable("profile") {
//                ProfileScreen(navController = navController)
//            }
//            composable("invoices") {
//                InvoiceScreen(navController = navController)
//            }
//            composable("customers") {
//                CustomerScreen(navController = navController)
//            }
        }
    }
}

// Bottom Navigation Bar composable
@Composable
fun BottomNavigation(
    navController: NavController,
    items: List<BottomNavItem>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
        items.forEach { item ->
            NavigationBarItem(
                icon = { item.icon() },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                }, colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary, // Change icon color when selected
                    selectedTextColor = MaterialTheme.colorScheme.primary, // Change text color when selected
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.inversePrimary,
                    unselectedTextColor = MaterialTheme.colorScheme.inversePrimary,
                )
            )
        }
    }
}
