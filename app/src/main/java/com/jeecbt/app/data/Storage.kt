package com.jeecbt.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder

// ─────────────────────────────────────────────────────────────────────────────
// Session persistence via SharedPreferences + Gson
// Mirrors storage.ts (localStorage)
// ─────────────────────────────────────────────────────────────────────────────

private const val PREFS_NAME  = "cbt_prefs"
private const val SESSION_KEY = "cbt_session_v1"
private const val META_KEY    = "cbt_meta_v1"

private val gson: Gson = GsonBuilder().create()

fun loadSession(context: Context): TestSession? {
    return try {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(SESSION_KEY, null) ?: return null
        gson.fromJson(raw, TestSession::class.java)
    } catch (e: Exception) {
        null
    }
}

fun saveSession(context: Context, session: TestSession) {
    try {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(SESSION_KEY, gson.toJson(session)).apply()
    } catch (_: Exception) {}
}

fun clearSession(context: Context) {
    try {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(SESSION_KEY).apply()
    } catch (_: Exception) {}
}

fun newSession(test: ParsedTest): TestSession {
    val answers = test.flatQuestions.associate { q ->
        q.id to AnswerState()
    }
    return TestSession(
        testId      = test.title + "_" + System.currentTimeMillis(),
        startedAt   = System.currentTimeMillis(),
        remainingSec = test.durationSec,
        currentQid  = test.flatQuestions.firstOrNull()?.id,
        answers     = answers,
        overrides   = emptyMap()
    )
}

fun mergeSessions(parsed: ParsedTest, prior: TestSession): TestSession {
    val fresh = newSession(parsed)
    val mergedAnswers = fresh.answers.toMutableMap()
    for ((qid, state) in prior.answers) {
        if (mergedAnswers.containsKey(qid)) mergedAnswers[qid] = state
    }
    val currentQid = if (prior.currentQid != null && mergedAnswers.containsKey(prior.currentQid))
        prior.currentQid else fresh.currentQid

    return fresh.copy(
        currentQid  = currentQid,
        remainingSec = minOf(parsed.durationSec, prior.remainingSec.takeIf { it > 0 } ?: parsed.durationSec),
        startedAt   = prior.startedAt,
        answers     = mergedAnswers,
        overrides   = prior.overrides
    )
}

// Simple meta store (mirrors setMeta / getMeta)
fun setMeta(context: Context, key: String, value: String) {
    try {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(META_KEY, "{}") ?: "{}"
        val map = gson.fromJson(raw, Map::class.java).toMutableMap()
        map[key] = value
        prefs.edit().putString(META_KEY, gson.toJson(map)).apply()
    } catch (_: Exception) {}
}

fun getMeta(context: Context, key: String, fallback: String = ""): String {
    return try {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(META_KEY, "{}") ?: "{}"
        @Suppress("UNCHECKED_CAST")
        val map = gson.fromJson(raw, Map::class.java) as? Map<String, Any> ?: return fallback
        map[key]?.toString() ?: fallback
    } catch (_: Exception) { fallback }
}
