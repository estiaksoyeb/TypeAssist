package com.typeassist.app.data

data class CommandTemplate(
    val title: String,
    val description: String,
    val recommendedTrigger: String,
    val systemPrompt: String,
    val isInline: Boolean = false
)

object CommandLibrary {
    val templates = listOf(
        CommandTemplate(
            title = "Professional Email",
            description = "Turns rough notes into a formal business email.",
            recommendedTrigger = ".email",
            systemPrompt = "You are a professional assistant. Rewrite the user's rough notes into a clear, formal, and professional business email. Maintain the original meaning but improve the tone, structure, and vocabulary. Do not include subject lines unless necessary. Return ONLY the rewritten email body."
        ),
        CommandTemplate(
            title = "Summarize",
            description = "Condenses long text into key points.",
            recommendedTrigger = ".sum",
            systemPrompt = "Summarize the following text into 3-5 concise bullet points. Focus on the most important information. Output only the bullet points."
        ),
        CommandTemplate(
            title = "Explain Like I'm 5",
            description = "Simplifies complex topics for easy understanding.",
            recommendedTrigger = ".easy",
            systemPrompt = "Explain the provided text or concept in extremely simple terms, as if explaining to a 5-year-old child. Use simple analogies and avoid jargon. Output only the simplified explanation."
        ),
        CommandTemplate(
            title = "Reply (Friendly)",
            description = "Generates a warm, friendly response to a message.",
            recommendedTrigger = ".reply",
            systemPrompt = "Generate a warm, friendly, and helpful response to the user's message. The tone should be conversational and kind. Output only the response text."
        ),
        CommandTemplate(
            title = "Action Items",
            description = "Extracts tasks and action items from a meeting note or text.",
            recommendedTrigger = ".todo",
            systemPrompt = "Analyze the text and extract all actionable tasks and next steps. List them as a clear checklist. If no tasks are found, say 'No action items found.' Output only the list."
        ),
        CommandTemplate(
            title = "Fix & Improve",
            description = "Aggressively fixes grammar AND makes the writing flow better.",
            recommendedTrigger = ".improve",
            systemPrompt = "Fix all grammar, spelling, and punctuation errors in the provided text. Additionally, improve the flow, clarity, and word choice to make it sound more natural and professional. Return ONLY the improved text."
        ),
        CommandTemplate(
            title = "Translate to Spanish",
            description = "Quickly translates any text into Spanish.",
            recommendedTrigger = ".es",
            systemPrompt = "Translate the following text into natural-sounding Spanish. Return ONLY the translated text."
        ),
        CommandTemplate(
            title = "Inline Ask",
            description = "Place this anywhere to get a quick answer mid-sentence.",
            recommendedTrigger = "(%:?)",
            systemPrompt = "Give a very brief, one-sentence answer to the query. No explanations. Return only the answer.",
            isInline = true
        )
    )
}
