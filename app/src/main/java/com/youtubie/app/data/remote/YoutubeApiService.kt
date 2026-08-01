package com.youtubie.app.data.remote

import com.youtubie.app.data.model.VideoInfoResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Retrofit definition for the RapidAPI YouTube endpoints used by YouTubie.
 */
interface YoutubeApiService {
    /**
     * Fetches metadata for a YouTube video.
     *
     * @param videoId YouTube video identifier without URL decorations.
     * @param extend API-specific detail level requested by the app.
     * @param apiKey RapidAPI key injected from build configuration.
     * @param apiHost RapidAPI host header required by the endpoint.
     * @return HTTP response containing [VideoInfoResponse] on success.
     */
    @GET("video/info")
    suspend fun getVideoInfo(
        @Query("id") videoId: String,
        @Query("extend") extend: String = "2",
        @Header("x-rapidapi-key") apiKey: String,
        @Header("x-rapidapi-host") apiHost: String = "yt-api.p.rapidapi.com"
    ): Response<VideoInfoResponse>

    /**
     * Fetches downloadable media URLs for a YouTube video.
     *
     * @param videoId YouTube video identifier without URL decorations.
     * @param cgeo country/region code used by the API when resolving download links.
     * @param apiKey RapidAPI key injected from build configuration.
     * @param apiHost RapidAPI host header required by the endpoint.
     * @return HTTP response containing media formats and direct URLs on success.
     */
    @GET("dl")
    suspend fun getDownloadUrl(
        @Query("id") videoId: String,
        @Query("cgeo") cgeo: String = "GR",
        @Header("x-rapidapi-key") apiKey: String,
        @Header("x-rapidapi-host") apiHost: String = "yt-api.p.rapidapi.com"
    ): Response<VideoInfoResponse>
}
