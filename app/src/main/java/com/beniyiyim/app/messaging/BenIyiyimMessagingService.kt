package com.beniyiyim.app.messaging

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.beniyiyim.app.BenIyiyimApp
import com.beniyiyim.app.main.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Backend (Cloud Functions) tarafından gönderilen push bildirimlerini alır:
 *  - Ayık Arkadaş'a giden kriz bildirimi ("İptal Et" aksiyonlu)
 *  - Grup üyelerine giden "Kaptan" bildirimleri
 *  - "Eve Vardım" doğrulama bildirimleri
 */
class BenIyiyimMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: "Ben İyiyim"
        val body = message.notification?.body ?: ""
        val alertType = message.data["alertType"] ?: "generic"

        showNotification(title, body, alertType)
    }

    override fun onNewToken(token: String) {
        // Token'ı Firestore'daki kullanıcı profiline kaydetmek için backend'e iletilir.
        runCatching {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("fcm_tokens")
                .document(token)
                .set(mapOf("token" to token, "updatedAt" to System.currentTimeMillis()))
        }
    }

    private fun showNotification(title: String, body: String, alertType: String) {
        val channel = when (alertType) {
            "crisis", "friend_alert" -> BenIyiyimApp.CHANNEL_SAFETY_ALERT
            else -> BenIyiyimApp.CHANNEL_TEST_PROMPT
        }

        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channel)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
    }
}
