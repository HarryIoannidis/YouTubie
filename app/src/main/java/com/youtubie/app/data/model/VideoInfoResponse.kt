package com.youtubie.app.data.model

import com.google.gson.annotations.SerializedName

data class VideoInfoResponse(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("thumbnail") val thumbnails: List<Thumbnail>?,
    @SerializedName("channelTitle") val channelTitle: String?,
    @SerializedName("viewCount") val viewCount: String?,
    @SerializedName("lengthText") val duration: String?,
    @SerializedName("description") val description: String?
)

data class Thumbnail(
    @SerializedName("url") val url: String,
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int
)
