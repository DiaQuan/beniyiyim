package com.beniyiyim.app.core

import com.beniyiyim.app.data.InterventionLevel
import com.beniyiyim.app.data.NightSession
import com.beniyiyim.app.data.TestResult
import com.beniyiyim.app.data.UserBaseline
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Uygulama sürecinde aktif gece oturumunu tutan merkezi (singleton benzeri) depo.
 * Accessibility Service, foreground service ve UI katmanları buradan okuma/yazma yapar.
 *
 * Not: Gerçek üretimde bu bir dependency-injection edilen sınıf olmalı (Hilt/Koin),
 * burada MVP basitliği için object (singleton) olarak tutuluyor.
 */
object NightSessionRepository {

    private val trustScoreEngine = TrustScoreEngine()

    private val _sessionState = MutableStateFlow<NightSession?>(null)
    val sessionState: StateFlow<NightSession?> = _sessionState.asStateFlow()

    private var currentBaseline: UserBaseline = UserBaseline()

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    fun startSession(userId: String, drivingMode: Boolean, groupId: String? = null): NightSession {
        val session = NightSession(
            sessionId = UUID.randomUUID().toString(),
            userId = userId,
            startedAtEpochMillis = System.currentTimeMillis(),
            drivingMode = drivingMode,
            trustScore = 100.0,
            interventionLevel = InterventionLevel.NORMAL,
            groupId = groupId
        )
        _sessionState.value = session
        persistSession(session)
        return session
    }

    fun currentSessionOrNull(): NightSession? = _sessionState.value

    fun currentInterventionLevel(): InterventionLevel =
        _sessionState.value?.interventionLevel ?: InterventionLevel.NORMAL

    fun setBaseline(baseline: UserBaseline) {
        currentBaseline = baseline
    }

    fun getBaseline(): UserBaseline = currentBaseline

    /** Yeni bir test sonucu geldiğinde oturumu günceller ve müdahale seviyesini yeniden hesaplar. */
    fun recordTestResult(result: TestResult): NightSession? {
        val session = _sessionState.value ?: return null
        val elapsedMinutes = (System.currentTimeMillis() - session.startedAtEpochMillis) / 60000L

        val newScore = trustScoreEngine.computeUpdatedScore(
            previousScore = session.trustScore,
            result = result,
            baseline = currentBaseline,
            elapsedMinutesSinceSessionStart = elapsedMinutes
        )

        // Araba modunda eşikler daha katı tutulur (senin "Araba İle Geldim Modu" fikrin):
        // skor %50'nin altına düşer düşmez tam kilit moduna geçilir.
        val effectiveLevel = if (session.drivingMode && newScore < 50.0) {
            InterventionLevel.CRISIS
        } else {
            trustScoreEngine.resolveInterventionLevel(newScore)
        }

        val updated = session.copy(
            trustScore = newScore,
            testHistory = session.testHistory + result,
            interventionLevel = effectiveLevel
        )
        _sessionState.value = updated
        persistSession(updated)
        return updated
    }

    fun endSession() {
        val session = _sessionState.value ?: return
        val ended = session.copy(endedAtEpochMillis = System.currentTimeMillis())
        persistSession(ended)
        _sessionState.value = null
    }

    private fun persistSession(session: NightSession) {
        // Firestore'a asenkron yazım; MVP'de best-effort, hata durumunda sessizce loglanır.
        runCatching {
            firestore.collection("night_sessions")
                .document(session.sessionId)
                .set(session)
        }
    }
}
