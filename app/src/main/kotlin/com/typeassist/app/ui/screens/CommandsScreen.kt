package com.typeassist.app.ui.screens

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
import com.typeassist.app.data.AppConfig
import com.typeassist.app.data.InlineCommand
import com.typeassist.app.data.Trigger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandsScreen(config: AppConfig, onSave: (AppConfig) -> Unit, onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    var showEditDialog by remember { mutableStateOf(false) }
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
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = primaryColor.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Commands") }, 
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryColor, titleContentColor = Color.White, navigationIconContentColor = Color.White)
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
                Icon(Icons.Default.Add, "Add New Command") 
            } 
        }
    ) { p ->
        Column(modifier = Modifier.padding(p)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = primaryColor,
                contentColor = Color.White
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Standard") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Inline") })
            }

            LazyColumn(modifier = Modifier.padding(16.dp)) {
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
                title = { Text(if (originalPattern == null) "New Command" else "Edit Command") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = tPattern, 
                            onValueChange = { tPattern = it }, 
                            label = { Text("Pattern") },
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tPrompt, 
                            onValueChange = { tPrompt = it }, 
                            label = { Text("System Prompt") }, 
                            minLines = 3
                        )
                        if (selectedTab == 1) {
                            Text("Use '%' as placeholder for user text.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
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
                    }) { Text("Save") }
                },
                dismissButton = { 
                    TextButton(onClick = { showEditDialog = false }) { Text("Cancel") } 
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) { 
                Text(pattern, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                Text(prompt, maxLines = 1, fontSize = 12.sp, color = Color.Gray) 
            }
            IconButton(onClick = onDelete) { 
                Icon(Icons.Default.Delete, "Delete", tint = Color.Red) 
            }
        }
    }
}

@Composable
fun DeleteDialog(pattern: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Command?") },
        text = { Text("Are you sure you want to delete '$pattern'?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete") }
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { Text("Cancel") } 
        }
    )
}