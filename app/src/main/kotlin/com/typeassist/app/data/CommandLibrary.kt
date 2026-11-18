package com.typeassist.app.data

import com.typeassist.app.R

data class CommandTemplate(
    val titleRes: Int,
    val descriptionRes: Int,
    val recommendedTriggerRes: Int,
    val systemPrompt: String,
    val isInline: Boolean = false
)

object CommandLibrary {
    val templates = listOf(
        CommandTemplate(
            titleRes = R.string.command_title_professional_email,
            descriptionRes = R.string.command_description_turns_rough_notes_into_a_formal_bu,
            recommendedTriggerRes = R.string.command_trigger_email,
            systemPrompt = "You are a professional assistant. Rewrite the user's rough notes into a clear, formal, and professional business email. Maintain the original meaning but improve the tone, structure, and vocabulary. Do not include subject lines unless necessary. Return ONLY the rewritten email body."
        ),
        CommandTemplate(
            titleRes = R.string.command_title_summarize,
            descriptionRes = R.string.command_description_condenses_long_text_into_key_point,
            recommendedTriggerRes = R.string.command_trigger_sum,
            systemPrompt = "Summarize the following text into 3-5 concise bullet points. Focus on the most important information. Output only the bullet points."
        ),
        CommandTemplate(
            titleRes = R.string.command_title_explain_like_i_m_5,
            descriptionRes = R.string.command_description_simplifies_complex_topics_for_easy,
            recommendedTriggerRes = R.string.command_trigger_easy,
            systemPrompt = "Explain the provided text or concept in extremely simple terms, as if explaining to a 5-year-old child. Use simple analogies and avoid jargon. Output only the simplified explanation."
        ),
        CommandTemplate(
            titleRes = R.string.command_title_reply_friendly,
            descriptionRes = R.string.command_description_generates_a_warm_friendly_response,
            recommendedTriggerRes = R.string.command_trigger_reply,
            systemPrompt = "Generate a warm, friendly, and helpful response to the user's message. The tone should be conversational and kind. Output only the response text."
        ),
        CommandTemplate(
            titleRes = R.string.command_title_action_items,
            descriptionRes = R.string.command_description_extracts_tasks_and_action_items_fr,
            recommendedTriggerRes = R.string.command_trigger_todo,
            systemPrompt = "Analyze the text and extract all actionable tasks and next steps. List them as a clear checklist. If no tasks are found, say 'No action items found.' Output only the list."
        ),
        CommandTemplate(
            titleRes = R.string.command_title_fix_improve,
            descriptionRes = R.string.command_description_aggressively_fixes_grammar_and_mak,
            recommendedTriggerRes = R.string.command_trigger_improve,
            systemPrompt = "Fix all grammar, spelling, and punctuation errors in the provided text. Additionally, improve the flow, clarity, and word choice to make it sound more natural and professional. Return ONLY the improved text."
        ),
        CommandTemplate(
            titleRes = R.string.command_title_translate_to_spanish,
            descriptionRes = R.string.command_description_quickly_translates_any_text_into_s,
            recommendedTriggerRes = R.string.command_trigger_es,
            systemPrompt = "Translate the following text into natural-sounding Spanish. Return ONLY the translated text."
        ),
        CommandTemplate(
            titleRes = R.string.ui_inline_ask,
            descriptionRes = R.string.command_description_place_this_anywhere_to_get_a_quick,
            recommendedTriggerRes = R.string.command_trigger_ta,
            systemPrompt = "Give a very brief, one-sentence answer to the query. No explanations. Return only the answer.",
            isInline = true
        )
    )
}
