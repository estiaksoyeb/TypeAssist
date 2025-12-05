package com.typeassist.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.typeassist.app.data.AppConfig
import okhttp3.*
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(config: AppConfig, client: OkHttpClient, onSave: (AppConfig) -> Unit, onBack: () -> Unit) {
    var key by remember { mutableStateOf(config.apiKey) }
    var selectedModel by remember { mutableStateOf(config.model) }
    var isKeyVisible by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val models = listOf("gemini-2.5-flash-lite", "gemini-2.5-flash", "gemini-2.5-pro", "gemma-3n-e2b-it", "gemma-3n-e4b-it")
    val context = LocalContext.current

    val view = LocalView.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = surfaceColor.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    fun verify(k: String) {
        val req = Request.Builder().url("https://generativelanguage.googleapis.com/v1beta/models?key=$k").build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { (context as ComponentActivity).runOnUiThread { Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show() } }
            override fun onResponse(call: Call, response: Response) { response.use { if(it.isSuccessful) (context as ComponentActivity).runOnUiThread { Toast.makeText(context, "API Verified! ✅", Toast.LENGTH_SHORT).show() } else (context as ComponentActivity).runOnUiThread { Toast.makeText(context, "Invalid Key ❌", Toast.LENGTH_SHORT).show() } } }
        })
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Gemini API Key") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) {
        Column(modifier = Modifier.padding(it).padding(16.dp).verticalScroll(rememberScrollState())) {
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