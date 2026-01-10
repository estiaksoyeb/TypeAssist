package com.typeassist.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun TypingAnimationPreview() {
    val scenarios = listOf(
        Pair("Capital of France? .ta", "Paris"),
        Pair("i go home yestarday .g", "I went home yesterday."),
        Pair("你好世界 .tr", "Hello World")
    )
    
    var currentScenarioIndex by remember { mutableStateOf(0) }
    var displayedText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var cursorVisible by remember { mutableStateOf(true) }

    // Cursor Blinking
    LaunchedEffect(Unit) {
        while (true) {
            cursorVisible = !cursorVisible
            delay(500)
        }
    }

    // Typing Loop
    LaunchedEffect(Unit) {
        while (true) {
            val (query, answer) = scenarios[currentScenarioIndex]

            // 1. Reset
            displayedText = ""
            isThinking = false
            delay(1000)

            // 2. Type Query
            for (i in 1..query.length) {
                displayedText = query.take(i)
                delay(80) // Typing speed
            }
            delay(500)

            // 3. Thinking (Trigger activated)
            isThinking = true
            delay(1500) // Simulated network delay

            // 4. Show Answer
            isThinking = false
            displayedText = answer
            delay(3000) // Hold result

            // 5. Next Scenario
            currentScenarioIndex = (currentScenarioIndex + 1) % scenarios.size
        }
    }

    // UI Representation
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .border(1.dp, Color.LightGray, RoundedCornerShape(16.dp))
            .background(Color(0xFFF3F4F6), RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (isThinking) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "AI is thinking...",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Text(
                buildAnnotatedString {
                    append(displayedText)
                    if (cursorVisible && !isThinking) {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("|")
                        }
                    }
                },
                fontSize = 18.sp,
                color = Color.Black
            )
        }
        
        // Label overlay
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
            Text(
                text = "Live Preview",
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}
