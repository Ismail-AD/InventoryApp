package com.appdev.inventoryapp.domain.model

import androidx.compose.runtime.Composable

data class BottomNavItem(
    val route: String,
    val icon: @Composable () -> Unit,
    val label: String
)