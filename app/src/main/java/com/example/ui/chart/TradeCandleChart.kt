package com.example.ui.chart

import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BearRed
import com.example.ui.theme.BullGreen
import com.example.ui.theme.PrimaryGold
import com.example.ui.theme.TextGray
import com.example.util.BollingerBandsResult
import com.example.util.CalculationCandle
import com.example.util.EmaResult
import com.example.util.FibLevel
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

@Composable
fun TradeCandleChart(
    candles: List<CalculationCandle>,
    emaResult: EmaResult?,
    bbands: BollingerBandsResult?,
    fibLevels: List<FibLevel>,
    showEma: Boolean,
    showBbands: Boolean,
    showFib: Boolean,
    modifier: Modifier = Modifier
) {
    if (candles.isEmpty()) {
        Box(modifier = modifier.background(BackgroundDark))
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(350.dp)
            .background(BackgroundDark)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                CombinedChart(context).apply {
                    description.isEnabled = false
                    setBackgroundColor(Color.parseColor("#121212"))
                    setDrawGridBackground(false)
                    setDrawBarShadow(false)
                    isHighlightPerDragEnabled = true
                    isDragEnabled = true
                    isScaleXEnabled = true
                    isScaleYEnabled = true
                    setPinchZoom(true)

                    // Axes setup
                    val l = legend
                    l.isEnabled = false

                    val xAxis = xAxis
                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                    xAxis.textColor = Color.WHITE
                    xAxis.setDrawGridLines(true)
                    xAxis.gridColor = Color.parseColor("#2C2C2C")
                    xAxis.setAvoidFirstLastClipping(true)

                    val leftAxis = axisLeft
                    leftAxis.textColor = Color.WHITE
                    leftAxis.setDrawGridLines(true)
                    leftAxis.gridColor = Color.parseColor("#2C2C2C")
                    leftAxis.setLabelCount(6, false)

                    val rightAxis = axisRight
                    rightAxis.isEnabled = false
                }
            },
            update = { chart ->
                val combinedData = CombinedData()

                // 1. Candlestick Data Mapping (MPAndroidChart)
                val candleEntries = candles.mapIndexed { index, candle ->
                    CandleEntry(
                        index.toFloat(),
                        candle.high.toFloat(),
                        candle.low.toFloat(),
                        candle.open.toFloat(),
                        candle.close.toFloat()
                    )
                }

                val candleDataSet = CandleDataSet(candleEntries, "Market Candles").apply {
                    decreasingColor = Color.parseColor("#EF5350") // BearRed
                    increasingColor = Color.parseColor("#26A69A") // BullGreen
                    shadowColor = Color.parseColor("#B0B0B0")
                    shadowWidth = 1f
                    neutralColor = Color.YELLOW
                    isHighlightEnabled = true
                    setDrawValues(false)
                }
                
                val candleData = CandleData(candleDataSet)
                combinedData.setData(candleData)

                // 2. Linear Overlays (Lines)
                val lineDataSets = mutableListOf<LineDataSet>()

                // EMA 20 line overlap
                if (showEma && emaResult != null) {
                    val emaEntries = mutableListOf<Entry>()
                    for (i in candles.indices) {
                        val valEma = emaResult.values.getOrNull(i)
                        if (valEma != null) {
                            emaEntries.add(Entry(i.toFloat(), valEma.toFloat()))
                        }
                    }
                    if (emaEntries.isNotEmpty()) {
                        val emaDataSet = LineDataSet(emaEntries, "EMA 20").apply {
                            color = Color.parseColor("#D4AF37") // Gold
                            lineWidth = 2f
                            setDrawCircles(false)
                            setDrawValues(false)
                        }
                        lineDataSets.add(emaDataSet)
                    }
                }

                // Bollinger Bands overlap
                if (showBbands && bbands != null) {
                    val upperEntries = mutableListOf<Entry>()
                    val lowerEntries = mutableListOf<Entry>()
                    val midEntries = mutableListOf<Entry>()

                    for (i in candles.indices) {
                        val u = bbands.upper.getOrNull(i)
                        val l = bbands.lower.getOrNull(i)
                        val m = bbands.middle.getOrNull(i)
                        if (u != null) upperEntries.add(Entry(i.toFloat(), u.toFloat()))
                        if (l != null) lowerEntries.add(Entry(i.toFloat(), l.toFloat()))
                        if (m != null) midEntries.add(Entry(i.toFloat(), m.toFloat()))
                    }

                    if (upperEntries.isNotEmpty()) {
                        lineDataSets.add(LineDataSet(upperEntries, "BB Upper").apply {
                            color = Color.parseColor("#448AFF") // Blue
                            lineWidth = 1f
                            setDrawCircles(false)
                            setDrawValues(false)
                        })
                    }
                    if (midEntries.isNotEmpty()) {
                        lineDataSets.add(LineDataSet(midEntries, "BB Middle").apply {
                            color = Color.GRAY
                            lineWidth = 1f
                            enableDashedLine(10f, 5f, 0f)
                            setDrawCircles(false)
                            setDrawValues(false)
                        })
                    }
                    if (lowerEntries.isNotEmpty()) {
                        lineDataSets.add(LineDataSet(lowerEntries, "BB Lower").apply {
                            color = Color.parseColor("#448AFF") // Blue
                            lineWidth = 1f
                            setDrawCircles(false)
                            setDrawValues(false)
                        })
                    }
                }

                // Draw horizontal Fibonacci levels across the chart
                if (showFib && fibLevels.isNotEmpty() && candles.isNotEmpty()) {
                    val length = candles.size.toFloat()
                    for (level in fibLevels) {
                        val fibEntries = mutableListOf(
                            Entry(0f, level.price.toFloat()),
                            Entry(length - 1, level.price.toFloat())
                        )
                        val fibDataSet = LineDataSet(fibEntries, "Fib ${level.percentage}%").apply {
                            color = Color.parseColor("#8E24AA") // Purple
                            lineWidth = 1.2f
                            setDrawCircles(false)
                            setDrawValues(false)
                        }
                        lineDataSets.add(fibDataSet)
                    }
                }

                if (lineDataSets.isNotEmpty()) {
                    val lineData = LineData(lineDataSets.toList())
                    combinedData.setData(lineData)
                }

                // Apply combined dataset to layout
                chart.data = combinedData
                chart.invalidate()
            }
        )
    }
}
