package com.jeecbt.app.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeecbt.app.AppUiState
import com.jeecbt.app.MainViewModel
import com.jeecbt.app.ui.theme.*

@Composable
fun HomeScreen(viewModel: MainViewModel, state: AppUiState, context: Context) {
    var durationMin by remember { mutableStateOf("180") }
    var akFileName by remember { mutableStateOf<String?>(null) }
    var akContent  by remember { mutableStateOf<String?>(null) }
    var zipUri     by remember { mutableStateOf<Uri?>(null) }
    var zipName    by remember { mutableStateOf<String?>(null) }

    // File pickers
    val zipPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        zipUri  = uri
        zipName = uri.lastPathSegment?.substringAfterLast('/') ?: "test.zip"
    }
    val akPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            akContent  = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            akFileName = uri.lastPathSegment?.substringAfterLast('/') ?: "answer_key.json"
        } catch (_: Exception) {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Blue500.copy(alpha = 0.15f))
                .border(1.dp, Blue500.copy(alpha = 0.3f), RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Blue400))
                Text("PREMIUM CBT PLATFORM", color = Blue400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("JEE CBT ", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
        Text("Arena", color = Blue400, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.offset(y = (-8).dp))
        Text(
            "Upload your test ZIP — auto-detects JSON, groups split images & builds the exam.",
            color = Slate400, fontSize = 14.sp, textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        // ── Card ────────────────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Slate800
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                // Saved session banner
                if (state.hasSavedSession) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Amber500.copy(alpha = 0.10f)
                    ) {
                        Row(
                            modifier = Modifier
                                .border(1.dp, Amber500.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Saved session found", color = Amber300, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Re-upload the same ZIP to resume.", color = Amber300.copy(alpha = 0.7f), fontSize = 11.sp)
                            }
                            TextButton(onClick = { viewModel.discardSaved(context) }) {
                                Text("Discard", color = Slate400, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Duration input
                Text("TEST DURATION (minutes)", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value         = durationMin,
                    onValueChange = { durationMin = it },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Blue500,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor     = Color.White,
                        unfocusedTextColor   = Color.White,
                        cursorColor          = Blue400,
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Text("Used only if the JSON doesn't specify a duration.", color = Slate600, fontSize = 11.sp)

                Spacer(Modifier.height(16.dp))

                // Answer key upload
                Text("ANSWER KEY JSON (optional override)", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate900.copy(alpha = 0.5f))
                        .border(1.dp, Slate700, RoundedCornerShape(12.dp))
                        .clickable { akPicker.launch("application/json") }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape    = RoundedCornerShape(10.dp),
                        color    = Emerald500.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald300, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column {
                        Text(akFileName ?: "Upload answer key JSON", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text(if (akFileName != null) "Tap to change" else "Provide to override correct answers & marks", color = Slate400, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ZIP upload area
                val isLoading = state.isLoading
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isLoading) Blue500.copy(alpha = 0.05f) else Color.Transparent)
                        .border(2.dp, if (isLoading) Blue500.copy(alpha = 0.5f) else Slate700, RoundedCornerShape(16.dp))
                        .clickable(enabled = !isLoading) { zipPicker.launch("application/zip") }
                        .padding(vertical = 28.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Blue400, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                            Spacer(Modifier.height(12.dp))
                            Text(state.loadingMessage.ifEmpty { "Working…" }, color = Blue300, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("Hang tight, processing your file", color = Slate400, fontSize = 12.sp)
                        } else {
                            Surface(
                                modifier = Modifier.size(56.dp),
                                shape    = RoundedCornerShape(16.dp),
                                color    = Blue500.copy(alpha = 0.15f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Upload, contentDescription = null, tint = Blue300, modifier = Modifier.size(26.dp))
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(zipName ?: "Tap to upload your test ZIP", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(if (zipName != null) "Tap to change • then press Start" else "Supports any JEE CBT bundle", color = Slate400, fontSize = 12.sp)
                        }
                    }
                }

                // Error
                if (state.error != null) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp),
                        color    = Red500.copy(alpha = 0.10f)
                    ) {
                        Text(
                            "⚠ ${state.error}",
                            color    = Red300,
                            fontSize = 13.sp,
                            modifier = Modifier.border(1.dp, Red500.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Start button (shown when ZIP is selected)
                if (zipUri != null && !isLoading) {
                    Button(
                        onClick = {
                            viewModel.loadZip(
                                context, zipUri!!, durationMin.toIntOrNull() ?: 180, akContent
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Blue500)
                    ) {
                        Text("Start Test →", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                }

                // Feature chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Auto" to "JSON detect", "Smart" to "Image group", "Offline" to "Autosave")
                        .forEach { (h, s) ->
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape    = RoundedCornerShape(10.dp),
                                color    = Slate900.copy(alpha = 0.4f)
                            ) {
                                Column(
                                    modifier = Modifier.border(1.dp, Slate800, RoundedCornerShape(10.dp)).padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(h, color = Blue400, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(s, color = Slate400, fontSize = 10.sp)
                                }
                            }
                        }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Inspired by NTA · Allen · Mathongo CBT interfaces", color = Slate600, fontSize = 11.sp)
    }
}
