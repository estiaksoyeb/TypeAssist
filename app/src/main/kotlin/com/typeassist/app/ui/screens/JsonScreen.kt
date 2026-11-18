package com.typeassist.app.ui.screens

import com.typeassist.app.R

import android.app.Activity
import androidx.compose.ui.res.stringResource
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
                    Toast.makeText(context, context.getString(R.string.ui_backup_saved_successfully), Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.ui_export_failed_1_s , e.message), Toast.LENGTH_LONG).show()
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
                                Toast.makeText(context, context.getString(R.string.ui_restore_successful), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.ui_read_failed_1_s , e.message), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text(stringResource(R.string.ui_backup_restore)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.ui_back)) } },
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
            Text(stringResource(R.string.ui_data_management), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
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
                    Text(stringResource(R.string.ui_backup_to_file))
                }
                
                OutlinedButton(
                    onClick = { openDocumentLauncher.launch(arrayOf("*/*")) }, 
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.ui_restore_from_file))
                }
            }
            Text(stringResource(R.string.ui_saves_all_settings_api_keys_and_snippe), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            // --- Raw JSON Section ---
            Text(stringResource(R.string.ui_advanced_raw_json), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(8.dp))
            
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) { 
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { 
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.ui_contains_api_keys_edit_carefully), color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } 
            }
            
            OutlinedTextField(
                value = txt, 
                onValueChange = { txt = it }, 
                modifier = Modifier.height(300.dp).fillMaxWidth(), 
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            )
            
            Row(modifier = Modifier.padding(top = 16.dp)) {
                Button(onClick = { val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; cm.setPrimaryClip(ClipData.newPlainText("Config", txt)); Toast.makeText(context, context.getString(R.string.ui_copied), Toast.LENGTH_SHORT).show() }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.ui_copy_json)) }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { try { onSave(gson.fromJson(txt, AppConfig::class.java)); onBack() } catch(e:Exception){ Toast.makeText(context, context.getString(R.string.ui_invalid_json), Toast.LENGTH_SHORT).show() } }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.ui_apply_json)) }
            }
        }
    }

    // --- Backup Dialog ---
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text(stringResource(R.string.ui_create_backup)) },
            text = {
                Column {
                    Text(stringResource(R.string.ui_would_you_like_to_encrypt_this_backup_))
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = useEncryption, onCheckedChange = { useEncryption = it })
                        Text(stringResource(R.string.ui_encrypt_with_password))
                    }
                    if (useEncryption) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(R.string.ui_password)) },
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(stringResource(R.string.ui_don_t_lose_this_password_data_cannot_b), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (useEncryption && password.isBlank()) {
                        Toast.makeText(context, context.getString(R.string.ui_password_cannot_be_empty), Toast.LENGTH_SHORT).show()
                    } else {
                        showBackupDialog = false
                        createDocumentLauncher.launch(BackupManager.generateFileName())
                    }
                }) {
                    Text(stringResource(R.string.ui_backup))
                }
            },
            dismissButton = { TextButton(onClick = { showBackupDialog = false }) { Text(stringResource(R.string.ui_cancel)) } }
        )
    }

    // --- Restore Password Dialog ---
    if (showRestorePasswordDialog) {
        AlertDialog(
            onDismissRequest = { showRestorePasswordDialog = false; pendingRestoreUri = null },
            icon = { Icon(Icons.Default.Lock, null) },
            title = { Text(stringResource(R.string.ui_encrypted_backup)) },
            text = {
                Column {
                    Text(stringResource(R.string.ui_this_file_is_encrypted_please_enter_th))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.ui_password)) },
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
                                        Toast.makeText(context, context.getString(R.string.ui_restore_successful), Toast.LENGTH_SHORT).show()
                                        showRestorePasswordDialog = false
                                        pendingRestoreUri = null
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, context.getString(R.string.ui_error_1_s , e.message), Toast.LENGTH_LONG).show()
                        }
                    }
                }) {
                    Text(stringResource(R.string.ui_decrypt_restore))
                }
            },
            dismissButton = { TextButton(onClick = { showRestorePasswordDialog = false; pendingRestoreUri = null }) { Text(stringResource(R.string.ui_cancel)) } }
        )
    }
}