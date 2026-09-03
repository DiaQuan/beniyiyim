package com.beniyiyim.app.core

import com.beniyiyim.app.data.ReputationProfile
import kotlin.math.max
import kotlin.math.min

/**
 * Kullanıcının "sistem güvenilirliği" skorunu yönetir.
 *
 * Amaç: Uygulamayı bilerek trolleyip taksi çağırtan / arkadaş onayı olmadan sürekli
 * kendi kendine iptal eden kullanıcıların "otomatik taksi çağırma" yetkisini kısıtlamak.
 *
 * Önemli ayrım:
 *  - Ayık arkadaş onayıyla iptal edilen bir taksi -> YANLIŞ ALARM, kullanıcının suçu değil,
 *    reputationScore'u ETKİLEMEZ (falseAlarmCancelCount artar ama skor düşmez).
 *  - Kullanıcının arkadaş onayı OLMADAN kendi kendine art arda iptal etmesi -> reputationScore
 *    düşürülür, çünkü bu kötüye kullanım sinyalidir.
 */
class ReputationEngine {

    companion object {
        const val AUTO_TAXI_DISABLE_THRESHOLD = 40.0
        private const val SELF_CANCEL_PENALTY = 12.0
        private const val COMPLETED_SESSION_REWARD = 3.0
        private const val MAX_SCORE = 100.0
        private const val MIN_SCORE = 0.0
    }

    /** Arkadaş onayıyla gerçekleşen (meşru) bir yanlış alarm iptalini işler. Skoru etkilemez. */
    fun recordFriendApprovedCancel(profile: ReputationProfile): ReputationProfile {
        return profile.copy(
            falseAlarmCancelCount = profile.falseAlarmCancelCount + 1
        )
    }

    /** Arkadaş onayı olmadan kullanıcının kendi kendine iptal etmesini işler. Skoru düşürür. */
    fun recordSelfCancelWithoutFriend(profile: ReputationProfile): ReputationProfile {
        val newScore = (profile.reputationScore - SELF_CANCEL_PENALTY).coerceIn(MIN_SCORE, MAX_SCORE)
        return profile.copy(
            reputationScore = newScore,
            selfCancelWithoutFriendCount = profile.selfCancelWithoutFriendCount + 1,
            autoTaxiDispatchEnabled = newScore >= AUTO_TAXI_DISABLE_THRESHOLD
        )
    }

    /** Sorunsuz tamamlanan bir gece oturumunu işler. Skoru zamanla iyileştirir. */
    fun recordCompletedSession(profile: ReputationProfile): ReputationProfile {
        val newScore = (profile.reputationScore + COMPLETED_SESSION_REWARD).coerceIn(MIN_SCORE, MAX_SCORE)
        return profile.copy(
            reputationScore = newScore,
            completedSessionsCount = profile.completedSessionsCount + 1,
            autoTaxiDispatchEnabled = newScore >= AUTO_TAXI_DISABLE_THRESHOLD
        )
    }

    /** Skor düşükse, sistem otomatik sipariş yerine yalnızca "öneri" moduna düşer. */
    fun shouldOfferInsteadOfAutoDispatch(profile: ReputationProfile): Boolean {
        return !profile.autoTaxiDispatchEnabled || profile.reputationScore < AUTO_TAXI_DISABLE_THRESHOLD
    }
}
