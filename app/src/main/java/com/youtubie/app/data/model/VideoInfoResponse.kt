package com.youtubie.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Metadata response returned by the YouTube API endpoints used by the app.
 *
 * @property id YouTube video identifier.
 * @property title video title shown in search results and download history.
 * @property thumbnails available thumbnail images ordered by the API response.
 * @property channelTitle publishing channel name.
 * @property viewCount raw view-count value returned by the API.
 * @property durationSeconds duration in seconds when available.
 * @property durationText formatted duration text when supplied by the API.
 * @property description video description text.
 * @property formats progressive formats that usually contain muxed audio and video.
 * @property adaptiveFormats separate audio or video streams used for higher-quality downloads.
 */
data class VideoInfoResponse(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("thumbnail") val thumbnails: List<Thumbnail>?,
    @SerializedName("channelTitle") val channelTitle: String?,
    @SerializedName("viewCount") val viewCount: String?,
    @SerializedName("lengthSeconds") val durationSeconds: String?,
    @SerializedName("lengthText") val durationText: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("formats") val formats: List<VideoFormat>?,
    @SerializedName("adaptiveFormats") val adaptiveFormats: List<AdaptiveVideoFormat>?
)

/**
 * Thumbnail image descriptor from the API response.
 *
 * @property url absolute URL for the thumbnail image.
 * @property width image width in pixels.
 * @property height image height in pixels.
 */
data class Thumbnail(
    @SerializedName("url") val url: String,
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int
)

/**
 * Progressive media format that can usually be downloaded as a single playable file.
 *
 * @property itag YouTube format identifier.
 * @property url direct media URL when the API exposes one.
 * @property mimeType media MIME type and codec information.
 * @property qualityLabel human-readable video quality such as 720p.
 * @property contentLength optional stream size in bytes as a string.
 */
data class VideoFormat(
    @SerializedName("itag") val itag: Int?,
    @SerializedName("url") val url: String?,
    @SerializedName("mimeType") val mimeType: String?,
    @SerializedName("qualityLabel") val qualityLabel: String?,
    @SerializedName("contentLength") val contentLength: String?
)

/**
 * Adaptive media format for separate audio-only or video-only streams.
 *
 * @property itag YouTube format identifier.
 * @property url direct media URL when the API exposes one.
 * @property mimeType media MIME type and codec information.
 * @property qualityLabel human-readable video quality for video streams.
 * @property audioQuality audio quality label for audio streams.
 * @property contentLength optional stream size in bytes as a string.
 */
data class AdaptiveVideoFormat(
    @SerializedName("itag") val itag: Int?,
    @SerializedName("url") val url: String?,
    @SerializedName("mimeType") val mimeType: String?,
    @SerializedName("qualityLabel") val qualityLabel: String?,
    @SerializedName("audioQuality") val audioQuality: String?,
    @SerializedName("contentLength") val contentLength: String?
)

/**
 * Persisted record for a completed download shown in the history screen.
 *
 * @property videoId YouTube video identifier used for de-duplication.
 * @property title title shown in the history list.
 * @property channelTitle publishing channel name.
 * @property thumbnailUrl thumbnail displayed beside the history item.
 * @property downloadDate local date string for when the item was saved.
 * @property format downloaded asset type, for example Audio, Video, or Image.
 * @property viewCount optional view-count metadata captured at download time.
 * @property duration optional formatted duration captured at download time.
 */
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

/**
 * Persisted search-history entry.
 *
 * @property query original URL or query text entered by the user.
 * @property title optional resolved video title added after a successful metadata fetch.
 */
data class SearchHistoryItem(
    val query: String,
    val title: String? = null
)
