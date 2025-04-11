package com.appdev.inventoryapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.appdev.inventoryapp.Utils.SessionManagement
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
    lateinit var supabaseClient: SupabaseClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userType = sessionManagement.getUserRole()
        val userId = sessionManagement.getUserId()

//        enableEdgeToEdge()
        lifecycleScope.launch {
            val isSessionValid = refreshSessionIfNeeded()
            setContent {
                InventoryAppTheme {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        NavGraph(userType, userId,isSessionValid)
                    }
                }
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


