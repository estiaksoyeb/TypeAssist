package com.typeassist.app.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(onBack: () -> Unit) {
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
                title = { Text("User Guide") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryColor, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { p ->
        LazyColumn(modifier = Modifier.padding(p).padding(16.dp)) {
            item {
                Text(
                    "Welcome to TypeAssist!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Master your new AI keyboard assistant.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            item { GuideSection("1. Basic AI Triggers", "Standard commands that work at the end of your text.", 
                listOf(
                    GuideItem(".ta", "Ask AI", "Type your question and end with .ta\nExample: 'Capital of France? .ta'"),
                    GuideItem(".g", "Fix Grammar", "Corrects spelling and grammar.\nExample: 'Im going home .g'"),
                    GuideItem(".tr", "Translate", "Translates text to English.\nExample: 'Hola mundo .tr'"),
                    GuideItem(".polite", "Polite Tone", "Rewrites text professionally.\nExample: 'Send me the file .polite'")
                )) 
            }

            item { GuideSection("2. Inline AI Commands", "Powerful commands you can place ANYWHERE in your text.", 
                listOf(
                    GuideItem("(.ta: ... )", "Inline Ask", "Ask AI in the middle of a sentence.\nExample: 'I am visiting (.ta: capital of Japan) next week.'"),
                    GuideItem("Custom", "Make Your Own", "You can create your own patterns like [fix:%] in the Commands tab.")
                ))
            }

            item { GuideSection("3. Snippets (Text Expander)", "Save frequently used text for quick access.", 
                listOf(
                    GuideItem("..name", "Expand Snippet", "Type the prefix '..' followed by the snippet name.\nExample: '..email' -> 'user@example.com'"),
                    GuideItem("(.save:name:content)", "Quick Save", "Save a new snippet instantly.\nExample: '(.save:addr:123 Main St)'")
                ))
            }

            item { GuideSection("4. Utility Belt (Offline Tools)", "Smart tools that run locally without AI.", 
                listOf(
                    GuideItem("(.c: math )", "Calculator", "Solves math expressions.\nExample: '(.c: 25 * 4 + 10)' -> '110'\nSupports: +, -, *, /, ^, (), sqrt, sin, cos, log"),
                    GuideItem(".now", "Time Stamp", "Inserts current time (YYYY-MM-DD HH:MM)."),
                    GuideItem(".date", "Date Stamp", "Inserts current date (Friday, Dec 19)."),
                    GuideItem(".pass", "Password Gen", "Generates a strong random password.")
                ))
            }

            item { GuideSection("5. Safety & History", "Never lose your work.", 
                listOf(
                    GuideItem("Undo", "Global Undo", "Press the UNDO button or type '.undo' to revert changes. Works for 2 minutes."),
                    GuideItem("History", "Clipboard History", "View and copy the last 2 minutes of original text from the History screen.")
                ))
            }
            
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun GuideSection(title: String, subtitle: String, items: List<GuideItem>) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(subtitle, fontSize = 12.sp, color = Color.Gray)
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = Color.Gray)
            }
            
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    items.forEach { item ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(item.trigger, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                            Text(item.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(item.desc, fontSize = 13.sp, color = Color.DarkGray, lineHeight = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

data class GuideItem(val trigger: String, val title: String, val desc: String)
