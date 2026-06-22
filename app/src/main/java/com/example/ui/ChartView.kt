package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import com.example.data.api.FinnhubProfileResponse
import com.example.data.api.NewsArticle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.chart.TradeCandleChart
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartView(
    symbol: String,
    viewModel: ChartViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val timeframe by viewModel.selectedTimeframe.collectAsState()
    val companyProfile by viewModel.profileState.collectAsState()
    val newsArticles by viewModel.newsState.collectAsState()

    // Overlay triggers
    val showEma by viewModel.showEma.collectAsState()
    val showBbands by viewModel.showBbands.collectAsState()
    val showFib by viewModel.showFib.collectAsState()
    val showFvg by viewModel.showFvg.collectAsState()
    val showOrderBlocks by viewModel.showOrderBlocks.collectAsState()
    val showSweeps by viewModel.showSweeps.collectAsState()
    val showBreakers by viewModel.showBreakers.collectAsState()

    // Trigger load when selected symbol transitions
    LaunchedEffect(symbol) {
        viewModel.selectSymbol(symbol)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$symbol - TradeAI Pro Analytics", color = PrimaryGold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PrimaryGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is ChartUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryGold)
                    }
                }

                is ChartUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.message, color = BearRed, lineHeight = 22.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadAnalysis() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black)
                        ) {
                            Text("Coba Lagi")
                        }
                    }
                }

                is ChartUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Timeframe Selectors horizontal bar
                        item {
                            TimeframeSelectorBar(
                                selected = timeframe,
                                onSelect = { viewModel.selectTimeframe(it) }
                            )
                        }

                        // 2. Main Candlestick Chart Area
                        item {
                            TradeCandleChart(
                                candles = state.candles,
                                emaResult = state.ema,
                                bbands = state.bbands,
                                fibLevels = state.fibLevels,
                                showEma = showEma,
                                showBbands = showBbands,
                                showFib = showFib,
                                modifier = Modifier.border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                            )
                        }

                        // 3. Technical & ICT Overlays Toggles Area
                        item {
                            IndicatorTogglesArea(
                                showEma = showEma,
                                showBbands = showBbands,
                                showFib = showFib,
                                showFvg = showFvg,
                                showOrderBlocks = showOrderBlocks,
                                showSweeps = showSweeps,
                                showBreakers = showBreakers,
                                onToggleEma = { viewModel.showEma.value = !showEma },
                                onToggleBbands = { viewModel.showBbands.value = !showBbands },
                                onToggleFib = { viewModel.showFib.value = !showFib },
                                onToggleFvg = { viewModel.showFvg.value = !showFvg },
                                onToggleOrderBlocks = { viewModel.showOrderBlocks.value = !showOrderBlocks },
                                onToggleSweeps = { viewModel.showSweeps.value = !showSweeps },
                                onToggleBreakers = { viewModel.showBreakers.value = !showBreakers }
                            )
                        }

                        // 4. ICT Detected Zones Summaries
                        item {
                            IctZonesLogCard(
                                state = state,
                                showFvg = showFvg,
                                showOrderBlocks = showOrderBlocks,
                                showSweeps = showSweeps,
                                showBreakers = showBreakers
                            )
                        }

                        // 5. Company Fundamentals Profile
                        item {
                            CompanyProfileCard(profile = companyProfile)
                        }

                        // 6. News & Labeled Sentiments
                        item {
                            Text(
                                "Market News & Sentiment Analysis",
                                color = PrimaryGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        if (newsArticles.isEmpty()) {
                            item {
                                Text("Berita sentiment belum terisi. Sebagian besar API Key NewsAPI dibatasi per detik.", color = TextGray)
                            }
                        } else {
                            items(newsArticles.take(8)) { (article, sentiment) ->
                                NewsSentimentCard(article = article, sentiment = sentiment)
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }

                else -> {}
            }
        }
    }
}

@Composable
fun TimeframeSelectorBar(
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        listOf("1m", "5m", "15m", "1H", "4H", "Daily", "Weekly").forEach { item ->
            val isSelected = item == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (isSelected) PrimaryGold else Color.Transparent,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelect(item) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item,
                    color = if (isSelected) Color.Black else TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IndicatorTogglesArea(
    showEma: Boolean,
    showBbands: Boolean,
    showFib: Boolean,
    showFvg: Boolean,
    showOrderBlocks: Boolean,
    showSweeps: Boolean,
    showBreakers: Boolean,
    onToggleEma: () -> Unit,
    onToggleBbands: () -> Unit,
    onToggleFib: () -> Unit,
    onToggleFvg: () -> Unit,
    onToggleOrderBlocks: () -> Unit,
    onToggleSweeps: () -> Unit,
    onToggleBreakers: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier.fillMaxWidth().border(1.dp, BorderDark, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Settings Indikator & ICT Overlay", color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = showEma,
                    onClick = onToggleEma,
                    label = { Text("EMA 20") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryGold, selectedLabelColor = Color.Black)
                )
                FilterChip(
                    selected = showBbands,
                    onClick = onToggleBbands,
                    label = { Text("Bollinger Bands") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryGold, selectedLabelColor = Color.Black)
                )
                FilterChip(
                    selected = showFib,
                    onClick = onToggleFib,
                    label = { Text("Fibonacci Levels") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryGold, selectedLabelColor = Color.Black)
                )
                FilterChip(
                    selected = showFvg,
                    onClick = onToggleFvg,
                    label = { Text("Fair Value Gap (FVG)") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryGold, selectedLabelColor = Color.Black)
                )
                FilterChip(
                    selected = showOrderBlocks,
                    onClick = onToggleOrderBlocks,
                    label = { Text("Order Blocks") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryGold, selectedLabelColor = Color.Black)
                )
                FilterChip(
                    selected = showSweeps,
                    onClick = onToggleSweeps,
                    label = { Text("Liquidity Sweeps") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryGold, selectedLabelColor = Color.Black)
                )
                FilterChip(
                    selected = showBreakers,
                    onClick = onToggleBreakers,
                    label = { Text("Breaker Blocks") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryGold, selectedLabelColor = Color.Black)
                )
            }
        }
    }
}

@Composable
fun IctZonesLogCard(
    state: ChartUiState.Success,
    showFvg: Boolean,
    showOrderBlocks: Boolean,
    showSweeps: Boolean,
    showBreakers: Boolean
) {
    if (!showFvg && !showOrderBlocks && !showSweeps && !showBreakers) return

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier.fillMaxWidth().border(1.dp, BorderDark, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("ICT Concepts Log Deteksi", color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (showFvg && state.fvgs.isNotEmpty()) {
                    state.fvgs.take(2).forEach { fvg ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, null, tint = if (fvg.isBullish) BullGreen else BearRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = String.format("%s FVG terdeteksi [Gap: $%.2f - $%.2f]", if (fvg.isBullish) "Bullish" else "Bearish", fvg.gapLow, fvg.gapHigh),
                                color = TextWhite,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (showOrderBlocks && state.obs.isNotEmpty()) {
                    state.obs.take(2).forEach { ob ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(if (ob.isBullish) BullGreen else BearRed, RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = String.format("%s Order Block (Level: $%.2f)", if (ob.isBullish) "Bullish" else "Bearish", ob.levelPrice),
                                color = TextWhite,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (showSweeps && state.sweeps.isNotEmpty()) {
                    state.sweeps.take(2).forEach { sweep ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = PrimaryGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = String.format("%s Sweep terdeteksi pada $%.2f", if (sweep.isHighSweep) "High Wick" else "Low Wick", sweep.levelPrice),
                                color = TextWhite,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (showBreakers && state.breakers.isNotEmpty()) {
                    state.breakers.take(2).forEach { breaker ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Clear, null, tint = if (breaker.isBullish) BullGreen else BearRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = String.format("%s Breaker Block (Rentang: $%.2f - $%.2f)", if (breaker.isBullish) "Bullish" else "Bearish", breaker.low, breaker.high),
                                color = TextWhite,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if ((!showFvg || state.fvgs.isEmpty()) && (!showOrderBlocks || state.obs.isEmpty()) && (!showSweeps || state.sweeps.isEmpty()) && (!showBreakers || state.breakers.isEmpty())) {
                    Text("Belum ada pola ICT terdeteksi pada rentang candle saat ini.", color = TextGray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun CompanyProfileCard(profile: FinnhubProfileResponse?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier.fillMaxWidth().border(1.dp, BorderDark, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Analisis Fundamental Sederhana", color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))

            if (profile == null) {
                Text("Profil fundamental tidak dapat diload. Sebagian besar API Key Finnhub sandbox membatasi data profil fundamental saham nyata.", color = TextGray, fontSize = 12.sp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!profile.logo.isNullOrEmpty()) {
                        AsyncImage(
                            model = profile.logo,
                            contentDescription = "Logo",
                            modifier = Modifier.size(40.dp).background(Color.White, RoundedCornerShape(6.dp)).padding(2.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Column {
                        Text(profile.name ?: "Unknown", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Industri: ${profile.finnhubIndustry ?: "N/A"}", color = TextGray, fontSize = 12.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = BorderDark)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Kapitalisasi Pasar:", color = TextGray, fontSize = 12.sp)
                    Text(
                        text = if (profile.marketCapitalization != null) String.format("$%.2f Million", profile.marketCapitalization) else "N/A",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Negara Asal:", color = TextGray, fontSize = 12.sp)
                    Text(profile.country ?: "N/A", color = TextWhite, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun NewsSentimentCard(article: NewsArticle, sentiment: String) {
    val chipColor = when (sentiment) {
        "BULLISH" -> BullGreen
        "BEARISH" -> BearRed
        else -> TextGray
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier.fillMaxWidth().border(1.dp, BorderDark, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = article.source?.name ?: "News Target",
                    color = PrimaryGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = sentiment,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    modifier = Modifier
                        .background(chipColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = article.title ?: "No Title",
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            
            if (!article.description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = article.description,
                    color = TextGray,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    maxLines = 2
                )
            }
        }
    }
}
