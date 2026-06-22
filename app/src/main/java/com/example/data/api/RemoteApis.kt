package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

// ==========================================
// 1. TWELVE DATA API SERVICE
// ==========================================

@JsonClass(generateAdapter = true)
data class TwelveTimeSeriesResponse(
    @Json(name = "meta") val meta: TwelveTimeSeriesMeta?,
    @Json(name = "values") val values: List<TwelveCandle>?,
    @Json(name = "status") val status: String?,
    @Json(name = "message") val message: String?
)

@JsonClass(generateAdapter = true)
data class TwelveTimeSeriesMeta(
    @Json(name = "symbol") val symbol: String,
    @Json(name = "interval") val interval: String,
    @Json(name = "currency") val currency: String?,
    @Json(name = "exchange") val exchange: String?
)

@JsonClass(generateAdapter = true)
data class TwelveCandle(
    @Json(name = "datetime") val datetime: String,
    @Json(name = "open") val open: String,
    @Json(name = "high") val high: String,
    @Json(name = "low") val low: String,
    @Json(name = "close") val close: String,
    @Json(name = "volume") val volume: String
)

interface TwelveDataService {
    @GET("time_series")
    suspend fun getTimeSeries(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("apikey") apiKey: String,
        @Query("outputsize") outputSize: Int = 50 // Limit candles to optimize rendering
    ): TwelveTimeSeriesResponse
}

// ==========================================
// 2. FINNHUB API SERVICE
// ==========================================

@JsonClass(generateAdapter = true)
data class FinnhubQuoteResponse(
    @Json(name = "c") val c: Double,   // Current price
    @Json(name = "d") val d: Double?,  // Change
    @Json(name = "dp") val dp: Double?,// Change percent
    @Json(name = "h") val h: Double?,  // High price of the day
    @Json(name = "l") val l: Double?,  // Low price of the day
    @Json(name = "o") val o: Double?,  // Open price of the day
    @Json(name = "pc") val pc: Double? // Previous close price
)

@JsonClass(generateAdapter = true)
data class FinnhubProfileResponse(
    @Json(name = "country") val country: String?,
    @Json(name = "name") val name: String?,
    @Json(name = "shareOutstanding") val shareOutstanding: Double?,
    @Json(name = "ticker") val ticker: String?,
    @Json(name = "marketCapitalization") val marketCapitalization: Double?,
    @Json(name = "logo") val logo: String?,
    @Json(name = "weburl") val weburl: String?,
    @Json(name = "finnhubIndustry") val finnhubIndustry: String?
)

interface FinnhubService {
    @GET("quote")
    suspend fun getQuote(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): FinnhubQuoteResponse

    @GET("stock/profile2")
    suspend fun getProfile(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): FinnhubProfileResponse
}

// ==========================================
// 3. NEWS API SERVICE
// ==========================================

@JsonClass(generateAdapter = true)
data class NewsApiResponse(
    @Json(name = "status") val status: String?,
    @Json(name = "totalResults") val totalResults: Int?,
    @Json(name = "articles") val articles: List<NewsArticle>?
)

@JsonClass(generateAdapter = true)
data class NewsArticle(
    @Json(name = "title") val title: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "url") val url: String?,
    @Json(name = "urlToImage") val urlToImage: String?,
    @Json(name = "publishedAt") val publishedAt: String?,
    @Json(name = "source") val source: NewsSource?
)

@JsonClass(generateAdapter = true)
data class NewsSource(
    @Json(name = "id") val id: String?,
    @Json(name = "name") val name: String?
)

interface NewsApiService {
    @GET("v2/everything")
    suspend fun getEverything(
        @Query("q") query: String,
        @Query("sortBy") sortBy: String = "publishedAt",
        @Query("apiKey") apiKey: String,
        @Query("pageSize") pageSize: Int = 15
    ): NewsApiResponse
}

// ==========================================
// 4. OPENROUTER API SERVICE
// ==========================================

@JsonClass(generateAdapter = true)
data class OpenRouterRequest(
    @Json(name = "model") val model: String = "openrouter/free",
    @Json(name = "messages") val messages: List<OpenRouterMessage>
)

@JsonClass(generateAdapter = true)
data class OpenRouterMessage(
    @Json(name = "role") val role: String, // "user" or "assistant"
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class OpenRouterResponse(
    @Json(name = "choices") val choices: List<OpenRouterChoice>?,
    @Json(name = "error") val error: OpenRouterError?
)

@JsonClass(generateAdapter = true)
data class OpenRouterChoice(
    @Json(name = "message") val message: OpenRouterMessage?
)

@JsonClass(generateAdapter = true)
data class OpenRouterError(
    @Json(name = "message") val message: String?
)

interface OpenRouterService {
    @POST("chat/completions")
    suspend fun chatWithAi(
        @Header("Authorization") bearerToken: String, // Contains "Bearer <apiKey>"
        @Header("HTTP-Referer") referer: String = "https://aistudio.google.com/build",
        @Header("X-Title") appTitle: String = "TradeAI Pro",
        @Body request: OpenRouterRequest
    ): OpenRouterResponse
}
