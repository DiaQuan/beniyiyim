package com.beniyiyim.app.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.beniyiyim.app.data.TrustedContact
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Kriz anında bilgilendirilecek "Ayık Arkadaş / Güvenilir Kişi" kaydını alan ekran.
 * Onboarding akışının son adımıdır.
 */
class TrustedContactSetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        val label = TextView(this).apply {
            text = "Güvenilir Kişi Ekle (en az 1 kişi)"
            textSize = 20f
        }
        val nameInput = EditText(this).apply { hint = "İsim" }
        val phoneInput = EditText(this).apply {
            hint = "Telefon Numarası"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
        }
        val saveButton = Button(this).apply {
            text = "Kaydet"
            setOnClickListener {
                saveContact(nameInput.text.toString(), phoneInput.text.toString())
            }
        }

        root.addView(label)
        root.addView(nameInput)
        root.addView(phoneInput)
        root.addView(saveButton)
        setContentView(root)
    }

    private fun saveContact(name: String, phone: String) {
        if (name.isBlank() || phone.isBlank()) return

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous_user"
        val contact = TrustedContact(
            contactId = java.util.UUID.randomUUID().toString(),
            name = name,
            phoneNumber = phone,
            priorityOrder = 0
        )

        runCatching {
            FirebaseFirestore.getInstance()
                .collection("users").document(userId)
                .collection("trusted_contacts").document(contact.contactId)
                .set(contact)
        }

        finish()
    }
}
