package com.typeassist.app

import android.content.Context
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
import com.google.gson.Gson
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

        loadCachedUpdateInfo()
        checkForUpdates()

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
                TypeAssistApp(client, updateInfo = updateInfoState)
                
                updateInfoState?.let { update ->
                    UpdateDialog(info = update, onDismiss = { updateInfoState = null })
                }
            }
        }
    }

    private fun getCurrentVersionCode(): Int {
        return try { packageManager.getPackageInfo(packageName, 0).versionCode } catch (e: Exception) { 0 }
    }

    private fun loadCachedUpdateInfo() {
        val prefs = getSharedPreferences("UpdateInfo", Context.MODE_PRIVATE)
        val json = prefs.getString("update_json", null)
        if (json != null) {
            try {
                val info = Gson().fromJson(json, UpdateInfo::class.java)
                if (info.versionCode > getCurrentVersionCode()) {
                    updateInfoState = info
                }
            } catch (e: Exception) {
                // Ignore parsing errors
            }
        }
    }
    
    private fun checkForUpdates() {
        lifecycleScope.launch {
            val remoteInfo = UpdateManager.checkForUpdates(this@MainActivity)
            val prefs = getSharedPreferences("UpdateInfo", Context.MODE_PRIVATE)
            val currentVersion = getCurrentVersionCode()
            
            if (remoteInfo != null) {
                if (remoteInfo.versionCode > currentVersion) {
                    // Valid new update found
                    prefs.edit().putString("update_json", Gson().toJson(remoteInfo)).apply()
                    updateInfoState = remoteInfo
                } else {
                    // Remote version is same or older (e.g. user updated, or server downgrade)
                    // Force clear cache and hide dialog
                    prefs.edit().remove("update_json").apply()
                    updateInfoState = null
                }
            } 
            // If remoteInfo is null (network error), we do nothing and let the cached info (loaded in onCreate) persist if it exists.
        }
    }
    
    fun isAccessibilityEnabled(): Boolean {
        val prefString = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return prefString?.contains("$packageName/com.typeassist.app.service.MyAccessibilityService") == true
    }
}