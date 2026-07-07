package com.youtubie.app.data.repository

import com.youtubie.app.BuildConfig
import com.youtubie.app.data.model.VideoInfoResponse
import com.youtubie.app.data.remote.YoutubeApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YoutubeRepository @Inject constructor(
    private val apiService: YoutubeApiService
) {
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
