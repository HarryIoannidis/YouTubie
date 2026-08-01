package com.youtubie.app.data.repository

import com.youtubie.app.BuildConfig
import com.youtubie.app.data.model.VideoInfoResponse
import com.youtubie.app.data.remote.YoutubeApiService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that wraps YouTube API calls and converts Retrofit responses into Kotlin [Result] values.
 *
 * @param apiService Retrofit service used for network requests.
 */
@Singleton
class YoutubeRepository @Inject constructor(
    private val apiService: YoutubeApiService
) {
    /**
     * Resolves display metadata for a video.
     *
     * @param videoId YouTube video identifier.
     * @return [Result.success] with metadata, or [Result.failure] when the API key is missing,
     * the HTTP response is unsuccessful, or the request throws.
     */
    suspend fun getVideoMetadata(videoId: String): Result<VideoInfoResponse> {
        if (BuildConfig.RAPID_API_KEY.isBlank()) {
            return Result.failure(Exception("RAPID_API_KEY is not configured. Please add it to your local.properties file."))
        }
        return try {
            val response = apiService.getVideoInfo(videoId = videoId, apiKey = BuildConfig.RAPID_API_KEY)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                Result.failure(Exception(errorMsg.ifEmpty { "HTTP ${response.code()}" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resolves downloadable media URLs for a video.
     *
     * @param videoId YouTube video identifier.
     * @return [Result.success] with format data, or [Result.failure] when the API key is missing,
     * the HTTP response is unsuccessful, or the request throws.
     */
    suspend fun getDownloadUrl(videoId: String): Result<VideoInfoResponse> {
        if (BuildConfig.RAPID_API_KEY.isBlank()) {
            return Result.failure(Exception("RAPID_API_KEY is not configured. Please add it to your local.properties file."))
        }
        return try {
            val response = apiService.getDownloadUrl(videoId = videoId, apiKey = BuildConfig.RAPID_API_KEY)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                Result.failure(Exception(errorMsg.ifEmpty { "HTTP ${response.code()}" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
