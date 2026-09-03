package com.beniyiyim.app.test

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * "3 kelime göster, X dakika sonra sor" mekaniğinin zamanlama kısmı.
 * AlarmManager ile X dakika sonra SobrietyTestActivity'yi "recall_check" modunda
 * yeniden açar.
 */
object RecallTestScheduler {

    const val EXTRA_RECALL_WORDS = "extra_recall_words"
    const val EXTRA_RECALL_PHASE = "extra_recall_phase" // "check"

    fun scheduleRecallCheck(context: Context, words: List<String>, waitMinutes: Long) {
        val intent = Intent(context, SobrietyTestActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(SobrietyTestActivity.EXTRA_REASON, "recall_check")
            putStringArrayListExtra(EXTRA_RECALL_WORDS, ArrayList(words))
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            words.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = System.currentTimeMillis() + (waitMinutes * 60_000L)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // setExactAndAllowWhileIdle: Doze modunda bile testin zamanında tetiklenmesini sağlar,
        // çünkü tam da bu an kullanıcının "unutma payının" ölçüldüğü kritik andır.
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }
}
