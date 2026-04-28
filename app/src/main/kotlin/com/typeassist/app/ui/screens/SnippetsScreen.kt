package com.typeassist.app.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
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
    var tContents by remember { mutableStateOf(mutableStateListOf<String>()) }
    
    var originalTrigger by remember { mutableStateOf<String?>(null) }
    var snippetToDelete by remember { mutableStateOf<Snippet?>(null) }
    
    var searchQuery by remember { mutableStateOf("") }
    var isSortAlphabetical by remember { mutableStateOf(false) }
    
    val snippets = config.snippets ?: mutableListOf()
    val context = androidx.compose.ui.platform.LocalContext.current
    val filteredSnippets = remember(snippets, searchQuery, isSortAlphabetical) {
        val filtered = snippets.filter { s ->
            s.trigger.contains(searchQuery, ignoreCase = true) || 
            s.contents.any { it.contains(searchQuery, ignoreCase = true) }
        }
        if (isSortAlphabetical) {
            filtered.sortedBy { it.trigger.lowercase() }
        } else {
            filtered
        }
    }

    val view = LocalView.current
    val primaryColor = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Snippets") }, 
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { isSortAlphabetical = !isSortAlphabetical }) {
                        Icon(
                            Icons.Default.Sort,
                            contentDescription = "Toggle Sort",
                            tint = if (isSortAlphabetical) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.primary, navigationIconContentColor = MaterialTheme.colorScheme.primary)
            ) 
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = { 
            FloatingActionButton(
                onClick = { 
                    tTrigger = ""
                    tContents.clear()
                    tContents.add("")
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
                    Text("Usage:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text("Type '${config.snippetTriggerPrefix}' + Trigger Name to expand.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.height(4.dp))
                    Text("Quick Save:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text("Type '(.save:name:content)' to save instantly.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search snippets...") },
                leadingIcon = { Icon(Icons.Default.Search, "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )

            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                items(filteredSnippets) { s ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { 
                                tTrigger = s.trigger
                                tContents.clear()
                                tContents.addAll(s.contents)
                                if (tContents.isEmpty()) tContents.add("")
                                originalTrigger = s.trigger
                                showEditDialog = true 
                            }, 
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp), 
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) { 
                                Text(s.trigger, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                                val subText = if (s.contents.size > 1) "${s.contents.size} variations" else if (s.contents.isNotEmpty()) s.contents[0] else ""
                                Text(subText, maxLines = 1, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) 
                            }
                            IconButton(onClick = { snippetToDelete = s }) { 
                                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) 
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
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = tTrigger, 
                            onValueChange = { tTrigger = it }, 
                            label = { Text("Trigger Name (e.g. email)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Variations:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        
                        Box(modifier = Modifier.weight(1f, fill = false).heightIn(max = 300.dp)) {
                            LazyColumn {
                                itemsIndexed(tContents.toList()) { index, content ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = content,
                                            onValueChange = { tContents[index] = it },
                                            label = { Text("Variation ${index + 1}") },
                                            modifier = Modifier.weight(1f),
                                            minLines = 1
                                        )
                                        if (tContents.size > 1) {
                                            IconButton(onClick = { tContents.removeAt(index) }) {
                                                Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                                item {
                                    TextButton(
                                        onClick = { tContents.add("") }
                                    ) {
                                        Icon(Icons.Default.Add, "Add")
                                        Spacer(Modifier.width(8.dp))
                                        Text("Add Variation")
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val validContents = tContents.filter { it.isNotBlank() }.toMutableList()
                        if (tTrigger.isNotBlank() && validContents.isNotEmpty()) {
                            val n = snippets.toMutableList()

                            // Check for duplicate trigger if creating NEW snippet OR changing trigger of existing one
                            if (tTrigger != originalTrigger && n.any { it.trigger == tTrigger }) {
                                Toast.makeText(context, "Snippet '$tTrigger' already exists!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (originalTrigger != null) n.removeIf { it.trigger == originalTrigger }
                            n.removeIf { it.trigger == tTrigger }
                            n.add(Snippet(tTrigger, contents = validContents))
                            onSave(config.copy(snippets = n))
                            showEditDialog = false
                        }
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
