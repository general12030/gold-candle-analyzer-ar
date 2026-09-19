package com.goldcandle.analyzer.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

class MarketRepository(
    private val database: AppDatabase,
    private val random: Random = Random(System.currentTimeMillis())
) {
    fun observeSignals(): Flow<List<SignalEntity>> = database.signalDao().observeAll()

    suspend fun storeSignals(signals: List<SignalEntity>) = withContext(Dispatchers.IO) {
        if (signals.isNotEmpty()) {
            database.signalDao().insertAll(signals)
        }
    }

    fun generateDemoCandles(): List<Candle> {
        val base = 4378.44
        val list = mutableListOf<Candle>()
        var value = base
        val now = System.currentTimeMillis()
        for (i in 0 until 30) {
            val open = value
            val drift = ((random.nextDouble() - 0.5) * 2.6)
            val close = open + drift
            val high = max(open, close) + (random.nextDouble() * 1.2)
            val low = minOf(open, close) - (random.nextDouble() * 1.2)
            value = close
            list.add(Candle(now - ((29 - i) * 5L * 60L * 1000L), open, high, low, close))
        }
        return list
    }
}

private fun minOf(a: Double, b: Double): Double = if (a < b) a else b

class SignalDetector {
    fun detect(candles: List<Candle>): List<SignalEntity> {
        if (candles.size < 5) return emptyList()
        val last = candles.last()
        val prev = candles[candles.size - 2]
        val emaFast = ema(candles.map { it.close }, 5)
        val emaSlow = ema(candles.map { it.close }, 14)
        val atr = averageTrueRange(candles)
        val signalReason = when {
            last.close > emaFast && emaFast > emaSlow && last.close > prev.high -> "اختراق صاعد فوق القمة السابقة مع دعم EMA"
            last.close < emaFast && emaFast < emaSlow && last.close < prev.low -> "اختراق هابط تحت القاع السابق مع مقاومة EMA"
            else -> return emptyList()
        }

        val side = if (last.close > prev.close) "BUY" else "SELL"
        val entry = last.close
        val stop = if (side == "BUY") last.low - atr * 0.8 else last.high + atr * 0.8
        val target = if (side == "BUY") entry + atr * 1.8 else entry - atr * 1.8

        return listOf(
            SignalEntity(
                side = side,
                entry = entry,
                stop = stop,
                target = target,
                model = "CandleBreak",
                confidence = 76,
                reason = signalReason,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    private fun ema(values: List<Double>, period: Int): Double {
        if (values.isEmpty()) return 0.0
        val k = 2.0 / (period + 1.0)
        var result = values.first()
        for (value in values.drop(1)) {
            result = (value - result) * k + result
        }
        return result
    }

    private fun averageTrueRange(candles: List<Candle>): Double {
        if (candles.size < 2) return 0.0
        val ranges = candles.map { candle -> candle.high - candle.low }
        return ranges.average().coerceAtLeast(0.5)
    }
}
