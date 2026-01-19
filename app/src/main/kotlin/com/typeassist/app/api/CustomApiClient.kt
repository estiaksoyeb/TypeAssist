package com.typeassist.app.api

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class CustomApiClient(private val client: OkHttpClient) {

    fun callCustomApi(
        baseUrl: String,
        apiKey: String,
        model: String,
        prompt: String,
        userText: String,
        callback: (Result<String>) -> Unit
    ) {
        val jsonBody = JSONObject()
        val messagesArray = JSONArray()
        
        // System message (prompt)
        val systemMessage = JSONObject()
        systemMessage.put("role", "system")
        systemMessage.put("content", prompt)
        messagesArray.put(systemMessage)
        
        // User message
        val userMessage = JSONObject()
        userMessage.put("role", "user")
        userMessage.put("content", userText)
        messagesArray.put(userMessage)
        
        jsonBody.put("model", model)
        jsonBody.put("messages", messagesArray)

        // Ensure baseUrl doesn't end with slash and append chat completions endpoint if not present
        // However, usually custom BaseURL is like "https://api.groq.com/openai/v1" and we append "/chat/completions"
        // Or user provides full URL? "BaseURL" usually implies the root for the API version.
        // Standard OpenAI SDK behavior: baseURL + "/chat/completions"
        
        val cleanBaseUrl = baseUrl.trim().removeSuffix("/")
        val url = if (cleanBaseUrl.endsWith("/chat/completions")) {
            cleanBaseUrl
        } else {
            "$cleanBaseUrl/chat/completions"
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
        
        val requestBuilder = Request.Builder()
            .url(url)
            .post(requestBody)

        if (apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $apiKey")
        }

        client.newCall(requestBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        val errorBody = it.body?.string()
                        val errorCode = it.code
                        val errorMessage = "$errorCode: $errorBody"
                        callback(Result.failure(IOException(errorMessage)))
                        return
                    }
                    try {
                        val responseData = it.body?.string() ?: throw IOException("Empty response body")
                        val jsonResponse = JSONObject(responseData)
                        val choices = jsonResponse.getJSONArray("choices")
                        if (choices.length() > 0) {
                             val resultText = choices.getJSONObject(0).getJSONObject("message").getString("content")
                             callback(Result.success(resultText.trim()))
                        } else {
                            callback(Result.failure(IOException("No choices returned")))
                        }
                    } catch (e: Exception) {
                        callback(Result.failure(e))
                    }
                }
            }
        })
    }
}
