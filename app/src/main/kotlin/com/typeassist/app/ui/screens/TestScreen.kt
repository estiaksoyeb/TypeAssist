package com.typeassist.app.ui.screens

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(onStartTest: () -> Unit, onStopTest: () -> Unit, onBack: () -> Unit) {
    var t by remember { mutableStateOf("") }
    
    val view = LocalView.current
    val primaryColor = MaterialTheme.colorScheme.primary
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = primaryColor.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    DisposableEffect(Unit) { onStartTest(); onDispose { onStopTest() } }

    val presets = listOf(
        "What is the capital of Bangladesh? .ta",
        "Sp3ll1ng and gr@mm3r mistake shall be fixing .g",
        "এটি একটি এআই ভিত্তিক অ্যাপ। .tr"
    )

    Scaffold(topBar = { TopAppBar(title = { Text("Test Lab") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryColor, titleContentColor = Color.White, navigationIconContentColor = Color.White)) }) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("The Accessibility Service is active here.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom=8.dp))
            OutlinedTextField(value = t, onValueChange = { t=it }, label = { Text("Type or tap a preset...") }, modifier = Modifier.fillMaxWidth().height(150.dp))
            Spacer(Modifier.height(24.dp))
            Text("Quick Test Triggers:", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            presets.forEach { item ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { t = item }, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.TouchApp, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp)); Text(item, fontSize = 14.sp) }
                }
            }
        }
    }
}