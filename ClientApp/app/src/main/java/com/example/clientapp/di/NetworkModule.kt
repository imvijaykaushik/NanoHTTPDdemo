package com.example.clientapp.di

import com.example.clientapp.data.remote.HostSelectionInterceptor
import com.example.clientapp.data.remote.ServerApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Placeholder base URL. Every request's host/port is rewritten by
     * [HostSelectionInterceptor]; Retrofit only requires *some* valid URL here.
     */
    private const val PLACEHOLDER_BASE_URL = "http://localhost/"

    private const val TIMEOUT_SECONDS = 5L

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        // Tolerate extra fields so a newer server cannot break an older client.
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        hostSelectionInterceptor: HostSelectionInterceptor,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // BASIC logs method/URL/status only - no bodies, so message contents
            // are not written to logcat.
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(hostSelectionInterceptor)
            .addInterceptor(logging)
            // Short timeouts: a LAN peer either answers quickly or is not there.
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            // Retrying would multiply the wait before the user sees an error.
            .retryOnConnectionFailure(false)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(PLACEHOLDER_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideServerApi(retrofit: Retrofit): ServerApi = retrofit.create(ServerApi::class.java)
}
