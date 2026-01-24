package com.typeassist.app.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.GsonBuilder
import com.typeassist.app.data.AppConfig
import com.typeassist.app.utils.BackupManager
import kotlinx.coroutines.launch
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JsonScreen(config: AppConfig, onSave: (AppConfig) -> Unit, onBack: () -> Unit) {
    val gson = GsonBuilder().setPrettyPrinting().create()
    var txt by remember { mutableStateOf(gson.toJson(config)) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // --- State for Dialogs ---
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestorePasswordDialog by remember { mutableStateOf(false) }
    var useEncryption by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    
    // Pending Uri for Restore (waiting for password)
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    // --- Launchers ---
    val createDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val bytes = BackupManager.exportBackup(config, if (useEncryption) password else null)
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        os.write(bytes)
                    }
                    Toast.makeText(context, "Backup Saved Successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    // Check if encrypted without consuming the stream? 
                    // BackupManager.importBackup handles logic. We might fail first if password needed.
                    // To do it cleanly, we read into byte array first or trust the manager.
                    // Let's rely on the manager returning (null, true) if password needed.
                    
                    // We need a fresh stream for the actual import
                    val verifyStream = context.contentResolver.openInputStream(it) ?: return@launch
                    val isEncrypted = BackupManager.isEncrypted(verifyStream)
                    verifyStream.close()
                    
                    if (isEncrypted) {
                        pendingRestoreUri = it
                        password = ""
                        showRestorePasswordDialog = true
                    } else {
                        // Import plain
                        context.contentResolver.openInputStream(it)?.use { ins ->
                            val (newConfig, _) = BackupManager.importBackup(ins, null)
                            if (newConfig != null) {
                                onSave(newConfig)
                                txt = gson.toJson(newConfig) // Update UI
                                Toast.makeText(context, "Restore Successful!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Read Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Backup & Restore") }, 
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }, 
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface) 
            ) 
        }
    ) { p ->
        Column(
            modifier = Modifier
                .padding(p)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // --- Actions Section ---
            Text("Data Management", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { 
                        password = ""
                        useEncryption = false
                        showBackupDialog = true 
                    }, 
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Backup to File")
                }
                
                OutlinedButton(
                    onClick = { openDocumentLauncher.launch(arrayOf("*/*")) }, 
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Restore from File")
                }
            }
            Text("Saves all settings, API keys, and snippets to a single .tabak file.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            // --- Raw JSON Section ---
            Text("Advanced: Raw JSON", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(8.dp))
            
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) { 
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { 
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.width(8.dp))
                    Text("Contains API Keys. Edit carefully.", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold) 
                } 
            }
            
            OutlinedTextField(
                value = txt, 
                onValueChange = { txt = it }, 
                modifier = Modifier.height(300.dp).fillMaxWidth(), 
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            )
            
            Row(modifier = Modifier.padding(top = 16.dp)) {
                Button(onClick = { val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; cm.setPrimaryClip(ClipData.newPlainText("Config", txt)); Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show() }, modifier = Modifier.weight(1f)) { Text("Copy JSON") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { try { onSave(gson.fromJson(txt, AppConfig::class.java)); onBack() } catch(e:Exception){ Toast.makeText(context, "Invalid JSON", Toast.LENGTH_SHORT).show() } }, modifier = Modifier.weight(1f)) { Text("Apply JSON") }
            }
        }
    }

    // --- Backup Dialog ---
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Create Backup") },
            text = {
                Column {
                    Text("Would you like to encrypt this backup with a password?")
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = useEncryption, onCheckedChange = { useEncryption = it })
                        Text("Encrypt with Password")
                    }
                    if (useEncryption) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Don't lose this password. Data cannot be recovered without it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (useEncryption && password.isBlank()) {
                        Toast.makeText(context, "Password cannot be empty", Toast.LENGTH_SHORT).show()
                    } else {
                        showBackupDialog = false
                        createDocumentLauncher.launch(BackupManager.generateFileName())
                    }
                }) {
                    Text("Backup")
                }
            },
            dismissButton = { TextButton(onClick = { showBackupDialog = false }) { Text("Cancel") } }
        )
    }

    // --- Restore Password Dialog ---
    if (showRestorePasswordDialog) {
        AlertDialog(
            onDismissRequest = { showRestorePasswordDialog = false; pendingRestoreUri = null },
            icon = { Icon(Icons.Default.Lock, null) },
            title = { Text("Encrypted Backup") },
            text = {
                Column {
                    Text("This file is encrypted. Please enter the password to restore.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        try {
                            pendingRestoreUri?.let { uri ->
                                context.contentResolver.openInputStream(uri)?.use { ins ->
                                    val (newConfig, _) = BackupManager.importBackup(ins, password)
                                    if (newConfig != null) {
                                        onSave(newConfig)
                                        txt = gson.toJson(newConfig)
                                        Toast.makeText(context, "Restore Successful!", Toast.LENGTH_SHORT).show()
                                        showRestorePasswordDialog = false
                                        pendingRestoreUri = null
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }) {
                    Text("Decrypt & Restore")
                }
            },
            dismissButton = { TextButton(onClick = { showRestorePasswordDialog = false; pendingRestoreUri = null }) { Text("Cancel") } }
        )
    }
}