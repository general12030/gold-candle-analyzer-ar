package com.goldcandle.analyzer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

data class Candle(
    val time: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double
)

@Entity(tableName = "signals")
data class SignalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val side: String,
    val entry: Double,
    val stop: Double,
    val target: Double,
    val model: String,
    val confidence: Int,
    val reason: String,
    val createdAt: Long = System.currentTimeMillis()
)
