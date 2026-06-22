package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WatchlistItem
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeView(
    viewModel: WatchlistViewModel,
    onSymbolSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val watchlist by viewModel.watchlist.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var showSearchDialog by remember { mutableStateOf(false) }
    var showAlertDialog by remember { mutableStateOf(false) }
    var showCompareDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "TradeAI Pro Watchlist",
                        color = PrimaryGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            val msg = viewModel.exportWatchlistToCSV(context)
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.testTag("export_button")
                    ) {
                        Icon(Icons.Default.Share, "Export Watchlist (CSV)", tint = PrimaryGold)
                    }
                    IconButton(onClick = { viewModel.refreshAll() }) {
                        Icon(Icons.Default.Refresh, "Refresh Prices", tint = PrimaryGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSearchDialog = true },
                containerColor = PrimaryGold,
                contentColor = Color.Black,
                modifier = Modifier.testTag("add_to_watchlist_button")
            ) {
                Icon(Icons.Default.Add, "Tambah Simbol")
            }
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = PrimaryGold,
                    trackColor = BorderDark
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Secondary control button rail
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { showAlertDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceDark,
                            contentColor = PrimaryGold
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp)
                            .border(1.dp, PrimaryGold, RoundedCornerShape(8.dp))
                            .testTag("price_alert_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Atur Alert", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { showCompareDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceDark,
                            contentColor = PrimaryGold
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 6.dp)
                            .border(1.dp, PrimaryGold, RoundedCornerShape(8.dp)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bandingkan", fontSize = 12.sp)
                    }
                }

                if (watchlist.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Watchlist masih kosong.\nKetuk tombol + di bawah untuk menambahkan.",
                            color = TextGray,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(watchlist) { item ->
                            WatchlistItemCard(
                                item = item,
                                onClick = { onSymbolSelected(item.symbol) },
                                onDelete = { viewModel.removeFromWatchlist(item.symbol) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Search dialog
    if (showSearchDialog) {
        SearchSymbolDialog(
            onDismiss = { showSearchDialog = false },
            onConfirm = { symbol, name, type ->
                viewModel.addToWatchlist(symbol, name, type)
                showSearchDialog = false
            }
        )
    }

    // Modal Alert configuration dialog
    if (showAlertDialog) {
        AddAlertDialog(
            watchlist = watchlist,
            onDismiss = { showAlertDialog = false },
            onConfirm = { symbol, price, cond ->
                viewModel.addPriceAlert(symbol, price, cond)
                showAlertDialog = false
            }
        )
    }

    // Modal Compare dialog
    if (showCompareDialog) {
        CompareDialog(
            watchlist = watchlist,
            onDismiss = { showCompareDialog = false }
        )
    }
}

@Composable
fun WatchlistItemCard(
    item: WatchlistItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isPositive = item.changePercent >= 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.symbol,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.assetType,
                        color = PrimaryGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(DarkGold.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = item.name,
                    color = TextGray,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            // Real-time custom Canvas sparkline
            if (item.sparklineData.isNotEmpty()) {
                MiniSparkline(
                    dataStr = item.sparklineData,
                    isPositive = isPositive,
                    modifier = Modifier
                        .size(60.dp, 30.dp)
                        .padding(horizontal = 6.dp)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format("$%.2f", item.lastPrice),
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    fontSize = 16.sp
                )
                Text(
                    text = String.format("%s%.2f%%", if (isPositive) "+" else "", item.changePercent),
                    color = if (isPositive) BullGreen else BearRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = BearRed.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun MiniSparkline(dataStr: String, isPositive: Boolean, modifier: Modifier = Modifier) {
    val prices = dataStr.split(",").mapNotNull { it.toDoubleOrNull() }
    if (prices.size < 2) return

    val min = prices.minOrNull() ?: 0.0
    val max = prices.maxOrNull() ?: 1.0
    val range = (max - min).coerceAtLeast(0.001)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val points = prices.mapIndexed { index, price ->
            val x = index.toFloat() / (prices.size - 1) * width
            val y = height - ((price - min) / range * height).toFloat()
            Offset(x, y)
        }

        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }

        drawPath(
            path = path,
            color = if (isPositive) BullGreen else BearRed,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

// Dialog views: Search, Alerts, Compare
@Composable
fun SearchSymbolDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var symbol by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var assetType by remember { mutableStateOf("STOCK") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Simbol Global", color = PrimaryGold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text("Ticker Symbol (misal: AAPL, BTCUSD)") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark
                    )
                )
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Perusahaan (misal: Apple Inc.)") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark
                    )
                )
                
                Text("Kategori Asset:", color = TextWhite, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("STOCK", "FOREX", "CRYPTO").forEach { type ->
                        val isSel = assetType == type
                        FilterChip(
                            selected = isSel,
                            onClick = { assetType = type },
                            label = { Text(type) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryGold,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (symbol.isNotEmpty()) onConfirm(symbol, if (name.isEmpty()) symbol else name, assetType) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
            ) {
                Text("Tambah", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextGray)
            }
        },
        containerColor = SurfaceDark
    )
}

@Composable
fun AddAlertDialog(
    watchlist: List<WatchlistItem>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String) -> Unit
) {
    if (watchlist.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Atur Price Alert", color = PrimaryGold) },
            text = { Text("Watchlist kosong. Tambahkan saham terlebih dahulu untuk memasang alert.", color = TextWhite) },
            confirmButton = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)) { Text("OK", color = Color.Black) } },
            containerColor = SurfaceDark
        )
        return
    }

    var selectedSymbol by remember { mutableStateOf(watchlist.first().symbol) }
    var targetPrice by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("ABOVE") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atur Market Price Alert", color = PrimaryGold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Pilih Simbol:", color = TextWhite)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    watchlist.take(4).forEach { item ->
                        FilterChip(
                            selected = selectedSymbol == item.symbol,
                            onClick = { selectedSymbol = item.symbol },
                            label = { Text(item.symbol) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryGold, selectedLabelColor = Color.Black)
                        )
                    }
                }

                TextField(
                    value = targetPrice,
                    onValueChange = { targetPrice = it },
                    label = { Text("Target Harga (USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(focusedTextColor = TextWhite, focusedContainerColor = SurfaceDark, unfocusedContainerColor = SurfaceDark)
                )

                Text("Kondisi Pemicu:", color = TextWhite)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("ABOVE", "BELOW").forEach { cond ->
                        FilterChip(
                            selected = condition == cond,
                            onClick = { condition = cond },
                            label = { Text(if (cond == "ABOVE") "Melebihi (>=)" else "Kurang dari (<=)") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryGold, selectedLabelColor = Color.Black)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = targetPrice.toDoubleOrNull()
                    if (price != null) {
                        onConfirm(selectedSymbol, price, condition)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
            ) {
                Text("Pasang Alert", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = TextGray) }
        },
        containerColor = SurfaceDark
    )
}

@Composable
fun CompareDialog(
    watchlist: List<WatchlistItem>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rasio Perbandingan Harga", color = PrimaryGold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (watchlist.size < 2) {
                    Text("Perlu minimal 2 simbol dalam watchlist untuk membandingkan.", color = TextWhite)
                } else {
                    Text("Harga Saat ini:", color = PrimaryGold, fontWeight = FontWeight.Bold)
                    watchlist.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.symbol, color = TextWhite)
                            Text(String.format("$%.2f (%+.2f%%)", item.lastPrice, item.changePercent), color = if (item.changePercent >= 0) BullGreen else BearRed)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)) {
                Text("Selesai", color = Color.Black)
            }
        },
        containerColor = SurfaceDark
    )
}
