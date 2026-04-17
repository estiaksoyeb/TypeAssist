package com.typeassist.app.api

import android.os.Handler
import android.os.Looper
import com.typeassist.app.data.AppConfig
import com.typeassist.app.service.MyAccessibilityService

class LocalLlmClient(private val service: MyAccessibilityService) : AiProvider {

    private var currentModelPath: String = ""
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun generateResponse(
        prompt: String,
        userText: String,
        config: AppConfig,
        callback: (Result<String>) -> Unit
    ) {
        val localLlmConfig = config.localLlmConfig
        val modelPath = localLlmConfig.modelPath
        
        if (modelPath.isBlank()) {
            callback(Result.failure(Exception("Local model path not configured in settings.")))
            return
        }

        // Run in a background thread as local LLM inference is CPU intensive and blocking
        Thread {
            try {
                if (currentModelPath != modelPath) {
                    val success = service.loadModel(modelPath)
                    if (!success) {
                        mainHandler.post {
                            callback(Result.failure(Exception("Failed to load local model at $modelPath")))
                        }
                        return@Thread
                    }
                    currentModelPath = modelPath
                }

                val fullPrompt = "$prompt\n\nInput: $userText\n\nResponse:"
                val response = service.generateResponseNative(
                    fullPrompt,
                    localLlmConfig.temperature,
                    localLlmConfig.topP,
                    localLlmConfig.maxTokens
                )

                mainHandler.post {
                    if (response.startsWith("Error:")) {
                        callback(Result.failure(Exception(response)))
                    } else {
                        callback(Result.success(response.trim()))
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    callback(Result.failure(e))
                }
            }
        }.start()
    }
}
