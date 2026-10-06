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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.onSizeChanged
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
            title = { Text("API Key Required") },
            text = { Text("You haven't set up an API key for ${config.provider.replaceFirstChar { it.uppercase() }}.\n\nAI features will not work without it, but you can still use offline features like Snippets.") },
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

    val scrollState = rememberScrollState()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val expandedHeight = statusTop + 208.dp
    val collapsedHeight = statusTop + 56.dp
    val collapseDistance = expandedHeight - collapsedHeight
    val collapsePx = with(androidx.compose.ui.platform.LocalDensity.current) { collapseDistance.toPx() }
    val collapseFraction = (scrollState.value / collapsePx).coerceIn(0f, 1f)
    val headerHeight = expandedHeight - (collapseDistance * collapseFraction)

    val masterSwitch: @Composable () -> Unit = {
        Switch(
            checked = config.isAppEnabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                uncheckedBorderColor = androidx.compose.ui.graphics.Color.Transparent
            ),
            onCheckedChange = { newState ->
                if (newState) {
                    if (!activity.isAccessibilityEnabled()) {
                        android.widget.Toast.makeText(context, "Please Enable Accessibility Service first", android.widget.Toast.LENGTH_SHORT).show()
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        return@Switch
                    }
                    // Skip API check for providers that don't require an API key
                    val isLocalReady = config.provider == "local" && config.localLlmConfig.modelPath.isNotBlank()
                    val isCustomReady = config.provider == "custom" // API key is optional for custom
                    val needsApiKey = !isLocalReady && !isCustomReady && config.apiKey.isBlank()
                    if (needsApiKey) {
                        showApiKeyDialog = true
                        return@Switch
                    }
                }
                onToggle(newState)
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
                // === 1. SINGLE LOGO MORPH: CENTER -> LEFT ===
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .zIndex(1f)
                .clipToBounds()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val maxWpx = with(density) { maxWidth.toPx() }
            var logoWidthPx by remember { mutableStateOf(0f) }
            val logoHalfWidthPx = logoWidthPx / 2f
            val startX = 0f
            val endX = with(density) { 16.dp.toPx() } + logoHalfWidthPx - (maxWpx / 2f)
            val startY = with(density) { statusTop.toPx() + 8.dp.toPx() }
            val endY = with(density) { statusTop.toPx() + (56.dp.toPx() - 28.dp.toPx()) / 2f }
            val curX = androidx.compose.ui.util.lerp(startX, endX, collapseFraction)
            val curY = androidx.compose.ui.util.lerp(startY, endY, collapseFraction)
            val curSize = androidx.compose.ui.util.lerp(28f, 22f, collapseFraction)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .padding(top = 36.dp)
                    .graphicsLayer {
                        alpha = (1f - collapseFraction * 3f).coerceIn(0f, 1f)
                        translationY = -16.dp.toPx() * collapseFraction
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("AI Power for your keyboard", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Master Switch", style = MaterialTheme.typography.titleMedium)
                            Text(if (config.isAppEnabled) "Service Active" else "Service Paused", style = MaterialTheme.typography.bodySmall, color = if (config.isAppEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (collapseFraction < 0.5f) masterSwitch()
                    }
                }
            }

            Text(
                text = "TypeAssist",
                fontWeight = FontWeight.Bold,
                fontSize = curSize.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .onSizeChanged { logoWidthPx = it.width.toFloat() }
                    .graphicsLayer {
                        translationX = curX
                        translationY = curY
                    }
            )

            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 16.dp).graphicsLayer { alpha = collapseFraction },
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                masterSwitch()
            }
        }

        // === 2. SCROLLABLE CONTENT ===
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
                .padding(top = expandedHeight)
        ) {
            
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
            
            Spacer(modifier = Modifier.height(20.dp))

            // Menu
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Menu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Jump to common actions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuCard(Modifier.weight(1f), "Commands", Icons.Default.Edit) { onNavigate("commands") }
                MenuCard(Modifier.weight(1f), "Settings", Icons.Default.Settings) { onNavigate("settings") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuCard(Modifier.weight(1f), "Backup", Icons.Default.Code) { onNavigate("json") }
                MenuCard(Modifier.weight(1f), "Test Lab", Icons.Default.Science) { onNavigate("test") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuCard(Modifier.weight(1f), "History", Icons.AutoMirrored.Filled.List) { onNavigate("history") }
                MenuCard(Modifier.weight(1f), "Snippets", Icons.Default.Favorite) { onNavigate("snippets") }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Live Preview
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "How it Works",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Watch sample triggers resolve",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            TypingAnimationPreview()

            // Instructions
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(1.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "How to Use",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Get started in four steps",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                    Spacer(modifier = Modifier.height(14.dp))
                    StepItem("1", "Enable Master Switch and permission above.")
                    StepItem("2", "Go to API Setup and add your Gemini key.")
                    StepItem("3", "Open any app such as WhatsApp or Notes.")
                    StepItem("4", "Type text plus a trigger, for example i go home yestarday .g")
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Command Reference",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Shortcuts you can type anywhere",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = num,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CommandItem(cmd: String, title: String, desc: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(76.dp)
                    .height(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp))
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = cmd,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MenuCard(modifier: Modifier, title: String, icon: ImageVector, onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.medium
    Card(
        modifier = modifier
            .height(90.dp)
            .clip(shape)
            .clickable { onClick() },
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun DeveloperCreditSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Developer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Created by Istiak Ahmmed Soyeb",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "This app is free and open source. Reach out on any platform below.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            SocialLink(
                icon = R.drawable.ic_fab_twitter,
                text = "Twitter",
                url = "https://twitter.com/estiaksoyeb"
            )
            Spacer(modifier = Modifier.height(8.dp))
            SocialLink(
                icon = R.drawable.ic_fab_github,
                text = "Source Code (GitHub)",
                url = "https://github.com/estiaksoyeb/TypeAssist"
            )
            Spacer(modifier = Modifier.height(8.dp))
            SocialLink(
                icon = R.drawable.ic_fab_telegram,
                text = "Telegram Group",
                url = "https://t.me/TypeAssist"
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Feedback and pull requests are welcome.",
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
        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)) {
            append(text)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { uriHandler.openUri(url) }
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = text,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = annotatedString,
                fontSize = 15.sp
            )
            Text(
                text = "Open link",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DonationSection() {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Support Development",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Keep TypeAssist free and open",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "If TypeAssist saves you time, you can support it with Binance Pay or USDT TRC20.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            DonationItem(
                label = "Binance Pay ID (No Fee)",
                value = "724197813",
                clipboardManager = clipboardManager,
                context = context
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), modifier = Modifier.padding(vertical = 8.dp))
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
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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

    val containerColor = if (!hasSeen) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (!hasSeen) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .scale(scale)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = contentColor.copy(alpha = alpha),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Did you know?",
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
                if (!hasSeen) {
                    Text(
                        text = "Tap to discover hidden power features!",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
