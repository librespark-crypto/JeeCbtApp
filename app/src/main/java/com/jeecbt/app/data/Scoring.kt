package com.jeecbt.app.data

import kotlin.math.abs

// ─────────────────────────────────────────────────────────────────────────────
// Result models
// ─────────────────────────────────────────────────────────────────────────────

enum class QResultStatus { CORRECT, WRONG, PARTIAL, UNATTEMPTED }

data class QResult(
    val q: ParsedQuestion,
    val state: AnswerState,
    val status: QResultStatus,
    val awarded: Int
)

data class SectionResult(
    val subject: String,
    val section: String,
    val total: Int,
    val correct: Int,
    val wrong: Int,
    val partial: Int,
    val unattempted: Int,
    val score: Int,
    val maxScore: Int
)

data class ScoreResult(
    val total: Int,
    val maxTotal: Int,
    val correct: Int,
    val wrong: Int,
    val partial: Int,
    val unattempted: Int,
    val accuracy: Float,
    val perQuestion: List<QResult>,
    val perSection: List<SectionResult>
)

// ─────────────────────────────────────────────────────────────────────────────
// Core evaluation (mirrors scoring.ts::evaluateQuestion)
// ─────────────────────────────────────────────────────────────────────────────

private fun arrEq(a: List<Int>, b: List<Int>): Boolean {
    if (a.size != b.size) return false
    return a.sorted() == b.sorted()
}

private fun evaluateQuestion(q: ParsedQuestion, st: AnswerState): Pair<QResultStatus, Int> {
    val attempted = !st.isEmpty
    if (!attempted) return QResultStatus.UNATTEMPTED to 0

    val (cm, im, pm) = Triple(q.marks.cm, q.marks.im, q.marks.pm)

    return when (q.type) {
        QType.MCQ -> {
            val correct = q.correctAnswerOptions
            val userVal = when {
                st.valueInt != null -> listOf(st.valueInt)
                st.valueInts != null && st.valueInts.isNotEmpty() -> listOf(st.valueInts.first())
                else -> emptyList()
            }
            if (correct.isNotEmpty() && arrEq(correct, userVal))
                QResultStatus.CORRECT to cm
            else
                QResultStatus.WRONG to im
        }

        QType.MSQ -> {
            val correct = q.correctAnswerOptions
            val userArr = st.valueInts ?: emptyList()
            val corrSet = correct.toSet()

            val anyWrong = userArr.any { it !in corrSet }
            when {
                anyWrong -> QResultStatus.WRONG to im
                arrEq(correct, userArr) -> QResultStatus.CORRECT to cm
                userArr.isNotEmpty() && userArr.size < correct.size -> {
                    val award = (pm ?: 1) * userArr.size
                    QResultStatus.PARTIAL to award
                }
                else -> QResultStatus.WRONG to im
            }
        }

        QType.NAT -> {
            val correct = q.correctAnswerNat.trim()
            val user = st.valueStr?.trim() ?: ""
            if (correct.isEmpty()) return QResultStatus.UNATTEMPTED to 0

            val cn = correct.toDoubleOrNull()
            val un = user.toDoubleOrNull()
            val ok = if (cn != null && un != null) abs(cn - un) < 1e-3 else correct == user
            if (ok) QResultStatus.CORRECT to cm else QResultStatus.WRONG to 0 // NAT: no negative
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main compute function (mirrors scoring.ts::computeScore)
// ─────────────────────────────────────────────────────────────────────────────

fun computeScore(test: ParsedTest, session: TestSession): ScoreResult {
    val perQuestion = mutableListOf<QResult>()
    val sectionMap = linkedMapOf<String, SectionResult>()

    var total = 0; var maxTotal = 0
    var correct = 0; var wrong = 0; var partial = 0; var unattempted = 0

    for (q in test.flatQuestions) {
        // Apply any override the candidate set during the session
        val effectiveQ = session.overrides[q.id]?.let { q.withOverride(it) } ?: q
        val st = session.answers[q.id] ?: AnswerState()
        val (status, awarded) = evaluateQuestion(effectiveQ, st)

        perQuestion.add(QResult(effectiveQ, st, status, awarded))

        total += awarded
        maxTotal += effectiveQ.marks.cm
        when (status) {
            QResultStatus.CORRECT -> correct++
            QResultStatus.WRONG -> wrong++
            QResultStatus.PARTIAL -> partial++
            else -> unattempted++
        }

        val sKey = "${q.subject}||${q.section}"
        val sec = sectionMap.getOrPut(sKey) {
            SectionResult(q.subject, q.section, 0, 0, 0, 0, 0, 0, 0)
        }
        sectionMap[sKey] = sec.copy(
            total = sec.total + 1,
            maxScore = sec.maxScore + effectiveQ.marks.cm,
            score = sec.score + awarded,
            correct = sec.correct + if (status == QResultStatus.CORRECT) 1 else 0,
            wrong = sec.wrong + if (status == QResultStatus.WRONG) 1 else 0,
            partial = sec.partial + if (status == QResultStatus.PARTIAL) 1 else 0,
            unattempted = sec.unattempted + if (status == QResultStatus.UNATTEMPTED) 1 else 0
        )
    }

    val attempted = correct + wrong + partial
    val accuracy = if (attempted > 0) correct.toFloat() / attempted * 100f else 0f

    return ScoreResult(total, maxTotal, correct, wrong, partial, unattempted, accuracy,
        perQuestion, sectionMap.values.toList())
}
