package com.beniyiyim.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class BenIyiyimApp : Application() {

    companion object {
        const val CHANNEL_TEST_PROMPT = "channel_test_prompt"
        const val CHANNEL_SAFETY_ALERT = "channel_safety_alert"
        const val CHANNEL_SESSION_STATUS = "channel_session_status"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(NotificationManager::class.java)

        val channels = listOf(
            NotificationChannel(
                CHANNEL_TEST_PROMPT,
                "Test Bildirimleri",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Ayıklık testi zamanı geldiğinde gösterilir" },

            NotificationChannel(
                CHANNEL_SAFETY_ALERT,
                "Güvenlik Uyarıları",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Acil durum kişisi bildirimleri ve kritik uyarılar" },

            NotificationChannel(
                CHANNEL_SESSION_STATUS,
                "Gece Oturumu Durumu",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Foreground servis durum bildirimi" }
        )

        channels.forEach { manager.createNotificationChannel(it) }
    }
}
