package com.typeassist.app.api

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.typeassist.app.data.AppConfig
import com.typeassist.app.service.MyAccessibilityService
import java.io.File
import java.io.FileOutputStream

class LocalLlmClient(private val service: MyAccessibilityService) : AiProvider {

    private val TAG = "LocalLlmClient"
    private var lastResolvedUri: String = ""
    private var currentModelPath: String = ""
    private var currentUseGpu: Boolean = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun generateResponse(
        prompt: String,
        userText: String,
        config: AppConfig,
        callback: (Result<String>) -> Unit
    ) {
        val localLlmConfig = config.localLlmConfig
        val modelUriOrPath = localLlmConfig.modelPath
        
        Log.d(TAG, "KOTLIN: Received request. Provider: LOCAL")
        Log.d(TAG, "KOTLIN: Model Path/URI: $modelUriOrPath")
        
        if (modelUriOrPath.isBlank()) {
            LogE("KOTLIN: Model path is blank!")
            callback(Result.failure(Exception("Local model path not configured in settings.")))
            return
        }

        Thread {
            try {
                // 0. Stop any previous generation
                service.stopGenerationNative()

                // 1. Resolve Path
                Log.d(TAG, "KOTLIN: Resolving model path...")
                val finalPath = resolveModelPath(modelUriOrPath)
                if (finalPath == null) {
                    LogE("KOTLIN: Path resolution failed")
                    mainHandler.post { callback(Result.failure(Exception("Could not access or resolve model: $modelUriOrPath"))) }
                    return@Thread
                }
                Log.d(TAG, "KOTLIN: Resolved path: $finalPath")

                // 2. Load Model if path or GPU setting changed
                if (currentModelPath != finalPath || currentUseGpu != localLlmConfig.useGpu) {
                    Log.d(TAG, "KOTLIN: Requesting native model load (GPU: ${localLlmConfig.useGpu})...")
                    val success = service.loadModel(finalPath, localLlmConfig.useGpu)
                    if (!success) {
                        LogE("KOTLIN: Native loadModel returned false")
                        mainHandler.post { callback(Result.failure(Exception("llama.cpp failed to load model at $finalPath"))) }
                        return@Thread
                    }
                    currentModelPath = finalPath
                    currentUseGpu = localLlmConfig.useGpu
                    Log.d(TAG, "KOTLIN: Native loadModel successful")
                }

                // 3. Inference - Use ChatML template for Instruct models
                // For reasoning models, optionally suppress thinking via /no_think hint
                val systemInstruction = if (localLlmConfig.disableReasoning) {
                    "$prompt\n/no_think"
                } else {
                    prompt
                }

                // Build ChatML with plain concatenation — trimIndent() misbehaves when
                // interpolated values contain newlines (e.g. multi-line prompts or /no_think)
                val fullPrompt = buildString {
                    append("<|im_start|>system\n").append(systemInstruction).append("<|im_end|>\n")
                    append("<|im_start|>user\n").append(userText).append("<|im_end|>\n")
                    append("<|im_start|>assistant\n")
                }
                
                Log.d(TAG, "KOTLIN: Calling native inference bridge...")
                Log.d(TAG, "KOTLIN: Full Prompt Sent to C++: [$fullPrompt]")
                
                val response = service.generateResponseNative(
                    fullPrompt,
                    localLlmConfig.temperature,
                    localLlmConfig.topP,
                    localLlmConfig.maxTokens
                )

                Log.d(TAG, "KOTLIN: Native bridge returned. Response length: ${response.length}")
                Log.d(TAG, "KOTLIN: Response Text: [$response]")

                mainHandler.post {
                    if (response.startsWith("Error:")) {
                        LogE("KOTLIN: Error from native: $response")
                        callback(Result.failure(Exception(response)))
                    } else {
                        Log.d(TAG, "KOTLIN: Success! Returning response to Service.")
                        val cleanedResponse = cleanModelResponse(response)
                        callback(Result.success(cleanedResponse))
                    }
                }
            } catch (e: Exception) {
                LogE("KOTLIN: Exception in thread: ${e.message}")
                mainHandler.post { callback(Result.failure(e)) }
            }
        }.start()
    }

    private fun LogE(msg: String) {
        Log.e(TAG, msg)
    }

    private fun resolveModelPath(uriOrPath: String): String? {
        if (uriOrPath.startsWith("/")) return uriOrPath
        
        if (uriOrPath.startsWith("content://")) {
            val cacheFile = File(service.cacheDir, "local_model.gguf")
            if (lastResolvedUri == uriOrPath && cacheFile.exists()) {
                Log.d(TAG, "KOTLIN: Using already cached model file")
                return cacheFile.absolutePath
            }

            return try {
                Log.d(TAG, "KOTLIN: Copying content URI to internal cache (this may take a moment)...")
                val uri = Uri.parse(uriOrPath)
                service.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                }
                lastResolvedUri = uriOrPath
                Log.d(TAG, "KOTLIN: Copy complete. Size: ${cacheFile.length() / 1024 / 1024} MB")
                cacheFile.absolutePath
            } catch (e: Exception) {
                Log.e(TAG, "KOTLIN: Failed to copy content URI", e)
                null
            }
        }
        return uriOrPath
    }
}
