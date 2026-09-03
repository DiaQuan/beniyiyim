package com.beniyiyim.app.core

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.beniyiyim.app.BenIyiyimApp
import com.beniyiyim.app.data.InterventionLevel
import com.beniyiyim.app.test.SobrietyTestActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Kullanıcı "İçmeye Başladım" dediğinde başlayan, gece boyunca çalışan foreground service.
 * Periyodik olarak test tetikler; süre ve sıklık TrustScoreEngine tarafından belirlenir.
 */
class NightSessionService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.beniyiyim.app.action.START_SESSION"
        const val ACTION_STOP = "com.beniyiyim.app.action.STOP_SESSION"
        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_DRIVING_MODE = "extra_driving_mode"

        fun startIntent(context: Context, userId: String, drivingMode: Boolean): Intent {
            return Intent(context, NightSessionService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_DRIVING_MODE, drivingMode)
            }
        }
    }

    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(serviceJob)
    private var testLoopJob: Job? = null
    private val trustScoreEngine = TrustScoreEngine()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val userId = intent.getStringExtra(EXTRA_USER_ID) ?: "unknown_user"
                val drivingMode = intent.getBooleanExtra(EXTRA_DRIVING_MODE, false)
                startForeground(NOTIFICATION_ID, buildStatusNotification())
                NightSessionRepository.startSession(userId, drivingMode)
                startTestLoop()
            }
            ACTION_STOP -> {
                stopTestLoop()
                NightSessionRepository.endSession()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startTestLoop() {
        testLoopJob?.cancel()
        testLoopJob = scope.launch {
            while (true) {
                val session = NightSessionRepository.currentSessionOrNull() ?: break
                val elapsedMinutes = (System.currentTimeMillis() - session.startedAtEpochMillis) / 60000L

                val delayMinutes = trustScoreEngine.computeNextTestDelayMinutes(
                    trustScore = session.trustScore,
                    elapsedMinutesSinceSessionStart = elapsedMinutes
                )
                delay(delayMinutes * 60_000L)

                // Oturum hâlâ aktifse test bildirimi/aktivitesini tetikle
                if (NightSessionRepository.currentSessionOrNull() != null) {
                    triggerScheduledTest()
                }
            }
        }
    }

    private fun stopTestLoop() {
        testLoopJob?.cancel()
        testLoopJob = null
    }

    private fun triggerScheduledTest() {
        val intent = Intent(this, SobrietyTestActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(SobrietyTestActivity.EXTRA_REASON, "scheduled")
        }
        startActivity(intent)
    }

    private fun buildStatusNotification(): Notification {
        return NotificationCompat.Builder(this, BenIyiyimApp.CHANNEL_SESSION_STATUS)
            .setContentTitle("Ben İyiyim aktif")
            .setContentText("Gece güvenlik oturumu çalışıyor")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
