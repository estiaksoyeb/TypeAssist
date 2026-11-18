package com.typeassist.app.ui.screens

import com.typeassist.app.R

import android.widget.Toast
import androidx.compose.ui.res.stringResource
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
                title = { Text(stringResource(R.string.ui_command_gallery)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.ui_back))
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
                    stringResource(R.string.ui_discover_pro_commands),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(R.string.ui_tap_to_add_a_command_to_your_collectio),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            items(CommandLibrary.templates) { template ->
                val recommendedTrigger = stringResource(template.recommendedTriggerRes)
                TemplateCard(
                    template = template,
                    onAdd = {
                        val currentTriggers = config.triggers ?: mutableListOf()
                        val currentInline = config.inlineCommands ?: mutableListOf()
                        
                        val exists = if (template.isInline) {
                            currentInline.any { it.pattern == recommendedTrigger }
                        } else {
                            currentTriggers.any { it.pattern == recommendedTrigger }
                        }

                        if (exists) {
                            Toast.makeText(context, context.getString(R.string.ui_trigger_1_s_already_exists , recommendedTrigger), Toast.LENGTH_SHORT).show()
                        } else {
                            if (template.isInline) {
                                val newList = currentInline.toMutableList()
                                newList.add(InlineCommand(recommendedTrigger, template.systemPrompt))
                                onSave(config.copy(inlineCommands = newList))
                            } else {
                                val newList = currentTriggers.toMutableList()
                                newList.add(Trigger(recommendedTrigger, template.systemPrompt))
                                onSave(config.copy(triggers = newList))
                            }
                            Toast.makeText(context, context.getString(R.string.ui_added_1_s_to_your_commands , context.getString(template.titleRes)), Toast.LENGTH_SHORT).show()
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
                title = { Text(selectedTemplate?.let { stringResource(it.titleRes) } ?: "") },
                text = {
                    Column {
                        Text(selectedTemplate?.let { stringResource(it.descriptionRes) } ?: "", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.ui_trigger), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(selectedTemplate?.let { stringResource(it.recommendedTriggerRes) } ?: "", fontSize = 14.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.ui_system_prompt), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(selectedTemplate?.systemPrompt ?: "", fontSize = 13.sp, lineHeight = 18.sp)
                    }
                },
                confirmButton = {
                    Button(onClick = { selectedTemplate = null }) {
                        Text(stringResource(R.string.ui_close))
                    }
                }
            )
        }
    }
}

@Composable
fun TemplateCard(template: CommandTemplate, onAdd: () -> Unit, onViewInfo: () -> Unit) {
    val title = stringResource(template.titleRes)
    val description = stringResource(template.descriptionRes)
    val recommendedTrigger = stringResource(template.recommendedTriggerRes)
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
                        title,
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
                                stringResource(R.string.ui_inline),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                Text(
                    description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.ui_trigger_1_s, recommendedTrigger),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }

            IconButton(onClick = onViewInfo) {
                Icon(Icons.Default.Info, stringResource(R.string.ui_info), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            IconButton(
                onClick = onAdd,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.Default.Add, stringResource(R.string.ui_add))
            }
        }
    }
}
