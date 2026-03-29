package com.typeassist.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.typeassist.app.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandLibraryScreen(config: AppConfig, onSave: (AppConfig) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedTemplate by remember { mutableStateOf<CommandTemplate?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Command Gallery") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            item {
                Text(
                    "Discover Pro Commands",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Tap '+' to add a command to your collection.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            items(CommandLibrary.templates) { template ->
                TemplateCard(
                    template = template,
                    onAdd = {
                        val currentTriggers = config.triggers ?: mutableListOf()
                        val currentInline = config.inlineCommands ?: mutableListOf()
                        
                        val exists = if (template.isInline) {
                            currentInline.any { it.pattern == template.recommendedTrigger }
                        } else {
                            currentTriggers.any { it.pattern == template.recommendedTrigger }
                        }

                        if (exists) {
                            Toast.makeText(context, "Trigger '${template.recommendedTrigger}' already exists!", Toast.LENGTH_SHORT).show()
                        } else {
                            if (template.isInline) {
                                val newList = currentInline.toMutableList()
                                newList.add(InlineCommand(template.recommendedTrigger, template.systemPrompt))
                                onSave(config.copy(inlineCommands = newList))
                            } else {
                                val newList = currentTriggers.toMutableList()
                                newList.add(Trigger(template.recommendedTrigger, template.systemPrompt))
                                onSave(config.copy(triggers = newList))
                            }
                            Toast.makeText(context, "Added '${template.title}' to your commands! ✅", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onViewInfo = { selectedTemplate = template }
                )
            }
            
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }

        if (selectedTemplate != null) {
            AlertDialog(
                onDismissRequest = { selectedTemplate = null },
                title = { Text(selectedTemplate?.title ?: "") },
                text = {
                    Column {
                        Text(selectedTemplate?.description ?: "", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(12.dp))
                        Text("Trigger: ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(selectedTemplate?.recommendedTrigger ?: "", fontSize = 14.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("System Prompt:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(selectedTemplate?.systemPrompt ?: "", fontSize = 13.sp, lineHeight = 18.sp)
                    }
                },
                confirmButton = {
                    Button(onClick = { selectedTemplate = null }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun TemplateCard(template: CommandTemplate, onAdd: () -> Unit, onViewInfo: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        template.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (template.isInline) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "INLINE",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                Text(
                    template.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Trigger: ${template.recommendedTrigger}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }

            IconButton(onClick = onViewInfo) {
                Icon(Icons.Default.Info, "Info", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            IconButton(
                onClick = onAdd,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.Default.Add, "Add")
            }
        }
    }
}
