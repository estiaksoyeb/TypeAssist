package com.typeassist.app.ui.screens

import com.typeassist.app.R

import android.app.Activity
import androidx.compose.ui.res.stringResource
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ui_user_guide)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, stringResource(R.string.ui_back)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.primary, navigationIconContentColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { p ->
        LazyColumn(modifier = Modifier.padding(p).padding(16.dp)) {
            item {
                Text(
                    stringResource(R.string.ui_welcome_to_typeassist),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(R.string.ui_master_your_new_ai_keyboard_assistant),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            item { GuideSection(stringResource(R.string.ui_1_basic_ai_triggers), stringResource(R.string.ui_standard_commands_that_work_at_the_end_o),
                listOf(
                    GuideItem(stringResource(R.string.ui_ta), stringResource(R.string.ui_ask_ai), stringResource(R.string.ui_type_your_question_and_end_with_ta_nexam)),
                    GuideItem(stringResource(R.string.ui_g), stringResource(R.string.ui_fix_grammar), stringResource(R.string.ui_corrects_spelling_and_grammar_nexample_i)),
                    GuideItem(stringResource(R.string.ui_tr), stringResource(R.string.ui_translate), stringResource(R.string.ui_translates_text_to_english_nexample_hola)),
                    GuideItem(stringResource(R.string.ui_polite), stringResource(R.string.ui_polite_tone), stringResource(R.string.ui_rewrites_text_professionally_nexample_se))
                )) 
            }

            item { GuideSection(stringResource(R.string.ui_2_inline_ai_commands), stringResource(R.string.ui_powerful_commands_you_can_place_anywhere),
                listOf(
                    GuideItem(stringResource(R.string.ui_ta_2), stringResource(R.string.ui_inline_ask), stringResource(R.string.ui_ask_ai_in_the_middle_of_a_sentence_nexam)),
                    GuideItem(stringResource(R.string.ui_custom), stringResource(R.string.ui_make_your_own), stringResource(R.string.ui_you_can_create_your_own_patterns_like_fi))
                ))
            }

            item { GuideSection(stringResource(R.string.ui_3_global_transformation), stringResource(R.string.ui_rewrite_everything_in_the_text_box_with_),
                listOf(
                    GuideItem(stringResource(R.string.ui_instruction), stringResource(R.string.ui_global_rewrite), stringResource(R.string.ui_transform_the_whole_text_field_nexample_)),
                    GuideItem(stringResource(R.string.ui_post_correction), stringResource(R.string.ui_refine_output), stringResource(R.string.ui_quickly_fix_ai_results_nexample_remove_m))
                ))
            }

            item { GuideSection(stringResource(R.string.ui_4_snippets_text_expander), stringResource(R.string.ui_save_frequently_used_text_for_quick_acce),
                listOf(
                    GuideItem(stringResource(R.string.ui_name), stringResource(R.string.ui_expand_snippet), stringResource(R.string.ui_type_the_prefix_followed_by_the_snippet_)),
                    GuideItem(stringResource(R.string.ui_save_name_content), stringResource(R.string.ui_quick_save_2), stringResource(R.string.ui_save_a_new_snippet_instantly_nexample_sa))
                ))
            }

            item { GuideSection(stringResource(R.string.ui_5_utility_belt_offline_tools), stringResource(R.string.ui_smart_tools_that_run_locally_without_ai),
                listOf(
                    GuideItem(stringResource(R.string.ui_c_math), stringResource(R.string.ui_calculator), stringResource(R.string.ui_solves_math_expressions_nexample_c_25_4_)),
                    GuideItem(stringResource(R.string.ui_now), stringResource(R.string.ui_time_stamp), stringResource(R.string.ui_inserts_current_time_yyyy_mm_dd_hh_mm)),
                    GuideItem(stringResource(R.string.ui_date), stringResource(R.string.ui_date_stamp), stringResource(R.string.ui_inserts_current_date_format_configurable)),
                    GuideItem(stringResource(R.string.ui_pass), stringResource(R.string.ui_password_gen), stringResource(R.string.ui_generates_a_strong_random_password))
                ))
            }

            item { GuideSection(stringResource(R.string.ui_6_safety_history), stringResource(R.string.ui_never_lose_your_work),
                listOf(
                    GuideItem(stringResource(R.string.ui_undo_2), stringResource(R.string.ui_global_undo), stringResource(R.string.ui_press_the_undo_button_or_type_undo_to_re)),
                    GuideItem(stringResource(R.string.ui_history), stringResource(R.string.ui_clipboard_history), stringResource(R.string.ui_view_and_copy_the_last_5_minutes_of_orig))
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
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    items.forEach { item ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(item.trigger, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                            Text(item.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(item.desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

data class GuideItem(val trigger: String, val title: String, val desc: String)
