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
import com.typeassist.app.data.Snippet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetsScreen(config: AppConfig, onSave: (AppConfig) -> Unit, onBack: () -> Unit) {
    var showEditDialog by remember { mutableStateOf(false) }
    var tTrigger by remember { mutableStateOf("") }
    var tContent by remember { mutableStateOf("") }
    
    var originalTrigger by remember { mutableStateOf<String?>(null) }
    var snippetToDelete by remember { mutableStateOf<Snippet?>(null) }
    
    val snippets = config.snippets ?: mutableListOf()

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
                title = { Text("Snippets") }, 
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryColor, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            ) 
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = { 
            FloatingActionButton(
                onClick = { 
                    tTrigger = ""
                    tContent = ""
                    originalTrigger = null
                    showEditDialog = true 
                }, 
                containerColor = MaterialTheme.colorScheme.primary
            ) { 
                Icon(Icons.Default.Add, "Add New Snippet") 
            } 
        }
    ) { p ->
        Column(modifier = Modifier.padding(p)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Usage:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Type '${config.snippetTriggerPrefix}' + Trigger Name to expand.", fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Quick Save:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Type '(.save:name:content)' to save instantly.", fontSize = 14.sp)
                }
            }

            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                items(snippets) { s ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { 
                                tTrigger = s.trigger
                                tContent = s.content
                                originalTrigger = s.trigger
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
                                Text(s.trigger, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                                Text(s.content, maxLines = 1, fontSize = 12.sp, color = Color.Gray) 
                            }
                            IconButton(onClick = { snippetToDelete = s }) { 
                                Icon(Icons.Default.Delete, "Delete", tint = Color.Red) 
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text(if (originalTrigger == null) "New Snippet" else "Edit Snippet") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = tTrigger, 
                            onValueChange = { tTrigger = it }, 
                            label = { Text("Trigger Name (e.g. email)") },
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tContent, 
                            onValueChange = { tContent = it }, 
                            label = { Text("Content") }, 
                            minLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val n = snippets.toMutableList()
                        if (originalTrigger != null) n.removeIf { it.trigger == originalTrigger }
                        n.removeIf { it.trigger == tTrigger }
                        n.add(Snippet(tTrigger, tContent))
                        onSave(config.copy(snippets = n))
                        showEditDialog = false
                    }) { Text("Save") }
                },
                dismissButton = { 
                    TextButton(onClick = { showEditDialog = false }) { Text("Cancel") } 
                }
            )
        }

        if (snippetToDelete != null) {
            AlertDialog(
                onDismissRequest = { snippetToDelete = null },
                title = { Text("Delete Snippet?") },
                text = { Text("Are you sure you want to delete '${snippetToDelete?.trigger}'?") },
                confirmButton = {
                    Button(
                        onClick = {
                            val n = snippets.toMutableList()
                            n.remove(snippetToDelete)
                            onSave(config.copy(snippets = n))
                            snippetToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = { 
                    TextButton(onClick = { snippetToDelete = null }) { Text("Cancel") } 
                }
            )
        }
    }
}
