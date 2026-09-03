package com.beniyiyim.app.onboarding

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.beniyiyim.app.R
import com.beniyiyim.app.main.MainActivity

/**
 * Kullanıcı AYIKKEN, "Gece Modu"nu ilk kez kullanmadan önce gerekli tüm izinleri
 * TEK TEK açıklayarak isteyen kurulum akışı. Rıza burada net şekilde alınır --
 * her izin için ayrı bir ekran/checkbox, tek bir "kabul ediyorum" ile geçiştirilmez.
 */
class OnboardingActivity : AppCompatActivity() {

    private val steps = listOf(
        OnboardingStep(
            title = "Erişilebilirlik İzni",
            description = "Gece modu aktifken hangi uygulamanın açık olduğunu tespit etmemiz için gerekli. İçeriğini OKUMAYIZ, sadece uygulama adını görürüz.",
            action = { openAccessibilitySettings() }
        ),
        OnboardingStep(
            title = "Diğer Uygulamaların Üzerinde Görüntüleme",
            description = "Test ekranını göstermek için gerekli.",
            action = { openOverlaySettings() }
        ),
        OnboardingStep(
            title = "Konum İzni",
            description = "Kriz anında taksı önerisi için konumuna ihtiyacımız var.",
            action = { requestLocationPermission() }
        ),
        OnboardingStep(
            title = "Bildirimler",
            description = "Test hatırlatmaları ve güvenlik uyarıları için gerekli.",
            action = { requestNotificationPermission() }
        ),
        OnboardingStep(
            title = "Güvenilir Kişi Ekle",
            description = "Kriz anında bilgilendirilecek en az bir kişi eklemelisin.",
            action = { navigateToTrustedContactSetup() }
        )
    )

    private var currentStepIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        val titleView = TextView(this).apply { textSize = 22f }
        val descView = TextView(this).apply { textSize = 16f; setPadding(0, 24, 0, 48) }
        val actionButton = Button(this)
        val skipInfo = TextView(this).apply {
            text = "Bu izin verilmeden Gece Modu başlatılamaz."
            textSize = 12f
        }

        root.addView(titleView)
        root.addView(descView)
        root.addView(actionButton)
        root.addView(skipInfo)
        setContentView(root)

        fun renderStep() {
            if (currentStepIndex >= steps.size) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return
            }
            val step = steps[currentStepIndex]
            titleView.text = step.title
            descView.text = step.description
            actionButton.text = "İzin Ver / Devam Et"
            actionButton.setOnClickListener {
                step.action()
                currentStepIndex++
                renderStep()
            }
        }

        renderStep()
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun requestLocationPermission() {
        androidx.core.app.ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            1001
        )
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.app.ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                1002
            )
        }
    }

    private fun navigateToTrustedContactSetup() {
        // MVP: basitleştirilmiş -- gerçek sürümde ayrı bir form ekranı olur.
        startActivity(Intent(this, com.beniyiyim.app.ui.TrustedContactSetupActivity::class.java))
    }

    private data class OnboardingStep(
        val title: String,
        val description: String,
        val action: () -> Unit
    )
}
