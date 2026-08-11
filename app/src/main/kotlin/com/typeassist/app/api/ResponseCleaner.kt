package com.typeassist.app.api

/**
 * Strips reasoning-model artifacts and common wrapping characters from a model response.
 * Handles both closed <think>...</think> blocks and open-ended <think> where the token
 * budget ran out before </think> was emitted.
 */
fun cleanModelResponse(text: String): String {
    var result = text.trim()

    // Strip fully closed <think>…</think> blocks (reasoning artifacts).
    result = result.replace(Regex("<think>[\\s\\S]*?</think>", RegexOption.IGNORE_CASE), "").trim()

    // Handle an unclosed <think> (model ran out of tokens before </think>).
    val openThink = result.indexOf("<think>", ignoreCase = true)
    if (openThink >= 0) {
        val beforeThink = result.substring(0, openThink).trim()
        if (beforeThink.isNotEmpty()) {
            // There is real content before the orphan <think>; keep it.
            result = beforeThink
        } else {
            // The model spent its entire budget reasoning and never
            // produced an actual answer.  Extract the thinking content
            // and use the last meaningful sentence/fragment so the user
            // sees *something* rather than blank output.
            val thinkContent = result.substring(openThink + "<think>".length).trim()
            result = extractFallbackFromThinking(thinkContent)
        }
    }

    if (result.startsWith("|") && result.endsWith("|")) {
        result = result.substring(1, result.length - 1).trim()
    }

    if ((result.startsWith("\"") && result.endsWith("\"")) ||
        (result.startsWith("'") && result.endsWith("'"))) {
        result = result.substring(1, result.length - 1).trim()
    }

    return result
}

/**
 * When a reasoning model exhausted its token budget inside <think> and
 * never produced an actual response, try to salvage something useful
 * from the thinking content.  We take the last non-empty line/sentence
 * as the most "refined" thought the model produced before stopping.
 */
private fun extractFallbackFromThinking(thinkContent: String): String {
    if (thinkContent.isBlank()) return ""

    // Split by double-newline (paragraph breaks) first, then single newlines.
    val paragraphs = thinkContent.split(Regex("\\n\\s*\\n"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    // Take the last paragraph — the model's most recent thought.
    val lastParagraph = paragraphs.lastOrNull() ?: thinkContent.trim()

    // If the last paragraph has multiple sentences, take the last complete one.
    val sentences = lastParagraph.split(Regex("(?<=[.!?])\\s+"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    return sentences.lastOrNull() ?: lastParagraph
}
