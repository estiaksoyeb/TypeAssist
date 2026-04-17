package com.typeassist.app.api

import com.typeassist.app.data.AppConfig

interface AiProvider {
    fun generateResponse(
        prompt: String,
        userText: String,
        config: AppConfig,
        callback: (Result<String>) -> Unit
    )
}
