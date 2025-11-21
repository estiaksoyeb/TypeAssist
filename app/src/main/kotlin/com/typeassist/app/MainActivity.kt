package com.typeassist.app

import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.ui.graphics.toArgb // Required for System Bars
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat // Required for Icon Colors
import com.google.gson.GsonBuilder
import java.io.Serializable
import okhttp3.*
import java.io.IOException

// --- DATA MODELS ---
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
    
    private val client = OkHttpClient() // Client for API verification

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- SYSTEM BAR COLORING LOGIC ---
        // 1. Define Colors
        val statusBarColor = Color(0xFF4F46E5) // Indigo (Matches Header)
        val navBarColor = Color(0xFFF3F4F6)    // Light Grey (Matches Background)

        // 2. Apply to Window
        window.statusBarColor = statusBarColor.toArgb()
        window.navigationBarColor = navBarColor.toArgb()

        // 3. Fix Icon Colors (White icons on Top, Black icons on Bottom)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false // False = White text
        insetsController.isAppearanceLightNavigationBars = true // True = Black icons
        // ---------------------------------

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = statusBarColor,
                    onPrimary = Color.White,
                    secondary = Color(0xFF10B981),
                    background = navBarColor,
                    surface = Color.White
                )
            ) {
                TypeAssistApp(client)
            }
        }
    }
}

@Composable
fun TypeAssistApp(client: OkHttpClient) {
    var currentScreen by remember { mutableStateOf("home") }
    val context = LocalContext.current
    val gson = GsonBuilder().setPrettyPrinting().create()
    val prefs = context.getSharedPreferences("GeminiConfig", Context.MODE_PRIVATE)
    
    var config by remember { 
        mutableStateOf(try {
            val json = prefs.getString("config_json", null)
            if (json != null) gson.fromJson(json, AppConfig::class.java) else AppConfig()
        } catch (e: Exception) { AppConfig() })
    }

    fun saveConfig(newConfig: AppConfig) {
        config = newConfig
        prefs.edit().putString("config_json", gson.toJson(newConfig)).apply()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (currentScreen) {
            "home" -> HomeScreen(
                config = config,
                onToggle = { saveConfig(config.copy(isAppEnabled = it)) },
                onNavigate = { currentScreen = it }
            )
            "commands" -> CommandsScreen(config, { saveConfig(it) }, { currentScreen = "home" })
            "settings" -> SettingsScreen(
                config = config,
                client = client, // Pass client for verification
                onSave = { saveConfig(it) },
                onBack = { currentScreen = "home" }
            )
            "json" -> JsonScreen(config, { saveConfig(it) }, { currentScreen = "home" })
            "test" -> TestScreen({ currentScreen = "home" })
        }
    }
}

// ================== SCREEN 1: HOME ==================
@Composable
fun HomeScreen(config: AppConfig, onToggle: (Boolean) -> Unit, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        // Blue Header (Matches Status Bar)
        Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(24.dp).padding(top = 10.dp, bottom = 40.dp)) {
            Column {
                Text("TypeAssist", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text("AI Power for your keyboard", color = Color.White.copy(alpha = 0.8f))
            }
        }
        
        // Content
        Column(modifier = Modifier.padding(horizontal = 16.dp).offset(y = (-30).dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)) {
                Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Master Switch", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(if(config.isAppEnabled) "Service Active" else "Service Paused", color = Color.Gray, fontSize = 12.sp)
                    }
                    Switch(checked = config.isAppEnabled, onCheckedChange = onToggle)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827)), shape = RoundedCornerShape(12.dp)) {
                Text("Enable Accessibility Permission")
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Menu", fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
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
    Card(modifier = modifier.height(100.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color); Spacer(modifier = Modifier.height(8.dp)); Text(title, fontWeight = FontWeight.Bold, color = color)
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
        floatingActionButton = { FloatingActionButton(onClick = { tTrigger="@"; tPrompt=""; showDialog=true }, containerColor = MaterialTheme.colorScheme.primary) { Icon(Icons.Default.Add, "Add") } }
    ) { p ->
        LazyColumn(modifier = Modifier.padding(p).padding(16.dp)) {
            items(triggers) { t ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { tTrigger=t.pattern; tPrompt=t.prompt; showDialog=true }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) { Text(t.pattern, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Text(t.prompt, maxLines=1, fontSize=12.sp, color=Color.Gray) }
                        IconButton(onClick = { val n = triggers.toMutableList(); n.remove(t); onSave(config.copy(triggers = n)) }) { Icon(Icons.Default.Delete, "Del", tint = Color.Red) }
                    }
                }
            }
        }
        if(showDialog) {
            AlertDialog(onDismissRequest={showDialog=false}, title={Text("Edit Command")}, text={Column{OutlinedTextField(value=tTrigger, onValueChange={tTrigger=it}, label={Text("Trigger")});Spacer(Modifier.height(8.dp));OutlinedTextField(value=tPrompt, onValueChange={tPrompt=it}, label={Text("Prompt")}, minLines=3)}}, confirmButton={Button(onClick={val n=triggers.toMutableList();n.removeIf{it.pattern==tTrigger};n.add(Trigger(tTrigger,tPrompt));onSave(config.copy(triggers=n));showDialog=false}){Text("Save")}}, dismissButton={TextButton(onClick={showDialog=false}){Text("Cancel")}})
        }
    }
}

// ================== SCREEN 3: SETTINGS (Verified) ==================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(config: AppConfig, client: OkHttpClient, onSave: (AppConfig) -> Unit, onBack: () -> Unit) {
    var key by remember { mutableStateOf(config.apiKey) }
    var model by remember { mutableStateOf(config.model) }
    var temp by remember { mutableStateOf(config.generationConfig.temperature.toString()) }
    var topP by remember { mutableStateOf(config.generationConfig.topP.toString()) }
    val context = LocalContext.current

    fun verify(k: String) {
        val req = Request.Builder().url("https://generativelanguage.googleapis.com/v1beta/models?key=$k").build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { (context as ComponentActivity).runOnUiThread { Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show() } }
            override fun onResponse(call: Call, response: Response) { response.use { if(it.isSuccessful) (context as ComponentActivity).runOnUiThread { Toast.makeText(context, "API Verified! ✅", Toast.LENGTH_SHORT).show() } else (context as ComponentActivity).runOnUiThread { Toast.makeText(context, "Invalid Key ❌", Toast.LENGTH_SHORT).show() } } }
        })
    }

    Scaffold(topBar = { TopAppBar(title = { Text("API Setup") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {
            OutlinedTextField(value = key, onValueChange = { key=it }, label = { Text("API Key") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = model, onValueChange = { model=it }, label = { Text("Model") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row { OutlinedTextField(value = temp, onValueChange = { temp=it }, label = { Text("Temp") }, modifier = Modifier.weight(1f)); Spacer(Modifier.width(8.dp)); OutlinedTextField(value = topP, onValueChange = { topP=it }, label = { Text("Top P") }, modifier = Modifier.weight(1f)) }
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                val nGen = config.generationConfig.copy(temperature=temp.toDoubleOrNull()?:0.2, topP=topP.toDoubleOrNull()?:0.95)
                onSave(config.copy(apiKey=key.trim(), model=model.trim(), generationConfig=nGen))
                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                if(key.isNotEmpty()) verify(key.trim())
                onBack()
            }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Save & Verify") }
        }
    }
}

// ================== SCREEN 4 & 5 ==================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JsonScreen(config: AppConfig, onSave: (AppConfig) -> Unit, onBack: () -> Unit) {
    val gson = GsonBuilder().setPrettyPrinting().create()
    var txt by remember { mutableStateOf(gson.toJson(config)) }
    Scaffold(topBar = { TopAppBar(title = { Text("Backup") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {
            OutlinedTextField(value = txt, onValueChange = { txt=it }, modifier = Modifier.weight(1f).fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace))
            Button(onClick = { try { onSave(gson.fromJson(txt, AppConfig::class.java)); onBack() } catch(e:Exception){} }, modifier = Modifier.fillMaxWidth()) { Text("Apply") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(onBack: () -> Unit) {
    var t by remember { mutableStateOf("") }
    Scaffold(topBar = { TopAppBar(title = { Text("Test Lab") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {
            OutlinedTextField(value = t, onValueChange = { t=it }, label = { Text("Type here (@fix)...") }, modifier = Modifier.fillMaxWidth().height(200.dp))
        }
    }
}
