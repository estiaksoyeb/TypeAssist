package com.typeassist.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.typeassist.app.MainActivity
import com.typeassist.app.ui.components.TypingAnimationPreview
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val activity = context as MainActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    var isAccessibilityEnabled by remember { mutableStateOf(false) }

    // --- Status Bar Color ---
    val view = LocalView.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = surfaceColor.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    // --- Lifecycle Observer for Permission Check ---
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityEnabled = activity.isAccessibilityEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // 1. Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Welcome to",
                fontSize = 20.sp,
                color = Color.Gray
            )
            Text(
                text = "TypeAssist",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // 2. Visualization (The Animation)
        TypingAnimationPreview()

        // 3. Explanation
        Text(
            text = "Type anywhere, add a trigger, and let AI handle the rest.",
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            color = Color.DarkGray,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        // 4. Permission & Action Buttons
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Permission Status Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isAccessibilityEnabled) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isAccessibilityEnabled) Icons.Default.CheckCircle else Icons.Default.Settings,
                        contentDescription = null,
                        tint = if (isAccessibilityEnabled) Color(0xFF16A34A) else Color(0xFFDC2626)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isAccessibilityEnabled) "Service Enabled" else "Service Required",
                            fontWeight = FontWeight.Bold,
                            color = if (isAccessibilityEnabled) Color(0xFF166534) else Color(0xFF991B1B)
                        )
                        if (!isAccessibilityEnabled) {
                            Text(
                                text = "TypeAssist needs Accessibility permission to read and replace text.",
                                fontSize = 12.sp,
                                color = Color(0xFF7F1D1D)
                            )
                        }
                    }
                }
            }

            if (!isAccessibilityEnabled) {
                Button(
                    onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Enable Accessibility Service")
                }
            } else {
                Button(
                    onClick = onFinished,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Get Started")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
}

