package com.typeassist.app.api

/**
 * Strips reasoning-model artifacts and common wrapping characters from a model response.
 * Handles both closed <think>...</think> blocks and open-ended <think> where the token
 * budget ran out before </think> was emitted.
 */
fun cleanModelResponse(text: String): String {
    var result = text.trim()

    result = result.replace(Regex("<think>[\\s\\S]*?</think>", RegexOption.IGNORE_CASE), "").trim()

    val openThink = result.indexOf("<think>", ignoreCase = true)
    if (openThink >= 0) {
        result = result.substring(0, openThink).trim()
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
