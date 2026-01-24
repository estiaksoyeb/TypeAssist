package com.typeassist.app.ui.screens

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DidYouKnowScreen(onFinished: () -> Unit) {
    // Status bar logic is handled by Theme.kt now, so we remove the SideEffect here
    // to prevent overriding the correct behavior.

    val features = listOf(
        DiscoveryFeature(
            title = "Prompt Anywhere",
            subtitle = "Reshape text with custom prompts",
            description = "Instead of fixed trigger commands, this super command lets you add custom trigger commands to reshape your texts.\n\nWrite your prompts between ...here... and it will process according to your prompts.\n\nImagine you are writing an email and want to make it formal. You don't need to delete and rewrite. Just append ...Make it formal... and watch the magic happen.",
            examples = listOf(
                "Hey boss i cant come today ...Make it formal..." to "Dear Manager,\nI am writing to inform you that I will be unable to attend work today.",
                "Hola amigo ...Translate to English..." to "Hello friend",
                "Meeting at 5pm ...Add calendar emoji..." to "Meeting at 5pm 📅"
            ),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        DiscoveryFeature(
            title = "Instant Math",
            subtitle = "Calculate inside any app",
            description = "Solve math problems instantly. Just wrap the expression in '(.c: ... )'.\n\nGreat for calculating costs, splitting bills, or quick conversions while chatting.",
            examples = listOf(
                "Total: (.c: 25 * 4)" to "Total: 100",
                "Share: (.c: 150 / 3)" to "Share: 50",
                "Area: (.c: 3.14 * 5^2)" to "Area: 78.5"
            ),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        DiscoveryFeature(
            title = "Snippets",
            subtitle = "Text Expander",
            description = "Save frequently used text for quick access.\n\nType the prefix '..' followed by the snippet name to expand it.\n\nUse '(.save:name:content)' to save a new snippet instantly without opening the app.",
            examples = listOf(
                "..email" to "my.name@example.com",
                "..addr" to "123 Main St, New York, NY",
                "(.save:ph:+15550199)" to "(Snippet 'ph' saved!)"
            ),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    )

    val pagerState = rememberPagerState(pageCount = { features.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (pagerState.currentPage < features.size - 1) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onFinished()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                if (pagerState.currentPage < features.size - 1) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next")
                } else {
                    Icon(Icons.Default.Check, "Got it")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onFinished) {
                    Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onBackground)
                }
            }

            Text(
                "Did You Know?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(Modifier.height(16.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 24.dp),
                pageSpacing = 16.dp
            ) { page ->
                FeatureDiscoveryCard(features[page])
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(features.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(if (pagerState.currentPage == iteration) 12.dp else 8.dp)
                    )
                }
            }
        }
    }
}

data class DiscoveryFeature(
    val title: String,
    val subtitle: String,
    val description: String,
    val examples: List<Pair<String, String>>,
    val containerColor: Color,
    val contentColor: Color
)

@Composable
fun FeatureDiscoveryCard(feature: DiscoveryFeature) {
    Card(
        colors = CardDefaults.cardColors(containerColor = feature.containerColor),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(feature.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = feature.contentColor)
            Text(feature.subtitle, style = MaterialTheme.typography.titleMedium, color = feature.contentColor.copy(alpha = 0.8f))
            
            Spacer(Modifier.height(8.dp))
            
            LivePreviewBox(feature.examples)
            
            Spacer(Modifier.height(8.dp))
            
            Text(feature.description, style = MaterialTheme.typography.bodyLarge, color = feature.contentColor.copy(alpha = 0.9f))
        }
    }
}

@Composable
fun LivePreviewBox(examples: List<Pair<String, String>>) {
    var displayedText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(true) }
    var currentExampleIndex by remember { mutableStateOf(0) }

    LaunchedEffect(examples) {
        while (true) {
            val (input, output) = examples[currentExampleIndex]
            
            isTyping = true
            displayedText = ""
            for (i in 1..input.length) {
                displayedText = input.take(i)
                delay(80)
            }
            delay(500)
            
            isTyping = false
            displayedText = "Processing..."
            delay(800)
            
            displayedText = output
            delay(2000)
            
            currentExampleIndex = (currentExampleIndex + 1) % examples.size
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(140.dp)
    ) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = buildAnnotatedString {
                    if (displayedText == "Processing...") {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic)) {
                            append(displayedText)
                        }
                    } else {
                        append(displayedText)
                        if (isTyping) {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) { append("|") }
                        }
                    }
                },
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

