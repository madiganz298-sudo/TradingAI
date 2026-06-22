package com.example.util

import android.util.Log
import com.example.data.local.ApiKeyDao
import com.example.data.model.ApiKeyEntity

class KeyManager(private val apiKeyDao: ApiKeyDao) {

    // Cache to keep track of current key index for each service
    // Service name -> Current working index
    private val keyIndexMap = mutableMapOf<String, Int>()

    // For fail-safe, we can define built-in free fallback keys or just require input
    private val defaultKeys = mapOf(
        "OPENROUTER" to "sk-or-v1-fallback-key-placeholder",
        "FINNHUB" to "sandbox_c89b2qiad3iefu0f6fag", // Sandbox or free demo key
        "TWELVEDATA" to "demo", // Twelve Data free demo key
        "NEWSAPI" to "cb25eeae0c6b45f1bba564bdffeffb18" // Sample or demo
    )

    /**
     * Retrieves the current API key to use for the target service.
     * Automatically handles index boundaries and rotates if necessary.
     */
    suspend fun getActiveKey(serviceType: String): String {
        val cleanService = serviceType.uppercase().trim()
        val keysInDb = apiKeyDao.getKeysByService(cleanService)
        
        if (keysInDb.isEmpty()) {
            // Return placeholder or fallback default
            return defaultKeys[cleanService] ?: ""
        }

        val currentIndex = keyIndexMap[cleanService] ?: 0
        val safeIndex = if (currentIndex in keysInDb.indices) currentIndex else 0
        
        // Cache safe index
        keyIndexMap[cleanService] = safeIndex
        return keysInDb[safeIndex].apiKey
    }

    /**
     * Rotates to the next API key in the list.
     * Call this when a request fails with 429 (Rate Limit) or 401 (Unauthorized).
     */
    suspend fun rotateKey(serviceType: String) {
        val cleanService = serviceType.uppercase().trim()
        val keysInDb = apiKeyDao.getKeysByService(cleanService)
        if (keysInDb.size <= 1) {
            Log.d("KeyManager", "No other keys to rotate to for service: $cleanService")
            return
        }

        val currentIndex = keyIndexMap[cleanService] ?: 0
        val nextIndex = (currentIndex + 1) % keysInDb.size
        keyIndexMap[cleanService] = nextIndex
        Log.d("KeyManager", "Rotated key for $cleanService from index $currentIndex to $nextIndex")
    }

    /**
     * Simple key validation helper logic
     */
    fun isPlaceholderKey(key: String): Boolean {
        return key.contains("placeholder") || key.isBlank() || key == "demo"
    }
}
