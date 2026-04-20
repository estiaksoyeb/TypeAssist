package com.typeassist.app.data

import java.io.Serializable

data class AppConfig(
    var isAppEnabled: Boolean = false,
    var provider: String = "gemini", // "gemini", "cloudflare", "custom", "local"
    var apiKey: String = "",
    var model: String = "gemini-2.5-flash-lite",
    var cloudflareConfig: CloudflareConfig = CloudflareConfig(),
    var customApiConfig: CustomApiConfig = CustomApiConfig(),
    var localLlmConfig: LocalLlmConfig = LocalLlmConfig(),
    var savedLocalModels: MutableList<String> = mutableListOf(),
    var savedCustomConfigs: MutableList<CustomApiConfig> = mutableListOf(),
    var savedGeminiConfigs: MutableList<SavedGeminiConfig> = mutableListOf(),
    var savedCloudflareConfigs: MutableList<CloudflareConfig> = mutableListOf(),
    var triggerDebounceMs: Long = 400L,
    var generationConfig: GenConfig = GenConfig(),
    var triggers: MutableList<Trigger> = mutableListOf(),
    var inlineCommands: MutableList<InlineCommand> = mutableListOf(),
    var snippets: MutableList<Snippet> = mutableListOf(),
    var undoCommandPattern: String = ".undo",
    var snippetTriggerPrefix: String = "ta#",
    var saveSnippetPattern: String = "(.save:%:%)",
    var globalTriggerPattern: String = "...%...",
    var isHistoryEnabled: Boolean = true,
    var enableUndoOverlay: Boolean = true,
    var enableLoadingOverlay: Boolean = true,
    var enablePreviewDialog: Boolean = false,
    var allowTriggerAnywhere: Boolean = false,
    var ignorePrecedingWhitespace: Boolean = false,
    var apiTimeoutSeconds: Long = 30L
) : Serializable

data class CloudflareConfig(
    var accountId: String = "",
    var apiToken: String = "",
    var model: String = "@cf/meta/llama-3-8b-instruct"
) : Serializable

data class SavedGeminiConfig(
    var apiKey: String = "",
    var model: String = ""
) : Serializable

data class CustomApiConfig(
    var baseUrl: String = "https://api.openai.com/v1",
    var apiKey: String = "",
    var model: String = "gpt-3.5-turbo"
) : Serializable

data class LocalLlmConfig(
    var modelPath: String = "",
    var temperature: Float = 0.7f,
    var topP: Float = 0.9f,
    var maxTokens: Int = 128,
    var numThreads: Int = 4
) : Serializable

data class GenConfig(
    var temperature: Double = 0.2,
    var topP: Double = 0.95
) : Serializable

data class Trigger(
    var pattern: String,
    var prompt: String
) : Serializable

data class InlineCommand(
    var pattern: String,
    var prompt: String
) : Serializable

data class Snippet(
    var trigger: String,
    var content: String
) : Serializable

fun createDefaultConfig(): AppConfig {
    return AppConfig(
        isAppEnabled = false,
        provider = "gemini",
        apiKey = "", 
        model = "gemini-2.5-flash-lite",
        cloudflareConfig = CloudflareConfig(),
        customApiConfig = CustomApiConfig(),
        localLlmConfig = LocalLlmConfig(),
        savedLocalModels = mutableListOf(),
        savedCustomConfigs = mutableListOf(),
        savedGeminiConfigs = mutableListOf(),
        savedCloudflareConfigs = mutableListOf(),
        triggerDebounceMs = 400L,
        generationConfig = GenConfig(temperature = 0.2, topP = 0.95),
        triggers = mutableListOf(
            Trigger(".ta", "Give only the most relevant and complete answer to the query. Do not explain, do not add introductions, disclaimers, or extra text. Output only the answer."),
            Trigger(".g", "Fix grammar, spelling, and punctuation. Return only the corrected text."),
            Trigger(".polite", "Rewrite the text in a polite and professional tone. Return only the rewritten text."),
            Trigger(".casual", "Rewrite in a casual, friendly tone. Return only the rewritten text."),
            Trigger(".improve", "Improve the writing quality and clarity. Return only the improved text."),
            Trigger(".tr", "Translate to English. Return only the translated text.")
        ),
        inlineCommands = mutableListOf(
            InlineCommand("(%:.ta)", "Give only the most relevant and complete answer to the query. Do not explain, do not add introductions, disclaimers, or extra text. Output only the answer."),
            InlineCommand("(%:.g)", "Fix grammar, spelling, and punctuation. Return only the corrected text."),
            InlineCommand("(%:.polite)", "Rewrite the text in a polite and professional tone. Return only the rewritten text.")
        ),
        snippets = mutableListOf(
            Snippet("email", "user@example.com"),
            Snippet("sign", "Best regards,\nUser")
        ),
        undoCommandPattern = ".undo",
        snippetTriggerPrefix = "..",
        saveSnippetPattern = "(.save:%:%)",
        globalTriggerPattern = "...%...",
        isHistoryEnabled = true,
        enableUndoOverlay = true,
        enableLoadingOverlay = true,
        enablePreviewDialog = false,
        allowTriggerAnywhere = false,
        ignorePrecedingWhitespace = false,
        apiTimeoutSeconds = 30L
    )
}
