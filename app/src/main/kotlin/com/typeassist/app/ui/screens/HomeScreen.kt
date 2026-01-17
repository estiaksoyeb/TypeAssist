package com.typeassist.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.typeassist.app.MainActivity
import com.typeassist.app.data.AppConfig
import com.typeassist.app.utils.UpdateInfo

import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.res.painterResource
import com.typeassist.app.R
import com.typeassist.app.ui.components.TypingAnimationPreview

import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite

@Composable
fun HomeScreen(config: AppConfig, context: Context, updateInfo: UpdateInfo?, onToggle: (Boolean) -> Unit, onNavigate: (String) -> Unit) {
    val activity = context as MainActivity
    var hasPermission by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showTroubleshootDialog by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Manual window color setting removed (Handled by AppTheme)
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { hasPermission = activity.isAccessibilityEnabled() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showTroubleshootDialog) {
        AlertDialog(
            onDismissRequest = { showTroubleshootDialog = false },
            title = { Text("App not working?") },
            text = { Text("If the app is not working, the Accessibility Service might be in a 'ghost' state.\n\nTry turning the Accessibility Service OFF and then ON again to reset it.") },
            confirmButton = {
                Button(onClick = {
                    showTroubleshootDialog = false
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showTroubleshootDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("API Key Missing") },
            text = { Text("You haven't set up a Gemini API Key.\n\nAI features will not work, but you can still use offline features like Snippets.") },
            confirmButton = {
                Button(onClick = { 
                    showApiKeyDialog = false
                    onNavigate("settings:1") 
                }) { Text("Setup API") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showApiKeyDialog = false
                    onToggle(true) // Enable offline mode
                }) { Text("Use Offline") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        
        // === 1. FIXED HEADER & MASTER SWITCH ===
        Column(
            modifier = Modifier.fillMaxWidth().zIndex(1f)
        ) {
            // Blue Background Section (Wraps Text)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 45.dp), // Extra bottom padding for card overlap
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("TypeAssist", color = MaterialTheme.colorScheme.onPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("AI Power for your keyboard", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), fontSize = 14.sp)
                }
            }

            // Master Switch Card (Overlaps upwards)
            Card(
                elevation = CardDefaults.cardElevation(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .offset(y = (-35).dp) // Move up to overlap
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Master Switch", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        val isUpdateForced = updateInfo?.forceUpdate == true
                        val statusText = if (isUpdateForced) "Update Required" else if(config.isAppEnabled) "Service Active" else "Service Paused"
                        val statusColor = if (isUpdateForced) MaterialTheme.colorScheme.error else if(config.isAppEnabled) MaterialTheme.colorScheme.secondary else Color.Gray
                        Text(statusText, color = statusColor, fontSize = 12.sp)
                    }
                    Switch(
                        checked = config.isAppEnabled, 
                        enabled = updateInfo?.forceUpdate != true,
                        onCheckedChange = { newState ->
                            if (newState) {
                                if (!activity.isAccessibilityEnabled()) {
                                    android.widget.Toast.makeText(context, "⚠️ Please Enable Accessibility Service first", android.widget.Toast.LENGTH_SHORT).show()
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                    return@Switch
                                }
                                if (config.apiKey.isBlank()) {
                                    showApiKeyDialog = true
                                    return@Switch
                                }
                            }
                            onToggle(newState)
                        }
                    )
                }
            }
        }

        // === 2. SCROLLABLE CONTENT ===
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            // Spacer removed to compensate for Header offset gap
            
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "App not working? Troubleshoot",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { showTroubleshootDialog = true }
                )
            }

            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Menus
            Text("Menu", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuCard(Modifier.weight(1f), "Commands", Icons.Default.Edit, MaterialTheme.colorScheme.primary) { onNavigate("commands") }
                MenuCard(Modifier.weight(1f), "Settings", Icons.Default.Settings, MaterialTheme.colorScheme.primary) { onNavigate("settings") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuCard(Modifier.weight(1f), "Backup", Icons.Default.Code, Color.Gray) { onNavigate("json") }
                MenuCard(Modifier.weight(1f), "Test Lab", Icons.Default.Science, MaterialTheme.colorScheme.secondary) { onNavigate("test") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuCard(Modifier.weight(1f), "History", Icons.Default.List, Color(0xFF8E24AA)) { onNavigate("history") }
                MenuCard(Modifier.weight(1f), "Snippets", Icons.Default.Star, Color(0xFFF59E0B)) { onNavigate("snippets") }
            }

            // Live Preview
            Spacer(modifier = Modifier.height(24.dp))
            Text("How it Works", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            TypingAnimationPreview()

            // Instructions
            Spacer(modifier = Modifier.height(24.dp))
            Card(elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
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
                    StepItem("4", "Type text + trigger (e.g. 'i go home yestarday .g').")
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { onNavigate("guide") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Learn More Features")
                    }
                }
            }

            // Useful Commands
            Spacer(modifier = Modifier.height(24.dp))
            Text("Command Reference", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
            
            CommandItem(".ta", "Ask AI", "Sends your text to AI and replaces it with the answer.")
            CommandItem(".g", "Grammar Fix", "Fixes spelling, punctuation, and grammar errors.")
            CommandItem(".tr", "Translate", "Translates your text into English.")
            CommandItem(".polite", "Polite Tone", "Rewrites your text to be more professional.")
            
            Spacer(modifier = Modifier.height(40.dp))

            DonationSection()

            Spacer(modifier = Modifier.height(24.dp))

            DeveloperCreditSection()

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun StepItem(num: String, text: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = "$num.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(24.dp))
        Text(text = text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun CommandItem(cmd: String, title: String, desc: String) {
    Card(
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(70.dp) 
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
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
fun MenuCard(modifier: Modifier, title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Card(modifier = modifier.height(90.dp).clickable { onClick() }, elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color); Spacer(modifier = Modifier.height(8.dp)); Text(title, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun DeveloperCreditSection() {
    val uriHandler = LocalUriHandler.current
    Card(
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "DEVELOPER",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "This app was created by Istiak Ahmmed Soyeb. You can find him on the following platforms:",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(24.dp))

            Column {
                SocialLink(
                    icon = R.drawable.ic_fab_twitter,
                    text = "Twitter",
                    url = "https://twitter.com/estiaksoyeb"
                )
                Spacer(modifier = Modifier.height(16.dp))
                SocialLink(
                    icon = R.drawable.ic_fab_github,
                    text = "Source Code (GitHub)",
                    url = "https://github.com/estiaksoyeb/TypeAssist"
                )
                Spacer(modifier = Modifier.height(16.dp))
                SocialLink(
                    icon = R.drawable.ic_fab_telegram,
                    text = "Telegram",
                    url = "https://t.me/estiaksoyeb"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "Feel free to reach out for any questions or feedback!",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun SocialLink(icon: Int, text: String, url: String) {
    val uriHandler = LocalUriHandler.current
    val annotatedString = buildAnnotatedString {
        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
            append(text)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { uriHandler.openUri(url) }
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = text,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = annotatedString,
            fontSize = 16.sp
        )
    }
}

@Composable
fun DonationSection() {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    Card(
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, null, tint = Color(0xFFE11D48)) // Pink/Red color for heart
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Support Development",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "This app is free and open source. If it saves you time, please consider supporting via Binance/Crypto.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            DonationItem(
                label = "Binance Pay ID (No Fee)",
                value = "724197813",
                clipboardManager = clipboardManager,
                context = context
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))
            DonationItem(
                label = "USDT (TRC20)",
                value = "TPP5S7HdV4Hrrtp5Cjz7TNtttUAfZXJz5a",
                clipboardManager = clipboardManager,
                context = context
            )
        }
    }
}

@Composable
fun DonationItem(label: String, value: String, clipboardManager: androidx.compose.ui.platform.ClipboardManager, context: Context) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace)
        }
        
        IconButton(onClick = {
            clipboardManager.setText(AnnotatedString(value))
            android.widget.Toast.makeText(context, "Copied $label!", android.widget.Toast.LENGTH_SHORT).show()
        }) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Gray)
        }
    }
}
