package com.beniyiyim.app.core

import com.beniyiyim.app.data.ReputationProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReputationEngineTest {

    private val engine = ReputationEngine()

    @Test
    fun `arkadas onayli iptal skoru dusurmez`() {
        val profile = ReputationProfile(userId = "u1", reputationScore = 80.0)
        val updated = engine.recordFriendApprovedCancel(profile)

        assertEquals(80.0, updated.reputationScore, 0.001)
        assertEquals(1, updated.falseAlarmCancelCount)
    }

    @Test
    fun `arkadas onayi olmadan kendi iptali skoru dusurur`() {
        val profile = ReputationProfile(userId = "u1", reputationScore = 80.0)
        val updated = engine.recordSelfCancelWithoutFriend(profile)

        assertTrue("Skor düşmeli", updated.reputationScore < 80.0)
        assertEquals(1, updated.selfCancelWithoutFriendCount)
    }

    @Test
    fun `tekrarlayan trolleme otomatik taksi yetkisini kisitlar`() {
        var profile = ReputationProfile(userId = "u1", reputationScore = 100.0)

        // Art arda birkaç kez kötüye kullanım
        repeat(6) {
            profile = engine.recordSelfCancelWithoutFriend(profile)
        }

        assertFalse(
            "Tekrarlayan kötüye kullanım sonrası otomatik taksi yetkisi kapanmalı",
            profile.autoTaxiDispatchEnabled
        )
        assertTrue(engine.shouldOfferInsteadOfAutoDispatch(profile))
    }

    @Test
    fun `basarili geceler skoru zamanla iyilestirir`() {
        var profile = ReputationProfile(userId = "u1", reputationScore = 50.0)

        repeat(3) {
            profile = engine.recordCompletedSession(profile)
        }

        assertTrue("Başarılı geceler sonrası skor artmalı", profile.reputationScore > 50.0)
    }

    @Test
    fun `skor 0-100 araligini asmiyor`() {
        var profile = ReputationProfile(userId = "u1", reputationScore = 5.0)
        repeat(10) { profile = engine.recordSelfCancelWithoutFriend(profile) }
        assertTrue(profile.reputationScore >= 0.0)

        var highProfile = ReputationProfile(userId = "u2", reputationScore = 98.0)
        repeat(10) { highProfile = engine.recordCompletedSession(highProfile) }
        assertTrue(highProfile.reputationScore <= 100.0)
    }
}
