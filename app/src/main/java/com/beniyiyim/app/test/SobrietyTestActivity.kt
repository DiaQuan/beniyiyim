package com.beniyiyim.app.test

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.beniyiyim.app.R
import com.beniyiyim.app.core.CrisisFlowManager
import com.beniyiyim.app.core.NightSessionRepository
import com.beniyiyim.app.data.InterventionLevel
import kotlin.random.Random

/**
 * Kısıtlanan bir uygulama açıldığında ya da zamanlanmış testin sırası geldiğinde
 * ekranın üstüne binen tam ekran aktivite. Test çözülmeden kapatılamaz
 * (geri tuşu devre dışı bırakılır, launchMode singleTask).
 *
 * Her çağrıda rastgele bir test tipi seçilir; DELAYED_WORD_RECALL testleri ise
 * iki aşamalı çalışır (kelimeleri göster -> X dakika sonra geri sor).
 */
class SobrietyTestActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TRIGGERING_PACKAGE = "extra_triggering_package"
        const val EXTRA_REASON = "extra_reason" // "scheduled" | "app_restriction"
    }

    private var testStartTimeMillis = 0L
    private var pendingReactionSpec: SobrietyTestSpec.ReactionTimeSpec? = null
    private var reactionButtonShownAtMillis = 0L

    private lateinit var container: LinearLayout
    private lateinit var promptText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sobriety_test)

        container = findViewById(R.id.test_container)
        promptText = findViewById(R.id.test_prompt)

        testStartTimeMillis = System.currentTimeMillis()
        presentRandomTest()
    }

    // Geri tuşuyla kapatmayı engelle -- test tamamlanmadan alta geçilemez.
    override fun onBackPressed() {
        // Kasıtlı olarak hiçbir şey yapmıyoruz.
    }

    private fun presentRandomTest() {
        val elapsedMinutes = elapsedSessionMinutes()
        val testTypeRoll = Random.nextInt(4)

        when (testTypeRoll) {
            0 -> presentReactionTimeTest()
            1 -> presentMathLogicTest()
            2 -> presentWordRecallPromptPhase(elapsedMinutes)
            else -> presentReactionTimeTest()
        }
    }

    private fun elapsedSessionMinutes(): Long {
        val session = NightSessionRepository.currentSessionOrNull() ?: return 0L
        return (System.currentTimeMillis() - session.startedAtEpochMillis) / 60000L
    }

    // ---------- Reaksiyon süresi testi ----------

    private fun presentReactionTimeTest() {
        val spec = SobrietyTestSpec.ReactionTimeSpec()
        pendingReactionSpec = spec
        promptText.text = getString(R.string.test_reaction_prompt)
        container.removeAllViews()

        val delay = spec.randomDelayMillis()
        container.postDelayed({
            if (isFinishing) return@postDelayed
            val button = Button(this).apply {
                text = getString(R.string.test_reaction_button)
                setOnClickListener { onReactionButtonPressed() }
            }
            reactionButtonShownAtMillis = System.currentTimeMillis()
            container.addView(button)
        }, delay)
    }

    private fun onReactionButtonPressed() {
        val reactionMillis = System.currentTimeMillis() - reactionButtonShownAtMillis
        val baseline = NightSessionRepository.getBaseline()
        val result = SobrietyTestScorer.scoreReactionTime(
            expectedDelayMillis = 0L,
            actualReactionMillis = reactionMillis,
            baseline = baseline
        )
        submitResult(result)
    }

    // ---------- Matematik/mantık testi ----------

    private fun presentMathLogicTest() {
        val spec = SobrietyTestSpec.MathLogicSpec.random()
        container.removeAllViews()
        promptText.text = getString(
            R.string.test_math_prompt,
            spec.operandA,
            spec.operator,
            spec.operandB
        )

        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        }
        val submitButton = Button(this).apply {
            text = getString(R.string.test_submit)
            setOnClickListener {
                val answer = input.text.toString().toIntOrNull()
                val responseTime = System.currentTimeMillis() - testStartTimeMillis
                submitResult(SobrietyTestScorer.scoreMathLogic(spec, answer, responseTime))
            }
        }
        container.addView(input)
        container.addView(submitButton)
    }

    // ---------- Gecikmeli hafıza testi (iki aşamalı) ----------

    private fun presentWordRecallPromptPhase(elapsedMinutes: Long) {
        val wordCount = com.beniyiyim.app.core.TrustScoreEngine().computeWordCountForRecallTest(elapsedMinutes)
        val waitMinutes = com.beniyiyim.app.core.TrustScoreEngine().computeDelayedRecallWaitMinutes(elapsedMinutes)
        val words = SobrietyTestSpec.DelayedWordRecallSpec.randomWords(wordCount)

        promptText.text = getString(R.string.test_recall_show_prompt, words.joinToString(", "))
        container.removeAllViews()

        val okButton = Button(this).apply {
            text = getString(R.string.test_recall_understood)
            setOnClickListener {
                RecallTestScheduler.scheduleRecallCheck(this@SobrietyTestActivity, words, waitMinutes)
                finish() // Kelimeler gösterildi, X dk sonra tekrar açılacak (scheduler ile)
            }
        }
        container.addView(okButton)
    }

    private fun submitResult(result: com.beniyiyim.app.data.TestResult) {
        val updatedSession = NightSessionRepository.recordTestResult(result)
        if (updatedSession == null) {
            finish()
            return
        }

        when (updatedSession.interventionLevel) {
            InterventionLevel.CRISIS -> {
                CrisisFlowManager.beginCrisisFlow(this, updatedSession)
                finish()
            }
            InterventionLevel.FRIEND_ALERT -> {
                CrisisFlowManager.notifyTrustedContact(this, updatedSession)
                showResultThenFinish("Bir arkadaşına haber verdik, endişelenme.")
            }
            else -> {
                showResultThenFinish(getString(R.string.test_completed_message))
            }
        }
    }

    private fun showResultThenFinish(message: String) {
        promptText.text = message
        container.removeAllViews()
        container.postDelayed({ finish() }, 2000L)
    }
}
