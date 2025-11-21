package com.typeassist.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.GsonBuilder
import java.io.Serializable

// --- DATA MODELS (Must match Service) ---
data class AppConfig(
    var isAppEnabled: Boolean = true,
    var apiKey: String = "",
    var model: String = "gemini-2.5-flash",
    var generationConfig: GenConfig = GenConfig(),
    var triggers: MutableList<Trigger> = mutableListOf()
) : Serializable

data class GenConfig(
    var temperature: Double = 0.2,
    var topP: Double = 0.95
) : Serializable

data class Trigger(
    var pattern: String,
    var prompt: String
) : Serializable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Define our Indigo/Green Theme here
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF4F46E5),       // Indigo
                    onPrimary = Color.White,
                    secondary = Color(0xFF10B981),     // Green
                    background = Color(0xFFF3F4F6),    // Light Grey
                    surface = Color.White
                )
            ) {
                TypeAssistApp()
            }
        }
    }
}

@Composable
fun TypeAssistApp() {
    // Simple "State Navigation" (Home -> Settings, etc.)
    var currentScreen by remember { mutableStateOf("home") }
    
    val context = LocalContext.current
    val gson = GsonBuilder().setPrettyPrinting().create()
    val prefs = context.getSharedPreferences("GeminiConfig", Context.MODE_PRIVATE)
    
    // Load Config (with error handling)
    var config by remember { 
        mutableStateOf(
            try {
                val json = prefs.getString("config_json", null)
                if (json != null) gson.fromJson(json, AppConfig::class.java) else AppConfig()
            } catch (e: Exception) { AppConfig() }
        )
    }

    // Helper to Save
    fun saveConfig(newConfig: AppConfig) {
        config = newConfig
        prefs.edit().putString("config_json", gson.toJson(newConfig)).apply()
    }

    // Screen Router
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (currentScreen) {
            "home" -> HomeScreen(
                config = config,
                onToggle = { saveConfig(config.copy(isAppEnabled = it)) },
                onNavigate = { currentScreen = it }
            )
            "commands" -> CommandsScreen(
                config = config,
                onSave = { saveConfig(it) },
                onBack = { currentScreen = "home" }
            )
            "settings" -> SettingsScreen(
                config = config,
                onSave = { saveConfig(it) },
                onBack = { currentScreen = "home" }
            )
            "json" -> JsonScreen(
                config = config,
                onSave = { saveConfig(it) },
                onBack = { currentScreen = "home" }
            )
            "test" -> TestScreen(onBack = { currentScreen = "home" })
        }
    }
}

// ================== SCREEN 1: HOME DASHBOARD ==================
@Composable
fun HomeScreen(config: AppConfig, onToggle: (Boolean) -> Unit, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Blue Header
        Box(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(24.dp).padding(top = 20.dp, bottom = 30.dp)
        ) {
            Column {
                Text("TypeAssist", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text("AI Power for your keyboard", color = Color.White.copy(alpha = 0.8f))
            }
        }

        // Content (shifted up to overlap)
        Column(modifier = Modifier.padding(horizontal = 16.dp).offset(y = (-30).dp)) {
            
            // Master Switch Card
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Master Switch", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(if(config.isAppEnabled) "App is Active" else "App is Paused", color = Color.Gray, fontSize = 12.sp)
                    }
                    Switch(checked = config.isAppEnabled, onCheckedChange = onToggle)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Permission Button
            Button(
                onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Enable Accessibility")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Menu", fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))

            // Menu Grid
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuCard(Modifier.weight(1f), "Commands", Icons.Default.Edit, MaterialTheme.colorScheme.primary) { onNavigate("commands") }
                MenuCard(Modifier.weight(1f), "API Setup", Icons.Default.Settings, MaterialTheme.colorScheme.primary) { onNavigate("settings") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuCard(Modifier.weight(1f), "Backup", Icons.Default.Code, Color.Gray) { onNavigate("json") }
                MenuCard(Modifier.weight(1f), "Test Lab", Icons.Default.Science, MaterialTheme.colorScheme.secondary) { onNavigate("test") }
            }
        }
    }
}

@Composable
fun MenuCard(modifier: Modifier, title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(100.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

// ================== SCREEN 2: COMMANDS ==================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandsScreen(config: AppConfig, onSave: (AppConfig) -> Unit, onBack: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var tTrigger by remember { mutableStateOf("") }
    var tPrompt by remember { mutableStateOf("") }
    val triggers = config.triggers ?: mutableListOf()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Commands") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { tTrigger="@"; tPrompt=""; showDialog=true }, containerColor = MaterialTheme.colorScheme.primary) { Icon(Icons.Default.Add, "Add") }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            items(triggers) { trigger ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { tTrigger=trigger.pattern; tPrompt=trigger.prompt; showDialog=true },
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(trigger.pattern, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(trigger.prompt, maxLines = 1, fontSize = 12.sp, color = Color.Gray)
                        }
                        IconButton(onClick = {
                            val newList = triggers.toMutableList(); newList.remove(trigger)
                            onSave(config.copy(triggers = newList))
                        }) { Icon(Icons.Default.Delete, "Del", tint = Color.Red) }
                    }
                }
            }
        }
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Edit Command") },
                text = {
                    Column {
                        OutlinedTextField(value = tTrigger, onValueChange = { tTrigger=it }, label = { Text("Trigger") })
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = tPrompt, onValueChange = { tPrompt=it }, label = { Text("Prompt") }, minLines = 3)
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val newList = triggers.toMutableList()
                        newList.removeIf { it.pattern == tTrigger }
                        newList.add(Trigger(tTrigger, tPrompt))
                        onSave(config.copy(triggers = newList))
                        showDialog = false
                    }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
            )
        }
    }
}

// ================== SCREEN 3: SETTINGS ==================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(config: AppConfig, onSave: (AppConfig) -> Unit, onBack: () -> Unit) {
    var key by remember { mutableStateOf(config.apiKey) }
    var model by remember { mutableStateOf(config.model) }
    var temp by remember { mutableStateOf(config.generationConfig.temperature.toString()) }
    var topP by remember { mutableStateOf(config.generationConfig.topP.toString()) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("API Setup") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(value = key, onValueChange = { key=it }, label = { Text("API Key") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = model, onValueChange = { model=it }, label = { Text("Model") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                OutlinedTextField(value = temp, onValueChange = { temp=it }, label = { Text("Temp") }, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(value = topP, onValueChange = { topP=it }, label = { Text("Top P") }, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val newGen = config.generationConfig.copy(temperature = temp.toDoubleOrNull()?:0.2, topP = topP.toDoubleOrNull()?:0.95)
                    onSave(config.copy(apiKey = key.trim(), model = model.trim(), generationConfig = newGen))
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text("Save Settings") }
        }
    }
}

// ================== SCREEN 4 & 5: JSON & TEST ==================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JsonScreen(config: AppConfig, onSave: (AppConfig) -> Unit, onBack: () -> Unit) {
    val gson = GsonBuilder().setPrettyPrinting().create()
    var text by remember { mutableStateOf(gson.toJson(config)) }
    Scaffold(topBar = { TopAppBar(title = { Text("Backup") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {
            OutlinedTextField(value = text, onValueChange = { text=it }, modifier = Modifier.weight(1f).fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace))
            Button(onClick = { try { onSave(gson.fromJson(text, AppConfig::class.java)); onBack() } catch(e:Exception){} }, modifier = Modifier.fillMaxWidth()) { Text("Apply") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(onBack: () -> Unit) {
    var text by remember { mutableStateOf("") }
    Scaffold(topBar = { TopAppBar(title = { Text("Test Lab") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {
            OutlinedTextField(value = text, onValueChange = { text=it }, label = { Text("Type here (@fix)...") }, modifier = Modifier.fillMaxWidth().height(200.dp))
        }
    }
}
