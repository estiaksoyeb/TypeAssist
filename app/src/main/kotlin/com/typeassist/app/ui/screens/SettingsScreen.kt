package com.typeassist.app.ui.screens

import com.typeassist.app.R

import android.app.Activity
import androidx.compose.ui.res.stringResource
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
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
import com.typeassist.app.data.isReasoningModel
import com.typeassist.app.api.CloudflareApiClient
import com.typeassist.app.api.CustomApiClient
import com.typeassist.app.BuildConfig
import kotlinx.coroutines.launch
import okhttp3.*
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(config: AppConfig, client: OkHttpClient, onSave: (AppConfig) -> Unit, onBack: () -> Unit, onNavigate: (String) -> Unit, initialTab: Int = 0) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    val context = LocalContext.current
    
    val view = LocalView.current
    val primaryColor = MaterialTheme.colorScheme.primary

    Scaffold(topBar = { 
        TopAppBar(
            title = { Text(stringResource(R.string.ui_settings)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.ui_back)) } },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.primary,
                navigationIconContentColor = MaterialTheme.colorScheme.primary
            ) 
        ) 
    }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(stringResource(R.string.ui_general)) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(stringResource(R.string.ui_ai_provider)) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text(stringResource(R.string.ui_local_llm)) })
            }
            
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                when (selectedTab) {
                    0 -> GeneralSettingsTab(config, onSave, onNavigate)
                    1 -> AiProviderSettingsTab(config, client, onSave)
                    2 -> {
                        LocalLlmSetup(config, onSave)
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = {
                                val updatedConfig = config.copy(provider = "local")
                                onSave(updatedConfig)
                                Toast.makeText(context, context.getString(R.string.ui_local_llm_saved_and_set_as_active_p), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text(stringResource(R.string.ui_set_local_as_active_provider))
                        }
                        Spacer(Modifier.height(24.dp))
                        LocalLlmHelp(MaterialTheme.colorScheme.primary, context)
                    }
                }
            }
        }
    }
}

@Composable
fun GeneralSettingsTab(config: AppConfig, onSave: (AppConfig) -> Unit, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    var enableUndoOverlay by remember { mutableStateOf(config.enableUndoOverlay) }
    var enableLoadingOverlay by remember { mutableStateOf(config.enableLoadingOverlay) }
    
    Text(stringResource(R.string.ui_overlay_settings), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(16.dp))
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.ui_show_undo_button), fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.ui_show_an_undo_button_overlay_after_text), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Text(stringResource(R.string.ui_show_loading_indicator), fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.ui_show_a_spinner_overlay_while_ai_is_pro), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = enableLoadingOverlay, onCheckedChange = { 
            enableLoadingOverlay = it
            onSave(config.copy(enableLoadingOverlay = it))
        })
    }

    Spacer(Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(Modifier.height(16.dp))
    
    // Permission & Troubleshooting
    Text(stringResource(R.string.ui_troubleshooting), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(16.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("permissions") },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.ui_check_permissions), fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.ui_fix_issues_with_background_service_bat), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
    }

    Spacer(Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(Modifier.height(16.dp))

    Text(stringResource(R.string.ui_triggers_history), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(16.dp))

    var allowTriggerAnywhere by remember { mutableStateOf(config.allowTriggerAnywhere) }
    var ignorePrecedingWhitespace by remember { mutableStateOf(config.ignorePrecedingWhitespace) }
    var globalTriggerPattern by remember { mutableStateOf(config.globalTriggerPattern) }
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.ui_allow_trigger_anywhere), fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.ui_process_commands_even_if_followed_by_o), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = allowTriggerAnywhere, onCheckedChange = { 
            allowTriggerAnywhere = it
            onSave(config.copy(allowTriggerAnywhere = it))
        })
    }

    Spacer(Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(Modifier.height(16.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.ui_ignore_preceding_space), fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.ui_allow_triggers_immediately_after_words), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = ignorePrecedingWhitespace, onCheckedChange = { 
            ignorePrecedingWhitespace = it
            onSave(config.copy(ignorePrecedingWhitespace = it))
        })
    }

    Spacer(Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(Modifier.height(16.dp))
    
    var dateFormatPattern by remember { mutableStateOf(config.dateFormatPattern) }
    Text(stringResource(R.string.ui_date_format_pattern), fontWeight = FontWeight.Bold)
    Text(stringResource(R.string.ui_java_simpledateformat_pattern_used_by_), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = dateFormatPattern,
        onValueChange = { v ->
            dateFormatPattern = v
            onSave(config.copy(dateFormatPattern = v))
        },
        label = { Text(stringResource(R.string.ui_pattern_e_g_yyyy_mm_dd)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(Modifier.height(16.dp))

    var snippetTriggerPrefix by remember { mutableStateOf(config.snippetTriggerPrefix) }
    Text(stringResource(R.string.ui_snippet_trigger_prefix), fontWeight = FontWeight.Bold)
    Text(stringResource(R.string.ui_prefix_typed_before_a_snippet_name_to_), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = snippetTriggerPrefix,
        onValueChange = { newValue ->
            val capped = newValue.take(4)
            snippetTriggerPrefix = capped
            onSave(config.copy(snippetTriggerPrefix = capped))
        },
        label = { Text(stringResource(R.string.ui_prefix_e_g)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(Modifier.height(16.dp))

    Text(stringResource(R.string.ui_global_trigger_pattern), fontWeight = FontWeight.Bold)
    Text(stringResource(R.string.ui_pattern_for_rewriting_text_use_as_the_), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = globalTriggerPattern, 
        onValueChange = { 
            globalTriggerPattern = it
            onSave(config.copy(globalTriggerPattern = it))
        },
        label = { Text(stringResource(R.string.ui_pattern_e_g)) },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(Modifier.height(16.dp))

    var isHistoryEnabled by remember { mutableStateOf(config.isHistoryEnabled) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.ui_enable_history), fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.ui_save_processed_text_to_history_for_lat), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = isHistoryEnabled, onCheckedChange = { 
            isHistoryEnabled = it
            onSave(config.copy(isHistoryEnabled = it))
        })
    }

    Spacer(Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(Modifier.height(16.dp))

    var enablePreviewDialog by remember { mutableStateOf(config.enablePreviewDialog) }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.ui_enable_preview_dialog), fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.ui_show_a_scrollable_preview_for_long_ai_), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = enablePreviewDialog, 
            onCheckedChange = { 
                enablePreviewDialog = it
                onSave(config.copy(enablePreviewDialog = it))
            }
        )
    }

    Spacer(Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(Modifier.height(16.dp))

    var apiTimeoutSeconds by remember { mutableStateOf(config.apiTimeoutSeconds.toFloat()) }

    Text(stringResource(R.string.ui_api_timeout), fontWeight = FontWeight.Bold)
    Text(stringResource(R.string.ui_maximum_time_to_wait_for_ai_response_1, apiTimeoutSeconds.toInt()), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    Slider(
        value = apiTimeoutSeconds,
        onValueChange = { apiTimeoutSeconds = it },
        onValueChangeFinished = {
            onSave(config.copy(apiTimeoutSeconds = apiTimeoutSeconds.toLong()))
        },
        valueRange = 10f..120f,
        steps = 22 // (120-10)/5 = 22 intervals for 5s steps (10, 15, ..., 120)
    )

    Spacer(Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(Modifier.height(16.dp))

    if (BuildConfig.SHOW_UPDATES) {
        Text(stringResource(R.string.ui_updates), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))

        val updateRepository = remember { com.typeassist.app.data.repository.UpdateRepository(context) }
        val scope = rememberCoroutineScope()
        var isCheckingForUpdate by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isCheckingForUpdate) {
                    isCheckingForUpdate = true
                    scope.launch {
                        val result = updateRepository.checkForUpdate("estiaksoyeb", "TypeAssist")
                        isCheckingForUpdate = false
                        result.onSuccess { release ->
                            if (release != null) {
                                Toast.makeText(context, context.getString(R.string.ui_new_update_available_1_s , release.tagName), Toast.LENGTH_LONG).show()
                                // We could trigger the dialog here, but for now a toast is fine since 
                                // MainActivity will show it on next restart or we can just open the link.
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl))
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, context.getString(R.string.ui_you_are_on_the_latest_version), Toast.LENGTH_SHORT).show()
                            }
                        }.onFailure {
                            Toast.makeText(context, context.getString(R.string.ui_failed_to_check_for_updates_1_s , it.message), Toast.LENGTH_SHORT).show()
                        }
                    }
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.ui_check_for_updates), fontWeight = FontWeight.Bold)
                Text(if (isCheckingForUpdate) stringResource(R.string.ui_checking) else stringResource(R.string.ui_current_version, BuildConfig.VERSION_NAME), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isCheckingForUpdate) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
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
    val geminiModels = listOf(
        "gemini-3.5-flash-lite",
        "gemini-3.5-flash",
        "gemini-3.6-flash",
        "gemini-2.5-flash",
        "gemma-4-31b-it",
        "gemma-4-26b-a4b-it"
    )
    val context = LocalContext.current

    fun verifyGemini(k: String) {
        val req = Request.Builder().url("https://generativelanguage.googleapis.com/v1beta/models?key=$k").build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { (context as ComponentActivity).runOnUiThread { Toast.makeText(context, context.getString(R.string.ui_network_error), Toast.LENGTH_SHORT).show() } }
            override fun onResponse(call: Call, response: Response) { response.use { if(it.isSuccessful) (context as ComponentActivity).runOnUiThread { Toast.makeText(context, context.getString(R.string.ui_gemini_api_verified), Toast.LENGTH_SHORT).show() } else (context as ComponentActivity).runOnUiThread { Toast.makeText(context, context.getString(R.string.ui_invalid_gemini_key), Toast.LENGTH_SHORT).show() } } }
        })
    }

    fun verifyCloudflare(acc: String, tok: String, mod: String) {
        val cloudflareClient = CloudflareApiClient(client)
        cloudflareClient.callCloudflare(acc, tok, mod, "You are a helpful assistant.", "hi", config.apiTimeoutSeconds) { result ->
            (context as ComponentActivity).runOnUiThread {
                result.onSuccess {
                    Toast.makeText(context, context.getString(R.string.ui_cloudflare_api_verified), Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, context.getString(R.string.ui_cloudflare_verification_failed_1_s , it.message), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun verifyCustomApi(baseUrl: String, apiKey: String, model: String) {
        val customClient = CustomApiClient(client)
        customClient.callCustomApi(baseUrl, apiKey, model, "You are a helpful assistant.", "hi", config.apiTimeoutSeconds) { result ->
            (context as ComponentActivity).runOnUiThread {
                result.onSuccess {
                    Toast.makeText(context, context.getString(R.string.ui_custom_api_verified), Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, context.getString(R.string.ui_custom_api_verification_failed_1_s , it.message), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    val primaryColor = MaterialTheme.colorScheme.primary

    // Provider Selection
    Text(stringResource(R.string.ui_ai_provider), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(8.dp))
    ExposedDropdownMenuBox(expanded = providerExpanded, onExpandedChange = { providerExpanded = !providerExpanded }, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(value = selectedProvider.uppercase(), onValueChange = {}, readOnly = true, label = { Text(stringResource(R.string.ui_select_provider)) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) }, colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(), modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = providerExpanded, onDismissRequest = { providerExpanded = false }) { providers.forEach { item -> DropdownMenuItem(text = { Text(text = item.uppercase()) }, onClick = { selectedProvider = item; providerExpanded = false }) } }
    }
    
    Spacer(Modifier.height(24.dp))
    HorizontalDivider()
    Spacer(Modifier.height(24.dp))

    if (selectedProvider == "gemini") {
        // Gemini Setup
        if (config.savedGeminiConfigs.isNotEmpty()) {
            Text(stringResource(R.string.ui_saved_gemini_configs), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = primaryColor)
            Spacer(Modifier.height(8.dp))
            config.savedGeminiConfigs.forEach { savedConfig ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                             geminiKey = savedConfig.apiKey
                             geminiModel = savedConfig.model
                        },
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = savedConfig.model.ifBlank { stringResource(R.string.ui_unknown_model) },
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(text = stringResource(R.string.ui_key) + savedConfig.apiKey.take(4) + "...", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = {
                        val newSavedList = config.savedGeminiConfigs.toMutableList()
                        newSavedList.remove(savedConfig)
                        onSave(config.copy(savedGeminiConfigs = newSavedList))
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.ui_delete), tint = MaterialTheme.colorScheme.error)
                    }
                }
                HorizontalDivider()
            }
            Spacer(Modifier.height(16.dp))
        }

        OutlinedTextField(value = geminiKey, onValueChange = { geminiKey = it }, label = { Text(stringResource(R.string.ui_gemini_api_key)) }, modifier = Modifier.fillMaxWidth(), visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { isKeyVisible = !isKeyVisible }) { Icon(if (isKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null) } })
        Spacer(Modifier.height(16.dp))
        ExposedDropdownMenuBox(expanded = modelExpanded, onExpandedChange = { modelExpanded = !modelExpanded }, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = geminiModel,
                onValueChange = { geminiModel = it },
                readOnly = false,
                label = { Text(stringResource(R.string.ui_gemini_model_name)) },
                placeholder = { Text(stringResource(R.string.ui_e_g_gemini_2_5_flash)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                geminiModels.forEach { item ->
                    DropdownMenuItem(text = { Text(text = item) }, onClick = { geminiModel = item; modelExpanded = false })
                }
            }
        }
    } else if (selectedProvider == "cloudflare") {
        // Cloudflare Setup
        if (config.savedCloudflareConfigs.isNotEmpty()) {
            Text(stringResource(R.string.ui_saved_cloudflare_configs), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = primaryColor)
            Spacer(Modifier.height(8.dp))
            config.savedCloudflareConfigs.forEach { savedConfig ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                             cfAccountId = savedConfig.accountId
                             cfApiToken = savedConfig.apiToken
                             cfModel = savedConfig.model
                        },
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = savedConfig.model.ifBlank { stringResource(R.string.ui_unknown_model) },
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(text = stringResource(R.string.ui_account) + savedConfig.accountId.take(4) + "...", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = {
                        val newSavedList = config.savedCloudflareConfigs.toMutableList()
                        newSavedList.remove(savedConfig)
                        onSave(config.copy(savedCloudflareConfigs = newSavedList))
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.ui_delete), tint = MaterialTheme.colorScheme.error)
                    }
                }
                HorizontalDivider()
            }
            Spacer(Modifier.height(16.dp))
        }

        OutlinedTextField(value = cfAccountId, onValueChange = { cfAccountId = it }, label = { Text(stringResource(R.string.ui_cloudflare_account_id)) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = cfApiToken, onValueChange = { cfApiToken = it }, label = { Text(stringResource(R.string.ui_cloudflare_api_token)) }, modifier = Modifier.fillMaxWidth(), visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { isKeyVisible = !isKeyVisible }) { Icon(if (isKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null) } })
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = cfModel, onValueChange = { cfModel = it }, label = { Text(stringResource(R.string.ui_cloudflare_model_id)) }, modifier = Modifier.fillMaxWidth(), placeholder = { Text(stringResource(R.string.ui_cf_meta_llama_3_8b_instruct)) })
    } else if (selectedProvider == "custom") {
        // Custom API Setup
        if (config.savedCustomConfigs.isNotEmpty()) {
            Text(stringResource(R.string.ui_saved_configurations), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = primaryColor)
            Spacer(Modifier.height(8.dp))
            config.savedCustomConfigs.forEach { savedConfig ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                             customBaseUrl = savedConfig.baseUrl
                             customApiKey = savedConfig.apiKey
                             customModel = savedConfig.model
                        },
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = savedConfig.model.ifBlank { stringResource(R.string.ui_unknown_model) },
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(text = savedConfig.baseUrl, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = {
                        val newSavedList = config.savedCustomConfigs.toMutableList()
                        newSavedList.remove(savedConfig)
                        onSave(config.copy(savedCustomConfigs = newSavedList))
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.ui_delete), tint = MaterialTheme.colorScheme.error)
                    }
                }
                HorizontalDivider()
            }
            Spacer(Modifier.height(16.dp))
        }

        OutlinedTextField(value = customBaseUrl, onValueChange = { customBaseUrl = it }, label = { Text(stringResource(R.string.ui_base_url)) }, modifier = Modifier.fillMaxWidth(), placeholder = { Text(stringResource(R.string.ui_https_api_openai_com_v1)) })
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = customApiKey, onValueChange = { customApiKey = it }, label = { Text(stringResource(R.string.ui_api_key_optional)) }, modifier = Modifier.fillMaxWidth(), visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { isKeyVisible = !isKeyVisible }) { Icon(if (isKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null) } })
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = customModel, onValueChange = { customModel = it }, label = { Text(stringResource(R.string.ui_model_name)) }, modifier = Modifier.fillMaxWidth(), placeholder = { Text(stringResource(R.string.ui_gpt_3_5_turbo)) })
    } else if (selectedProvider == "local") {
        // Local LLM Saved Models List
        if (config.savedLocalModels.isNotEmpty()) {
            Text(stringResource(R.string.ui_previously_selected_models), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = primaryColor)
            Spacer(Modifier.height(8.dp))
            config.savedLocalModels.forEach { savedPath ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                             // Update the config with selected model
                             onSave(config.copy(localLlmConfig = config.localLlmConfig.copy(modelPath = savedPath)))
                             Toast.makeText(context, context.getString(R.string.ui_model_selected_1_s , getFileName(context, savedPath)), Toast.LENGTH_SHORT).show()
                        },
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val isCurrent = config.localLlmConfig.modelPath == savedPath
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = getFileName(context, savedPath),
                            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                            color = if (isCurrent) primaryColor else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = {
                        val newSavedList = config.savedLocalModels.toMutableList()
                        newSavedList.remove(savedPath)
                        onSave(config.copy(savedLocalModels = newSavedList))
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.ui_delete), tint = MaterialTheme.colorScheme.error)
                    }
                }
                HorizontalDivider()
            }
            Spacer(Modifier.height(16.dp))
        }
        
        Text(stringResource(R.string.ui_go_to_the_local_llm_tab_to_select_a_ne), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Spacer(Modifier.height(24.dp))
    
    Button(
        onClick = { 
            val newCustomConfig = CustomApiConfig(
                baseUrl = customBaseUrl.trim(),
                apiKey = customApiKey.trim(),
                model = customModel.trim()
            )
            val newGeminiConfig = com.typeassist.app.data.SavedGeminiConfig(
                apiKey = geminiKey.trim(),
                model = geminiModel.trim()
            )
            val newCloudflareConfig = CloudflareConfig(
                accountId = cfAccountId.trim(),
                apiToken = cfApiToken.trim(),
                model = cfModel.trim()
            )

            val updatedSavedCustom = config.savedCustomConfigs.toMutableList()
            if (selectedProvider == "custom" && customBaseUrl.isNotBlank() && customModel.isNotBlank()) {
                 if (updatedSavedCustom.none { it == newCustomConfig }) updatedSavedCustom.add(newCustomConfig)
            }

            val updatedSavedGemini = config.savedGeminiConfigs.toMutableList()
            if (selectedProvider == "gemini" && geminiKey.isNotBlank()) {
                 if (updatedSavedGemini.none { it == newGeminiConfig }) updatedSavedGemini.add(newGeminiConfig)
            }

            val updatedSavedCloudflare = config.savedCloudflareConfigs.toMutableList()
            if (selectedProvider == "cloudflare" && cfApiToken.isNotBlank()) {
                 if (updatedSavedCloudflare.none { it == newCloudflareConfig }) updatedSavedCloudflare.add(newCloudflareConfig)
            }

            val updatedSavedLocal = config.savedLocalModels.toMutableList()
            if (selectedProvider == "local" && config.localLlmConfig.modelPath.isNotBlank()) {
                if (updatedSavedLocal.none { it == config.localLlmConfig.modelPath }) {
                    updatedSavedLocal.add(config.localLlmConfig.modelPath)
                }
            }

            var newConfig = config.copy(
                provider = selectedProvider,
                apiKey = geminiKey.trim(),
                model = geminiModel.trim(),
                cloudflareConfig = newCloudflareConfig,
                customApiConfig = newCustomConfig,
                localLlmConfig = config.localLlmConfig, // This is updated via the sliders in LocalLlmSetup
                savedCustomConfigs = updatedSavedCustom,
                savedGeminiConfigs = updatedSavedGemini,
                savedCloudflareConfigs = updatedSavedCloudflare,
                savedLocalModels = updatedSavedLocal
            )
            
            // Auto-enable logic
            val hasValidKey = when (selectedProvider) {
                "gemini" -> geminiKey.isNotBlank()
                "cloudflare" -> cfApiToken.isNotBlank()
                "custom" -> customBaseUrl.isNotBlank() && customModel.isNotBlank()
                "local" -> config.localLlmConfig.modelPath.isNotBlank()
                else -> false
            }
            
            if (!config.isAppEnabled && hasValidKey) {
                 newConfig = newConfig.copy(isAppEnabled = true)
            }
            
            onSave(newConfig)
            Toast.makeText(context, context.getString(R.string.ui_config_saved), Toast.LENGTH_SHORT).show()
            
            when (selectedProvider) {
                "gemini" -> if (geminiKey.isNotBlank()) verifyGemini(geminiKey.trim())
                "cloudflare" -> if (cfApiToken.isNotBlank()) verifyCloudflare(cfAccountId.trim(), cfApiToken.trim(), cfModel.trim())
                "custom" -> if (customBaseUrl.isNotBlank()) verifyCustomApi(customBaseUrl.trim(), customApiKey.trim(), customModel.trim())
            }
        }, 
        modifier = Modifier.fillMaxWidth().height(50.dp), 
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) { 
        Text(stringResource(R.string.ui_test_and_save_config))
    } 
    
    Spacer(Modifier.height(32.dp))
    
    when (selectedProvider) {
        "gemini" -> GeminiHelp(primaryColor, context)
        "cloudflare" -> CloudflareHelp(primaryColor, context)
        "custom" -> CustomApiHelp(primaryColor, context)
        "local" -> LocalLlmHelp(primaryColor, context)
    }
}

@Composable
fun LocalLlmSetup(config: AppConfig, onSave: (AppConfig) -> Unit) {
    val context = LocalContext.current
    var modelPath by remember { mutableStateOf(config.localLlmConfig.modelPath) }
    var temperature by remember { mutableStateOf(config.localLlmConfig.temperature) }
    var topP by remember { mutableStateOf(config.localLlmConfig.topP) }
    var maxTokens by remember { mutableStateOf(config.localLlmConfig.maxTokens.toFloat()) }
    var threads by remember { mutableStateOf(config.localLlmConfig.numThreads.toFloat()) }
    var useGpu by remember { mutableStateOf(config.localLlmConfig.useGpu) }
    var disableReasoning by remember { mutableStateOf(config.localLlmConfig.disableReasoning) }

    val cacheFile = remember(modelPath) { java.io.File(context.cacheDir, "local_model.gguf") }
    var cachedSize by remember(modelPath) { mutableStateOf(if (cacheFile.exists()) cacheFile.length() else 0L) }

    fun clearLocalModelStorage() {
        val freedBytes = if (cacheFile.exists()) cacheFile.length() else 0L
        if (cacheFile.exists()) {
            try {
                cacheFile.delete()
            } catch (e: Exception) {
                android.util.Log.e("SettingsScreen", "Failed to delete cached model file: ${e.message}")
            }
        }
        cachedSize = 0L
        modelPath = ""
        onSave(config.copy(localLlmConfig = config.localLlmConfig.copy(modelPath = "")))
        val freedText = if (freedBytes > 0) " Freed ${formatFileSize(freedBytes)} of storage." else ""
        Toast.makeText(context, context.getString(R.string.ui_local_model_reference_cleared_1_s , freedText), Toast.LENGTH_LONG).show()
    }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                android.util.Log.w("SettingsScreen", "Could not take persistable permission: ${e.message}")
            }
            modelPath = it.toString()
            onSave(config.copy(localLlmConfig = config.localLlmConfig.copy(modelPath = modelPath)))
        }
    }

    Text(stringResource(R.string.ui_model_configuration), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(8.dp))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.ui_selected_model), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = getFileName(context, modelPath),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            if (cachedSize > 0L) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(stringResource(R.string.ui_internal_app_cache), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text(formatFileSize(cachedSize), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { launcher.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (modelPath.isBlank()) stringResource(R.string.ui_select_model) else stringResource(R.string.ui_change_model))
                }

                if (modelPath.isNotBlank() || cachedSize > 0L) {
                    OutlinedButton(
                        onClick = { clearLocalModelStorage() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.ui_clear_storage), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.ui_clear_model_cache))
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.ui_low_parameter_models_e_g_under_1_5b_pa),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                    lineHeight = 14.sp
                )
            }
        }
    }

    Spacer(Modifier.height(24.dp))

    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.ui_use_gpu_acceleration_vulkan), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                stringResource(R.string.ui_significantly_faster_on_modern_devices),
                fontSize = 11.sp, 
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = useGpu,
            onCheckedChange = { 
                useGpu = it
                onSave(config.copy(localLlmConfig = config.localLlmConfig.copy(useGpu = it)))
            }
        )
    }

    Spacer(Modifier.height(16.dp))

    // --- Reasoning Model Detection ---
    val looksLikeReasoningModel = isReasoningModel(modelPath)
    if (looksLikeReasoningModel) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.ui_label), fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.ui_reasoning_model_detected),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        stringResource(R.string.ui_this_model_outputs_internal_thinking_u) +
                        stringResource(R.string.ui_typeassist_strips_thinking),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        lineHeight = 14.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    // --- Disable Reasoning Toggle ---
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.ui_disable_reasoning_output), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                if (looksLikeReasoningModel)
                    stringResource(R.string.ui_send_nothink_hint)
                else
                    stringResource(R.string.ui_reasoning_suppress_thinking),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )
        }
        Switch(
            checked = disableReasoning,
            onCheckedChange = {
                disableReasoning = it
                onSave(config.copy(localLlmConfig = config.localLlmConfig.copy(disableReasoning = it)))
            }
        )
    }

    Spacer(Modifier.height(16.dp))
    
    Text(stringResource(R.string.ui_temperature_1_s, String.format("%.2f", temperature)), fontSize = 14.sp)
    Slider(value = temperature, onValueChange = { 
        temperature = it
        onSave(config.copy(localLlmConfig = config.localLlmConfig.copy(temperature = it)))
    }, valueRange = 0f..2f)

    Spacer(Modifier.height(8.dp))

    Text(stringResource(R.string.ui_top_p_1_s, String.format("%.2f", topP)), fontSize = 14.sp)
    Slider(value = topP, onValueChange = { 
        topP = it
        onSave(config.copy(localLlmConfig = config.localLlmConfig.copy(topP = it)))
    }, valueRange = 0f..1f)

    Spacer(Modifier.height(8.dp))

    Text(stringResource(R.string.ui_max_tokens_1_s, maxTokens.toInt()), fontSize = 14.sp)
    Slider(value = maxTokens, onValueChange = { 
        maxTokens = it
        onSave(config.copy(localLlmConfig = config.localLlmConfig.copy(maxTokens = it.toInt())))
    }, valueRange = 64f..2048f, steps = 31)

    Spacer(Modifier.height(8.dp))

    Text(stringResource(R.string.ui_threads_1_s, threads.toInt()), fontSize = 14.sp)
    Slider(value = threads, onValueChange = { 
        threads = it
        onSave(config.copy(localLlmConfig = config.localLlmConfig.copy(numThreads = it.toInt())))
    }, valueRange = 1f..8f, steps = 7)
}

@Composable
fun LocalLlmHelp(primaryColor: Color, context: android.content.Context) {
    Text(stringResource(R.string.ui_local_llm_llama_cpp_setup), fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Spacer(Modifier.height(16.dp))
    Text(stringResource(R.string.ui_run_ai_entirely_on_your_device_with_no), fontSize = 14.sp)
    Spacer(Modifier.height(8.dp))
    Text(stringResource(R.string.ui_1_download_a_gguf_model_from_hugging_f), fontSize = 14.sp)
    Text(stringResource(R.string.ui_2_place_the_file_in_your_device_storag), fontSize = 14.sp)
    Text(stringResource(R.string.ui_3_provide_the_full_absolute_path_to_th), fontSize = 14.sp)
    Text(stringResource(R.string.ui_4_set_threads_to_match_your_cpu_cores_), fontSize = 14.sp)
    Spacer(Modifier.height(8.dp))
    Text(stringResource(R.string.ui_warning_local_inference_is_slow_on_old), fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
}

@Composable
fun GeminiHelp(primaryColor: Color, context: android.content.Context) {
    Text(stringResource(R.string.ui_how_to_get_gemini_api_key), fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(Modifier.height(16.dp))
    val steps = listOf(stringResource(R.string.ui_gemini_sign_in), stringResource(R.string.ui_gemini_get_key), stringResource(R.string.ui_gemini_create_key), stringResource(R.string.ui_gemini_copy_key), stringResource(R.string.ui_gemini_select_model), stringResource(R.string.ui_gemini_save))
    val annotatedString = buildAnnotatedString {
        append(stringResource(R.string.ui_gemini_visit_prefix))
        pushStringAnnotation(tag = "URL", annotation = "https://aistudio.google.com/")
        withStyle(style = SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold)) { append(stringResource(R.string.ui_gemini_link)) }
        pop(); append(".\n")
        steps.forEach { append(it + "\n") }
    }
    ClickableText(text = annotatedString, style = LocalTextStyle.current.copy(fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface), onClick = { offset -> annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset).firstOrNull()?.let { annotation -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))) } })
}

@Composable
fun CloudflareHelp(primaryColor: Color, context: android.content.Context) {
    Text(stringResource(R.string.ui_how_to_setup_cloudflare_ai), fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(Modifier.height(16.dp))
    val steps = listOf(
        stringResource(R.string.ui_cloudflare_sign_in), stringResource(R.string.ui_cloudflare_rest_api),
        stringResource(R.string.ui_cloudflare_create_token), stringResource(R.string.ui_cloudflare_copy_key),
        stringResource(R.string.ui_cloudflare_account_id), stringResource(R.string.ui_cloudflare_paste_ids),
        stringResource(R.string.ui_cloudflare_test_save)
    )
    val annotatedString = buildAnnotatedString {
        append(stringResource(R.string.ui_cloudflare_visit_prefix))
        pushStringAnnotation(tag = "URL", annotation = "https://dash.cloudflare.com/?to=/:account/ai/workers-ai")
        withStyle(style = SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold)) { append(stringResource(R.string.ui_cloudflare_link)) }
        pop(); append(".\n")
        steps.forEach { append(it + "\n") }
    }
    ClickableText(text = annotatedString, style = LocalTextStyle.current.copy(fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface), onClick = { offset -> annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset).firstOrNull()?.let { annotation -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))) } })
}

@Composable
fun CustomApiHelp(primaryColor: Color, context: android.content.Context) {
    Text(stringResource(R.string.ui_custom_api_setup), fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Spacer(Modifier.height(16.dp))
    Text(stringResource(R.string.ui_compatible_with_any_openai_style_chat_), fontSize = 14.sp)
    Spacer(Modifier.height(8.dp))
    Text(stringResource(R.string.ui_1_base_url_the_api_endpoint_e_g_https_), fontSize = 14.sp)
    Text(stringResource(R.string.ui_2_api_key_your_provider_s_api_key_leav), fontSize = 14.sp)
    Text(stringResource(R.string.ui_3_model_name_the_specific_model_id_e_g), fontSize = 14.sp)
}

private fun getFileName(context: android.content.Context, uriOrPath: String): String {
    if (uriOrPath.isBlank()) return context.getString(R.string.ui_no_model_selected)
    if (!uriOrPath.startsWith("content://")) {
        return uriOrPath.substringAfterLast("/")
    }
    return try {
        val uri = Uri.parse(uriOrPath)
        var name: String? = null
        
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx != -1) {
                    name = cursor.getString(idx)
                }
            }
        }

        if (name.isNullOrBlank()) {
            val lastSeg = Uri.decode(uri.lastPathSegment ?: "")
            name = when {
                lastSeg.contains(":") -> lastSeg.substringAfterLast(":")
                lastSeg.contains("/") -> lastSeg.substringAfterLast("/")
                else -> lastSeg
            }
        }

        if (!name.isNullOrBlank()) name!! else "Local GGUF Model"
    } catch (e: Exception) {
        val last = uriOrPath.substringAfterLast("/")
        Uri.decode(last).substringAfterLast(":")
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val clampedGroup = digitGroups.coerceIn(0, units.size - 1)
    return String.format("%.2f %s", bytes / Math.pow(1024.0, clampedGroup.toDouble()), units[clampedGroup])
}
