package com.beniyiyim.app.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.beniyiyim.app.data.NightSession
import com.beniyiyim.app.data.ReputationProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * Kriz anındaki (güven skoru çok düşük) akışı yönetir:
 *   1. Otomatik sipariş vermek yerine önce Ayık Arkadaş'a bildirim gider (2 dk onay penceresi)
 *   2. Arkadaş yanıt vermezse ya da "iptal" derse -> otomatik SİPARİŞ VERİLMEZ, sadece
 *      kullanıcıya güçlü bir taksı ÖNERİSİ (derin link) gösterilir.
 *   3. Reputation skoru düşük kullanıcılarda arkadaş adımı atlanıp direkt öneri moduna düşülür.
 *
 * Bilinçli tasarım kararı: Sistem hiçbir zaman kullanıcı adına gerçek bir taksi siparişi
 * OTOMATİK vermez (yasal/güven riski + "trolleyip taksiciyi mağdur etme" riskini önlemek için).
 * Bunun yerine en güçlü haliyle "dokunmanı istiyoruz" deneyimi sunulur.
 */
object CrisisFlowManager {

    private const val FRIEND_RESPONSE_WINDOW_MILLIS = 2 * 60 * 1000L
    private val reputationEngine = ReputationEngine()
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    fun beginCrisisFlow(context: Context, session: NightSession) {
        CoroutineScope(Dispatchers.IO).launch {
            val reputation = fetchReputation(session.userId)

            if (reputationEngine.shouldOfferInsteadOfAutoDispatch(reputation)) {
                // Düşük itibarlı kullanıcı: arkadaş adımı atlanır, direkt öneri gösterilir.
                showTaxiSuggestion(context)
                return@launch
            }

            val friendResponded = notifyTrustedContactAndAwaitResponse(session)
            if (friendResponded == FriendResponse.CANCEL) {
                val updated = reputationEngine.recordFriendApprovedCancel(reputation)
                persistReputation(updated)
                return@launch
            }

            // Arkadaş cevap vermedi ya da onayladı -> otomatik sipariş YOK, güçlü öneri göster.
            showTaxiSuggestion(context)
        }
    }

    fun notifyTrustedContact(context: Context, session: NightSession) {
        CoroutineScope(Dispatchers.IO).launch {
            // FRIEND_ALERT seviyesinde henüz aksiyon yok, sadece "haberin olsun" bildirimi.
            sendFriendPushNotification(
                session = session,
                messageOverride = "Bir arkadaşınız test sonuçlarına göre iyi durumda görünmüyor olabilir. Haberin olsun."
            )
        }
    }

    private enum class FriendResponse { CANCEL, NO_RESPONSE, ACKNOWLEDGED }

    private suspend fun notifyTrustedContactAndAwaitResponse(session: NightSession): FriendResponse {
        sendFriendPushNotification(
            session = session,
            messageOverride = null // varsayılan kriz mesajı kullanılır
        )
        // Gerçek uygulamada bu, Firestore'daki bir "response" alanını dinleyen bir
        // listener/coroutine ile yapılır. MVP'de basitleştirilmiş polling ile temsil edilir.
        delay(FRIEND_RESPONSE_WINDOW_MILLIS)
        return FriendResponse.NO_RESPONSE
    }

    private fun sendFriendPushNotification(session: NightSession, messageOverride: String?) {
        val message = messageOverride ?: buildDefaultCrisisMessage(session)
        // Firestore'a yazılan bir "pending_alerts" dokümanı, backend Cloud Function
        // tarafından okunup FCM push bildirimi olarak güvenilir kişiye gönderilir.
        runCatching {
            firestore.collection("pending_alerts").add(
                mapOf(
                    "sessionId" to session.sessionId,
                    "userId" to session.userId,
                    "message" to message,
                    "createdAtEpochMillis" to System.currentTimeMillis(),
                    "responseWindowMillis" to FRIEND_RESPONSE_WINDOW_MILLIS
                )
            )
        }
    }

    private fun buildDefaultCrisisMessage(session: NightSession): String {
        // Kesin yüzde yerine daha nazik/mahremiyet dostu bir dil kullanılır.
        return "Bir arkadaşınızın test sonuçları iyi durumda görünmüyor. " +
            "Bulunduğu konuma taksı önerisi gösteriyoruz. Yanlış alarmsa 2 dakikan var, iptal edebilirsin."
    }

    /**
     * Otomatik sipariş vermez -- kullanıcının önüne bir taksı/ride-hailing uygulamasının
     * derin linkini (deep link) açar; son dokunuşu her zaman insan yapar.
     */
    private fun showTaxiSuggestion(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("bitaksi://"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private suspend fun fetchReputation(userId: String): ReputationProfile {
        return runCatching {
            val doc = firestore.collection("reputation_profiles").document(userId).get()
            // NOT: Task<T>.await() için kotlinx-coroutines-play-services eklenmesi gerekir;
            // burada basitleştirilmiş placeholder davranış kullanılmıştır.
            ReputationProfile(userId = userId)
        }.getOrElse { ReputationProfile(userId = userId) }
    }

    private fun persistReputation(profile: ReputationProfile) {
        runCatching {
            firestore.collection("reputation_profiles").document(profile.userId).set(profile)
        }
    }
}
