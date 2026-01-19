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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.typeassist.app.data.AppConfig
import com.typeassist.app.data.CloudflareConfig
import com.typeassist.app.data.CustomApiConfig
import com.typeassist.app.api.CloudflareApiClient
import com.typeassist.app.api.CustomApiClient
import okhttp3.*
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(config: AppConfig, client: OkHttpClient, onSave: (AppConfig) -> Unit, onBack: () -> Unit, initialTab: Int = 0) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    
    val view = LocalView.current
    val primaryColor = MaterialTheme.colorScheme.primary

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryColor, titleContentColor = Color.White, navigationIconContentColor = Color.White)) }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = primaryColor,
                contentColor = Color.White
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("General") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("AI Provider") })
            }
            
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                if (selectedTab == 0) {
                    GeneralSettingsTab(config, onSave)
                } else {
                    AiProviderSettingsTab(config, client, onSave)
                }
            }
        }
    }
}

@Composable
fun GeneralSettingsTab(config: AppConfig, onSave: (AppConfig) -> Unit) {
    var enableUndoOverlay by remember { mutableStateOf(config.enableUndoOverlay) }
    var enableLoadingOverlay by remember { mutableStateOf(config.enableLoadingOverlay) }
    
    Text("Overlay Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(16.dp))
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Show Undo Button", fontWeight = FontWeight.Bold)
            Text("Show an 'UNDO' button overlay after text replacement.", fontSize = 12.sp, color = Color.Gray)
        }
        Switch(checked = enableUndoOverlay, onCheckedChange = { 
            enableUndoOverlay = it
            onSave(config.copy(enableUndoOverlay = it))
        })
    }
    
    Spacer(Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(Modifier.height(16.dp))
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Show Loading Indicator", fontWeight = FontWeight.Bold)
            Text("Show a spinner overlay while AI is processing.", fontSize = 12.sp, color = Color.Gray)
        }
        Switch(checked = enableLoadingOverlay, onCheckedChange = { 
            enableLoadingOverlay = it
            onSave(config.copy(enableLoadingOverlay = it))
        })
    }

    Spacer(Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(Modifier.height(16.dp))

    val context = LocalContext.current
    var hasOverlayPermission by remember { mutableStateOf(android.provider.Settings.canDrawOverlays(context)) }
    var enablePreviewDialog by remember { mutableStateOf(config.enablePreviewDialog && hasOverlayPermission) }

    // Check permission on resume
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val hasPerm = android.provider.Settings.canDrawOverlays(context)
                hasOverlayPermission = hasPerm
                if (!hasPerm && config.enablePreviewDialog) {
                    // Permission revoked externally
                    enablePreviewDialog = false
                    onSave(config.copy(enablePreviewDialog = false))
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        if (config.enablePreviewDialog) {
            onSave(config.copy(enablePreviewDialog = false))
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Enable Preview Dialog", fontWeight = FontWeight.Bold)
            Text("Show a scrollable preview for long AI responses (>15 words). Requires 'Display over other apps' permission.", fontSize = 12.sp, color = Color.Gray)
            Text("Unstable. Under development", fontSize = 12.sp, color = Color.Red, fontWeight = FontWeight.Bold)
        }
        Switch(
            checked = false, 
            enabled = false,
            onCheckedChange = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiProviderSettingsTab(config: AppConfig, client: OkHttpClient, onSave: (AppConfig) -> Unit) {
    var selectedProvider by remember { mutableStateOf(config.provider) }
    
    // Gemini States
    var geminiKey by remember { mutableStateOf(config.apiKey) }
    var geminiModel by remember { mutableStateOf(config.model) }
    
    // Cloudflare States
    var cfAccountId by remember { mutableStateOf(config.cloudflareConfig.accountId) }
    var cfApiToken by remember { mutableStateOf(config.cloudflareConfig.apiToken) }
    var cfModel by remember { mutableStateOf(config.cloudflareConfig.model) }

    // Custom API States
    var customBaseUrl by remember { mutableStateOf(config.customApiConfig.baseUrl) }
    var customApiKey by remember { mutableStateOf(config.customApiConfig.apiKey) }
    var customModel by remember { mutableStateOf(config.customApiConfig.model) }

    var isKeyVisible by remember { mutableStateOf(false) }
    var providerExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    
    val providers = listOf("gemini", "cloudflare", "custom")
    val geminiModels = listOf("gemini-2.5-flash-lite", "gemini-2.5-flash", "gemini-2.5-pro", "gemma-3n-e2b-it", "gemma-3n-e4b-it")
    val context = LocalContext.current

    fun verifyGemini(k: String) {
        val req = Request.Builder().url("https://generativelanguage.googleapis.com/v1beta/models?key=$k").build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { (context as ComponentActivity).runOnUiThread { Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show() } }
            override fun onResponse(call: Call, response: Response) { response.use { if(it.isSuccessful) (context as ComponentActivity).runOnUiThread { Toast.makeText(context, "Gemini API Verified! ✅", Toast.LENGTH_SHORT).show() } else (context as ComponentActivity).runOnUiThread { Toast.makeText(context, "Invalid Gemini Key ❌", Toast.LENGTH_SHORT).show() } } }
        })
    }

    fun verifyCloudflare(acc: String, tok: String, mod: String) {
        val cloudflareClient = CloudflareApiClient(client)
        cloudflareClient.callCloudflare(acc, tok, mod, "You are a helpful assistant.", "hi") { result ->
            (context as ComponentActivity).runOnUiThread {
                result.onSuccess {
                    Toast.makeText(context, "Cloudflare API Verified! ✅", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, "Cloudflare Verification Failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun verifyCustomApi(baseUrl: String, apiKey: String, model: String) {
        val customClient = CustomApiClient(client)
        customClient.callCustomApi(baseUrl, apiKey, model, "You are a helpful assistant.", "hi") { result ->
            (context as ComponentActivity).runOnUiThread {
                result.onSuccess {
                    Toast.makeText(context, "Custom API Verified! ✅", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, "Custom API Verification Failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    val primaryColor = MaterialTheme.colorScheme.primary

    // Provider Selection
    Text("AI Provider", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(8.dp))
    ExposedDropdownMenuBox(expanded = providerExpanded, onExpandedChange = { providerExpanded = !providerExpanded }, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(value = selectedProvider.uppercase(), onValueChange = {}, readOnly = true, label = { Text("Select Provider") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) }, colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(), modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = providerExpanded, onDismissRequest = { providerExpanded = false }) { providers.forEach { item -> DropdownMenuItem(text = { Text(text = item.uppercase()) }, onClick = { selectedProvider = item; providerExpanded = false }) } }
    }
    
    Spacer(Modifier.height(24.dp))
    HorizontalDivider()
    Spacer(Modifier.height(24.dp))

    if (selectedProvider == "gemini") {
        // Gemini Setup
        OutlinedTextField(value = geminiKey, onValueChange = { geminiKey = it }, label = { Text("Gemini API Key") }, modifier = Modifier.fillMaxWidth(), visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { isKeyVisible = !isKeyVisible }) { Icon(if (isKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null) } })
        Spacer(Modifier.height(16.dp))
        ExposedDropdownMenuBox(expanded = modelExpanded, onExpandedChange = { modelExpanded = !modelExpanded }, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(value = geminiModel, onValueChange = {}, readOnly = true, label = { Text("Select Gemini Model") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) }, colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(), modifier = Modifier.menuAnchor().fillMaxWidth())
            ExposedDropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) { geminiModels.forEach { item -> DropdownMenuItem(text = { Text(text = item) }, onClick = { geminiModel = item; modelExpanded = false }) } }
        }
    } else if (selectedProvider == "cloudflare") {
        // Cloudflare Setup
        OutlinedTextField(value = cfAccountId, onValueChange = { cfAccountId = it }, label = { Text("Cloudflare Account ID") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = cfApiToken, onValueChange = { cfApiToken = it }, label = { Text("Cloudflare API Token") }, modifier = Modifier.fillMaxWidth(), visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { isKeyVisible = !isKeyVisible }) { Icon(if (isKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null) } })
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = cfModel, onValueChange = { cfModel = it }, label = { Text("Cloudflare Model ID") }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("@cf/meta/llama-3-8b-instruct") })
    } else {
        // Custom API Setup
        OutlinedTextField(value = customBaseUrl, onValueChange = { customBaseUrl = it }, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("https://api.openai.com/v1") })
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = customApiKey, onValueChange = { customApiKey = it }, label = { Text("API Key (Optional)") }, modifier = Modifier.fillMaxWidth(), visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { isKeyVisible = !isKeyVisible }) { Icon(if (isKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null) } })
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = customModel, onValueChange = { customModel = it }, label = { Text("Model Name") }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("gpt-3.5-turbo") })
    }

    Spacer(Modifier.height(24.dp))
    
    Button(
        onClick = { 
            var newConfig = config.copy(
                provider = selectedProvider,
                apiKey = geminiKey.trim(),
                model = geminiModel,
                cloudflareConfig = CloudflareConfig(
                    accountId = cfAccountId.trim(),
                    apiToken = cfApiToken.trim(),
                    model = cfModel.trim()
                ),
                customApiConfig = CustomApiConfig(
                    baseUrl = customBaseUrl.trim(),
                    apiKey = customApiKey.trim(),
                    model = customModel.trim()
                )
            )
            
            // Auto-enable logic
            val hasValidKey = when (selectedProvider) {
                "gemini" -> geminiKey.isNotBlank()
                "cloudflare" -> cfApiToken.isNotBlank()
                "custom" -> customBaseUrl.isNotBlank() && customModel.isNotBlank() // Key might be optional for local
                else -> false
            }
            
            if (!config.isAppEnabled && hasValidKey) {
                 newConfig = newConfig.copy(isAppEnabled = true)
            }
            
            onSave(newConfig)
            Toast.makeText(context, "Config Saved", Toast.LENGTH_SHORT).show()
            
            when (selectedProvider) {
                "gemini" -> if (geminiKey.isNotBlank()) verifyGemini(geminiKey.trim())
                "cloudflare" -> if (cfApiToken.isNotBlank()) verifyCloudflare(cfAccountId.trim(), cfApiToken.trim(), cfModel.trim())
                "custom" -> if (customBaseUrl.isNotBlank()) verifyCustomApi(customBaseUrl.trim(), customApiKey.trim(), customModel.trim())
            }
        }, 
        modifier = Modifier.fillMaxWidth().height(50.dp), 
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) { 
        Text("Test and Save Config") 
    } 
    
    Spacer(Modifier.height(32.dp))
    
    when (selectedProvider) {
        "gemini" -> GeminiHelp(primaryColor, context)
        "cloudflare" -> CloudflareHelp(primaryColor, context)
        "custom" -> CustomApiHelp(primaryColor, context)
    }
}

@Composable
fun GeminiHelp(primaryColor: Color, context: android.content.Context) {
    Text("How to get Gemini API Key?", fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(Modifier.height(16.dp))
    val steps = listOf("1. Visit Google AI Studio at ", "2. Sign in with your Google account.", "3. Click 'Get API key'.", "4. Click 'Create API key'.", "5. Copy and paste above.", "6. Select Model.", "7. Click Save.")
    val annotatedString = buildAnnotatedString { steps.forEach { step -> if (step.contains("Google AI Studio")) { append("1. Visit Google AI Studio at "); pushStringAnnotation(tag = "URL", annotation = "https://aistudio.google.com/"); withStyle(style = SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold)) { append("aistudio.google.com") }; pop(); append(".\n") } else { append(step + "\n") } } }
    ClickableText(text = annotatedString, style = LocalTextStyle.current.copy(fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface), onClick = { offset -> annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset).firstOrNull()?.let { annotation -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))) } })
}

@Composable
fun CloudflareHelp(primaryColor: Color, context: android.content.Context) {
    Text("How to setup Cloudflare AI?", fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(Modifier.height(16.dp))
    val steps = listOf(
        "1. Visit Workers AI at ",
        "2. Sign in with your Cloudflare account.",
        "3. Click on 'REST API'.",
        "4. Click 'Create a Workers AI API token'.",
        "5. Copy and paste the API Key above.",
        "6. Copy your 'Account ID' from the same page.",
        "7. Paste Account ID and Token above.",
        "8. Click Test and Save."
    )
    val annotatedString = buildAnnotatedString { 
        steps.forEach { step -> 
            if (step.contains("Visit Workers AI at")) { 
                append("1. Visit Workers AI at ")
                pushStringAnnotation(tag = "URL", annotation = "https://dash.cloudflare.com/?to=/:account/ai/workers-ai")
                withStyle(style = SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold)) { append("Cloudflare Dash") }
                pop()
                append(".\n")
            } else { 
                append(step + "\n") 
            } 
        } 
    }
    ClickableText(text = annotatedString, style = LocalTextStyle.current.copy(fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface), onClick = { offset -> annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset).firstOrNull()?.let { annotation -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))) } })
}

@Composable
fun CustomApiHelp(primaryColor: Color, context: android.content.Context) {
    Text("Custom API Setup", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Spacer(Modifier.height(16.dp))
    Text("Compatible with any OpenAI-style Chat Completion API.", fontSize = 14.sp)
    Spacer(Modifier.height(8.dp))
    Text("1. Base URL: The API endpoint (e.g. https://api.groq.com/openai/v1 or http://localhost:11434/v1)", fontSize = 14.sp)
    Text("2. API Key: Your provider's API key (leave blank for local LLMs).", fontSize = 14.sp)
    Text("3. Model Name: The specific model ID (e.g. llama3-70b-8192, gpt-4o).", fontSize = 14.sp)
}
