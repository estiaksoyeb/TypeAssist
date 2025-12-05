package com.typeassist.app.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.gson.GsonBuilder
import com.typeassist.app.MainActivity
import com.typeassist.app.data.AppConfig
import com.typeassist.app.data.createDefaultConfig
import com.typeassist.app.ui.screens.*
import okhttp3.OkHttpClient

@Composable
fun TypeAssistApp(client: OkHttpClient) {
    var currentScreen by remember { mutableStateOf("home") }
    val context = LocalContext.current
    val gson = GsonBuilder().setPrettyPrinting().create()
    val prefs = context.getSharedPreferences("GeminiConfig", Context.MODE_PRIVATE)
    
    var config by remember { 
        mutableStateOf(try {
            val json = prefs.getString("config_json", null)
            if (json != null) gson.fromJson(json, AppConfig::class.java) else createDefaultConfig()
        } catch (e: Exception) { createDefaultConfig() })
    }

    fun saveConfig(newConfig: AppConfig) {
        config = newConfig
        prefs.edit().putString("config_json", gson.toJson(newConfig)).apply()
    }

    BackHandler(enabled = currentScreen != "home") {
        currentScreen = "home"
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (currentScreen) {
            "home" -> HomeScreen(
                config = config,
                context = context,
                onToggle = { newState -> 
                    val activity = context as MainActivity
                    if (newState) {
                        if (!activity.isAccessibilityEnabled()) {
                            Toast.makeText(context, "⚠️ Please Enable Accessibility Service first", Toast.LENGTH_SHORT).show()
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            return@HomeScreen
                        }
                        if (config.apiKey.isBlank()) {
                            Toast.makeText(context, "⚠️ Please setup API Key first", Toast.LENGTH_SHORT).show()
                            currentScreen = "settings"
                            return@HomeScreen
                        }
                    }
                    saveConfig(config.copy(isAppEnabled = newState)) 
                },
                onNavigate = { currentScreen = it }
            )
            "commands" -> CommandsScreen(config, { saveConfig(it) }, { currentScreen = "home" })
            "settings" -> SettingsScreen(
                config = config,
                client = client,
                onSave = { saveConfig(it) },
                onBack = { currentScreen = "home" }
            )
            "json" -> JsonScreen(config, { saveConfig(it) }, { currentScreen = "home" })
            "test" -> TestScreen(
                onStartTest = { prefs.edit().putBoolean("is_testing_active", true).apply() },
                onStopTest = { prefs.edit().putBoolean("is_testing_active", false).apply() },
                onBack = { currentScreen = "home" }
            )
        }
    }
}
