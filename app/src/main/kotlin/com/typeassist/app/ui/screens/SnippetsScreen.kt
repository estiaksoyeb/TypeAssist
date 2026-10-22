package com.typeassist.app.ui.screens

import com.typeassist.app.R

import android.app.Activity
import androidx.compose.ui.res.stringResource
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
                title = { Text(stringResource(R.string.ui_snippets)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, stringResource(R.string.ui_back)) } },
                actions = {
                    IconButton(onClick = { isSortAlphabetical = !isSortAlphabetical }) {
                        Icon(
                            Icons.Default.Sort,
                            contentDescription = stringResource(R.string.ui_toggle_sort),
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
                Icon(Icons.Default.Add, stringResource(R.string.ui_add_new_snippet))
            } 
        }
    ) { p ->
        Column(modifier = Modifier.padding(p)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.ui_usage), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(stringResource(R.string.ui_type_1_s_trigger_name_to_expand, config.snippetTriggerPrefix), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.ui_quick_save), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(stringResource(R.string.ui_type_save_name_content_to_save_instant), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.ui_search_snippets)) },
                leadingIcon = { Icon(Icons.Default.Search, stringResource(R.string.ui_search)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, stringResource(R.string.ui_clear))
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
                title = { Text(if (originalTrigger == null) stringResource(R.string.ui_new_snippet) else stringResource(R.string.ui_edit_snippet)) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = tTrigger, 
                            onValueChange = { tTrigger = it }, 
                            label = { Text(stringResource(R.string.ui_trigger_name_e_g_email)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.ui_variations), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        
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
                                            label = { Text(stringResource(R.string.ui_variation_1_s, index + 1)) },
                                            modifier = Modifier.weight(1f),
                                            minLines = 1
                                        )
                                        if (tContents.size > 1) {
                                            IconButton(onClick = { tContents.removeAt(index) }) {
                                                Icon(Icons.Default.Close, stringResource(R.string.ui_remove), tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                                item {
                                    TextButton(
                                        onClick = { tContents.add("") }
                                    ) {
                                        Icon(Icons.Default.Add, stringResource(R.string.ui_add))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.ui_add_variation))
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
                                Toast.makeText(context, context.getString(R.string.ui_snippet_1_s_already_exists , tTrigger), Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (originalTrigger != null) n.removeIf { it.trigger == originalTrigger }
                            n.removeIf { it.trigger == tTrigger }
                            n.add(Snippet(tTrigger, contents = validContents))
                            onSave(config.copy(snippets = n))
                            showEditDialog = false
                        }
                    }) { Text(stringResource(R.string.ui_save)) }
                },
                dismissButton = { 
                    TextButton(onClick = { showEditDialog = false }) { Text(stringResource(R.string.ui_cancel)) }
                }
            )
        }

        if (snippetToDelete != null) {
            AlertDialog(
                onDismissRequest = { snippetToDelete = null },
                title = { Text(stringResource(R.string.ui_delete_snippet)) },
                text = { Text(stringResource(R.string.ui_are_you_sure_you_want_to_delete_1_s, snippetToDelete?.trigger.orEmpty())) },
                confirmButton = {
                    Button(
                        onClick = {
                            val n = snippets.toMutableList()
                            n.remove(snippetToDelete)
                            onSave(config.copy(snippets = n))
                            snippetToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text(stringResource(R.string.ui_delete)) }
                },
                dismissButton = { 
                    TextButton(onClick = { snippetToDelete = null }) { Text(stringResource(R.string.ui_cancel)) }
                }
            )
        }
    }
}
