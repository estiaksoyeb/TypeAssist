package com.typeassist.app.ui.screens

import com.typeassist.app.R

import android.app.Activity
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.runtime.saveable.rememberSaveable
import com.typeassist.app.data.AppConfig
import com.typeassist.app.data.InlineCommand
import com.typeassist.app.data.Trigger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandsScreen(config: AppConfig, onSave: (AppConfig) -> Unit, onBack: () -> Unit, onNavigateLibrary: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showTip by rememberSaveable { mutableStateOf(true) }
    var tPattern by remember { mutableStateOf("") }
    var tPrompt by remember { mutableStateOf("") }
    
    // We use a simple way to track what we are editing: if it matches an existing pattern.
    // Ideally we should track the original object, but for this simple app, pattern is the key.
    var originalPattern by remember { mutableStateOf<String?>(null) }
    
    var triggerToDelete by remember { mutableStateOf<Trigger?>(null) }
    var inlineToDelete by remember { mutableStateOf<InlineCommand?>(null) }
    
    val triggers = config.triggers ?: mutableListOf()
    val inlineCommands = config.inlineCommands ?: mutableListOf()

    val view = LocalView.current
    val primaryColor = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text(stringResource(R.string.ui_commands)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, stringResource(R.string.ui_back)) } },
                actions = {
                    IconButton(onClick = onNavigateLibrary) {
                        Icon(Icons.Default.AutoAwesome, stringResource(R.string.ui_browse_gallery), tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                ) 
            ) 
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = { 
            FloatingActionButton(
                onClick = { 
                    tPattern = if (selectedTab == 0) "@" else "(.ta:%)"
                    tPrompt = ""
                    originalPattern = null
                    showEditDialog = true 
                }, 
                containerColor = MaterialTheme.colorScheme.primary
            ) { 
                Icon(Icons.Default.Add, stringResource(R.string.ui_add_new_command))
            } 
        }
    ) { p ->
        Column(modifier = Modifier.padding(p)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(stringResource(R.string.ui_standard)) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(stringResource(R.string.ui_inline_2)) })
            }

            LazyColumn(modifier = Modifier.padding(16.dp)) {
                if (showTip) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lightbulb, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.ui_pro_tip_edit_mid_sentence), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Spacer(Modifier.weight(1f))
                                    IconButton(onClick = { showTip = false }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, stringResource(R.string.ui_dismiss), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    stringResource(R.string.ui_you_can_create_custom_commands_to_tran),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    stringResource(R.string.ui_example_create_a_command_syn_with_prom),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                if (selectedTab == 0) {
                    items(triggers) { t ->
                        CommandItem(
                            pattern = t.pattern, 
                            prompt = t.prompt, 
                            onEdit = { 
                                tPattern = t.pattern
                                tPrompt = t.prompt
                                originalPattern = t.pattern
                                showEditDialog = true
                            },
                            onDelete = { triggerToDelete = t }
                        )
                    }
                } else {
                    items(inlineCommands) { t ->
                        CommandItem(
                            pattern = t.pattern, 
                            prompt = t.prompt, 
                            onEdit = { 
                                tPattern = t.pattern
                                tPrompt = t.prompt
                                originalPattern = t.pattern
                                showEditDialog = true
                            },
                            onDelete = { inlineToDelete = t }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text(if (originalPattern == null) stringResource(R.string.ui_new_command) else stringResource(R.string.ui_edit_command)) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = tPattern, 
                            onValueChange = { tPattern = it }, 
                            label = { Text(stringResource(R.string.ui_pattern)) },
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tPrompt, 
                            onValueChange = { tPrompt = it }, 
                            label = { Text(stringResource(R.string.ui_system_prompt_2)) },
                            minLines = 3
                        )
                        if (selectedTab == 1) {
                            Text(stringResource(R.string.ui_use_as_placeholder_for_user_text), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (selectedTab == 0) {
                            val n = triggers.toMutableList()
                            if (originalPattern != null) n.removeIf { it.pattern == originalPattern }
                            // Also remove if new pattern conflicts? existing logic just removed by trigger name.
                            // We should probably just add/update.
                            n.removeIf { it.pattern == tPattern } 
                            n.add(Trigger(tPattern, tPrompt))
                            onSave(config.copy(triggers = n))
                        } else {
                            val n = inlineCommands.toMutableList()
                            if (originalPattern != null) n.removeIf { it.pattern == originalPattern }
                            n.removeIf { it.pattern == tPattern }
                            n.add(InlineCommand(tPattern, tPrompt))
                            onSave(config.copy(inlineCommands = n))
                        }
                        showEditDialog = false
                    }) { Text(stringResource(R.string.ui_save)) }
                },
                dismissButton = { 
                    TextButton(onClick = { showEditDialog = false }) { Text(stringResource(R.string.ui_cancel)) }
                }
            )
        }

        if (triggerToDelete != null) {
            DeleteDialog(
                pattern = triggerToDelete?.pattern ?: "",
                onConfirm = {
                    val n = triggers.toMutableList()
                    n.remove(triggerToDelete)
                    onSave(config.copy(triggers = n))
                    triggerToDelete = null
                },
                onDismiss = { triggerToDelete = null }
            )
        }
        
        if (inlineToDelete != null) {
            DeleteDialog(
                pattern = inlineToDelete?.pattern ?: "",
                onConfirm = {
                    val n = inlineCommands.toMutableList()
                    n.remove(inlineToDelete)
                    onSave(config.copy(inlineCommands = n))
                    inlineToDelete = null
                },
                onDismiss = { inlineToDelete = null }
            )
        }
    }
}

@Composable
fun CommandItem(pattern: String, prompt: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable { onEdit() }, 
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) { 
                Text(pattern, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                Text(prompt, maxLines = 1, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) 
            }
            IconButton(onClick = onDelete) { 
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) 
            }
        }
    }
}

@Composable
fun DeleteDialog(pattern: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ui_delete_command)) },
        text = { Text(stringResource(R.string.ui_are_you_sure_you_want_to_delete_1_s, pattern)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text(stringResource(R.string.ui_delete)) }
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel)) }
        }
    )
}