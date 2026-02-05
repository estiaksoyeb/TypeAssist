package com.typeassist.app.ui.screens

import com.typeassist.app.MainActivity
import com.typeassist.app.data.AppConfig
import com.typeassist.app.data.model.GitHubRelease
import com.typeassist.app.R
import com.typeassist.app.ui.components.TypingAnimationPreview

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.filled.Lightbulb

@Composable
fun HomeScreen(config: AppConfig, context: Context, updateInfo: GitHubRelease?, onToggle: (Boolean) -> Unit, onNavigate: (String) -> Unit) {
    val activity = context as MainActivity
    var hasPermission by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showTroubleshootDialog by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Read preference for Did You Know
    val prefs = context.getSharedPreferences("GeminiConfig", Context.MODE_PRIVATE)
    // Check version to show "shine" for new features even if previously seen
    val lastSeenVersion = prefs.getInt("did_you_know_version", 0)
    val currentContentVersion = 2 
    val hasSeenDidYouKnow = lastSeenVersion >= currentContentVersion
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { hasPermission = activity.isAccessibilityEnabled() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    // ... (Dialogs remain the same) ...
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
        
        // === 1. HEADER & MASTER SWITCH ===
        Column(
            modifier = Modifier.fillMaxWidth().zIndex(1f)
        ) {
            // Header Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("TypeAssist", color = MaterialTheme.colorScheme.primary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("AI Power for your keyboard", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }

            // Master Switch Card
            Card(
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Master Switch", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                        val statusText = if(config.isAppEnabled) "Service Active" else "Service Paused"
                        val statusColor = if(config.isAppEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        Text(statusText, color = statusColor, fontSize = 12.sp)
                    }
                    Switch(
                        checked = config.isAppEnabled, 
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
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

            // === DID YOU KNOW BUTTON ===
            DidYouKnowButton(
                onClick = { onNavigate("did_you_know") },
                hasSeen = hasSeenDidYouKnow
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Menus
            Text("Menu", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuCard(Modifier.weight(1f), "Commands", Icons.Default.Edit, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer) { onNavigate("commands") }
                MenuCard(Modifier.weight(1f), "Settings", Icons.Default.Settings, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant) { onNavigate("settings") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuCard(Modifier.weight(1f), "Backup", Icons.Default.Code, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer) { onNavigate("json") }
                MenuCard(Modifier.weight(1f), "Test Lab", Icons.Default.Science, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer) { onNavigate("test") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuCard(Modifier.weight(1f), "History", Icons.AutoMirrored.Filled.List, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant) { onNavigate("history") }
                MenuCard(Modifier.weight(1f), "Snippets", Icons.Default.Favorite, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer) { onNavigate("snippets") }
            }

            // Live Preview
            Spacer(modifier = Modifier.height(24.dp))
            Text("How it Works", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            TypingAnimationPreview()

            // Instructions
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                elevation = CardDefaults.cardElevation(1.dp), 
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("How to Use", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
fun MenuCard(modifier: Modifier, title: String, icon: ImageVector, containerColor: Color, contentColor: Color, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(90.dp).clickable { onClick() }, 
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = contentColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, color = contentColor)
        }
    }
}

@Composable
fun DeveloperCreditSection() {
    val uriHandler = LocalUriHandler.current
    Card(
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "DEVELOPER",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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
                    text = "Telegram Group",
                    url = "https://t.me/TypeAssist"
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
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun DonationSection() {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    Card(
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.error) // Heart uses error color (red)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Support Development",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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

@Composable
fun DidYouKnowButton(onClick: () -> Unit, hasSeen: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "shine")
    val alpha by if (!hasSeen) {
        infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    val scale by if (!hasSeen) {
        infiniteTransition.animateFloat(
            initialValue = 0.98f,
            targetValue = 1.02f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    val containerColor = if (!hasSeen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (!hasSeen) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (!hasSeen) MaterialTheme.colorScheme.primary else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .scale(scale)
            .clickable { onClick() }
            .then(if (!hasSeen) Modifier.border(2.dp, borderColor.copy(alpha = alpha), RoundedCornerShape(12.dp)) else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Lightbulb, null, tint = contentColor.copy(alpha = alpha), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Did you know?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = contentColor
                )
                if (!hasSeen) {
                    Text(
                        "Tap to discover hidden power features!",
                        fontSize = 12.sp,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
