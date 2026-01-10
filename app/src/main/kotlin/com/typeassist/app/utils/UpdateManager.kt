package com.typeassist.app.utils

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val changelog: String,
    val forceUpdate: Boolean
)

object UpdateManager {
    private val client = OkHttpClient()
    private const val UPDATE_URL = "https://raw.githubusercontent.com/estiaksoyeb/TypeAssist-Releases/refs/heads/main/version.json"

    suspend fun checkForUpdates(context: Context): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url("$UPDATE_URL?t=${System.currentTimeMillis()}").build()
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    val info = Gson().fromJson(json, UpdateInfo::class.java)
                    
                    // Return info regardless of version check so MainActivity can handle cache clearing logic
                    return@withContext info
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext null
        }
    }
}
