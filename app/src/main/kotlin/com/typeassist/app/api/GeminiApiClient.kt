package com.typeassist.app.api


import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class GeminiApiClient(private val client: OkHttpClient) {

    fun callGemini(
        apiKey: String,
        model: String,
        prompt: String,
        userText: String,
        temp: Double,
        topP: Double,
        callback: (Result<String>) -> Unit
    ) {
        val jsonBody = JSONObject()
        val contentsArray = JSONArray()
        val contentObject = JSONObject()
        val partsArray = JSONArray()
        val partObject = JSONObject()
        partObject.put("text", "$prompt\n\nInput: $userText")
        partsArray.put(partObject)
        contentObject.put("parts", partsArray)
        contentsArray.put(contentObject)
        jsonBody.put("contents", contentsArray)

        val genConfig = JSONObject()
        genConfig.put("temperature", temp)
        genConfig.put("topP", topP)
        jsonBody.put("generationConfig", genConfig)

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        val errorBody = it.body?.string()
                        val errorCode = it.code
                        val errorMessage = getErrorMessage(errorCode, errorBody)
                        callback(Result.failure(IOException(errorMessage)))
                        return
                    }
                    try {
                        val responseData = it.body?.string()
                        val jsonResponse = JSONObject(responseData)
                        val resultText = jsonResponse.getJSONArray("candidates").getJSONObject(0)
                            .getJSONObject("content").getJSONArray("parts").getJSONObject(0)
                            .getString("text")
                        callback(Result.success(resultText.trim()))
                    } catch (e: Exception) {
                        callback(Result.failure(e))
                    }
                }
            }
        })
    }

    private fun getErrorMessage(code: Int, body: String?): String {
        val message = try {
            JSONObject(body).getJSONObject("error").getString("message")
        } catch (e: Exception) {
            getErrorMessageForCode(code)
        }
        return "$code: $message"
    }

    private fun getErrorMessageForCode(code: Int): String {
        return when (code) {
            400 -> "Bad Request"
            401 -> "Unauthorized"
            403 -> "Forbidden"
            404 -> "Not Found"
            429 -> "Too Many Requests"
            500 -> "Internal Server Error"
            503 -> "Service Unavailable"
            else -> "Unexpected code $code"
        }
    }
}
