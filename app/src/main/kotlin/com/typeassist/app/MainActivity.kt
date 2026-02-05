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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.typeassist.app.ui.AppTheme
import com.typeassist.app.ui.TypeAssistApp
import com.typeassist.app.ui.components.UpdateDialog
import com.typeassist.app.data.model.GitHubRelease
import com.typeassist.app.data.repository.UpdateRepository
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {
    
    private val client = OkHttpClient()
    private var updateInfoState by mutableStateOf<GitHubRelease?>(null)
    private lateinit var updateRepository: UpdateRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        updateRepository = UpdateRepository(this)

        loadCachedUpdateInfo()
        checkForUpdates()

        setContent {
            AppTheme {
                TypeAssistApp(client, updateInfo = updateInfoState)
                
                updateInfoState?.let { update ->
                    UpdateDialog(release = update, onDismiss = { updateInfoState = null })
                }
            }
        }
    }

    private fun loadCachedUpdateInfo() {
        val prefs = getSharedPreferences("UpdateInfo", Context.MODE_PRIVATE)
        
        // Migration: Clear old update info key if it exists
        if (prefs.contains("update_json")) {
            prefs.edit().remove("update_json").apply()
        }

        val json = prefs.getString("github_release_json", null)
        if (json != null) {
            try {
                val info = Gson().fromJson(json, GitHubRelease::class.java)
                // Safety: Ensure critical fields aren't null (Gson bypasses Kotlin nullability)
                if (info.tagName != null && info.htmlUrl != null) {
                    updateInfoState = info
                }
            } catch (e: Exception) {
                prefs.edit().remove("github_release_json").apply()
            }
        }
    }
    
    private fun checkForUpdates() {
        lifecycleScope.launch {
            val result = updateRepository.checkForUpdate("estiaksoyeb", "TypeAssist")
            val prefs = getSharedPreferences("UpdateInfo", Context.MODE_PRIVATE)
            
            result.onSuccess { release ->
                if (release != null) {
                    // Valid new update found
                    prefs.edit().putString("github_release_json", Gson().toJson(release)).apply()
                    updateInfoState = release
                } else {
                    // No update available
                    prefs.edit().remove("github_release_json").apply()
                    updateInfoState = null
                }
            }.onFailure {
                // Keep cached info if network fails
            }
        }
    }
    
    fun isAccessibilityEnabled(): Boolean {
        val prefString = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return prefString?.contains("$packageName/com.typeassist.app.service.MyAccessibilityService") == true
    }
}