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
        return try {
            val response = apiService.getVideoInfo(videoId = videoId, apiKey = BuildConfig.RAPID_API_KEY)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error fetching metadata: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDownloadUrl(videoId: String): Result<String> {
        return try {
            val response = apiService.getDownloadUrl(videoId = videoId, apiKey = BuildConfig.RAPID_API_KEY)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error fetching download URL: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
