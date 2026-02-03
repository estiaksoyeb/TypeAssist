package com.typeassist.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.typeassist.app.utils.XiaomiUtils

@Composable
fun PermissionsScreen(
    onFinished: () -> Unit,
    isStandalone: Boolean = false // If true, acts as a settings sub-screen
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    var isNotificationsEnabled by remember { mutableStateOf(false) }
    var isBatteryOptimized by remember { mutableStateOf(true) } 
    
    // Xiaomi specific
    val isXiaomi = remember { XiaomiUtils.isXiaomi() }
    var isXiaomiBgStartEnabled by remember { mutableStateOf(false) } 
    
    var showSkipDialog by remember { mutableStateOf(false) }

    fun checkPermissions() {
        // Accessibility Check
        val prefString = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        isAccessibilityEnabled = prefString?.contains("${context.packageName}/com.typeassist.app.service.MyAccessibilityService") == true

        // Notification Check (Android 13+)
        isNotificationsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        
        // Battery Optimization Check
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        isBatteryOptimized = !powerManager.isIgnoringBatteryOptimizations(context.packageName)

        // Xiaomi Check
        if (isXiaomi) {
            isXiaomiBgStartEnabled = XiaomiUtils.hasBackgroundStartPermission(context)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        checkPermissions()
    }

    val requiredGranted = isAccessibilityEnabled
    // "Recommended" are notifications (if Android 13+) and Battery Opt + Xiaomi stuff
    val recommendedGranted = isNotificationsEnabled && !isBatteryOptimized && (!isXiaomi || isXiaomiBgStartEnabled)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        if (isStandalone) {
            // Header handled by Scaffold in parent usually, but we can add title here
        }
        
        Text(
            text = if (isStandalone) "Troubleshooting" else "Permissions",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "TypeAssist works best with these permissions.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // --- REQUIRED ---
            Text(
                text = "REQUIRED",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            PermissionToggle(
                title = "Accessibility Service",
                description = "Required to read and replace text.",
                isGranted = isAccessibilityEnabled,
                onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // --- RECOMMENDED ---
            Text(
                text = "RECOMMENDED FOR STABILITY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionToggle(
                    title = "Notifications",
                    description = "Required to keep the service running.",
                    isGranted = isNotificationsEnabled,
                    onClick = { 
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            PermissionToggle(
                title = "Ignore Battery Opt.",
                description = "Prevent system from killing the app.",
                isGranted = !isBatteryOptimized,
                onClick = {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isXiaomi) {
                PermissionToggle(
                    title = "Xiaomi Setup",
                    description = "Enable 'Autostart' & 'Pop-up windows'.",
                    isGranted = isXiaomiBgStartEnabled,
                    onClick = { 
                        XiaomiUtils.openAutostartSettings(context)
                        XiaomiUtils.openPermissionSettings(context)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (recommendedGranted) {
                    onFinished()
                } else {
                    showSkipDialog = true
                }
            },
            enabled = requiredGranted,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(if (isStandalone) "Back to Settings" else "Get Started")
        }
    }
    
    if (showSkipDialog) {
        AlertDialog(
            onDismissRequest = { showSkipDialog = false },
            title = { Text("Skip Stability Settings?") },
            text = { Text("Without these permissions, TypeAssist may stop working unexpectedly.\n\nAre you sure?") },
            confirmButton = {
                Button(
                    onClick = {
                        showSkipDialog = false
                        onFinished()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yes, Proceed")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSkipDialog = false }) {
                    Text("Go Back")
                }
            }
        )
    }
}

@Composable
fun PermissionToggle(
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = !isGranted, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 16.sp)
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error),
            contentAlignment = Alignment.Center
        ) {
            if (isGranted) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
            } else {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(16.dp))
            }
        }
    }
}
