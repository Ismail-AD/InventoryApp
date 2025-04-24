package com.appdev.inventoryapp

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
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


//        enableEdgeToEdge()
        lifecycleScope.launch {
            val isSessionValid = refreshSessionIfNeeded()

            Log.d("SESSIONINFO","${isSessionValid}")
            // Only attempt to get userRole and userId if the session is valid
            val userType = if(isSessionValid) sessionManagement.getUserRole() else null
            val userId = if(isSessionValid) sessionManagement.getUserId() else null
            Log.d("SESSIONINFO","${userType} & id ${userId}")

            setContent {
                InventoryAppTheme {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        NavGraph(userType, userId, isSessionValid)
                    }
                }
            }
        }
    }




    private suspend fun refreshSessionIfNeeded(): Boolean {

        // Are both local stores totally empty? -> no session
        val prefsToken   = sessionManagement.getAccessToken()
        val supaSession  = supabaseClient.auth.currentSessionOrNull()
        if (prefsToken == null && supaSession == null) return false

        // mismatch -> wipe everything
        if (prefsToken == null && supaSession != null) {
            supabaseClient.auth.signOut()
            return false
        }

        // from here prefsToken is not null
        if (!sessionManagement.isSessionValid()) {
            val refresh = sessionManagement.getRefreshToken() ?: return false
            return try {
                val session = supabaseClient.auth.refreshSession(refresh)
                sessionManagement.saveSession(
                    session.accessToken,
                    session.refreshToken,
                    session.expiresAt.epochSeconds,
                    session.user!!.id,
                    session.user!!.email ?: ""
                )
                true
            } catch (e: Exception) {
                supabaseClient.auth.signOut()
                sessionManagement.clearSession()
                false
            }
        }

        return true           // prefs say it is still valid
    }



}


