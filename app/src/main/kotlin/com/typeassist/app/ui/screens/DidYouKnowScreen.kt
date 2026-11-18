package com.typeassist.app.ui.screens

import com.typeassist.app.R

import android.app.Activity
import androidx.compose.ui.res.stringResource
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
            title = stringResource(R.string.ui_prompt_anywhere),
            subtitle = stringResource(R.string.ui_reshape_text_with_custom_prompts),
            description = stringResource(R.string.ui_instead_of_fixed_trigger_commands_this),
            examples = listOf(
                stringResource(R.string.ui_demo_formal_input) to stringResource(R.string.ui_demo_formal_output),
                stringResource(R.string.ui_demo_translate_input) to stringResource(R.string.ui_demo_translate_output),
                stringResource(R.string.ui_demo_calendar_input) to stringResource(R.string.ui_demo_calendar_output)
            ),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        DiscoveryFeature(
            title = stringResource(R.string.ui_instant_math),
            subtitle = stringResource(R.string.ui_calculate_inside_any_app),
            description = stringResource(R.string.ui_solve_math_problems_instantly_just_wra),
            examples = listOf(
                stringResource(R.string.ui_demo_math_total) to stringResource(R.string.ui_demo_math_total_output),
                stringResource(R.string.ui_demo_math_share) to stringResource(R.string.ui_demo_math_share_output),
                stringResource(R.string.ui_demo_math_area) to stringResource(R.string.ui_demo_math_area_output)
            ),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        DiscoveryFeature(
            title = stringResource(R.string.ui_snippets),
            subtitle = stringResource(R.string.ui_text_expander),
            description = stringResource(R.string.ui_save_frequently_used_text_for_quick_ac),
            examples = listOf(
                stringResource(R.string.ui_demo_email_prefix) to stringResource(R.string.ui_demo_email_output),
                stringResource(R.string.ui_demo_address_prefix) to stringResource(R.string.ui_demo_address_output),
                stringResource(R.string.ui_demo_save_command) to stringResource(R.string.ui_demo_save_output)
            ),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        DiscoveryFeature(
            title = stringResource(R.string.ui_wrong_language),
            subtitle = stringResource(R.string.ui_customize_ai_prompts),
            description = stringResource(R.string.ui_is_the_ai_replying_in_english_when_you),
            examples = emptyList(),
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            showLivePreview = false
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
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, stringResource(R.string.ui_next))
                } else {
                    Icon(Icons.Default.Check, stringResource(R.string.ui_got_it))
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
                    Icon(Icons.Default.Close, stringResource(R.string.ui_close), tint = MaterialTheme.colorScheme.onBackground)
                }
            }

            Text(
                stringResource(R.string.ui_did_you_know),
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
    val contentColor: Color,
    val showLivePreview: Boolean = true
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
            
            if (feature.examples.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                if (feature.showLivePreview) {
                    LivePreviewBox(feature.examples)
                } else {
                    StaticExamplesBox(feature.examples)
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(feature.description, style = MaterialTheme.typography.bodyLarge, color = feature.contentColor.copy(alpha = 0.9f))
        }
    }
}

@Composable
fun StaticExamplesBox(examples: List<Pair<String, String>>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(140.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            examples.forEach { (input, output) ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(input, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text(output, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun LivePreviewBox(examples: List<Pair<String, String>>) {
    val processingText = stringResource(R.string.ui_processing)
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
            displayedText = processingText
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
                    if (displayedText == processingText) {
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

