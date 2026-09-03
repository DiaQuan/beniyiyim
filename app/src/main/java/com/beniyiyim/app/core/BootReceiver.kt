package com.beniyiyim.app.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Cihaz yeniden başladığında herhangi bir aktif gece oturumunu KASITLI OLARAK geri
 * yüklemez -- yeni bir gece her zaman kullanıcının "İçmeye Başladım" demesiyle başlar.
 * Bu receiver ileride "planlı gece" (ör. önceden ayarlanmış hatırlatma) gibi
 * özellikler için ayrılmıştır.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Şimdilik kasıtlı olarak boş -- oturumlar otomatik yeniden başlatılmaz.
    }
}
