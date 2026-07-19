package com.jeecbt.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

// ─────────────────────────────────────────────────────────────────────────────
// Filename parsing   (mirrors parser.ts::parseImageFilename)
// Format: "<section>__--<qno>--__<part>.png"  OR "<section>--<qno>--<part>.png"
// ─────────────────────────────────────────────────────────────────────────────

data class ParsedFilename(
    val sectionName: String,
    val questionNumber: Int,
    val partNumber: Int
)

fun parseImageFilename(name: String): ParsedFilename? {
    val base = name.split("/").last()
    val dot = base.lastIndexOf('.')
    val stem = if (dot > 0) base.substring(0, dot) else base
    val parts = stem.split("--").filter { it.isNotEmpty() }

    return when {
        parts.size < 2 -> null
        parts.size == 2 -> {
            val section = parts[0].trim('_', ' ').trim()
            val qNum = Regex("\\d+").find(parts[1])?.value?.toIntOrNull() ?: return null
            if (section.isEmpty() || qNum == 0) null
            else ParsedFilename(section, qNum, 1)
        }
        else -> {
            val sectionName = parts[0].trim('_', ' ').trim()
            val qNum = Regex("\\d+").find(parts[1])?.value?.toIntOrNull() ?: return null
            val partNum = Regex("\\d+").find(parts.last())?.value?.toIntOrNull() ?: 1
            ParsedFilename(sectionName, qNum, partNum)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ZIP loading result
// bitmapCache: "sectionName|qno|part"  →  Bitmap
// groupMap:    "sectionName|qno"       →  sorted list of (partNo, bitmapKey)
// ─────────────────────────────────────────────────────────────────────────────

data class ZipLoadResult(
    val json: JSONObject,
    val groupMap: Map<String, List<Pair<Int, String>>>,  // section|qno → [(part, key)]
    val bitmapCache: Map<String, Bitmap>                 // key → Bitmap
)

fun loadZipFromUri(context: Context, uri: Uri): ZipLoadResult {
    val inputStream = context.contentResolver.openInputStream(uri)
        ?: throw Exception("Cannot open selected file")

    val rawEntries = mutableMapOf<String, ByteArray>()

    ZipInputStream(inputStream).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) {
                val baos = ByteArrayOutputStream()
                zis.copyTo(baos)
                rawEntries[entry.name] = baos.toByteArray()
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
    }

    // Find JSON
    val jsonKey = rawEntries.keys.firstOrNull { it.endsWith(".json", ignoreCase = true) }
        ?: throw Exception("No JSON configuration found inside ZIP")
    val jsonText = rawEntries[jsonKey]!!.toString(Charsets.UTF_8)
    val json = try {
        JSONObject(jsonText)
    } catch (e: Exception) {
        throw Exception("Failed to parse JSON inside ZIP: ${e.message}")
    }

    // Parse images
    val imageKeys = rawEntries.keys.filter { name ->
        name.lowercase().let { it.endsWith(".png") || it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".webp") }
    }

    val bitmapCache = mutableMapOf<String, Bitmap>()
    val groupMutable = mutableMapOf<String, MutableList<Pair<Int, String>>>()

    for (name in imageKeys) {
        val parsed = parseImageFilename(name) ?: continue
        val bytes = rawEntries[name] ?: continue
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: continue

        val cacheKey = "${parsed.sectionName}|${parsed.questionNumber}|${parsed.partNumber}"
        bitmapCache[cacheKey] = bitmap

        val groupKey = "${parsed.sectionName}|${parsed.questionNumber}"
        groupMutable.getOrPut(groupKey) { mutableListOf() }.add(parsed.partNumber to cacheKey)
    }

    // Sort each group by part number
    val groupMap = groupMutable.mapValues { (_, list) -> list.sortedBy { it.first } }

    return ZipLoadResult(json, groupMap, bitmapCache)
}

// ─────────────────────────────────────────────────────────────────────────────
// JSON traversal helpers  (mirrors parser.ts::findRoots, normalizeType, etc.)
// ─────────────────────────────────────────────────────────────────────────────

private fun JSONObject.keysList(): List<String> {
    val result = mutableListOf<String>()
    val iter = keys()
    while (iter.hasNext()) result.add(iter.next())
    return result
}

private fun findRoots(json: JSONObject): Pair<JSONObject?, JSONObject?> {
    var cropper: JSONObject? = null
    var answerKey: JSONObject? = null

    fun walk(obj: JSONObject) {
        for (key in obj.keysList()) {
            when (key) {
                "pdfCropperData", "questionData", "questions" ->
                    if (cropper == null) cropper = obj.optJSONObject(key)
                "testAnswerKey", "answerKey" ->
                    if (answerKey == null) answerKey = obj.optJSONObject(key)
            }
            obj.optJSONObject(key)?.let { walk(it) }
        }
    }

    // Try direct first
    cropper = json.optJSONObject("pdfCropperData")
        ?: json.optJSONObject("questionData")
        ?: json.optJSONObject("questions")
    answerKey = json.optJSONObject("testAnswerKey") ?: json.optJSONObject("answerKey")

    // Deep walk if not found
    if (cropper == null || answerKey == null) walk(json)

    return cropper to answerKey
}

private fun normalizeType(t: Any?): QType {
    val v = t?.toString()?.lowercase() ?: return QType.MCQ
    return when {
        "msq" in v || "multi" in v -> QType.MSQ
        "nat" in v || "numerical" in v || "integer" in v -> QType.NAT
        else -> QType.MCQ
    }
}

private fun normalizeMarks(m: JSONObject?, type: QType): Marks {
    val cm = (m?.optInt("cm", Int.MIN_VALUE)?.takeIf { it != Int.MIN_VALUE }
        ?: m?.optInt("correctMarks", Int.MIN_VALUE)?.takeIf { it != Int.MIN_VALUE }
        ?: 4)
    val im = (m?.optInt("im", Int.MIN_VALUE)?.takeIf { it != Int.MIN_VALUE }
        ?: m?.optInt("incorrectMarks", Int.MIN_VALUE)?.takeIf { it != Int.MIN_VALUE }
        ?: if (type == QType.NAT) 0 else -1)
    val pm = if (m?.has("pm") == true) m.optInt("pm") else if (type == QType.MSQ) 1 else null
    return Marks(cm, im, pm)
}

private fun normalizeCorrectOptions(a: Any?): List<Int> {
    if (a == null) return emptyList()
    return when (a) {
        is Int -> listOf(a)
        is String -> a.split(Regex("[,\\s]+")).mapNotNull { it.trim().toIntOrNull() }
        else -> emptyList()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Build ParsedTest from loaded ZIP   (mirrors parser.ts::buildTest)
// Returns ParsedTest + the image bitmap map (qid → ordered bitmaps)
// ─────────────────────────────────────────────────────────────────────────────

data class BuildOptions(
    val defaultDurationMin: Int = 180,
    val answerKeyOverride: JSONObject? = null
)

data class BuildResult(
    val test: ParsedTest,
    val imageMap: Map<String, List<Bitmap>>   // qid → ordered List<Bitmap>
)

fun buildTest(zipResult: ZipLoadResult, opts: BuildOptions = BuildOptions()): BuildResult {
    val (json, groupMap, bitmapCache) = zipResult
    val (cropperRaw, answerKeyRaw) = findRoots(json)

    val finalAnswerKey = opts.answerKeyOverride ?: answerKeyRaw
    val cropper = cropperRaw ?: throw Exception("pdfCropperData not found in JSON")

    // Title & duration
    val title = json.optString("testName").ifEmpty {
        json.optString("title").ifEmpty { json.optString("name").ifEmpty { "JEE Mock Test" } }
    }
    val durationMin = (json.optInt("duration", 0).takeIf { it > 0 }
        ?: json.optInt("durationMin", 0).takeIf { it > 0 }
        ?: opts.defaultDurationMin)
    val durationSec = durationMin * 60

    val subjects = mutableListOf<ParsedSubject>()
    val questionImageMap = mutableMapOf<String, List<Bitmap>>()

    for (subjectKey in cropper.keysList()) {
        val subjectNode = cropper.optJSONObject(subjectKey) ?: continue
        val sections = mutableListOf<ParsedSection>()

        for (sectionKey in subjectNode.keysList()) {
            val sectionNode = subjectNode.optJSONObject(sectionKey) ?: continue
            val questions = mutableListOf<ParsedQuestion>()

            val qkeys = sectionNode.keysList().sortedBy { it.toIntOrNull() ?: 0 }

            for (qk in qkeys) {
                val qNode = sectionNode.optJSONObject(qk) ?: continue
                val qno = qNode.optInt("que", 0).takeIf { it != 0 } ?: qk.toIntOrNull() ?: 0
                val type = normalizeType(qNode.opt("type"))
                val marks = normalizeMarks(qNode.optJSONObject("marks"), type)

                // Pull answer from key (override has priority)
                val akNode = finalAnswerKey
                    ?.optJSONObject(subjectKey)
                    ?.optJSONObject(sectionKey)
                    ?.optJSONObject(qk)
                val rawCorrect = akNode?.opt("correctAnswer")
                    ?: qNode.opt("answerOptions")
                    ?: qNode.opt("correctAnswer")

                val correctOptions: List<Int>
                val correctNat: String
                if (type == QType.NAT) {
                    correctNat = rawCorrect?.toString() ?: ""
                    correctOptions = emptyList()
                } else {
                    correctOptions = normalizeCorrectOptions(rawCorrect)
                    correctNat = ""
                }

                // Find images – exact match first, then fuzzy
                val exactKey = "$sectionKey|$qno"
                val group = groupMap[exactKey] ?: run {
                    val altKey = groupMap.keys.firstOrNull { k ->
                        val parts = k.split("|")
                        parts.size == 2 &&
                        parts[1].toIntOrNull() == qno &&
                        parts[0].replace(Regex("\\s+"), "").lowercase() ==
                            sectionKey.replace(Regex("\\s+"), "").lowercase()
                    }
                    altKey?.let { groupMap[it] }
                }

                val qid = "$subjectKey|$sectionKey|$qno"
                val imageIds = group?.map { (_, key) -> key } ?: emptyList()
                val bitmaps = imageIds.mapNotNull { bitmapCache[it] }

                questionImageMap[qid] = bitmaps

                questions.add(
                    ParsedQuestion(
                        id = qid,
                        subject = subjectKey,
                        section = sectionKey,
                        qno = qno,
                        type = type,
                        marks = marks,
                        imageIds = imageIds,
                        correctAnswerOptions = correctOptions,
                        correctAnswerNat = correctNat
                    )
                )
            }
            sections.add(ParsedSection(sectionKey, subjectKey, questions))
        }
        subjects.add(ParsedSubject(subjectKey, sections))
    }

    val flat = subjects.flatMap { s -> s.sections.flatMap { sec -> sec.questions } }
    return BuildResult(ParsedTest(title, durationSec, subjects, flat), questionImageMap)
}
