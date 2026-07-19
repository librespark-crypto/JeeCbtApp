package com.jeecbt.app.data

// ─────────────────────────────────────────────────────────────────────────────
// Question types & marks
// ─────────────────────────────────────────────────────────────────────────────

enum class QType { MCQ, MSQ, NAT }

data class Marks(
    val cm: Int = 4,
    val im: Int = -1,
    val pm: Int? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// Test structure (mirrors original ParsedQuestion / ParsedTest)
// Images are kept OUT of this class – stored in ViewModel as Map<qid, List<Bitmap>>
// so they don't need to be serialised.
// ─────────────────────────────────────────────────────────────────────────────

data class ParsedQuestion(
    val id: String,                      // "subject|section|qno"
    val subject: String,
    val section: String,
    val qno: Int,
    val type: QType,
    val marks: Marks,
    val imageIds: List<String>,          // ordered keys into the bitmap cache
    val correctAnswerOptions: List<Int> = emptyList(), // MCQ / MSQ
    val correctAnswerNat: String = ""    // NAT
) {
    /** Apply an in-session admin override and return a new instance. */
    fun withOverride(o: QuestionOverride) = copy(
        type               = o.type                ?: type,
        marks              = o.marks               ?: marks,
        correctAnswerOptions = o.correctAnswerOptions ?: correctAnswerOptions,
        correctAnswerNat   = o.correctAnswerNat    ?: correctAnswerNat,
        imageIds           = o.imageIds            ?: imageIds
    )
}

data class ParsedSection(
    val name: String,
    val subject: String,
    val questions: List<ParsedQuestion>
)

data class ParsedSubject(
    val name: String,
    val sections: List<ParsedSection>
)

data class ParsedTest(
    val title: String,
    val durationSec: Int,
    val subjects: List<ParsedSubject>,
    val flatQuestions: List<ParsedQuestion>
)

// ─────────────────────────────────────────────────────────────────────────────
// Session state
// ─────────────────────────────────────────────────────────────────────────────

enum class QStatus { NOT_VISITED, VISITED, ANSWERED, MARKED, MARKED_ANSWERED }

data class AnswerState(
    val valueInt: Int? = null,           // MCQ
    val valueInts: List<Int>? = null,    // MSQ
    val valueStr: String? = null,        // NAT
    val status: QStatus = QStatus.NOT_VISITED,
    val timeSpentSec: Int = 0
) {
    val isEmpty: Boolean
        get() = valueInt == null &&
                (valueInts == null || valueInts.isEmpty()) &&
                (valueStr == null || valueStr.isEmpty())
}

data class TestSession(
    val testId: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val remainingSec: Int,
    val currentQid: String?,
    val answers: Map<String, AnswerState>,
    val overrides: Map<String, QuestionOverride> = emptyMap()
)

data class QuestionOverride(
    val type: QType? = null,
    val marks: Marks? = null,
    val correctAnswerOptions: List<Int>? = null,
    val correctAnswerNat: String? = null,
    val imageIds: List<String>? = null
)
