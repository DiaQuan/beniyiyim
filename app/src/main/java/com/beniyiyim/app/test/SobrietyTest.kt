package com.beniyiyim.app.test

import com.beniyiyim.app.data.TestResult
import com.beniyiyim.app.data.TestType
import com.beniyiyim.app.data.UserBaseline
import java.util.UUID
import kotlin.random.Random

/**
 * Farklı test tiplerinin ortak arayüzü. Her test, kullanıcı etkileşiminden
 * bir TestResult üretir.
 */
sealed class SobrietyTestSpec {

    abstract val type: TestType

    /** Reaksiyon süresi testi: rastgele bir gecikmeden sonra ekranda buton belirir. */
    data class ReactionTimeSpec(
        val minDelayMillis: Long = 1500L,
        val maxDelayMillis: Long = 5000L
    ) : SobrietyTestSpec() {
        override val type = TestType.REACTION_TIME

        fun randomDelayMillis(): Long = Random.nextLong(minDelayMillis, maxDelayMillis)
    }

    /** Gecikmeli hafıza testi: N kelime gösterilir, X dakika sonra geri sorulur. */
    data class DelayedWordRecallSpec(
        val words: List<String>,
        val waitMinutes: Long
    ) : SobrietyTestSpec() {
        override val type = TestType.DELAYED_WORD_RECALL

        companion object {
            private val WORD_POOL = listOf(
                "masa", "kalem", "bulut", "deniz", "kitap", "anahtar", "pencere",
                "yıldız", "orman", "köprü", "gitar", "elma", "saat", "bahçe", "tren"
            )

            fun randomWords(count: Int): List<String> = WORD_POOL.shuffled().take(count)
        }
    }

    /** Basit matematik/mantık testi. */
    data class MathLogicSpec(
        val operandA: Int,
        val operandB: Int,
        val operator: Char // '+' veya '-'
    ) : SobrietyTestSpec() {
        override val type = TestType.MATH_LOGIC

        val correctAnswer: Int
            get() = if (operator == '+') operandA + operandB else operandA - operandB

        companion object {
            fun random(): MathLogicSpec {
                val a = Random.nextInt(10, 99)
                val b = Random.nextInt(1, 50)
                val op = if (Random.nextBoolean()) '+' else '-'
                return MathLogicSpec(a, b, op)
            }
        }
    }

    /** Motor koordinasyon testi: ekranda beliren hedefi takip etme / düz çizgi çizme. */
    data object MotorCoordinationSpec : SobrietyTestSpec() {
        override val type = TestType.MOTOR_COORDINATION
    }
}

/**
 * Kullanıcı etkileşiminden ham ölçümleri alıp bir TestResult'a dönüştürür.
 */
object SobrietyTestScorer {

    fun scoreReactionTime(
        expectedDelayMillis: Long,
        actualReactionMillis: Long,
        baseline: UserBaseline
    ): TestResult {
        // Çok erken basma (delay bitmeden) = hile/hazır bekleme, düşük puan
        val prematurePress = actualReactionMillis < 0
        val accuracy = if (prematurePress) 0.0 else {
            val baselineMs = baseline.avgReactionTimeMillis.takeIf { it > 0 } ?: 400L
            (baselineMs.toDouble() / actualReactionMillis.toDouble()).coerceIn(0.0, 1.0)
        }

        return TestResult(
            testId = UUID.randomUUID().toString(),
            type = TestType.REACTION_TIME,
            timestampEpochMillis = System.currentTimeMillis(),
            accuracyScore = accuracy,
            reactionTimeMillis = actualReactionMillis.coerceAtLeast(0),
            baselineDeviationRatio = 1.0 - accuracy,
            passed = accuracy > 0.5
        )
    }

    fun scoreWordRecall(
        originalWords: List<String>,
        userAnswer: List<String>,
        responseTimeMillis: Long
    ): TestResult {
        val correctCount = userAnswer.count { it.trim().lowercase() in originalWords.map { w -> w.lowercase() } }
        val accuracy = correctCount.toDouble() / originalWords.size.toDouble()

        return TestResult(
            testId = UUID.randomUUID().toString(),
            type = TestType.DELAYED_WORD_RECALL,
            timestampEpochMillis = System.currentTimeMillis(),
            accuracyScore = accuracy,
            reactionTimeMillis = responseTimeMillis,
            baselineDeviationRatio = 1.0 - accuracy,
            passed = accuracy >= 0.66
        )
    }

    fun scoreMathLogic(
        spec: SobrietyTestSpec.MathLogicSpec,
        userAnswer: Int?,
        responseTimeMillis: Long
    ): TestResult {
        val correct = userAnswer == spec.correctAnswer
        return TestResult(
            testId = UUID.randomUUID().toString(),
            type = TestType.MATH_LOGIC,
            timestampEpochMillis = System.currentTimeMillis(),
            accuracyScore = if (correct) 1.0 else 0.0,
            reactionTimeMillis = responseTimeMillis,
            baselineDeviationRatio = if (correct) 0.0 else 1.0,
            passed = correct
        )
    }

    /** Motor koordinasyon testi: yol sapması oranından skor üretir (0.0 = mükemmel, 1.0 = tam sapma). */
    fun scoreMotorCoordination(pathDeviationRatio: Double, responseTimeMillis: Long): TestResult {
        val deviation = pathDeviationRatio.coerceIn(0.0, 1.0)
        return TestResult(
            testId = UUID.randomUUID().toString(),
            type = TestType.MOTOR_COORDINATION,
            timestampEpochMillis = System.currentTimeMillis(),
            accuracyScore = 1.0 - deviation,
            reactionTimeMillis = responseTimeMillis,
            baselineDeviationRatio = deviation,
            passed = deviation < 0.35
        )
    }
}
