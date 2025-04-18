package com.appdev.inventoryapp

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.appdev.inventoryapp.Utils.NotificationPreferenceManager
import com.appdev.inventoryapp.Utils.SessionManagement
import com.appdev.inventoryapp.Utils.StockCheckWorker
import com.appdev.inventoryapp.navigation.NavGraph
import com.appdev.inventoryapp.ui.theme.InventoryAppTheme
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var sessionManagement: SessionManagement

    @Inject
    lateinit var notificationPreferenceManager: NotificationPreferenceManager

    @Inject
    lateinit var supabaseClient: SupabaseClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userType = sessionManagement.getUserRole()
        val userId = sessionManagement.getUserId()


//        enableEdgeToEdge()
        checkNotificationPermission()
        lifecycleScope.launch {
            val isSessionValid = refreshSessionIfNeeded()
            setContent {
                InventoryAppTheme {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        NavGraph(userType, userId, isSessionValid)
                    }
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted, schedule worker
                    scheduleStockCheck()
                }
            }
        } else {

            scheduleStockCheck()
        }
    }

    private fun scheduleStockCheck() {
        if (notificationPreferenceManager.isLowStockNotificationEnabled()) {
            sessionManagement.getShopId()?.let { shopID ->
                StockCheckWorker.schedulePeriodicWork(this, shopID)
            }
        }
    }

    private suspend fun refreshSessionIfNeeded(): Boolean {
        if (!sessionManagement.isSessionValid() && sessionManagement.getRefreshToken() != null) {
            try {
                val getRefreshToken = sessionManagement.getRefreshToken()

                val session =
                    supabaseClient.auth.refreshSession(getRefreshToken!!)

                sessionManagement.saveSession(
                    accessToken = session.accessToken,
                    refreshToken = session.refreshToken,
                    expiresAt = session.expiresAt.epochSeconds,
                    userId = session.user!!.id,
                    userEmail = session.user!!.email ?: ""
                )
                return true
            } catch (e: Exception) {
                sessionManagement.clearSession()
                return false
            }
        }
        return sessionManagement.isSessionValid()
    }

}


