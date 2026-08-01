package com.youtubie.app.di

import com.youtubie.app.data.remote.YoutubeApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module that provides singleton networking dependencies for the app.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Creates the shared API OkHttp client.
     *
     * @return configured [OkHttpClient] with 30-second connect, read, and write timeouts.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Creates the Retrofit client for RapidAPI's YouTube host.
     *
     * @param okHttpClient HTTP client supplied by Hilt.
     * @return configured [Retrofit] instance with Gson conversion.
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://yt-api.p.rapidapi.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Creates the Retrofit implementation of [YoutubeApiService].
     *
     * @param retrofit Retrofit instance supplied by Hilt.
     * @return generated YouTube API service.
     */
    @Provides
    @Singleton
    fun provideYoutubeApiService(retrofit: Retrofit): YoutubeApiService {
        return retrofit.create(YoutubeApiService::class.java)
    }
}
