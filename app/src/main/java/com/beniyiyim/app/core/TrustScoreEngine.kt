package com.beniyiyim.app.core

import com.beniyiyim.app.data.InterventionLevel
import com.beniyiyim.app.data.TestResult
import com.beniyiyim.app.data.UserBaseline
import kotlin.math.max
import kotlin.math.min

/**
 * Kullanıcının test sonuçlarına göre "güven skorunu" (0-100, 100 = tam ayık/güvenilir)
 * hesaplayan motor.
 *
 * Tasarım kararları:
 *  - Skor, üstel ağırlıklı hareketli ortalama (EWMA) ile güncellenir: en yeni testler
 *    en yüksek ağırlığa sahiptir, çünkü alkolün etkisi kümülatiftir ve eski bir "iyi"
 *    sonuç şu anki durumu göstermez.
 *  - Her testin doğruluğu kullanıcının KENDİ ayık baseline'ına göre normalize edilir;
 *    mutlak bir eşik yerine kişiye özel sapma oranı kullanılır.
 */
class TrustScoreEngine(
    private val decayFactor: Double = 0.35 // yeni testin ağırlığı (0-1)
) {

    /**
     * Yeni bir test sonucu geldiğinde güncel skoru hesaplar.
     * @param previousScore önceki güven skoru (0-100)
     * @param result yeni test sonucu
     * @param baseline kullanıcının ayık referans performansı
     * @param elapsedMinutesSinceSessionStart gece başlangıcından bu yana geçen süre (dk) —
     *        zorluk/hassasiyet artışı için kullanılır
     */
    fun computeUpdatedScore(
        previousScore: Double,
        result: TestResult,
        baseline: UserBaseline,
        elapsedMinutesSinceSessionStart: Long
    ): Double {
        val instantScore = computeInstantScore(result, baseline)

        // Gece ilerledikçe sistem daha az "af" ediyor: zaman ağırlığı artırılır.
        val timeAdjustedDecay = min(0.6, decayFactor + (elapsedMinutesSinceSessionStart / 600.0))

        val updated = (previousScore * (1 - timeAdjustedDecay)) + (instantScore * timeAdjustedDecay)
        return updated.coerceIn(0.0, 100.0)
    }

    /**
     * Tek bir test sonucunun anlık (o teste özel) skorunu hesaplar.
     * Doğruluk, tepki süresi sapması ve baseline karşılaştırması bir arada değerlendirilir.
     */
    private fun computeInstantScore(result: TestResult, baseline: UserBaseline): Double {
        // 1. Doğruluk bileşeni (0-100)
        val accuracyComponent = result.accuracyScore.coerceIn(0.0, 1.0) * 100.0

        // 2. Tepki süresi bileşeni: baseline'a göre ne kadar yavaşladığı
        val reactionRatio = if (baseline.avgReactionTimeMillis > 0) {
            result.reactionTimeMillis.toDouble() / baseline.avgReactionTimeMillis.toDouble()
        } else {
            1.0
        }
        // ratio 1.0 = baseline ile aynı hız -> 100 puan
        // ratio 2.0 (iki kat yavaş) -> ~0 puan civarı
        val reactionComponent = (100.0 - ((reactionRatio - 1.0).coerceAtLeast(0.0) * 100.0))
            .coerceIn(0.0, 100.0)

        // 3. Baseline sapma oranı (motor koordinasyon / hafıza testlerinde kullanılan genel sapma)
        val deviationComponent = (100.0 - (result.baselineDeviationRatio.coerceIn(0.0, 1.0) * 100.0))

        // Ağırlıklı bileşim: doğruluk en önemli, sonra tepki süresi, sonra genel sapma
        return (accuracyComponent * 0.5) + (reactionComponent * 0.3) + (deviationComponent * 0.2)
    }

    /** Güven skoruna göre müdahale seviyesini belirler. */
    fun resolveInterventionLevel(trustScore: Double): InterventionLevel {
        return when {
            trustScore >= 70.0 -> InterventionLevel.NORMAL
            trustScore >= 50.0 -> InterventionLevel.MILD_WARNING
            trustScore >= 30.0 -> InterventionLevel.APP_RESTRICTION
            trustScore >= 8.0 -> InterventionLevel.FRIEND_ALERT
            else -> InterventionLevel.CRISIS
        }
    }

    /**
     * Bir sonraki test için bekleme süresini (dakika) hesaplar.
     * Gece ilerledikçe ve skor düştükçe süre kısalır (soru sıklığı artar).
     */
    fun computeNextTestDelayMinutes(trustScore: Double, elapsedMinutesSinceSessionStart: Long): Long {
        val baseDelay = 30L // dakika
        val scoreFactor = (trustScore / 100.0).coerceIn(0.2, 1.0) // düşük skor -> daha sık test
        val timeFactor = max(0.4, 1.0 - (elapsedMinutesSinceSessionStart / 480.0)) // gece ilerledikçe azalır

        val delay = (baseDelay * scoreFactor * timeFactor).toLong()
        return delay.coerceIn(3L, baseDelay)
    }

    /**
     * Gecikmeli hafıza testi için bekleme süresini (dk) belirler.
     * Örn: başta 20 dk sonra sorulan 3 kelime, gece ilerledikçe daha kısa sürede sorulur
     * (test zorlaşır çünkü daha az "unutma payı" verilir... aslında tam tersi mantıkla
     * -- burada süreyi UZATIYORUZ çünkü unutma ihtimali alkolle beraber zaten artıyor,
     * bu yüzden sabit 20 dk tutup zorluğu kelime sayısıyla artırmak daha doğru bir tasarım).
     */
    fun computeDelayedRecallWaitMinutes(elapsedMinutesSinceSessionStart: Long): Long = 20L

    /** Gece ilerledikçe hafıza testinde kaç kelime hatırlaması istendiğini artırır. */
    fun computeWordCountForRecallTest(elapsedMinutesSinceSessionStart: Long): Int {
        return when {
            elapsedMinutesSinceSessionStart < 90 -> 3
            elapsedMinutesSinceSessionStart < 180 -> 4
            else -> 5
        }
    }
}
