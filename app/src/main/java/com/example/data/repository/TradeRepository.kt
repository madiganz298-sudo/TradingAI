package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.api.FinnhubProfileResponse
import com.example.data.api.FinnhubService
import com.example.data.api.NewsArticle
import com.example.data.api.NewsApiService
import com.example.data.api.OpenRouterMessage
import com.example.data.api.OpenRouterRequest
import com.example.data.api.OpenRouterService
import com.example.data.api.TwelveDataService
import com.example.data.local.ApiKeyDao
import com.example.data.local.ChatMessageDao
import com.example.data.local.PriceAlertDao
import com.example.data.local.WatchlistDao
import com.example.data.model.ApiKeyEntity
import com.example.data.model.ChatMessage
import com.example.data.model.PriceAlert
import com.example.data.model.WatchlistItem
import com.example.util.CalculationCandle
import com.example.util.IndicatorsCalculator
import com.example.util.KeyManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException

class TradeRepository(
    private val watchlistDao: WatchlistDao,
    private val priceAlertDao: PriceAlertDao,
    private val chatMessageDao: ChatMessageDao,
    private val apiKeyDao: ApiKeyDao,
    private val twelveDataService: TwelveDataService,
    private val finnhubService: FinnhubService,
    private val newsApiService: NewsApiService,
    private val openRouterService: OpenRouterService,
    private val keyManager: KeyManager,
    private val context: Context
) {

    // Reactive Watchlist, Alerts, Chat, API Keys flows
    val watchlistFlow: Flow<List<WatchlistItem>> = watchlistDao.getAllWatchlist()
    val priceAlertsFlow: Flow<List<PriceAlert>> = priceAlertDao.getAllAlerts()
    val chatHistoryFlow: Flow<List<ChatMessage>> = chatMessageDao.getChatHistory()
    val apiKeysFlow: Flow<List<ApiKeyEntity>> = apiKeyDao.getAllKeysFlow()

    // ==========================================
    // 1. WATCHLIST OPERATIONS
    // ==========================================

    suspend fun addToWatchlist(symbol: String, name: String, assetType: String) {
        val cleanSymbol = symbol.uppercase().trim()
        val item = WatchlistItem(
            symbol = cleanSymbol,
            name = name,
            assetType = assetType,
            lastPrice = 0.0,
            changePercent = 0.0
        )
        watchlistDao.insertWatchlist(item)
        // Fetch current quote immediately
        fetchQuoteForSymbol(cleanSymbol)
    }

    suspend fun removeFromWatchlist(symbol: String) {
        watchlistDao.deleteBySymbol(symbol.uppercase().trim())
    }

    private suspend fun fetchQuoteForSymbol(symbol: String) {
        try {
            executeWithRotation("FINNHUB") { apiKey ->
                val response = finnhubService.getQuote(symbol, apiKey)
                val currentPrice = response.c
                val percentChange = response.dp ?: 0.0

                // Keep previous state sparkline if any, or generate simulated trend line
                val currentWatchlist = watchlistDao.getAllWatchlistSync()
                val existing = currentWatchlist.firstOrNull { it.symbol == symbol }
                val updatedSparkline = if (existing != null && existing.sparklineData.isNotEmpty()) {
                    val list = existing.sparklineData.split(",").toMutableList()
                    if (list.size >= 10) list.removeAt(0)
                    list.add(currentPrice.toString())
                    list.joinToString(",")
                } else {
                    val prevPrice = response.pc ?: currentPrice
                    "$prevPrice,${(prevPrice + currentPrice)/2},$currentPrice"
                }

                watchlistDao.insertWatchlist(
                    WatchlistItem(
                        symbol = symbol,
                        name = existing?.name ?: symbol,
                        assetType = existing?.assetType ?: "STOCK",
                        lastPrice = currentPrice,
                        changePercent = percentChange,
                        sparklineData = updatedSparkline,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("TradeRepository", "Failed to get quote for $symbol: ${e.message}")
        }
    }

    suspend fun refreshWatchlistPrices() {
        val list = watchlistDao.getAllWatchlistSync()
        for (item in list) {
            fetchQuoteForSymbol(item.symbol)
        }
    }

    // ==========================================
    // 2. CHART CANDLES & INDICATOR FETCHING
    // ==========================================

    suspend fun getCandles(symbol: String, interval: String): List<CalculationCandle> {
        val mappedInterval = when (interval) {
            "1m" -> "1min"
            "5m" -> "5min"
            "15m" -> "15min"
            "1H" -> "1h"
            "4H" -> "4h"
            "Daily" -> "1day"
            "Weekly" -> "1week"
            else -> "5min"
        }

        return try {
            executeWithRotation("TWELVEDATA") { apiKey ->
                val response = twelveDataService.getTimeSeries(symbol, mappedInterval, apiKey, outputSize = 60)
                if (response.status == "error") {
                    throw IOException("TwelveData Error: ${response.message}")
                }
                IndicatorsCalculator.parseRawCandles(response.values)
            }
        } catch (e: Exception) {
            Log.e("TradeRepository", "Failed to retrieve candles: ${e.message}")
            emptyList()
        }
    }

    // ==========================================
    // 3. SECURED KEY SELECTION AND ROTATION WRAPPER
    // ==========================================

    private suspend fun <T> executeWithRotation(
        serviceType: String,
        block: suspend (apiKey: String) -> T
    ): T {
        var attempts = 0
        val serviceKeysCount = apiKeyDao.getKeysByService(serviceType).size.coerceAtLeast(1)
        val maxAttempts = serviceKeysCount + 1

        while (attempts < maxAttempts) {
            val key = keyManager.getActiveKey(serviceType)
            try {
                return block(key)
            } catch (e: HttpException) {
                if (e.code() == 401 || e.code() == 429) {
                    Log.w("TradeRepository", "Key error ${e.code()} on service: $serviceType. Rotating...")
                    keyManager.rotateKey(serviceType)
                    attempts++
                } else {
                    throw e
                }
            } catch (e: Exception) {
                // Network or internal errors can be retried or simply surfaced
                Log.e("TradeRepository", "Execution error: ${e.message}")
                throw e
            }
        }
        throw IOException("All keys exhausted for service $serviceType")
    }

    // ==========================================
    // 4. FUNDAMENTALS
    // ==========================================

    suspend fun getCompanyProfile(symbol: String): FinnhubProfileResponse? {
        return try {
            executeWithRotation("FINNHUB") { apiKey ->
                finnhubService.getProfile(symbol, apiKey)
            }
        } catch (e: Exception) {
            Log.e("TradeRepository", "Failed to query company profile: ${e.message}")
            null
        }
    }

    // ==========================================
    // 5. NEWS & SENTIMENT AGGREGATION
    // ==========================================

    suspend fun getSentimentNews(query: String): List<Pair<NewsArticle, String>> {
        return try {
            val results = executeWithRotation("NEWSAPI") { apiKey ->
                val response = newsApiService.getEverything(query = query, apiKey = apiKey)
                response.articles ?: emptyList()
            }
            results.map { article ->
                val text = "${article.title ?: ""} ${article.description ?: ""}".lowercase()
                val score = calculateSentimentScore(text)
                val label = when {
                    score > 1 -> "BULLISH"
                    score < -1 -> "BEARISH"
                    else -> "NEUTRAL"
                }
                Pair(article, label)
            }
        } catch (e: Exception) {
            Log.e("TradeRepository", "Failed to query NewsAPI: ${e.message}")
            emptyList()
        }
    }

    private fun calculateSentimentScore(text: String): Int {
        val bullWords = listOf("bullish", "profit", "gain", "rise", "grow", "buy", "surge", "up", "success", "positive", "high", "breakout")
        val bearWords = listOf("bearish", "loss", "fall", "drop", "decline", "sell", "crash", "down", "fail", "negative", "low", "dump")
        
        var bullCount = 0
        var bearCount = 0

        for (word in bullWords) {
            if (text.contains(word)) bullCount++
        }
        for (word in bearWords) {
            if (text.contains(word)) bearCount++
        }

        return bullCount - bearCount
    }

    // ==========================================
    // 6. CHATBOT INTERACTION
    // ==========================================

    suspend fun sendMessage(content: String): String {
        // 1. Store user message in history
        watchlistDao.getAllWatchlistSync() // standard database access bypass
        val userMsg = ChatMessage(role = "user", message = content)
        chatMessageDao.insertMessage(userMsg)

        // 2. Fetch history context to pass to OpenRouter
        val history = chatMessageDao.getChatHistory().first().takeLast(10)
        val openRouterMessages = history.map {
            OpenRouterMessage(role = it.role, content = it.message)
        }

        return try {
            val replyText = executeWithRotation("OPENROUTER") { apiKey ->
                val request = OpenRouterRequest(
                    model = "openrouter/free",
                    messages = openRouterMessages
                )
                val response = openRouterService.chatWithAi(
                    bearerToken = "Bearer $apiKey",
                    request = request
                )
                if (response.error != null) {
                    throw IOException("OpenRouter error: ${response.error.message}")
                }
                response.choices?.firstOrNull()?.message?.content ?: "Maaf, saya tidak mendapatkan balasan."
            }

            // 3. Save Assistant reply
            chatMessageDao.insertMessage(ChatMessage(role = "assistant", message = replyText))
            replyText
        } catch (e: Exception) {
            val errorMsg = "Gagal memproses. Silakan periksa kembali API Key OpenRouter Anda di Settings. Detail: ${e.localizedMessage}"
            chatMessageDao.insertMessage(ChatMessage(role = "assistant", message = errorMsg))
            errorMsg
        }
    }

    suspend fun clearHistory() {
        chatMessageDao.clearChatHistory()
    }

    // ==========================================
    // 7. API KEY MANAGEMENT
    // ==========================================

    suspend fun addApiKey(serviceType: String, key: String) {
        val cleanService = serviceType.uppercase().trim()
        val entry = ApiKeyEntity(serviceType = cleanService, apiKey = key.trim())
        apiKeyDao.insertKey(entry)
    }

    suspend fun deleteApiKey(id: Int) {
        apiKeyDao.deleteKey(id)
    }

    // ==========================================
    // 8. PRICE ALERT MONITORING
    // ==========================================

    suspend fun addAlert(symbol: String, targetPrice: Double, condition: String) {
        val alert = PriceAlert(
            symbol = symbol.uppercase().trim(),
            targetPrice = targetPrice,
            condition = condition.uppercase().trim()
        )
        priceAlertDao.insertAlert(alert)
    }

    suspend fun deletePriceAlert(id: Int) {
        priceAlertDao.deleteAlert(id)
    }

    suspend fun checkAndTriggerAlerts(): List<String> {
        val triggered = mutableListOf<String>()
        val activeAlerts = priceAlertDao.getActiveAlerts()
        if (activeAlerts.isEmpty()) return emptyList()

        for (alert in activeAlerts) {
            try {
                // Query current price via Finnhub
                val key = keyManager.getActiveKey("FINNHUB")
                val response = finnhubService.getQuote(alert.symbol, key)
                val currentPrice = response.c

                var isTriggered = false
                if (alert.condition == "ABOVE" && currentPrice >= alert.targetPrice) {
                    isTriggered = true
                } else if (alert.condition == "BELOW" && currentPrice <= alert.targetPrice) {
                    isTriggered = true
                }

                if (isTriggered) {
                    priceAlertDao.updateAlertStatus(alert.id, false)
                    triggered.add("PRICE ALERT: ${alert.symbol} telah mencapai target ${alert.condition} ${alert.targetPrice} (Harga saat ini: $currentPrice)!")
                }
            } catch (e: Exception) {
                Log.e("TradeRepository", "Failed to check alert for ${alert.symbol}: ${e.message}")
            }
        }
        return triggered
    }
}
