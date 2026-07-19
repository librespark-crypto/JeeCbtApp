package com.jeecbt.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeecbt.app.data.AnswerState
import com.jeecbt.app.data.ParsedQuestion
import com.jeecbt.app.data.QType
import com.jeecbt.app.ui.theme.*

private val OPTION_LETTERS = listOf("A", "B", "C", "D", "E", "F")

@Composable
fun AnswerPanel(
    question: ParsedQuestion,
    state: AnswerState,
    onMcqSelect: (Int) -> Unit,
    onMsqToggle: (Int) -> Unit,
    onNatKey: (String) -> Unit
) {
    when (question.type) {
        QType.MCQ -> McqPanel(state, onMcqSelect)
        QType.MSQ -> MsqPanel(state, onMsqToggle)
        QType.NAT -> NatPanel(state, onNatKey)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MCQ – single select (4 options)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun McqPanel(state: AnswerState, onSelect: (Int) -> Unit) {
    val selected = state.valueInt ?: (state.valueInts?.firstOrNull())

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
        (1..4).forEach { opt ->
            val isSelected = selected == opt
            OptionRow(
                letter     = OPTION_LETTERS[opt - 1],
                isSelected = isSelected,
                isCheckbox = false,
                onClick    = { onSelect(opt) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MSQ – multi select
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MsqPanel(state: AnswerState, onToggle: (Int) -> Unit) {
    val selected = state.valueInts ?: emptyList()

    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            "Multiple Select — choose ALL correct options",
            color = Amber300, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            (1..4).forEach { opt ->
                val isSelected = opt in selected
                OptionRow(
                    letter     = OPTION_LETTERS[opt - 1],
                    isSelected = isSelected,
                    isCheckbox = true,
                    onClick    = { onToggle(opt) }
                )
            }
        }
    }
}

@Composable
private fun OptionRow(letter: String, isSelected: Boolean, isCheckbox: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Blue500.copy(alpha = 0.20f) else Slate900.copy(alpha = 0.4f))
            .border(1.dp, if (isSelected) Blue400 else Slate700, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) Blue500 else Slate800)
                .border(1.dp, if (isSelected) Blue400 else Slate700, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (isCheckbox && isSelected) "✓" else letter,
                color = if (isSelected) Color.White else Slate300,
                fontWeight = FontWeight.Bold, fontSize = 13.sp
            )
        }
        Text("Option $letter", color = if (isSelected) Color.White else Slate300, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NAT – numerical keypad
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NatPanel(state: AnswerState, onKey: (String) -> Unit) {
    val value = state.valueStr ?: ""

    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            "Numerical Answer Type — no negative marking",
            color = Emerald300, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        // Display field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Slate900.copy(alpha = 0.6f))
                .border(1.dp, Slate700, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text(
                text = value.ifEmpty { "Enter your answer" },
                color = if (value.isEmpty()) Slate600 else Color.White,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(12.dp))

        // Keypad
        val keys = listOf(
            listOf("7", "8", "9", "⌫", "CLR"),
            listOf("4", "5", "6", ".", "-"),
            listOf("1", "2", "3", "0", "00")
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            keys.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { key ->
                        NatKey(key = key, modifier = Modifier.weight(1f)) { onKey(key) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NatKey(key: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val (bg, border, textColor) = when (key) {
        "⌫"  -> Triple(Amber500.copy(alpha = 0.15f), Amber500.copy(alpha = 0.4f), Amber300)
        "CLR" -> Triple(Red500.copy(alpha = 0.15f),  Red500.copy(alpha = 0.4f),  Red300)
        else -> Triple(Slate800.copy(alpha = 0.7f),  Slate700,                   Color.White)
    }
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(key, color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}
