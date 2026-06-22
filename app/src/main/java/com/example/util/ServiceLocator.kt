package com.example.util

import android.content.Context
import com.example.data.api.FinnhubService
import com.example.data.api.NewsApiService
import com.example.data.api.OpenRouterService
import com.example.data.api.TwelveDataService
import com.example.data.local.AppDatabase
import com.example.data.repository.TradeRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ServiceLocator {

    private lateinit var appDatabase: AppDatabase
    lateinit var tradeRepository: TradeRepository
        private set
    lateinit var keyManager: KeyManager
        private set

    fun initialize(context: Context) {
        // 1. Initialize DB and KeyManager
        appDatabase = AppDatabase.getDatabase(context)
        keyManager = KeyManager(appDatabase.apiKeyDao())

        // 2. Initialize Network Services
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val moshiConverterFactory = MoshiConverterFactory.create(moshi)

        // Web Client - Twelve Data
        val twelveDataService = Retrofit.Builder()
            .baseUrl("https://api.twelvedata.com/")
            .client(okHttpClient)
            .addConverterFactory(moshiConverterFactory)
            .build()
            .create(TwelveDataService::class.java)

        // Web Client - Finnhub
        val finnhubService = Retrofit.Builder()
            .baseUrl("https://finnhub.io/api/v1/")
            .client(okHttpClient)
            .addConverterFactory(moshiConverterFactory)
            .build()
            .create(FinnhubService::class.java)

        // Web Client - NewsAPI
        val newsApiService = Retrofit.Builder()
            .baseUrl("https://newsapi.org/")
            .client(okHttpClient)
            .addConverterFactory(moshiConverterFactory)
            .build()
            .create(NewsApiService::class.java)

        // Web Client - OpenRouter
        val openRouterService = Retrofit.Builder()
            .baseUrl("https://openrouter.ai/api/v1/")
            .client(okHttpClient)
            .addConverterFactory(moshiConverterFactory)
            .build()
            .create(OpenRouterService::class.java)

        // 3. Initialize Unified Repository
        tradeRepository = TradeRepository(
            watchlistDao = appDatabase.watchlistDao(),
            priceAlertDao = appDatabase.priceAlertDao(),
            chatMessageDao = appDatabase.chatMessageDao(),
            apiKeyDao = appDatabase.apiKeyDao(),
            twelveDataService = twelveDataService,
            finnhubService = finnhubService,
            newsApiService = newsApiService,
            openRouterService = openRouterService,
            keyManager = keyManager,
            context = context.applicationContext
        )
    }
}
