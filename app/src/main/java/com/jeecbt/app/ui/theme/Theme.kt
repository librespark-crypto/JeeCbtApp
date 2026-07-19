package com.jeecbt.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Color palette  (mirrors original Tailwind slate/blue dark theme)
// ─────────────────────────────────────────────────────────────────────────────

val Blue400  = Color(0xFF60A5FA)
val Blue500  = Color(0xFF3B82F6)
val Blue600  = Color(0xFF2563EB)
val Slate900 = Color(0xFF0F172A)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val Slate600 = Color(0xFF475569)
val Slate400 = Color(0xFF94A3B8)
val Slate300 = Color(0xFFCBD5E1)
val BgDeep   = Color(0xFF0D1530)   // original #0d1530

val Emerald300 = Color(0xFF6EE7B7)
val Emerald500 = Color(0xFF10B981)
val Red300     = Color(0xFFFCA5A5)
val Red500     = Color(0xFFEF4444)
val Purple300  = Color(0xFFD8B4FE)
val Purple500  = Color(0xFFA855F7)
val Amber300   = Color(0xFFFCD34D)
val Amber500   = Color(0xFFF59E0B)

private val DarkColorScheme = darkColorScheme(
    primary          = Blue500,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFF1E3A5F),
    secondary        = Slate700,
    onSecondary      = Slate300,
    background       = BgDeep,
    onBackground     = Color.White,
    surface          = Slate800,
    onSurface        = Color.White,
    surfaceVariant   = Slate800,
    onSurfaceVariant = Slate400,
    outline          = Slate700,
    error            = Red500,
    onError          = Color.White,
)

@Composable
fun JeeCbtTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography(),
        content     = content
    )
}
