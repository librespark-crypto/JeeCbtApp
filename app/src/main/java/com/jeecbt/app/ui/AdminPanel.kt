package com.jeecbt.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jeecbt.app.data.*
import com.jeecbt.app.ui.theme.*

@Composable
fun AdminPanel(
    question: ParsedQuestion,
    onApply: (QuestionOverride) -> Unit,
    onClose: () -> Unit
) {
    var type    by remember(question) { mutableStateOf(question.type) }
    var cm      by remember(question) { mutableStateOf(question.marks.cm.toString()) }
    var im      by remember(question) { mutableStateOf(question.marks.im.toString()) }
    var pm      by remember(question) { mutableStateOf((question.marks.pm ?: 0).toString()) }
    var correct by remember(question) {
        val v = if (question.type == QType.NAT) question.correctAnswerNat
                else question.correctAnswerOptions.joinToString(",")
        mutableStateOf(v)
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable { onClose() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
                    .wrapContentHeight(),
                shape    = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color    = Slate800
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Title bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ADMIN OVERRIDE", color = Blue400, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text("${question.section} · Q${question.qno}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Type selector
                    FieldLabel("QUESTION TYPE")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QType.entries.forEach { t ->
                            val selected = type == t
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) Blue500 else Slate900)
                                    .border(1.dp, if (selected) Blue400 else Slate700, RoundedCornerShape(10.dp))
                                    .clickable { type = t }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(t.name, color = if (selected) Color.White else Slate300, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Correct answer
                    FieldLabel("CORRECT ANSWER")
                    AdminTextField(
                        value       = correct,
                        onValueChange = { correct = it },
                        placeholder = if (type == QType.NAT) "e.g. 6 or 3.14" else "e.g. 2 (MCQ) or 1,2,4 (MSQ)"
                    )

                    Spacer(Modifier.height(12.dp))

                    // Marks
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            FieldLabel("CORRECT (+)")
                            AdminTextField(cm, { cm = it }, "4", KeyboardType.Number)
                        }
                        Column(Modifier.weight(1f)) {
                            FieldLabel("INCORRECT (−)")
                            AdminTextField(im, { im = it }, "-1", KeyboardType.Number)
                        }
                        Column(Modifier.weight(1f)) {
                            FieldLabel("PARTIAL")
                            AdminTextField(pm, { pm = it }, "1", KeyboardType.Number)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onClose,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300)
                        ) { Text("Cancel", fontWeight = FontWeight.SemiBold) }

                        Button(
                            onClick = {
                                val cmInt = cm.toIntOrNull() ?: question.marks.cm
                                val imInt = im.toIntOrNull() ?: question.marks.im
                                val pmInt = pm.toIntOrNull()
                                val parsedCorrectOptions = if (type == QType.NAT) emptyList()
                                    else correct.split(Regex("[,\\s]+")).mapNotNull { it.trim().toIntOrNull() }
                                val parsedNat = if (type == QType.NAT) correct.trim() else null

                                onApply(QuestionOverride(
                                    type                 = type,
                                    marks                = Marks(cmInt, imInt, if (type == QType.MSQ) pmInt else null),
                                    correctAnswerOptions = parsedCorrectOptions.ifEmpty { null },
                                    correctAnswerNat     = parsedNat
                                ))
                                onClose()
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape  = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Blue500)
                        ) { Text("Apply Override", fontWeight = FontWeight.Bold) }
                    }

                    Text(
                        "Overrides apply to this session only and never modify your source ZIP.",
                        color = Slate600, fontSize = 10.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
private fun AdminTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        modifier      = Modifier.fillMaxWidth(),
        placeholder   = { Text(placeholder, color = Slate600, fontSize = 13.sp) },
        singleLine    = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape  = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = Blue500,
            unfocusedBorderColor    = Slate700,
            focusedTextColor        = Color.White,
            unfocusedTextColor      = Color.White,
            focusedContainerColor   = Slate900,
            unfocusedContainerColor = Slate900
        )
    )
}
