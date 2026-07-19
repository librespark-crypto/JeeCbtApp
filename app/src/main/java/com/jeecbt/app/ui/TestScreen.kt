package com.jeecbt.app.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeecbt.app.AppUiState
import com.jeecbt.app.MainViewModel
import com.jeecbt.app.data.*
import com.jeecbt.app.ui.theme.*
import kotlinx.coroutines.delay

// ─── Timer formatter ─────────────────────────────────────────────────────────
private fun fmt(sec: Int): String {
    val s = maxOf(0, sec)
    val h = s / 3600; val m = (s % 3600) / 60; val ss = s % 60
    return "%02d:%02d:%02d".format(h, m, ss)
}

// ─── Palette color per status ────────────────────────────────────────────────
@Composable
private fun palColor(status: QStatus?): Color = when (status) {
    QStatus.ANSWERED        -> Emerald500
    QStatus.MARKED          -> Purple500
    QStatus.MARKED_ANSWERED -> Color(0xFF7C3AED)
    QStatus.VISITED         -> Red500
    else                    -> Slate700
}

@Composable
fun TestScreen(viewModel: MainViewModel, state: AppUiState, context: Context) {
    val test    = state.test    ?: return
    val session = state.session ?: return

    val currentQid  = session.currentQid ?: test.flatQuestions.first().id
    val currentIdx  = test.flatQuestions.indexOfFirst { it.id == currentQid }
    val currentQ    = test.flatQuestions[currentIdx]
    val effectiveQ  = remember(currentQ, session.overrides) {
        session.overrides[currentQ.id]?.let { currentQ.withOverride(it) } ?: currentQ
    }
    val bitmaps: List<Bitmap> = state.imageMap[currentQid] ?: emptyList()

    var showPalette     by remember { mutableStateOf(false) }
    var showAdmin       by remember { mutableStateOf(false) }
    var showSubmitDlg   by remember { mutableStateOf(false) }

    // Mark visited on question change
    LaunchedEffect(currentQid) { viewModel.markVisited(context, currentQid) }

    // Countdown timer
    LaunchedEffect(session.endedAt) {
        if (session.endedAt != null) return@LaunchedEffect
        while (true) {
            delay(1000L)
            val keepGoing = viewModel.tick(context)
            if (!keepGoing) break
        }
    }

    val isLowTime   = session.remainingSec < 300
    val timerColor  = if (isLowTime) Red300 else Blue300
    val timerBg     = if (isLowTime) Red500.copy(alpha = 0.15f) else Blue500.copy(alpha = 0.10f)
    val timerBorder = if (isLowTime) Red500.copy(alpha = 0.4f)  else Blue500.copy(alpha = 0.3f)

    // Stats for palette
    var answered = 0; var marked = 0; var ansMark = 0; var notVisited = 0; var visited = 0
    for (q in test.flatQuestions) {
        when (session.answers[q.id]?.status ?: QStatus.NOT_VISITED) {
            QStatus.ANSWERED        -> answered++
            QStatus.MARKED          -> marked++
            QStatus.MARKED_ANSWERED -> ansMark++
            QStatus.VISITED         -> visited++
            else                    -> notVisited++
        }
    }

    Scaffold(
        containerColor = BgDeep,
        topBar = {
            Column(
                modifier = Modifier
                    .background(BgDeep)
                    .statusBarsPadding()
            ) {
                // Row 1: logo + title + timer + icons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                            .background(Blue500.copy(alpha = 0.2f))
                            .border(1.dp, Blue500.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text("J", color = Blue300, fontWeight = FontWeight.Bold) }
                    Column(Modifier.weight(1f)) {
                        Text("JEE CBT Arena", color = Slate400, fontSize = 10.sp)
                        Text(test.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                    }
                    // Timer chip
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(timerBg)
                            .border(1.dp, timerBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = timerColor, modifier = Modifier.size(14.dp))
                        Text(fmt(session.remainingSec), color = timerColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    IconButton(onClick = { showAdmin = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Admin", tint = Slate400)
                    }
                    IconButton(onClick = { showPalette = true }) {
                        Icon(Icons.Default.GridView, contentDescription = "Palette", tint = Slate400)
                    }
                }
                // Row 2: subject/section tabs
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    test.subjects.forEach { sub ->
                        val active = sub.name == currentQ.subject
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) Blue500 else Slate800.copy(alpha = 0.5f))
                                .border(1.dp, if (active) Blue400 else Slate700, RoundedCornerShape(8.dp))
                                .clickable {
                                    val first = test.flatQuestions.firstOrNull { it.subject == sub.name }
                                    if (first != null) viewModel.goToQid(context, first.id)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) { Text(sub.name, color = if (active) Color.White else Slate300, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    }
                    // Section tabs for current subject
                    test.subjects.find { it.name == currentQ.subject }?.sections?.forEach { sec ->
                        val active = sec.name == currentQ.section
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) Blue500.copy(alpha = 0.15f) else Color.Transparent)
                                .border(1.dp, if (active) Blue500.copy(alpha = 0.5f) else Slate800, RoundedCornerShape(6.dp))
                                .clickable {
                                    val first = test.flatQuestions.firstOrNull { it.subject == currentQ.subject && it.section == sec.name }
                                    if (first != null) viewModel.goToQid(context, first.id)
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) { Text(sec.name, color = if (active) Blue300 else Slate400, fontSize = 10.sp) }
                    }
                }
                HorizontalDivider(color = Slate800)
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(BgDeep)
                    .navigationBarsPadding()
            ) {
                HorizontalDivider(color = Slate800)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionBtn("Mark & Next", Purple500.copy(alpha = 0.15f), Purple500.copy(alpha = 0.4f), Purple300, Modifier.weight(1f)) {
                        viewModel.markAndNext(context)
                    }
                    ActionBtn("Clear", Slate800.copy(alpha = 0.5f), Slate700, Slate300, Modifier.weight(1f)) {
                        viewModel.clearResponse(context)
                    }
                    ActionBtn("← Prev", Slate800.copy(alpha = 0.5f), Slate700, Slate300, Modifier.weight(1f),
                        enabled = currentIdx > 0) {
                        viewModel.goToIndex(context, currentIdx - 1)
                    }
                    ActionBtn("Save & Next →", Blue500, Blue400, Color.White, Modifier.weight(1f)) {
                        viewModel.saveAndNext(context)
                    }
                }
                // Submit button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Red500.copy(alpha = 0.10f))
                        .border(topWidth = 1.dp, color = Red500.copy(alpha = 0.3f), shape = RoundedCornerShape(0.dp))
                        .clickable { showSubmitDlg = true }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⏻  Submit Test", color = Red300, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            // Question header chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Chip("Question ${effectiveQ.qno}", Blue500.copy(alpha = 0.15f), Blue500.copy(alpha = 0.3f), Blue300)
                Chip(effectiveQ.type.name, Slate800.copy(alpha = 0.7f), Slate700, Slate300)
                Chip("+${effectiveQ.marks.cm} / ${effectiveQ.marks.im}", Emerald500.copy(alpha = 0.10f), Emerald500.copy(alpha = 0.3f), Emerald300)
                Spacer(Modifier.weight(1f))
                Text(effectiveQ.section, color = Slate500, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterVertically))
            }

            Spacer(Modifier.height(10.dp))

            // Question image
            QuestionImage(bitmaps = bitmaps, modifier = Modifier.fillMaxWidth())

            // Answer panel
            AnswerPanel(
                question    = effectiveQ,
                state       = session.answers[currentQid] ?: AnswerState(),
                onMcqSelect = { opt -> viewModel.updateAnswerValue(currentQid, valueInt = opt) },
                onMsqToggle = { opt ->
                    val cur = (session.answers[currentQid]?.valueInts ?: emptyList()).toMutableList()
                    if (opt in cur) cur.remove(opt) else cur.add(opt)
                    viewModel.updateAnswerValue(currentQid, valueInts = cur.sorted())
                },
                onNatKey    = { key ->
                    val cur = session.answers[currentQid]?.valueStr ?: ""
                    val next = when (key) {
                        "⌫"  -> cur.dropLast(1)
                        "CLR" -> ""
                        "."   -> if ("." in cur) cur else cur + key
                        "-"   -> if (cur.isEmpty()) "-" else cur
                        else  -> cur + key
                    }
                    viewModel.updateAnswerValue(currentQid, valueStr = next)
                }
            )

            Spacer(Modifier.height(80.dp))
        }
    }

    // ── Question Palette Drawer ───────────────────────────────────────────────
    if (showPalette) {
        val sectionQs = test.flatQuestions.filter { it.subject == currentQ.subject && it.section == currentQ.section }
        PaletteDrawer(
            sectionQuestions = sectionQs,
            answers          = session.answers,
            activeQid        = currentQid,
            answered = answered, marked = marked, ansMark = ansMark,
            notVisited = notVisited, visited = visited,
            activeSection    = currentQ.section,
            onJump           = { qid -> viewModel.goToQid(context, qid); showPalette = false },
            onSubmit         = { showPalette = false; showSubmitDlg = true },
            onClose          = { showPalette = false }
        )
    }

    // ── Submit confirmation ───────────────────────────────────────────────────
    if (showSubmitDlg) {
        AlertDialog(
            onDismissRequest = { showSubmitDlg = false },
            containerColor   = Slate800,
            title = { Text("Submit Test?", color = Color.White, fontWeight = FontWeight.Bold) },
            text  = {
                Column {
                    Text("You won't be able to change your answers after submission.", color = Slate400, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatBox("${answered + ansMark}", "Answered", Emerald500.copy(alpha = 0.15f), Emerald300, Modifier.weight(1f))
                        StatBox("${test.flatQuestions.size - answered - ansMark}", "Unanswered", Slate700.copy(alpha = 0.3f), Slate300, Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.submitTest(context) },
                    colors  = ButtonDefaults.buttonColors(containerColor = Blue500)
                ) { Text("Yes, Submit", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDlg = false }) { Text("Cancel", color = Slate400) }
            }
        )
    }

    // ── Admin override panel ──────────────────────────────────────────────────
    if (showAdmin) {
        AdminPanel(
            question = effectiveQ,
            onApply  = { override -> viewModel.applyOverride(context, currentQ.id, override) },
            onClose  = { showAdmin = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Small reusable composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun Chip(text: String, bg: Color, border: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) { Text(text, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun ActionBtn(label: String, bg: Color, border: Color, textColor: Color, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) bg else bg.copy(alpha = 0.3f))
            .border(1.dp, if (enabled) border else border.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(label, color = if (enabled) textColor else textColor.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun StatBox(value: String, label: String, bg: Color, textColor: Color, modifier: Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(bg).padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = textColor, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text(label, color = textColor.copy(alpha = 0.7f), fontSize = 11.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Palette Drawer (bottom sheet style)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PaletteDrawer(
    sectionQuestions: List<ParsedQuestion>,
    answers: Map<String, AnswerState>,
    activeQid: String,
    answered: Int, marked: Int, ansMark: Int, notVisited: Int, visited: Int,
    activeSection: String,
    onJump: (String) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable { onClose() },
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(300.dp)
                .clickable(enabled = false) {},
            color = BgDeep
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Question Palette", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    TextButton(onClick = onClose) { Text("✕", color = Slate400) }
                }

                Spacer(Modifier.height(12.dp))

                // Stats grid
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MiniStat("Answered", answered, Emerald500, Modifier.weight(1f))
                    MiniStat("Visited", visited, Red500, Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MiniStat("Not Visited", notVisited, Slate600, Modifier.weight(1f))
                    MiniStat("Marked", marked, Purple500, Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                MiniStat("Answered & Marked", ansMark, Color(0xFF7C3AED), Modifier.fillMaxWidth())

                Spacer(Modifier.height(16.dp))

                Text(activeSection, color = Slate500, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))

                // Question number grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(sectionQuestions) { q ->
                        val status = answers[q.id]?.status ?: QStatus.NOT_VISITED
                        val isActive = q.id == activeQid
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(palColor(status))
                                .then(if (isActive) Modifier.border(2.dp, Blue400, RoundedCornerShape(6.dp)) else Modifier)
                                .clickable { onJump(q.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(q.qno.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Legend
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        Emerald500 to "Answered",
                        Red500     to "Not Answered",
                        Slate600   to "Not Visited",
                        Purple500  to "Marked for Review",
                        Color(0xFF7C3AED) to "Answered & Marked"
                    ).forEach { (color, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(color))
                            Text(label, color = Slate400, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onSubmit,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue500)
                ) { Text("Submit Test", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: Int, color: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Text(label, color = color.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text(value.toString(), color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// Needed for border on one side only workaround
fun Modifier.border(topWidth: androidx.compose.ui.unit.Dp, color: Color, shape: androidx.compose.ui.graphics.Shape): Modifier =
    this.border(androidx.compose.foundation.BorderStroke(topWidth, color), shape)
