package com.jeecbt.app

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeecbt.app.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────────
// App phases
// ─────────────────────────────────────────────────────────────────────────────

sealed class AppPhase { object Home : AppPhase(); object Test : AppPhase(); object Result : AppPhase() }

// ─────────────────────────────────────────────────────────────────────────────
// Top-level UI state
// ─────────────────────────────────────────────────────────────────────────────

data class AppUiState(
    val phase: AppPhase = AppPhase.Home,
    val test: ParsedTest? = null,
    val session: TestSession? = null,
    val imageMap: Map<String, List<Bitmap>> = emptyMap(), // qid → sorted bitmaps
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val error: String? = null,
    val hasSavedSession: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

class MainViewModel : ViewModel() {

    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    // ── Initialise: check saved session ──────────────────────────────────────

    fun init(context: Context) {
        val saved = loadSession(context)
        _state.value = _state.value.copy(hasSavedSession = saved != null)
    }

    // ── Load ZIP from URI ─────────────────────────────────────────────────────

    fun loadZip(
        context: Context,
        uri: Uri,
        durationMin: Int = 180,
        answerKeyJson: String? = null
    ) {
        _state.value = _state.value.copy(isLoading = true, error = null, loadingMessage = "Reading ZIP archive…")
        viewModelScope.launch {
            try {
                val zipResult = withContext(Dispatchers.IO) {
                    loadZipFromUri(context, uri)
                }
                _state.value = _state.value.copy(loadingMessage = "Parsing JSON & grouping images…")

                val akOverride = answerKeyJson?.let { raw ->
                    try { org.json.JSONObject(raw) } catch (_: Exception) { null }
                }

                val buildResult = withContext(Dispatchers.Default) {
                    buildTest(zipResult, BuildOptions(durationMin, akOverride))
                }

                if (buildResult.test.flatQuestions.isEmpty())
                    throw Exception("No questions detected after parsing.")

                // Merge with saved session if present
                val priorSession = loadSession(context)
                val session = if (priorSession != null) {
                    mergeSessions(buildResult.test, priorSession)
                } else {
                    newSession(buildResult.test)
                }
                saveSession(context, session)

                _state.value = _state.value.copy(
                    phase          = AppPhase.Test,
                    test           = buildResult.test,
                    session        = session,
                    imageMap       = buildResult.imageMap,
                    isLoading      = false,
                    loadingMessage = "",
                    hasSavedSession = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading      = false,
                    loadingMessage = "",
                    error          = e.message ?: "Failed to load ZIP"
                )
            }
        }
    }

    // ── Session mutations (all update _state + schedule autosave) ─────────────

    /** Navigate to question by index in flatQuestions list. */
    fun goToIndex(context: Context, idx: Int) {
        val test = _state.value.test ?: return
        if (idx < 0 || idx >= test.flatQuestions.size) return
        val qid = test.flatQuestions[idx].id
        updateSession(context) { it.copy(currentQid = qid) }
    }

    fun goToQid(context: Context, qid: String) {
        updateSession(context) { it.copy(currentQid = qid) }
    }

    /** Mark current question as visited when user lands on it. */
    fun markVisited(context: Context, qid: String) {
        updateSession(context) { session ->
            val a = session.answers[qid] ?: AnswerState()
            if (a.status == QStatus.NOT_VISITED) {
                session.copy(answers = session.answers + (qid to a.copy(status = QStatus.VISITED)))
            } else session
        }
    }

    /** Update the in-progress answer value for current question. */
    fun updateAnswerValue(qid: String, valueInt: Int? = null, valueInts: List<Int>? = null, valueStr: String? = null) {
        updateSession(null) { session ->
            val a = session.answers[qid] ?: AnswerState()
            session.copy(answers = session.answers + (qid to a.copy(
                valueInt  = valueInt,
                valueInts = valueInts,
                valueStr  = valueStr
            )))
        }
    }

    /** Save & move to next question. */
    fun saveAndNext(context: Context) {
        val s   = _state.value.session ?: return
        val test = _state.value.test ?: return
        val qid  = s.currentQid ?: return
        val idx  = test.flatQuestions.indexOfFirst { it.id == qid }

        updateSession(context) { session ->
            val a = session.answers[qid] ?: AnswerState()
            val newStatus = if (a.isEmpty) QStatus.VISITED else QStatus.ANSWERED
            val updated = session.copy(answers = session.answers + (qid to a.copy(status = newStatus)))
            if (idx + 1 < test.flatQuestions.size)
                updated.copy(currentQid = test.flatQuestions[idx + 1].id)
            else updated
        }
    }

    /** Mark for review & move to next. */
    fun markAndNext(context: Context) {
        val s   = _state.value.session ?: return
        val test = _state.value.test ?: return
        val qid  = s.currentQid ?: return
        val idx  = test.flatQuestions.indexOfFirst { it.id == qid }

        updateSession(context) { session ->
            val a = session.answers[qid] ?: AnswerState()
            val newStatus = if (a.isEmpty) QStatus.MARKED else QStatus.MARKED_ANSWERED
            val updated = session.copy(answers = session.answers + (qid to a.copy(status = newStatus)))
            if (idx + 1 < test.flatQuestions.size)
                updated.copy(currentQid = test.flatQuestions[idx + 1].id)
            else updated
        }
    }

    /** Clear current response. */
    fun clearResponse(context: Context) {
        val qid = _state.value.session?.currentQid ?: return
        updateSession(context) { session ->
            val a = session.answers[qid] ?: AnswerState()
            val newStatus = if (a.status == QStatus.MARKED || a.status == QStatus.MARKED_ANSWERED)
                QStatus.MARKED else QStatus.VISITED
            session.copy(answers = session.answers + (qid to a.copy(
                valueInt = null, valueInts = null, valueStr = null, status = newStatus
            )))
        }
    }

    /** Timer tick – called every second. Auto-submits when time is up. */
    fun tick(context: Context): Boolean {
        val session = _state.value.session ?: return false
        if (session.endedAt != null) return false
        val qid = session.currentQid

        var newSession = session.copy(remainingSec = maxOf(0, session.remainingSec - 1))
        // Increment time-on-current-question
        if (qid != null && newSession.answers.containsKey(qid)) {
            val a = newSession.answers[qid]!!
            newSession = newSession.copy(
                answers = newSession.answers + (qid to a.copy(timeSpentSec = a.timeSpentSec + 1))
            )
        }

        if (newSession.remainingSec <= 0) {
            submitTest(context, newSession)
            return false // stop ticking
        }

        _state.value = _state.value.copy(session = newSession)
        return true
    }

    /** Submit test. */
    fun submitTest(context: Context, overrideSession: TestSession? = null) {
        val session = overrideSession ?: _state.value.session ?: return
        val finalSession = session.copy(endedAt = System.currentTimeMillis())
        saveSession(context, finalSession)
        _state.value = _state.value.copy(session = finalSession, phase = AppPhase.Result)
    }

    /** Apply admin override to a question. */
    fun applyOverride(context: Context, qid: String, override: QuestionOverride) {
        updateSession(context) { session ->
            val overrides = session.overrides.toMutableMap()
            val existing = overrides[qid]
            overrides[qid] = QuestionOverride(
                type                 = override.type ?: existing?.type,
                marks                = override.marks ?: existing?.marks,
                correctAnswerOptions = override.correctAnswerOptions ?: existing?.correctAnswerOptions,
                correctAnswerNat     = override.correctAnswerNat ?: existing?.correctAnswerNat,
                imageIds             = override.imageIds ?: existing?.imageIds
            )
            session.copy(overrides = overrides)
        }
    }

    /** Restart: clear everything and go back to home. */
    fun restart(context: Context) {
        clearSession(context)
        _state.value = AppUiState()
    }

    /** Discard saved session without restarting the UI. */
    fun discardSaved(context: Context) {
        clearSession(context)
        _state.value = _state.value.copy(hasSavedSession = false)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun updateSession(context: Context?, transform: (TestSession) -> TestSession) {
        val session = _state.value.session ?: return
        val updated = transform(session)
        _state.value = _state.value.copy(session = updated)
        context?.let { saveSession(it, updated) }
    }
}
