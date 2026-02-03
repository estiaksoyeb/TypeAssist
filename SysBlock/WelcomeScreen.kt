package com.self.sysblock.ui.welcome

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.self.sysblock.receivers.AdminReceiver
import com.self.sysblock.ui.home.isAccessibilityServiceEnabled
import com.self.sysblock.ui.home.isUsageAccessGranted
import com.self.sysblock.util.XiaomiUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(
    onFinished: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { pageIdx ->
                when (pageIdx) {
                    0 -> WelcomePage1()
                    1 -> WelcomePage2()
                    2 -> PermissionPage(
                        onPermissionsGranted = onFinished,
                        isStandalone = false
                    )
                }
            }

            // Bottom Navigation / Indicators (Hide on last page as it has its own action)
            if (pagerState.currentPage < 2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Page Indicators
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) { iteration ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                    }

                    // Next Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) {
                        Text("Next")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomePage1() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Welcome to SysBlock",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Take back control of your digital life.\n\nSysBlock allows you to set unbreakable rules for your app usage, ensuring you stay focused on what matters.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

@Composable
fun WelcomePage2() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Unstoppable",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "• Freeze Protocol: Lock your settings during focus hours.\n\n• Uninstall Protection: Prevent bypassing rules by removing the app.\n\n• Strict Limits: Once time is up, it's really up.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            lineHeight = 24.sp
        )
    }
}

@Composable
fun PermissionPage(
    onPermissionsGranted: () -> Unit,
    isStandalone: Boolean = false // If true, it's shown as a recovery screen
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    var isUsageEnabled by remember { mutableStateOf(false) }
    var isNotificationsEnabled by remember { mutableStateOf(false) }
    var isBatteryOptimized by remember { mutableStateOf(true) }
    // Admin is optional for initial setup but good to show
    var isAdminActive by remember { mutableStateOf(false) }
    
    // Xiaomi specific
    val isXiaomi = remember { XiaomiUtils.isXiaomi() }
    var isXiaomiBgStartEnabled by remember { mutableStateOf(false) }
    
    // Dialog state
    var showSkipDialog by remember { mutableStateOf(false) }

    fun checkPermissions() {
        isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
        isUsageEnabled = isUsageAccessGranted(context)
        
        isNotificationsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        isBatteryOptimized = !powerManager.isIgnoringBatteryOptimizations(context.packageName)

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val cn = ComponentName(context, AdminReceiver::class.java)
        isAdminActive = dpm.isAdminActive(cn)
        
        if (isXiaomi) {
            isXiaomiBgStartEnabled = XiaomiUtils.hasBackgroundStartPermission(context)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        checkPermissions()
    }

    val requiredGranted = isAccessibilityEnabled && isUsageEnabled
    val recommendedGranted = isNotificationsEnabled && !isBatteryOptimized && (!isXiaomi || isXiaomiBgStartEnabled)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        if (isStandalone) {
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        Text(
            text = if (isStandalone) "Permissions Required" else "Grant Permissions",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "SysBlock requires these permissions to function correctly.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Scrollable Content Area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // --- REQUIRED SECTION ---
            Text(
                text = "REQUIRED",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Accessibility
            PermissionToggle(
                title = "Accessibility Service",
                description = "Required to detect and block apps.",
                isGranted = isAccessibilityEnabled,
                onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            // Usage Access
            PermissionToggle(
                title = "Usage Access",
                description = "Required to track app usage time.",
                isGranted = isUsageEnabled,
                onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- RECOMMENDED SECTION ---
            Text(
                text = "RECOMMENDED FOR STABILITY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Notifications (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionToggle(
                    title = "Notifications",
                    description = "Required for stability in background.",
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

            // Battery Optimization
            PermissionToggle(
                title = "Ignore Battery Opt.",
                description = "Prevent system from killing the app.",
                isGranted = !isBatteryOptimized,
                onClick = {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isXiaomi) {
                PermissionToggle(
                    title = "Xiaomi Setup",
                    description = "Enable 'Autostart' & 'Display pop-up windows'.",
                    isGranted = isXiaomiBgStartEnabled,
                    onClick = { 
                        XiaomiUtils.openAutostartSettings(context)
                        XiaomiUtils.openPermissionSettings(context)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Device Admin (Optional/Recommended)
            PermissionToggle(
                title = "Device Admin (Optional)",
                description = "Prevents uninstalling to bypass rules.",
                isGranted = isAdminActive,
                onClick = {
                    val cn = ComponentName(context, AdminReceiver::class.java)
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, cn)
                        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Protects SysBlock from being removed.")
                    }
                    context.startActivity(intent)
                }
            )
            
            // Extra spacer at bottom of list so it doesn't look cramped when scrolled to bottom
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (recommendedGranted) {
                    onPermissionsGranted()
                } else {
                    showSkipDialog = true
                }
            },
            enabled = requiredGranted,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(
                text = if (isStandalone) "Resume App" else "Finish",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    if (showSkipDialog) {
        AlertDialog(
            onDismissRequest = { showSkipDialog = false },
            title = { Text("Skip Stability Settings?") },
            text = { Text("Without Battery Optimization disabled or Notification permissions, the app may be killed by the system randomly.\n\nAre you sure you want to proceed?") },
            confirmButton = {
                Button(
                    onClick = {
                        showSkipDialog = false
                        onPermissionsGranted()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yes, Risk It")
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
                Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(16.dp))
            }
        }
    }
}