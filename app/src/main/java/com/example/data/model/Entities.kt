package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistItem(
    @PrimaryKey val symbol: String,
    val name: String,
    val assetType: String, // "STOCK", "FOREX", "CRYPTO"
    val lastPrice: Double = 0.0,
    val changePercent: Double = 0.0,
    val sparklineData: String = "", // Comma-separated history values, e.g. "1.24,1.25,1.23,1.26"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "price_alerts")
data class PriceAlert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val targetPrice: Double,
    val condition: String, // "ABOVE" or "BELOW"
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_history")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String, // "user" or "assistant"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "api_keys")
data class ApiKeyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val serviceType: String, // "OPENROUTER", "FINNHUB", "TWELVEDATA", "NEWSAPI"
    val apiKey: String, // The actual API Key
    val timestamp: Long = System.currentTimeMillis()
)
