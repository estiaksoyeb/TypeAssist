package com.typeassist.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.gson.GsonBuilder
import okhttp3.*
import java.io.IOException

// --- HOME SCREEN ---
@Composable
fun HomeScreen(config: AppConfig, context: Context, onToggle: (Boolean) -> Unit, onNavigate: (String) -> Unit) {
    val activity = context as MainActivity
    var hasPermission by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { hasPermission = activity.isAccessibilityEnabled() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        
        // === 1. FIXED HEADER & MASTER SWITCH ===
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxWidth().zIndex(1f)
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(130.dp).background(MaterialTheme.colorScheme.primary))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(top = 16.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TypeAssist", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("AI Power for your keyboard", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Master Switch", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(if(config.isAppEnabled) "Service Active" else "Service Paused", color = if(config.isAppEnabled) MaterialTheme.colorScheme.secondary else Color.Gray, fontSize = 12.sp)
                        }
                        Switch(checked = config.isAppEnabled, onCheckedChange = onToggle)
                    }
                }
            }
        }

        // === 2. SCROLLABLE CONTENT ===
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, 
                modifier = Modifier.fillMaxWidth().height(50.dp), 
                colors = ButtonDefaults.buttonColors(containerColor = if (hasPermission) Color(0xFF10B981) else Color(0xFF111827)), 
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (hasPermission) "Permission Granted ✅" else "Enable Accessibility Permission ⚠️")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Menus
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

            // Instructions
            Spacer(modifier = Modifier.height(24.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("How to Use", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    StepItem("1", "Enable Master Switch & Permission above.")
                    StepItem("2", "Go to API Setup and add your Gemini Key.")
                    StepItem("3", "Open any app (WhatsApp, Notes, etc).")
                    StepItem("4", "Type text + trigger (e.g. 'Hello @ta').")
                }
            }

            // Useful Commands
            Spacer(modifier = Modifier.height(24.dp))
            Text("Command Reference", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
            
            CommandItem("@ta", "Ask AI", "Sends your text to AI and replaces it with the answer.")
            CommandItem("!g", "Grammar Fix", "Fixes spelling, punctuation, and grammar errors.")
            CommandItem("!tr", "Translate", "Translates your text into English.")
            CommandItem("@polite", "Polite Tone", "Rewrites your text to be more professional.")
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// --- HELPER COMPOSABLES ---
@Composable
fun StepItem(num: String, text: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = "$num.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(24.dp))
        Text(text = text, fontSize = 14.sp, color = Color.DarkGray)
    }
}

@Composable
fun CommandItem(cmd: String, title: String, desc: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- THE "PURPLE BOX" ALIGNMENT FIX ---
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(70.dp) // Fixed width for perfect alignment
                    .height(50.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer, 
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Text(
                    text = cmd, 
                    fontWeight = FontWeight.Bold, 
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 16.sp
                )
            }
            // --------------------------------------

            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Text(desc, fontSize = 13.sp, color = Color.Gray, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
fun MenuCard(modifier: Modifier, title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Card(modifier = modifier.height(90.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color); Spacer(modifier = Modifier.height(8.dp)); Text(title, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

// --- COMMANDS SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandsScreen(config: AppConfig, onSave: (AppConfig) -> Unit, onBack: () -> Unit) {
    // State for Add/Edit Dialog
    var showEditDialog by remember { mutableStateOf(false) }
    var tTrigger by remember { mutableStateOf("") }
    var tPrompt by remember { mutableStateOf("") }
    
    // State for Delete Confirmation Dialog
    var commandToDelete by remember { mutableStateOf<Trigger?>(null) }
    
    val triggers = config.triggers ?: mutableListOf()

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Commands") }, 
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            ) 
        },
        // MOVED FAB TO CENTER
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = { 
            FloatingActionButton(
                onClick = { 
                    tTrigger = "@"
                    tPrompt = ""
                    showEditDialog = true 
                }, 
                containerColor = MaterialTheme.colorScheme.primary
            ) { 
                Icon(Icons.Default.Add, "Add New Command") 
            } 
        }
    ) { p ->
        LazyColumn(modifier = Modifier.padding(p).padding(16.dp)) {
            items(triggers) { t ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { 
                            tTrigger = t.pattern
                            tPrompt = t.prompt
                            showEditDialog = true 
                        }, 
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) { 
                            Text(t.pattern, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                            Text(t.prompt, maxLines = 1, fontSize = 12.sp, color = Color.Gray) 
                        }
                        // Click Delete -> Opens Confirmation Dialog
                        IconButton(onClick = { commandToDelete = t }) { 
                            Icon(Icons.Default.Delete, "Delete", tint = Color.Red) 
                        }
                    }
                }
            }
            // Spacer to prevent FAB from covering the last item
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        // --- ADD / EDIT DIALOG ---
        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("Edit Command") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = tTrigger, 
                            onValueChange = { tTrigger = it }, 
                            label = { Text("Trigger (e.g. !fix)") },
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tPrompt, 
                            onValueChange = { tPrompt = it }, 
                            label = { Text("System Prompt") }, 
                            minLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val n = triggers.toMutableList()
                        // Remove existing if editing (based on pattern match), then add new
                        n.removeIf { it.pattern == tTrigger }
                        n.add(Trigger(tTrigger, tPrompt))
                        onSave(config.copy(triggers = n))
                        showEditDialog = false
                    }) { Text("Save") }
                },
                dismissButton = { 
                    TextButton(onClick = { showEditDialog = false }) { Text("Cancel") } 
                }
            )
        }

        // --- DELETE CONFIRMATION DIALOG ---
        if (commandToDelete != null) {
            AlertDialog(
                onDismissRequest = { commandToDelete = null },
                title = { Text("Delete Command?") },
                text = { Text("Are you sure you want to delete '${commandToDelete?.pattern}'?") },
                confirmButton = {
                    Button(
                        onClick = {
                            val n = triggers.toMutableList()
                            n.remove(commandToDelete)
                            onSave(config.copy(triggers = n))
                            commandToDelete = null // Close dialog
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = { 
                    TextButton(onClick = { commandToDelete = null }) { Text("Cancel") } 
                }
            )
        }
    }
}


// --- SETTINGS SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(config: AppConfig, client: OkHttpClient, onSave: (AppConfig) -> Unit, onBack: () -> Unit) {
    var key by remember { mutableStateOf(config.apiKey) }
    var selectedModel by remember { mutableStateOf(config.model) }
    var isKeyVisible by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val models = listOf("gemini-2.5-flash-lite", "gemini-2.5-flash", "gemini-2.5-pro", "gemma-3n-e2b-it", "gemma-3n-e4b-it")
    val context = LocalContext.current

    fun verify(k: String) {
        val req = Request.Builder().url("https://generativelanguage.googleapis.com/v1beta/models?key=$k").build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { (context as ComponentActivity).runOnUiThread { Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show() } }
            override fun onResponse(call: Call, response: Response) { response.use { if(it.isSuccessful) (context as ComponentActivity).runOnUiThread { Toast.makeText(context, "API Verified! ✅", Toast.LENGTH_SHORT).show() } else (context as ComponentActivity).runOnUiThread { Toast.makeText(context, "Invalid Key ❌", Toast.LENGTH_SHORT).show() } } }
        })
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Gemini API Key") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("API Key") }, modifier = Modifier.fillMaxWidth(), visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { isKeyVisible = !isKeyVisible }) { Icon(if (isKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null) } })
            Spacer(Modifier.height(16.dp))
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = selectedModel, onValueChange = {}, readOnly = true, label = { Text("Select Model") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(), modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { models.forEach { item -> DropdownMenuItem(text = { Text(text = item) }, onClick = { selectedModel = item; expanded = false }) } }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = { val validKey = key.trim(); val shouldEnable = validKey.isNotEmpty(); onSave(config.copy(apiKey = validKey, model = selectedModel, isAppEnabled = if (shouldEnable) true else config.isAppEnabled)); Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show(); if (validKey.isNotEmpty()) verify(validKey); onBack() }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Test and Save API Key") }
            Spacer(Modifier.height(32.dp))
            Text("How to get Gemini API Key?", fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(Modifier.height(16.dp))
            val steps = listOf("1. Visit Google AI Studio at ", "2. Sign in with your Google account.", "3. Click 'Get API key'.", "4. Click 'Create API key'.", "5. Copy and paste above.", "6. Select Model.", "7. Click Save.")
            val annotatedString = buildAnnotatedString { steps.forEach { step -> if (step.contains("Google AI Studio")) { append("1. Visit Google AI Studio at "); pushStringAnnotation(tag = "URL", annotation = "https://aistudio.google.com/"); withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold)) { append("aistudio.google.com") }; pop(); append(".\n") } else { append(step + "\n") } } }
            ClickableText(text = annotatedString, style = LocalTextStyle.current.copy(fontSize = 14.sp, lineHeight = 20.sp, color = Color.Black), onClick = { offset -> annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset).firstOrNull()?.let { annotation -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))) } })
        }
    }
}

// --- JSON SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JsonScreen(config: AppConfig, onSave: (AppConfig) -> Unit, onBack: () -> Unit) {
    val gson = GsonBuilder().setPrettyPrinting().create()
    var txt by remember { mutableStateOf(gson.toJson(config)) }
    val context = LocalContext.current
    Scaffold(topBar = { TopAppBar(title = { Text("Backup") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) { Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer); Spacer(Modifier.width(8.dp)); Text("Caution: Contains API Key! Do not share.", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
            OutlinedTextField(value = txt, onValueChange = { txt=it }, modifier = Modifier.weight(1f).fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace))
            Row(modifier = Modifier.padding(top = 16.dp)) {
                Button(onClick = { val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; cm.setPrimaryClip(ClipData.newPlainText("Config", txt)); Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show() }, modifier = Modifier.weight(1f)) { Text("Copy") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { try { onSave(gson.fromJson(txt, AppConfig::class.java)); onBack() } catch(e:Exception){ Toast.makeText(context, "Invalid JSON", Toast.LENGTH_SHORT).show() } }, modifier = Modifier.weight(1f)) { Text("Apply") }
            }
        }
    }
}

// --- TEST SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(onStartTest: () -> Unit, onStopTest: () -> Unit, onBack: () -> Unit) {
    var t by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    
    DisposableEffect(Unit) { onStartTest(); onDispose { onStopTest() } }

    val presets = listOf(
        "What is the capital of Bangladesh? @ta",
        "Sp3ll1ng and gr@mm3r mistake shall be fixing !g",
        "এটি একটি এআই ভিত্তিক অ্যাপ। !tr"
    )

    Scaffold(topBar = { TopAppBar(title = { Text("Test Lab") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("The Accessibility Service is active here.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom=8.dp))
            OutlinedTextField(value = t, onValueChange = { t=it }, label = { Text("Type or tap a preset...") }, modifier = Modifier.fillMaxWidth().height(150.dp).focusRequester(focusRequester))
            Spacer(Modifier.height(24.dp))
            Text("Quick Test Triggers:", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            presets.forEach { item ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { t = item; focusRequester.requestFocus() }, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.TouchApp, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp)); Text(item, fontSize = 14.sp) }
                }
            }
        }
    }
}
