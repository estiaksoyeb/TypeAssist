package com.typeassist.app

import java.io.Serializable

data class AppConfig(
    var isAppEnabled: Boolean = false,
    var apiKey: String = "",
    var model: String = "gemini-2.5-flash-lite",
    var generationConfig: GenConfig = GenConfig(),
    var triggers: MutableList<Trigger> = mutableListOf()
) : Serializable

data class GenConfig(
    var temperature: Double = 0.2,
    var topP: Double = 0.95
) : Serializable

data class Trigger(
    var pattern: String,
    var prompt: String
) : Serializable

fun createDefaultConfig(): AppConfig {
    return AppConfig(
        isAppEnabled = false,
        apiKey = "", 
        model = "gemini-2.5-flash-lite",
        generationConfig = GenConfig(temperature = 0.2, topP = 0.95),
        triggers = mutableListOf(
            Trigger("@ta", "Give only the most relevant and complete answer to the query. \nDo not explain, do not add introductions, disclaimers, or extra text. \nOutput only the answer."),
            Trigger("!g", "Fix grammar, spelling, and punctuation. Return only the corrected text."),
            Trigger("@polite", "Rewrite the text in a polite and professional tone. Return only the rewritten text."),
            Trigger("@casual", "Rewrite in a casual, friendly tone. Return only the rewritten text."),
            Trigger("@improve", "Improve the writing quality and clarity. Return only the improved text."),
            Trigger("!tr", "Translate to English. Return only the translated text.")
        )
    )
}
