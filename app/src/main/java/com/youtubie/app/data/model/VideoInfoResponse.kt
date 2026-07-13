package com.youtubie.app.data.model

import com.google.gson.annotations.SerializedName

data class VideoInfoResponse(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("thumbnail") val thumbnails: List<Thumbnail>?,
    @SerializedName("channelTitle") val channelTitle: String?,
    @SerializedName("viewCount") val viewCount: String?,
    @SerializedName("lengthSeconds") val durationSeconds: String?,
    @SerializedName("lengthText") val durationText: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("formats") val formats: List<Format>?,
    @SerializedName("adaptiveFormats") val adaptiveFormats: List<AdaptiveFormat>?
)

data class Thumbnail(
    @SerializedName("url") val url: String,
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int
)

data class Format(
    @SerializedName("itag") val itag: Int?,
    @SerializedName("url") val url: String?,
    @SerializedName("mimeType") val mimeType: String?,
    @SerializedName("qualityLabel") val qualityLabel: String?,
    @SerializedName("contentLength") val contentLength: String?
)

data class AdaptiveFormat(
    @SerializedName("itag") val itag: Int?,
    @SerializedName("url") val url: String?,
    @SerializedName("mimeType") val mimeType: String?,
    @SerializedName("qualityLabel") val qualityLabel: String?,
    @SerializedName("audioQuality") val audioQuality: String?,
    @SerializedName("contentLength") val contentLength: String?
)

data class DownloadHistoryItem(
    val videoId: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val downloadDate: String,
    val format: String, // "Audio" or "Video"
    val viewCount: String? = null,
    val duration: String? = null
)
