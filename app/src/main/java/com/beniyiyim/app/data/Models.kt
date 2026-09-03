package com.beniyiyim.app.data

/**
 * Bir gece oturumu boyunca biriken durumu tutar.
 * Firestore'a periyodik olarak senkronize edilir.
 */
data class NightSession(
    val sessionId: String = "",
    val userId: String = "",
    val startedAtEpochMillis: Long = 0L,
    val endedAtEpochMillis: Long? = null,
    val drivingMode: Boolean = false,          // "Arabamla geldim" modu
    val trustScore: Double = 100.0,            // 0-100 arası, 100 = tam güven (ayık)
    val testHistory: List<TestResult> = emptyList(),
    val interventionLevel: InterventionLevel = InterventionLevel.NORMAL,
    val groupId: String? = null,               // "Aynı Masadayız" grubu varsa
    val isCaptain: Boolean = false
)

enum class InterventionLevel {
    NORMAL,          // %70-100: müdahale yok
    MILD_WARNING,    // %50-70: hafif uyarı bildirimi
    APP_RESTRICTION, // Skor eşiğine göre belirli uygulamalar kısıtlanır
    FRIEND_ALERT,    // Ayık arkadaşa "haberin olsun" bildirimi
    CRISIS           // Taksı / acil durum akışı tetiklenir
}

/** Tek bir testin sonucu. */
data class TestResult(
    val testId: String = "",
    val type: TestType = TestType.REACTION_TIME,
    val timestampEpochMillis: Long = 0L,
    val accuracyScore: Double = 0.0,      // 0.0 - 1.0
    val reactionTimeMillis: Long = 0L,
    val baselineDeviationRatio: Double = 0.0, // kullanıcının kendi ayık baseline'ına göre sapma
    val passed: Boolean = false
)

enum class TestType {
    DELAYED_WORD_RECALL,   // 3 kelime göster, X dk sonra sor
    REACTION_TIME,         // rastgele anda beliren butona basma
    MOTOR_COORDINATION,    // beliren noktayı takip etme / çizgi çizme
    MATH_LOGIC,            // basit matematik sorusu
    VOICE_DICTION          // ekrandaki cümleyi sesli okuma (faz 2)
}

/** Kullanıcının ayık haldeki referans (baseline) test performansı. */
data class UserBaseline(
    val userId: String = "",
    val avgReactionTimeMillis: Long = 0L,
    val avgAccuracy: Double = 1.0,
    val recordedAtEpochMillis: Long = 0L
)

/** Kullanıcının sistem güven skoru (Reputation Score) — trollemeyi/kötüye kullanımı önler. */
data class ReputationProfile(
    val userId: String = "",
    val reputationScore: Double = 100.0, // 0-100
    val falseAlarmCancelCount: Int = 0,   // arkadaş onayıyla iptal edilen (kullanıcının suçu değil)
    val selfCancelWithoutFriendCount: Int = 0, // arkadaş onayı olmadan kendi iptal etmesi (skoru düşürür)
    val completedSessionsCount: Int = 0,
    val autoTaxiDispatchEnabled: Boolean = true // reputationScore düşükse false olur
)

/** Acil durum / güvenilir kişi kaydı. */
data class TrustedContact(
    val contactId: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val priorityOrder: Int = 0 // birden fazla kişi varsa sıra
)

/** "Aynı Masadayız" grubu. */
data class NightGroup(
    val groupId: String = "",
    val venueId: String? = null,
    val memberUserIds: List<String> = emptyList(),
    val captainUserId: String? = null,
    val createdAtEpochMillis: Long = 0L
)
