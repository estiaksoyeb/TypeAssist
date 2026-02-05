package com.typeassist.app.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("name") val name: String,
    @SerializedName("body") val body: String,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("published_at") val publishedAt: String
)
