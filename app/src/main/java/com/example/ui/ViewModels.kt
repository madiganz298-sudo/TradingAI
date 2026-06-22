package com.example.ui

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.api.FinnhubProfileResponse
import com.example.data.api.NewsArticle
import com.example.data.model.ApiKeyEntity
import com.example.data.model.ChatMessage
import com.example.data.model.PriceAlert
import com.example.data.model.WatchlistItem
import com.example.util.AlertCheckWorker
import com.example.util.BollingerBandsResult
import com.example.util.BreakerBlock
import com.example.util.CalculationCandle
import com.example.util.EmaResult
import com.example.util.FibLevel
import com.example.util.FvgZone
import com.example.util.IndicatorsCalculator
import com.example.util.LiquiditySweep
import com.example.util.MacdResult
import com.example.util.OrderBlock
import com.example.util.RsiResult
import com.example.util.ServiceLocator
import com.example.util.VolumeProfileBin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

// =========================================================
// 1. WATCHLIST & HOME VIEWMODEL
// =========================================================
class WatchlistViewModel : ViewModel() {
    private val repository = ServiceLocator.tradeRepository

    val watchlist: StateFlow<List<WatchlistItem>> = repository.watchlistFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alerts: StateFlow<List<PriceAlert>> = repository.priceAlertsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        // Hydrate default items if watchlist is empty
        viewModelScope.launch {
            val list = repository.watchlistFlow.stateIn(viewModelScope).value
            if (list.isEmpty()) {
                repository.addToWatchlist("AAPL", "Apple Inc.", "STOCK")
                repository.addToWatchlist("EURUSD", "Euro / US Dollar", "FOREX")
                repository.addToWatchlist("BTCUSD", "Bitcoin / USD", "CRYPTO")
            }
            refreshAll()
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshWatchlistPrices()
            _isRefreshing.value = false
        }
    }

    fun addToWatchlist(symbol: String, name: String, type: String) {
        viewModelScope.launch {
            repository.addToWatchlist(symbol, name, type)
        }
    }

    fun removeFromWatchlist(symbol: String) {
        viewModelScope.launch {
            repository.removeFromWatchlist(symbol)
        }
    }

    fun addPriceAlert(symbol: String, targetPrice: Double, condition: String) {
        viewModelScope.launch {
            repository.addAlert(symbol, targetPrice, condition)
        }
    }

    fun removePriceAlert(id: Int) {
        viewModelScope.launch {
            repository.deletePriceAlert(id)
        }
    }

    /**
     * Exports watchlist as a CSV file to local cache and returns content or message
     */
    fun exportWatchlistToCSV(context: Context): String {
        val items = watchlist.value
        if (items.isEmpty()) return "Watchlist kosong"

        return try {
            val file = File(context.cacheDir, "TradeAI_Pro_Watchlist.csv")
            FileOutputStream(file).use { out ->
                val header = "Symbol,Name,AssetType,LastPrice,ChangePercent\n"
                out.write(header.toByteArray())
                for (item in items) {
                    val line = "${item.symbol},${item.name.replace(",", " ")},${item.assetType},${item.lastPrice},${item.changePercent}%\n"
                    out.write(line.toByteArray())
                }
            }
            "Berhasil diekspor ke file: ${file.absolutePath}"
        } catch (e: Exception) {
            "Gagal mengekspor CSV: ${e.localizedMessage}"
        }
    }
}

// =========================================================
// 2. CHART & ANALYSIS VIEWMODEL
// =========================================================
sealed interface ChartUiState {
    object Idle : ChartUiState
    object Loading : ChartUiState
    data class Success(
        val candles: List<CalculationCandle>,
        val ema: EmaResult?,
        val rsi: RsiResult?,
        val macd: MacdResult?,
        val bbands: BollingerBandsResult?,
        val volumeProfile: List<VolumeProfileBin>,
        val fvgs: List<FvgZone>,
        val obs: List<OrderBlock>,
        val sweeps: List<LiquiditySweep>,
        val breakers: List<BreakerBlock>,
        val fibLevels: List<FibLevel>
    ) : ChartUiState
    data class Error(val message: String) : ChartUiState
}

class ChartViewModel : ViewModel() {
    private val repository = ServiceLocator.tradeRepository

    private val _uiState = MutableStateFlow<ChartUiState>(ChartUiState.Idle)
    val uiState: StateFlow<ChartUiState> = _uiState.asStateFlow()

    private val _selectedSymbol = MutableStateFlow("AAPL")
    val selectedSymbol: StateFlow<String> = _selectedSymbol.asStateFlow()

    private val _selectedTimeframe = MutableStateFlow("5m")
    val selectedTimeframe: StateFlow<String> = _selectedTimeframe.asStateFlow()

    private val _profileState = MutableStateFlow<FinnhubProfileResponse?>(null)
    val profileState: StateFlow<FinnhubProfileResponse?> = _profileState.asStateFlow()

    private val _newsState = MutableStateFlow<List<Pair<NewsArticle, String>>>(emptyList())
    val newsState: StateFlow<List<Pair<NewsArticle, String>>> = _newsState.asStateFlow()

    // Active overlay and technical indicator configs toggles
    val showEma = MutableStateFlow(true)
    val showBbands = MutableStateFlow(false)
    val showVolumeProfile = MutableStateFlow(false)
    val showFib = MutableStateFlow(false)
    
    // ICT Concepts flags
    val showFvg = MutableStateFlow(true)
    val showOrderBlocks = MutableStateFlow(true)
    val showSweeps = MutableStateFlow(false)
    val showBreakers = MutableStateFlow(false)

    fun selectSymbol(symbol: String) {
        _selectedSymbol.value = symbol.uppercase()
        loadAnalysis()
    }

    fun selectTimeframe(interval: String) {
        _selectedTimeframe.value = interval
        loadAnalysis()
    }

    fun loadAnalysis() {
        val symbol = _selectedSymbol.value
        val interval = _selectedTimeframe.value

        viewModelScope.launch {
            _uiState.value = ChartUiState.Loading
            
            // 1. Fetch CompanyProfile background
            launch {
                _profileState.value = repository.getCompanyProfile(symbol)
            }

            // 2. Fetch related sentiment news
            launch {
                _newsState.value = repository.getSentimentNews(symbol)
            }

            // 3. Fetch candlesticks and calculate standard and ICT parameters
            try {
                val candles = repository.getCandles(symbol, interval)
                if (candles.isEmpty()) {
                    _uiState.value = ChartUiState.Error("Data historis tidak tersedia. Periksa koneksi jaringan atau Twelve Data API Key Anda di Settings.")
                    return@launch
                }

                // Standard Calculations
                val ema = IndicatorsCalculator.calculateEMA(candles, 20)
                val rsi = IndicatorsCalculator.calculateRSI(candles, 14)
                val macd = IndicatorsCalculator.calculateMACD(candles)
                val bbands = IndicatorsCalculator.calculateBollingerBands(candles, 20)
                val volProfile = IndicatorsCalculator.calculateVolumeProfile(candles)
                val fib = IndicatorsCalculator.calculateFibonacci(candles)

                // ICT Calculations
                val fvgs = IndicatorsCalculator.detectFVG(candles)
                val obs = IndicatorsCalculator.detectOrderBlocks(candles)
                val sweeps = IndicatorsCalculator.detectLiquiditySweeps(candles)
                val breakers = IndicatorsCalculator.detectBreakerBlocks(candles, obs)

                _uiState.value = ChartUiState.Success(
                    candles = candles,
                    ema = ema,
                    rsi = rsi,
                    macd = macd,
                    bbands = bbands,
                    volumeProfile = volProfile,
                    fvgs = fvgs,
                    obs = obs,
                    sweeps = sweeps,
                    breakers = breakers,
                    fibLevels = fib
                )
            } catch (e: Exception) {
                _uiState.value = ChartUiState.Error("Analisis gagal: ${e.localizedMessage}")
            }
        }
    }
}

// =========================================================
// 3. CHATBOT VIEWMODEL
// =========================================================
class ChatViewModel : ViewModel() {
    private val repository = ServiceLocator.tradeRepository

    val chatMessages: StateFlow<List<ChatMessage>> = repository.chatHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isAILoading = MutableStateFlow(false)
    val isAILoading = _isAILoading.asStateFlow()

    init {
        viewModelScope.launch {
            // Put introductory greetings
            val current = chatMessages.value
            if (current.isEmpty()) {
                val intro = "Halo! Saya TradeAI Pro Assistant, analis pasar berdaya AI cerdas.\n\n" +
                        "Tanyakan saya mengenai:\n" +
                        "✓ Konsep analisis teknikal & fundamental\n" +
                        "✓ Konsep ICT (FVG, Order Blocks, Liquidity Sweeps)\n" +
                        "✓ Strategi trading, manajemen risiko, atau insight market."
                repository.sendMessage(intro) // Seed introductory dialogue
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _isAILoading.value = true
            repository.sendMessage(text)
            _isAILoading.value = false
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}

// =========================================================
// 4. SETTINGS VIEWMODEL
// =========================================================
class SettingsViewModel : ViewModel() {
    private val repository = ServiceLocator.tradeRepository

    val apiKeys: StateFlow<List<ApiKeyEntity>> = repository.apiKeysFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isAlertChecking = MutableStateFlow(false)
    val isAlertChecking = _isAlertChecking.asStateFlow()

    fun addApiKey(service: String, key: String) {
        if (key.isBlank()) return
        viewModelScope.launch {
            repository.addApiKey(service, key)
        }
    }

    fun deleteApiKey(id: Int) {
        viewModelScope.launch {
            repository.deleteApiKey(id)
        }
    }

    fun testManualAlertChecks(context: Context) {
        viewModelScope.launch {
            _isAlertChecking.value = true
            val triggered = repository.checkAndTriggerAlerts()
            if (triggered.isEmpty()) {
                Toast.makeText(context, "Semua alert aman (belum terpicu atau watchlist kosong). Check selesai!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "${triggered.size} alert terpicu! Notifikasi sistem dikirim.", Toast.LENGTH_LONG).show()
            }
            _isAlertChecking.value = false
        }
    }

    fun scheduleBackgroundChecks(context: Context) {
        // Enqueue WorkManager periodic check every 15 minutes
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicCheck = PeriodicWorkRequestBuilder<AlertCheckWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            "TradeAIPro_PriceAlert_Checks",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            periodicCheck
        )
        Toast.makeText(context, "Pengecekan alert terjadwal di background otomatis (setiap 15 menit)!", Toast.LENGTH_SHORT).show()
    }
}
