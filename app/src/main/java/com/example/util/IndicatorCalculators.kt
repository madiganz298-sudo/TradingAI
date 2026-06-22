package com.example.util

import kotlin.math.abs
import kotlin.math.sqrt

// Simple internal representation of a Candle for calculations
data class CalculationCandle(
    val high: Double,
    val low: Double,
    val open: Double,
    val close: Double,
    val volume: Double,
    val datetime: String
)

// Output data structures
data class EmaResult(val values: List<Double?>)
data class RsiResult(val values: List<Double?>)
data class MacdResult(val macdLine: List<Double?>, val signalLine: List<Double?>, val histogram: List<Double?>)
data class BollingerBandsResult(val upper: List<Double?>, val middle: List<Double?>, val lower: List<Double?>)
data class VolumeProfileBin(val priceLevel: Double, val volume: Double)

data class FvgZone(
    val index: Int,
    val isBullish: Boolean,
    val gapHigh: Double,
    val gapLow: Double,
    val isMitigated: Boolean = false
)

data class OrderBlock(
    val index: Int,
    val isBullish: Boolean,
    val high: Double,
    val low: Double,
    val levelPrice: Double
)

data class LiquiditySweep(
    val index: Int,
    val isHighSweep: Boolean, // Swep high or swep low
    val levelPrice: Double,
    val datetime: String
)

data class BreakerBlock(
    val index: Int,
    val isBullish: Boolean, // Bullish breaker (former bearish OB) or Bearish breaker
    val high: Double,
    val low: Double
)

data class FibLevel(val percentage: Double, val price: Double)

object IndicatorsCalculator {

    // Converts Raw Twelve Candles to Calculation Candles safely
    fun parseRawCandles(raw: List<com.example.data.api.TwelveCandle>?): List<CalculationCandle> {
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.mapNotNull {
            val high = it.high.toDoubleOrNull() ?: return@mapNotNull null
            val low = it.low.toDoubleOrNull() ?: return@mapNotNull null
            val open = it.open.toDoubleOrNull() ?: return@mapNotNull null
            val close = it.close.toDoubleOrNull() ?: return@mapNotNull null
            val volume = it.volume.toDoubleOrNull() ?: 0.0
            CalculationCandle(
                high = high,
                low = low,
                open = open,
                close = close,
                volume = volume,
                datetime = it.datetime
            )
        }.reversed() // Reverse so the oldest is first, newest is last (essential for moving averages)
    }

    // Calculasi EMA
    fun calculateEMA(candles: List<CalculationCandle>, period: Int): EmaResult {
        val result = mutableListOf<Double?>()
        if (candles.size < period) {
            return EmaResult(List(candles.size) { null })
        }
        val multiplier = 2.0 / (period + 1.0)
        
        // Simulating EMA calculation
        var previousEma: Double? = null
        for (i in candles.indices) {
            if (i < period - 1) {
                result.add(null)
            } else if (i == period - 1) {
                // Initial EMA (SMA of close prices)
                var sum = 0.0
                for (j in 0 until period) {
                    sum += candles[j].close
                }
                val sma = sum / period
                previousEma = sma
                result.add(sma)
            } else {
                val currentEma = (candles[i].close - previousEma!!) * multiplier + previousEma
                previousEma = currentEma
                result.add(currentEma)
            }
        }
        return EmaResult(result)
    }

    // Calculasi RSI
    fun calculateRSI(candles: List<CalculationCandle>, period: Int = 14): RsiResult {
        val result = mutableListOf<Double?>()
        if (candles.size <= period) {
            return RsiResult(List(candles.size) { null })
        }

        var avgGain = 0.0
        var avgLoss = 0.0

        // Calculate initial SMA of gain/losses
        for (i in 1..period) {
            val change = candles[i].close - candles[i - 1].close
            if (change > 0) {
                avgGain += change
            } else {
                avgLoss += abs(change)
            }
        }
        avgGain /= period
        avgLoss /= period

        for (i in candles.indices) {
            if (i < period) {
                result.add(null)
            } else if (i == period) {
                val rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
                result.add(100.0 - (100.0 / (1.0 + rs)))
            } else {
                val change = candles[i].close - candles[i - 1].close
                val gain = if (change > 0) change else 0.0
                val loss = if (change < 0) abs(change) else 0.0

                avgGain = (avgGain * (period - 1) + gain) / period
                avgLoss = (avgLoss * (period - 1) + loss) / period

                val rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
                result.add(100.0 - (100.0 / (1.0 + rs)))
            }
        }
        return RsiResult(result)
    }

    // Calculasi MACD
    fun calculateMACD(
        candles: List<CalculationCandle>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): MacdResult {
        val macdList = mutableListOf<Double?>()
        val signalList = mutableListOf<Double?>()
        val histogramList = mutableListOf<Double?>()

        val fastEma = calculateEMA(candles, fastPeriod).values
        val slowEma = calculateEMA(candles, slowPeriod).values

        val tempMacd = mutableListOf<Double>()
        for (i in candles.indices) {
            val f = fastEma[i]
            val s = slowEma[i]
            if (f != null && s != null) {
                val macd = f - s
                macdList.add(macd)
                tempMacd.add(macd)
            } else {
                macdList.add(null)
            }
        }

        // Calculate Signal (EMA of MACD)
        val signalEma = calculateEMAOfList(tempMacd, signalPeriod)
        var signalIndex = 0
        for (i in candles.indices) {
            if (macdList[i] == null) {
                signalList.add(null)
                histogramList.add(null)
            } else {
                val sig = signalEma.getOrNull(signalIndex++)
                signalList.add(sig)
                if (sig != null) {
                    histogramList.add(macdList[i]!! - sig)
                } else {
                    histogramList.add(null)
                }
            }
        }

        return MacdResult(macdList, signalList, histogramList)
    }

    private fun calculateEMAOfList(data: List<Double>, period: Int): List<Double?> {
        val result = mutableListOf<Double?>()
        if (data.size < period) {
            return List(data.size) { null }
        }
        val multiplier = 2.0 / (period + 1.0)
        var prev: Double? = null
        for (i in data.indices) {
            if (i < period - 1) {
                result.add(null)
            } else if (i == period - 1) {
                val avg = data.subList(0, period).average()
                prev = avg
                result.add(avg)
            } else {
                val curr = (data[i] - prev!!) * multiplier + prev
                prev = curr
                result.add(curr)
            }
        }
        return result
    }

    // Calculasi Bollinger Bands
    fun calculateBollingerBands(
        candles: List<CalculationCandle>,
        period: Int = 20,
        stdDevMultiplier: Double = 2.0
    ): BollingerBandsResult {
        val upper = mutableListOf<Double?>()
        val middle = mutableListOf<Double?>()
        val lower = mutableListOf<Double?>()

        if (candles.size < period) {
            return BollingerBandsResult(
                List(candles.size) { null },
                List(candles.size) { null },
                List(candles.size) { null }
            )
        }

        for (i in candles.indices) {
            if (i < period - 1) {
                upper.add(null)
                middle.add(null)
                lower.add(null)
            } else {
                val window = candles.subList(i - period + 1, i + 1)
                val closes = window.map { it.close }
                val mean = closes.average()
                
                // Variansi & Std Dev
                val sumSqDiff = closes.sumOf { (it - mean) * (it - mean) }
                val stdDev = sqrt(sumSqDiff / period)

                middle.add(mean)
                upper.add(mean + stdDevMultiplier * stdDev)
                lower.add(mean - stdDevMultiplier * stdDev)
            }
        }

        return BollingerBandsResult(upper, middle, lower)
    }

    // Volume Profile (Horizontal volume distribution)
    fun calculateVolumeProfile(candles: List<CalculationCandle>, binsCount: Int = 10): List<VolumeProfileBin> {
        if (candles.isEmpty()) return emptyList()
        val minPrice = candles.minOf { it.low }
        val maxPrice = candles.maxOf { it.high }
        val priceDiff = maxPrice - minPrice
        if (priceDiff <= 0) return emptyList()

        val binSize = priceDiff / binsCount
        val bins = Array(binsCount) { 0.0 }

        for (candle in candles) {
            val avgPrice = (candle.high + candle.low) / 2.0
            val binIndex = ((avgPrice - minPrice) / binSize).toInt().coerceIn(0, binsCount - 1)
            bins[binIndex] += candle.volume
        }

        return bins.mapIndexed { index, volume ->
            VolumeProfileBin(
                priceLevel = minPrice + (index * binSize) + (binSize / 2),
                volume = volume
            )
        }
    }

    // Fibonacci Layers based on Swing Range
    fun calculateFibonacci(candles: List<CalculationCandle>): List<FibLevel> {
        if (candles.isEmpty()) return emptyList()
        val maxPointObj = candles.maxByOrNull { it.high }
        val minPointObj = candles.minByOrNull { it.low }

        if (maxPointObj == null || minPointObj == null) return emptyList()
        val high = maxPointObj.high
        val low = minPointObj.low
        val diff = high - low

        // Assuming uptrend if low occurs before high
        val isUptrend = candles.indexOf(minPointObj) < candles.indexOf(maxPointObj)

        val levels = listOf(0.0, 0.236, 0.382, 0.5, 0.618, 0.786, 1.0, 1.618)

        return levels.map { level ->
            val price = if (isUptrend) {
                high - (diff * level)
            } else {
                low + (diff * level)
            }
            FibLevel(percentage = level * 100, price = price)
        }
    }

    // =============================================================
    // ICT (INNER CIRCLE TRADER) CONCEPTS DETECTION ENGINE
    // =============================================================

    // 1. Fair Value Gap (FVG)
    // Bullish: Low[i] > High[i-2] with a large bullish impulsive candle in-between
    // Bearish: High[i] < Low[i-2] with a large bearish impulsive candle in-between
    fun detectFVG(candles: List<CalculationCandle>): List<FvgZone> {
        val list = mutableListOf<FvgZone>()
        if (candles.size < 3) return list

        for (i in 2 until candles.size) {
            val candle1 = candles[i - 2]
            val candle2 = candles[i - 1] // Impulse candle
            val candle3 = candles[i]

            // Check Bullish FVG
            if (candle2.close > candle2.open && (candle2.high - candle2.low) > (candles.map { it.high - it.low }.average() * 1.5)) {
                if (candle3.low > candle1.high) {
                    list.add(
                        FvgZone(
                            index = i - 1,
                            isBullish = true,
                            gapHigh = candle3.low,
                            gapLow = candle1.high
                        )
                    )
                }
            }

            // Check Bearish FVG
            if (candle2.close < candle2.open && (candle2.high - candle2.low) > (candles.map { it.high - it.low }.average() * 1.5)) {
                if (candle3.high < candle1.low) {
                    list.add(
                        FvgZone(
                            index = i - 1,
                            isBullish = false,
                            gapHigh = candle1.low,
                            gapLow = candle3.high
                        )
                    )
                }
            }
        }
        return list
    }

    // 2. Order Blocks (OB)
    // Bullish OB: Last down-candle prior to a displacement up-move that breaks a structural high.
    // Bearish OB: Last up-candle prior to a displacement down-move that breaks a structural low.
    fun detectOrderBlocks(candles: List<CalculationCandle>): List<OrderBlock> {
        val list = mutableListOf<OrderBlock>()
        if (candles.size < 5) return list

        val avgSize = candles.map { abs(it.close - it.open) }.average()

        for (i in 3 until candles.size - 1) {
            val prevCandle = candles[i - 1]
            val triggerCandle1 = candles[i]
            val triggerCandle2 = candles[i + 1]

            // Search for strong upward impulse
            val isStrongUpMove = triggerCandle1.close > triggerCandle1.open &&
                    triggerCandle2.close > triggerCandle2.open &&
                    (triggerCandle1.close - triggerCandle1.open + triggerCandle2.close - triggerCandle2.open) > (avgSize * 2.5)

            if (isStrongUpMove && prevCandle.close < prevCandle.open) {
                list.add(
                    OrderBlock(
                        index = i - 1,
                        isBullish = true,
                        high = prevCandle.high,
                        low = prevCandle.low,
                        levelPrice = prevCandle.close
                    )
                )
            }

            // Search for strong downward impulse
            val isStrongDownMove = triggerCandle1.close < triggerCandle1.open &&
                    triggerCandle2.close < triggerCandle2.open &&
                    (triggerCandle1.open - triggerCandle1.close + triggerCandle2.open - triggerCandle2.close) > (avgSize * 2.5)

            if (isStrongDownMove && prevCandle.close > prevCandle.open) {
                list.add(
                    OrderBlock(
                        index = i - 1,
                        isBullish = false,
                        high = prevCandle.high,
                        low = prevCandle.low,
                        levelPrice = prevCandle.close
                    )
                )
            }
        }
        // Keep unique zones
        return list.takeLast(6)
    }

    // 3. Liquidity Sweeps
    // Price sweeps previous high/low (by wick) then closes back inside.
    fun detectLiquiditySweeps(candles: List<CalculationCandle>): List<LiquiditySweep> {
        val list = mutableListOf<LiquiditySweep>()
        if (candles.size < 15) return list

        for (i in 10 until candles.size) {
            val current = candles[i]
            val lookback = candles.subList(i - 10, i)
            val highestHigh = lookback.maxOf { it.high }
            val lowestLow = lookback.minOf { it.low }

            // High sweep: wick goes above local high but candle closes below it
            if (current.high > highestHigh && current.close < highestHigh) {
                list.add(
                    LiquiditySweep(
                        index = i,
                        isHighSweep = true,
                        levelPrice = highestHigh,
                        datetime = current.datetime
                    )
                )
            }

            // Low sweep: wick goes below local low but candle closes above it
            if (current.low < lowestLow && current.close > lowestLow) {
                list.add(
                    LiquiditySweep(
                        index = i,
                        isHighSweep = false,
                        levelPrice = lowestLow,
                        datetime = current.datetime
                    )
                )
            }
        }
        return list.takeLast(5)
    }

    // 4. Breaker Blocks (BB)
    // A breaker block is former order block that was broken through cleanly.
    fun detectBreakerBlocks(candles: List<CalculationCandle>, obs: List<OrderBlock>): List<BreakerBlock> {
        val breakers = mutableListOf<BreakerBlock>()
        if (candles.isEmpty() || obs.isEmpty()) return breakers

        for (ob in obs) {
            // Find candles after the Order Block creation
            val postObCandles = candles.subList(ob.index + 1, candles.size)
            for (candle in postObCandles) {
                if (ob.isBullish) {
                    // Former Bull OB broken to the downside (Bearish Breaker)
                    if (candle.close < ob.low) {
                        breakers.add(
                            BreakerBlock(
                                index = ob.index,
                                isBullish = false,
                                high = ob.high,
                                low = ob.low
                            )
                        )
                        break
                    }
                } else {
                    // Former Bear OB broken to the upside (Bullish Breaker)
                    if (candle.close > ob.high) {
                        breakers.add(
                            BreakerBlock(
                                index = ob.index,
                                isBullish = true,
                                high = ob.high,
                                low = ob.low
                            )
                        )
                        break
                    }
                }
            }
        }
        return breakers.distinctBy { it.index }.takeLast(4)
    }
}
