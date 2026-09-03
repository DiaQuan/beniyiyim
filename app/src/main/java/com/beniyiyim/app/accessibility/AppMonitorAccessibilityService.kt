package com.beniyiyim.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.beniyiyim.app.core.NightSessionRepository
import com.beniyiyim.app.data.InterventionLevel
import com.beniyiyim.app.test.SobrietyTestActivity

/**
 * Kullanıcının GECE MODU'nu başlatmadan önce, ayıkken bilinçli olarak verdiği izinle
 * çalışan erişilebilirlik servisi.
 *
 * Ne yapar: İzlenen paket listesindeki (Instagram, online alışveriş, SMS vb.) bir
 * uygulama ön plana geldiğinde, eğer gece oturumu aktif ve müdahale seviyesi
 * APP_RESTRICTION veya üzeriyse, tam ekran test aktivitesini overlay olarak açar.
 *
 * Ne yapmaz: SMS gönderimini API seviyesinde engellemez (Android bunu desteklemiyor),
 * ekran içeriğini okumaz (canRetrieveWindowContent=false), sadece hangi PAKETİN önde
 * olduğunu bilir.
 */
class AppMonitorAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AppMonitorService"

        // Kısıtlanacak uygulama paket adları (kurulumda kullanıcı tarafından
        // değiştirilebilir/onaylanabilir bir liste; burada varsayılan set örneklenmiştir)
        val RESTRICTED_PACKAGES = setOf(
            "com.instagram.android",
            "com.whatsapp",
            "com.google.android.apps.messaging", // SMS
            "com.android.mms",
            "com.trendyol",
            "com.hepsiburada.ana",
            "com.dsmobile.dolap"
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return

        if (packageName !in RESTRICTED_PACKAGES) return
        if (packageName == this.packageName) return // kendi uygulamamızı yoksay

        val session = NightSessionRepository.currentSessionOrNull() ?: return
        if (!session.let { true }) return // oturum yoksa müdahale etme

        val level = NightSessionRepository.currentInterventionLevel()
        if (level == InterventionLevel.APP_RESTRICTION ||
            level == InterventionLevel.FRIEND_ALERT ||
            level == InterventionLevel.CRISIS
        ) {
            Log.d(TAG, "Kısıtlanan uygulama tespit edildi: $packageName, seviye: $level")
            launchOverlayTest(packageName)
        }
    }

    private fun launchOverlayTest(triggeringPackage: String) {
        val intent = Intent(this, SobrietyTestActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(SobrietyTestActivity.EXTRA_TRIGGERING_PACKAGE, triggeringPackage)
            putExtra(SobrietyTestActivity.EXTRA_REASON, "app_restriction")
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "AppMonitorAccessibilityService bağlandı")
    }
}
