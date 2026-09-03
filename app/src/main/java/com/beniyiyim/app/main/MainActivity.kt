package com.beniyiyim.app.main

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.beniyiyim.app.core.NightSessionService
import com.google.firebase.auth.FirebaseAuth

/**
 * "İçmeye Başladım" butonunun bulunduğu ana ekran. Kullanıcı burada
 * "Arabamla geldim" modunu işaretleyip gece oturumunu başlatır.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        val title = TextView(this).apply {
            text = "Ben İyiyim"
            textSize = 28f
        }

        val drivingModeCheckbox = CheckBox(this).apply {
            text = "Arabamla geldim (Sürüş Güvenliği Modu)"
        }

        val startButton = Button(this).apply {
            text = "İçmeye Başladım"
            setOnClickListener {
                startNightSession(drivingModeCheckbox.isChecked)
            }
        }

        root.addView(title)
        root.addView(drivingModeCheckbox)
        root.addView(startButton)
        setContentView(root)
    }

    private fun startNightSession(drivingMode: Boolean) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: FirebaseAuth.getInstance().uid.orEmpty().ifEmpty { "anonymous_user" }

        val intent = NightSessionService.startIntent(this, userId, drivingMode)
        androidx.core.content.ContextCompat.startForegroundService(this, intent)
        finish()
    }
}

private val FirebaseAuth.uid: String?
    get() = currentUser?.uid
