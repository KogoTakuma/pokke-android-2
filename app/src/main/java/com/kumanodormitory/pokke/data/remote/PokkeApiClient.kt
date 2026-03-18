package com.kumanodormitory.pokke.data.remote

import com.kumanodormitory.pokke.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object PokkeApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val apiKey = BuildConfig.POKKE_API_KEY
            val request = if (apiKey.isNotBlank()) {
                chain.request().newBuilder()
                    .addHeader("X-API-Key", apiKey)
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }
        .build()

    val service: PokkeApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.POKKE_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(PokkeApiService::class.java)
    }
}
