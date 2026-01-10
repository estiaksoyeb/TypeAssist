package com.typeassist.app.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.gson.GsonBuilder
import com.typeassist.app.MainActivity
import com.typeassist.app.data.AppConfig
import com.typeassist.app.data.createDefaultConfig
import com.typeassist.app.ui.screens.*
import com.typeassist.app.utils.UpdateInfo
import okhttp3.OkHttpClient

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TypeAssistApp(client: OkHttpClient, updateInfo: UpdateInfo?) {
    val context = LocalContext.current
    val gson = GsonBuilder().setPrettyPrinting().create()
    val prefs = context.getSharedPreferences("GeminiConfig", Context.MODE_PRIVATE)

    // Determine initial screen
    val hasSeenOnboarding = prefs.getBoolean("has_seen_onboarding", false)
    var currentScreen by rememberSaveable { mutableStateOf(if (hasSeenOnboarding) "home" else "welcome") }
    var previousScreen by rememberSaveable { mutableStateOf("home") } // Track previous screen for animation

    var config by remember(currentScreen) { 
        mutableStateOf(try {
            val json = prefs.getString("config_json", null)
            if (json != null) gson.fromJson(json, AppConfig::class.java) else createDefaultConfig()
        } catch (e: Exception) { createDefaultConfig() })
    }

    fun saveConfig(newConfig: AppConfig) {
        config = newConfig
        prefs.edit().putString("config_json", gson.toJson(newConfig)).apply()
    }

    // Effect to handle forced updates
    LaunchedEffect(updateInfo) {
        if (updateInfo?.forceUpdate == true && config.isAppEnabled) {
            saveConfig(config.copy(isAppEnabled = false))
        }
    }

    // Custom navigate function to track previous screen
    val navigateTo: (String) -> Unit = { screen ->
        previousScreen = currentScreen
        currentScreen = screen
    }

    BackHandler(enabled = currentScreen != "home" && currentScreen != "welcome") {
        navigateTo("home") // Use the custom navigate function for back press
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AnimatedContent(
            targetState = currentScreen,
            label = "Screen Animation",
            transitionSpec = {
                if (targetState == "home" && previousScreen != "home") { // Navigating back to home
                    slideInHorizontally { fullWidth -> -fullWidth } togetherWith // New screen from left
                    slideOutHorizontally { fullWidth -> fullWidth } // Old screen to right
                } else { // Navigating forward or within sub-screens not back to home
                    slideInHorizontally { fullWidth -> fullWidth } togetherWith // New screen from right
                    slideOutHorizontally { fullWidth -> -fullWidth } // Old screen to left
                }
            }
        ) { screen ->
            when (screen) {
                "welcome" -> WelcomeScreen(
                    onFinished = {
                        prefs.edit().putBoolean("has_seen_onboarding", true).apply()
                        navigateTo("home")
                    }
                )
                "home" -> HomeScreen(
                    config = config,
                    context = context,
                    updateInfo = updateInfo, // Pass updateInfo down
                    onToggle = { newState -> 
                        saveConfig(config.copy(isAppEnabled = newState)) 
                    },
                    onNavigate = { navigateTo(it) } // Use custom navigate
                )
                "commands" -> CommandsScreen(config, { saveConfig(it) }, { navigateTo("home") }) // Use custom navigate
                "settings" -> SettingsScreen(
                    config = config,
                    client = client,
                    onSave = { saveConfig(it) },
                    onBack = { navigateTo("home") } // Use custom navigate
                )
                "json" -> JsonScreen(config, { saveConfig(it) }, { navigateTo("home") }) // Use custom navigate
                "history" -> HistoryScreen({ navigateTo("home") }) // Use custom navigate
                "snippets" -> SnippetsScreen(config, { saveConfig(it) }, { navigateTo("home") })
                "guide" -> GuideScreen({ navigateTo("home") })
                "test" -> TestScreen(
                    onStartTest = { prefs.edit().putBoolean("is_testing_active", true).apply() },
                    onStopTest = { prefs.edit().putBoolean("is_testing_active", false).apply() },
                    onBack = { navigateTo("home") } // Use custom navigate
                )
            }
        }
    }
}