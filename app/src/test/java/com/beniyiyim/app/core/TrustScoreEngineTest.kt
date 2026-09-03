package com.beniyiyim.app.core

import com.beniyiyim.app.data.InterventionLevel
import com.beniyiyim.app.data.TestResult
import com.beniyiyim.app.data.TestType
import com.beniyiyim.app.data.UserBaseline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrustScoreEngineTest {

    private val engine = TrustScoreEngine()
    private val baseline = UserBaseline(
        userId = "test_user",
        avgReactionTimeMillis = 400L,
        avgAccuracy = 1.0,
        recordedAtEpochMillis = 0L
    )

    @Test
    fun `mukemmel test sonucu skoru yukseltir veya yuksek tutar`() {
        val perfectResult = TestResult(
            type = TestType.REACTION_TIME,
            accuracyScore = 1.0,
            reactionTimeMillis = 400L,
            baselineDeviationRatio = 0.0
        )

        val newScore = engine.computeUpdatedScore(
            previousScore = 80.0,
            result = perfectResult,
            baseline = baseline,
            elapsedMinutesSinceSessionStart = 30L
        )

        assertTrue("Mükemmel sonuç sonrası skor 80'den düşük olmamalı", newScore >= 80.0)
    }

    @Test
    fun `kotu test sonucu skoru dusurur`() {
        val poorResult = TestResult(
            type = TestType.REACTION_TIME,
            accuracyScore = 0.1,
            reactionTimeMillis = 1200L, // baseline'ın 3 katı yavaş
            baselineDeviationRatio = 0.8
        )

        val newScore = engine.computeUpdatedScore(
            previousScore = 90.0,
            result = poorResult,
            baseline = baseline,
            elapsedMinutesSinceSessionStart = 30L
        )

        assertTrue("Kötü sonuç sonrası skor düşmeli", newScore < 90.0)
    }

    @Test
    fun `gece ilerledikce ayni kotu sonuc skoru daha fazla dusurur`() {
        val poorResult = TestResult(
            type = TestType.REACTION_TIME,
            accuracyScore = 0.3,
            reactionTimeMillis = 900L,
            baselineDeviationRatio = 0.5
        )

        val earlyScore = engine.computeUpdatedScore(90.0, poorResult, baseline, elapsedMinutesSinceSessionStart = 10L)
        val lateScore = engine.computeUpdatedScore(90.0, poorResult, baseline, elapsedMinutesSinceSessionStart = 300L)

        assertTrue(
            "Gece ilerledikçe aynı kötü sonuç skoru daha fazla düşürmeli",
            lateScore < earlyScore
        )
    }

    @Test
    fun `skor esiklerine gore dogru mudahale seviyesi donuyor`() {
        assertEquals(InterventionLevel.NORMAL, engine.resolveInterventionLevel(85.0))
        assertEquals(InterventionLevel.MILD_WARNING, engine.resolveInterventionLevel(60.0))
        assertEquals(InterventionLevel.APP_RESTRICTION, engine.resolveInterventionLevel(35.0))
        assertEquals(InterventionLevel.FRIEND_ALERT, engine.resolveInterventionLevel(15.0))
        assertEquals(InterventionLevel.CRISIS, engine.resolveInterventionLevel(5.0))
    }

    @Test
    fun `skor sinirlar disina cikmiyor`() {
        val extremeGoodResult = TestResult(accuracyScore = 1.0, reactionTimeMillis = 100L)
        val extremeBadResult = TestResult(accuracyScore = 0.0, reactionTimeMillis = 5000L, baselineDeviationRatio = 1.0)

        val highScore = engine.computeUpdatedScore(100.0, extremeGoodResult, baseline, 0L)
        val lowScore = engine.computeUpdatedScore(0.0, extremeBadResult, baseline, 500L)

        assertTrue(highScore <= 100.0)
        assertTrue(lowScore >= 0.0)
    }

    @Test
    fun `test bekleme suresi skor dustukce kisalir`() {
        val delayHighScore = engine.computeNextTestDelayMinutes(trustScore = 95.0, elapsedMinutesSinceSessionStart = 30L)
        val delayLowScore = engine.computeNextTestDelayMinutes(trustScore = 40.0, elapsedMinutesSinceSessionStart = 30L)

        assertTrue(
            "Düşük skorda bekleme süresi yüksek skordan kısa olmalı",
            delayLowScore < delayHighScore
        )
    }

    @Test
    fun `hafiza testi kelime sayisi gece ilerledikce artar`() {
        val early = engine.computeWordCountForRecallTest(elapsedMinutesSinceSessionStart = 30L)
        val mid = engine.computeWordCountForRecallTest(elapsedMinutesSinceSessionStart = 120L)
        val late = engine.computeWordCountForRecallTest(elapsedMinutesSinceSessionStart = 240L)

        assertTrue(early <= mid)
        assertTrue(mid <= late)
    }
}
