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
import com.typeassist.app.data.Trigger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandsScreen(config: AppConfig, onSave: (AppConfig) -> Unit, onBack: () -> Unit) {
    var showEditDialog by remember { mutableStateOf(false) }
    var tTrigger by remember { mutableStateOf("") }
    var tPrompt by remember { mutableStateOf("") }
    
    var commandToDelete by remember { mutableStateOf<Trigger?>(null) }
    
    val triggers = config.triggers ?: mutableListOf()

    val view = LocalView.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = surfaceColor.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Commands") }, 
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            ) 
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = { 
            FloatingActionButton(
                onClick = { 
                    tTrigger = "@"
                    tPrompt = ""
                    showEditDialog = true 
                }, 
                containerColor = MaterialTheme.colorScheme.primary
            ) { 
                Icon(Icons.Default.Add, "Add New Command") 
            } 
        }
    ) { p ->
        LazyColumn(modifier = Modifier.padding(p).padding(16.dp)) {
            items(triggers) { t ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { 
                            tTrigger = t.pattern
                            tPrompt = t.prompt
                            showEditDialog = true 
                        }, 
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) { 
                            Text(t.pattern, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                            Text(t.prompt, maxLines = 1, fontSize = 12.sp, color = Color.Gray) 
                        }
                        IconButton(onClick = { commandToDelete = t }) { 
                            Icon(Icons.Default.Delete, "Delete", tint = Color.Red) 
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("Edit Command") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = tTrigger, 
                            onValueChange = { tTrigger = it }, 
                            label = { Text("Trigger (e.g. !fix)") },
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tPrompt, 
                            onValueChange = { tPrompt = it }, 
                            label = { Text("System Prompt") }, 
                            minLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val n = triggers.toMutableList()
                        n.removeIf { it.pattern == tTrigger }
                        n.add(Trigger(tTrigger, tPrompt))
                        onSave(config.copy(triggers = n))
                        showEditDialog = false
                    }) { Text("Save") }
                },
                dismissButton = { 
                    TextButton(onClick = { showEditDialog = false }) { Text("Cancel") } 
                }
            )
        }

        if (commandToDelete != null) {
            AlertDialog(
                onDismissRequest = { commandToDelete = null },
                title = { Text("Delete Command?") },
                text = { Text("Are you sure you want to delete '${commandToDelete?.pattern}'?") },
                confirmButton = {
                    Button(
                        onClick = {
                            val n = triggers.toMutableList()
                            n.remove(commandToDelete)
                            onSave(config.copy(triggers = n))
                            commandToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = { 
                    TextButton(onClick = { commandToDelete = null }) { Text("Cancel") } 
                }
            )
        }
    }
}