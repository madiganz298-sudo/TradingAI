package com.example

import android.app.Application
import com.example.util.ServiceLocator

class TradeAIProApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize our Service Locator (manual Dependency Injection container)
        ServiceLocator.initialize(this)
    }
}
