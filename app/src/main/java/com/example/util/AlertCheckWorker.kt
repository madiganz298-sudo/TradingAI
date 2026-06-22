package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.R

class AlertCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("AlertCheckWorker", "Execution triggered for background Price Alert checking")
        try {
            // Lazy initialization check
            if (!this::class.java.classLoader?.toString().isNullOrEmpty()) {
                // Ensure repository and managers are loaded
                // Fallback check if locator isn't initialized yet
                try {
                    ServiceLocator.tradeRepository
                } catch (e: Exception) {
                    ServiceLocator.initialize(applicationContext)
                }
            }

            val repository = ServiceLocator.tradeRepository
            val triggeredLogs = repository.checkAndTriggerAlerts()
            
            for (message in triggeredLogs) {
                showNotification(message)
            }
            
            return Result.success()
        } catch (e: Exception) {
            Log.e("AlertCheckWorker", "Error in price checks cycle: ${e.message}")
            return Result.retry()
        }
    }

    private fun showNotification(message: String) {
        val channelId = "TRADE_AI_PRO_ALERTS_CHANNEL"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "TradeAI Pro Price Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi untuk alert target harga saham, forex, dan crypto"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Using standard logo as small icon, or android default
        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("TradeAI Pro - Market Alert")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
