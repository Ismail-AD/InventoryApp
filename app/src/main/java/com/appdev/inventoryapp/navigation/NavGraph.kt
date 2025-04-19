package com.appdev.inventoryapp.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.appdev.inventoryapp.ui.Screens.Login.LoginScreen
import com.appdev.inventoryapp.ui.Screens.Main.MainScreen
import com.appdev.inventoryapp.ui.Screens.SignUp.SignupScreen


@Composable
fun NavGraph(userType: String?, userId: String?, isSessionValid: Boolean) {
    val controller = rememberNavController()
    val initialRoute = if (userId != null && userType != null && isSessionValid) {
        when (userType) {
            "Admin", "Edit User", "View Only" -> Routes.Home.route
            else -> Routes.Login.route
        }
    } else {
        Routes.Login.route
    }

    if (initialRoute != null) {
        NavHost(
            navController = controller,
            startDestination = initialRoute
        ) {
            composable(route = Routes.Login.route) {
                LoginScreen(navigateToHome = {
                    controller.navigate(Routes.Home.route) {
                        // Clear backstack so user can't go back to login after successful auth
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                }) {
                    controller.navigate(Routes.Register.route)
                }
            }
            composable(route = Routes.Register.route) {
                SignupScreen(navigateToHome = {
                    controller.navigate(Routes.Home.route) {
                        // Clear backstack so user can't go back to register after successful signup
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                }) {
                    controller.popBackStack()
                }
            }
            composable(route = Routes.Home.route) {
                MainScreen(onLogout = {
                    // When logout is triggered from MainScreen, navigate back to Login
                    controller.navigate(Routes.Login.route) {
                        // Clear the entire back stack
                        popUpTo(0) { inclusive = true }
                    }
                })
            }
        }
    }
}