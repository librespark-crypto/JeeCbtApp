package com.jeecbt.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jeecbt.app.ui.theme.*

@Composable
fun QuestionImage(
    bitmaps: List<Bitmap>,
    modifier: Modifier = Modifier
) {
    var scale by remember(bitmaps) { mutableStateOf(1f) }
    var showFullscreen by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        if (bitmaps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No image available for this question.", color = Slate600, textAlign = TextAlign.Center)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .verticalScroll(rememberScrollState())
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = {
                            scale = if (scale > 1f) 1f else 2f
                        })
                    }
            ) {
                bitmaps.forEachIndexed { i, bitmap ->
                    Image(
                        bitmap             = bitmap.asImageBitmap(),
                        contentDescription = "Question image part ${i + 1}",
                        contentScale       = ContentScale.FillWidth,
                        modifier           = Modifier
                            .fillMaxWidth()
                            .graphicsLayer(scaleX = scale, scaleY = scale, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f))
                    )
                }
            }

            // Fullscreen button (top-right overlay)
            IconButton(
                onClick = { showFullscreen = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate900.copy(alpha = 0.85f))
            ) {
                Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }

    // Fullscreen dialog
    if (showFullscreen) {
        Dialog(
            onDismissRequest = { showFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable { showFullscreen = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .verticalScroll(rememberScrollState())
                        .padding(4.dp)
                ) {
                    bitmaps.forEach { bitmap ->
                        Image(
                            bitmap             = bitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale       = ContentScale.FillWidth,
                            modifier           = Modifier.fillMaxWidth()
                        )
                    }
                }
                Text(
                    "Tap anywhere to close",
                    color    = Slate300,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
                )
            }
        }
    }
}
