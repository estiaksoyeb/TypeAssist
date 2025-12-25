package com.typeassist.app

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.typeassist.app.ui.TypeAssistApp
import com.typeassist.app.ui.components.UpdateDialog
import com.typeassist.app.utils.UpdateInfo
import com.typeassist.app.utils.UpdateManager
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {
    
    private val client = OkHttpClient()
    private var updateInfoState by mutableStateOf<UpdateInfo?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navBarColor = Color(0xFFF3F4F6)
        window.navigationBarColor = navBarColor.toArgb()
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightNavigationBars = true

        // Use the modularized UpdateManager
        lifecycleScope.launch {
            updateInfoState = UpdateManager.checkForUpdates(this@MainActivity)
        }

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF4F46E5),
                    onPrimary = Color.White,
                    secondary = Color(0xFF10B981),
                    background = navBarColor,
                    surface = Color.White
                )
            ) {
                TypeAssistApp(client)
                
                // Show the modularized UpdateDialog
                updateInfoState?.let { update ->
                    UpdateDialog(info = update, onDismiss = { updateInfoState = null })
                }
            }
        }
    }
    
    fun isAccessibilityEnabled(): Boolean {
        val prefString = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return prefString?.contains("$packageName/com.typeassist.app.service.MyAccessibilityService") == true
    }
}