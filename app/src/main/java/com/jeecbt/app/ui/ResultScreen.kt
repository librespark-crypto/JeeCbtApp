package com.jeecbt.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeecbt.app.AppUiState
import com.jeecbt.app.data.*
import com.jeecbt.app.ui.theme.*

private fun fmtTime(sec: Int): String {
    val m = sec / 60; val s = sec % 60
    return "${m}m ${s}s"
}

@Composable
fun ResultScreen(state: AppUiState, onRestart: () -> Unit) {
    val test    = state.test    ?: return
    val session = state.session ?: return

    val result by remember(test, session) { mutableStateOf(computeScore(test, session)) }

    var filter  by remember { mutableStateOf<String>("all") }
    var openQid by remember { mutableStateOf<String?>(null) }

    val filtered = remember(filter, result) {
        result.perQuestion.filter { r ->
            when (filter) {
                "correct"     -> r.status == QResultStatus.CORRECT
                "wrong"       -> r.status == QResultStatus.WRONG
                "unattempted" -> r.status == QResultStatus.UNATTEMPTED
                else          -> true
            }
        }
    }

    val totalTime = result.perQuestion.sumOf { it.state.timeSpentSec }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(50))
                .background(Emerald500.copy(alpha = 0.15f))
                .border(1.dp, Emerald500.copy(alpha = 0.3f), RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) { Text("TEST SUBMITTED", color = Emerald300, fontSize = 11.sp, fontWeight = FontWeight.Bold) }

        Spacer(Modifier.height(8.dp))
        Text(test.title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text("Detailed analytics & question-wise breakdown", color = Slate400, fontSize = 13.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(20.dp))

        // ── Big score card ───────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(20.dp),
            color    = Slate800
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("TOTAL SCORE", color = Blue300, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        result.total.toString(),
                        color      = if (result.total >= 0) Emerald300 else Red300,
                        fontSize   = 56.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(" / ${result.maxTotal}", color = Slate500, fontSize = 24.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Slate700.copy(alpha = 0.3f))
                        .border(1.dp, Slate700, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "Accuracy ${"%.1f".format(result.accuracy)}% · Time ${fmtTime(totalTime)}",
                        color = Slate300, fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Quick stats ──────────────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigStat("Correct",     result.correct,     Emerald300, Emerald500, Modifier.weight(1f))
            BigStat("Wrong",       result.wrong,       Red300,     Red500,     Modifier.weight(1f))
            BigStat("Partial",     result.partial,     Amber300,   Amber500,   Modifier.weight(1f))
            BigStat("Skipped",     result.unattempted, Slate300,   Slate600,   Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        // ── Section-wise ─────────────────────────────────────────────────────
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Slate800) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Section-wise Performance", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                result.perSection.forEach { sec ->
                    val pct = if (sec.maxScore > 0) (sec.score.toFloat() / sec.maxScore * 100f).coerceIn(0f, 100f) else 0f
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Slate900.copy(alpha = 0.4f))
                            .border(1.dp, Slate700, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Text(sec.subject, color = Slate400, fontSize = 11.sp)
                                Text(sec.section, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Text(
                                "${sec.score}",
                                color = if (sec.score >= 0) Emerald300 else Red300,
                                fontWeight = FontWeight.ExtraBold, fontSize = 20.sp
                            )
                            Text("/${sec.maxScore}", color = Slate500, fontSize = 13.sp, modifier = Modifier.align(Alignment.Bottom).padding(bottom = 2.dp, start = 2.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50))
                                .background(Slate700)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(pct / 100f).fillMaxHeight().clip(RoundedCornerShape(50))
                                    .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Blue500, Emerald500)))
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("✅ ${sec.correct}", color = Slate400, fontSize = 11.sp)
                            Text("❌ ${sec.wrong}",   color = Slate400, fontSize = 11.sp)
                            Text("~ ${sec.partial}",  color = Slate400, fontSize = 11.sp)
                            Text("· ${sec.unattempted} skipped", color = Slate400, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Filter tabs ──────────────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("all" to "All", "correct" to "Correct", "wrong" to "Wrong", "unattempted" to "Skipped")
                .forEach { (k, l) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (filter == k) Blue500 else Slate800.copy(alpha = 0.5f))
                            .border(1.dp, if (filter == k) Blue400 else Slate700, RoundedCornerShape(8.dp))
                            .clickable { filter = k }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) { Text(l, color = if (filter == k) Color.White else Slate300, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                }
            Spacer(Modifier.weight(1f))
            Text("${filtered.size} questions", color = Slate500, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterVertically))
        }

        Spacer(Modifier.height(10.dp))

        // ── Per-question list ─────────────────────────────────────────────────
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Slate800) {
            Column {
                filtered.forEachIndexed { i, r ->
                    val leftColor = when (r.status) {
                        QResultStatus.CORRECT     -> Emerald500
                        QResultStatus.WRONG       -> Red500
                        QResultStatus.PARTIAL     -> Amber500
                        QResultStatus.UNATTEMPTED -> Slate600
                    }
                    val isOpen = openQid == r.q.id

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (i > 0) Modifier.border(topWidth = 1.dp, color = Slate700, shape = RoundedCornerShape(0.dp)) else Modifier)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(leftColor.copy(alpha = 0.05f))
                                .border(start = 4.dp, color = leftColor, shape = RoundedCornerShape(0.dp))
                                .clickable { openQid = if (isOpen) null else r.q.id }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp))
                                    .background(Slate900).border(1.dp, Slate700, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Q", color = Slate500, fontSize = 10.sp, lineHeight = 12.sp)
                                    Text(r.q.qno.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 16.sp)
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(r.q.section, color = Slate400, fontSize = 11.sp, maxLines = 1)
                                Text("${r.q.type.name} · ${fmtTime(r.state.timeSpentSec)}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            Text(
                                if (r.awarded > 0) "+${r.awarded}" else r.awarded.toString(),
                                color = if (r.awarded > 0) Emerald300 else if (r.awarded < 0) Red300 else Slate300,
                                fontWeight = FontWeight.Bold, fontSize = 15.sp
                            )
                            Text(if (isOpen) "▴" else "▾", color = Slate500, fontSize = 11.sp)
                        }

                        // Expanded detail
                        if (isOpen) {
                            val bitmaps: List<Bitmap> = state.imageMap[r.q.id] ?: emptyList()
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Image preview
                                    if (bitmaps.isNotEmpty()) {
                                        Column(
                                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                                .background(Color.White).heightIn(max = 180.dp).verticalScroll(rememberScrollState())
                                        ) {
                                            bitmaps.forEach { bmp ->
                                                Image(bitmap = bmp.asImageBitmap(), contentDescription = null,
                                                    contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth())
                                            }
                                        }
                                    }
                                    // Info rows
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val yourAnswerStr = when {
                                            r.state.isEmpty -> "—"
                                            r.q.type == QType.NAT -> r.state.valueStr ?: "—"
                                            r.state.valueInts != null -> r.state.valueInts.joinToString(", ")
                                            r.state.valueInt != null  -> r.state.valueInt.toString()
                                            else -> "—"
                                        }
                                        val correctStr = if (r.q.type == QType.NAT) r.q.correctAnswerNat
                                            else r.q.correctAnswerOptions.joinToString(", ")

                                        InfoRow("Your Answer",  yourAnswerStr, if (r.status == QResultStatus.CORRECT) Emerald300 else Red300)
                                        InfoRow("Correct Answer", correctStr,  Emerald300)
                                        InfoRow("Marking", "+${r.q.marks.cm}/${r.q.marks.im}${r.q.marks.pm?.let { " +$it partial" } ?: ""}", Blue300)
                                        InfoRow("Awarded", (if (r.awarded > 0) "+${r.awarded}" else r.awarded.toString()),
                                            if (r.awarded > 0) Emerald300 else if (r.awarded < 0) Red300 else Slate300)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── CTA buttons ──────────────────────────────────────────────────────
        Button(
            onClick  = onRestart,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Blue500)
        ) { Text("Take Another Test", fontWeight = FontWeight.Bold, fontSize = 16.sp) }

        Spacer(Modifier.height(16.dp))
        Text("JEE CBT Arena · session results stored locally", color = Slate600, fontSize = 11.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun BigStat(label: String, value: Int, textColor: Color, bgColor: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor.copy(alpha = 0.15f))
            .border(1.dp, bgColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = textColor.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        Text(value.toString(), color = textColor, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(Slate900.copy(alpha = 0.4f)).border(1.dp, Slate700, RoundedCornerShape(8.dp)).padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = valueColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

// Border extension helpers for per-side borders
fun Modifier.border(start: androidx.compose.ui.unit.Dp, color: Color, shape: androidx.compose.ui.graphics.Shape): Modifier =
    this.border(androidx.compose.foundation.BorderStroke(start, color), shape)
