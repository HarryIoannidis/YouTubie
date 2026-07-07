package com.youtubie.app.data.remote

import com.youtubie.app.data.model.VideoInfoResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface YoutubeApiService {
    @GET("video/info")
    suspend fun getVideoInfo(
        @Query("id") videoId: String,
        @Query("extend") extend: String = "2",
        @Header("x-rapidapi-key") apiKey: String,
        @Header("x-rapidapi-host") apiHost: String = "yt-api.p.rapidapi.com"
    ): Response<VideoInfoResponse>

    @GET("dl")
    suspend fun getDownloadUrl(
        @Query("id") videoId: String,
        @Query("cgeo") cgeo: String = "GR",
        @Header("x-rapidapi-key") apiKey: String,
        @Header("x-rapidapi-host") apiHost: String = "yt-api.p.rapidapi.com"
    ): Response<VideoInfoResponse>
}
