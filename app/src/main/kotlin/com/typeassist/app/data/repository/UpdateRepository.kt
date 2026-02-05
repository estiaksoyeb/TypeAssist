package com.typeassist.app.data.repository

import android.content.Context
import com.typeassist.app.BuildConfig
import com.typeassist.app.data.model.GitHubRelease
import com.typeassist.app.data.network.GitHubApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class UpdateRepository(context: Context) {

    private val apiService: GitHubApiService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(GitHubApiService::class.java)
    }

    suspend fun checkForUpdate(owner: String, repo: String): Result<GitHubRelease?> {
        return try {
            val release = apiService.getLatestRelease(owner, repo)
            val currentVersion = BuildConfig.VERSION_NAME
            
            val cleanTagName = release.tagName.removePrefix("v")
            val cleanCurrentVersion = currentVersion.removePrefix("v").substringBefore("-") // Remove -debug if present

            if (isNewerVersion(cleanTagName, cleanCurrentVersion)) {
                Result.success(release)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isNewerVersion(newVersion: String, currentVersion: String): Boolean {
        val newParts = newVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }

        val length = maxOf(newParts.size, currentParts.size)
        
        for (i in 0 until length) {
            val newPart = if (i < newParts.size) newParts[i] else 0
            val currentPart = if (i < currentParts.size) currentParts[i] else 0
            
            if (newPart > currentPart) return true
            if (newPart < currentPart) return false
        }
        
        return false
    }
}
