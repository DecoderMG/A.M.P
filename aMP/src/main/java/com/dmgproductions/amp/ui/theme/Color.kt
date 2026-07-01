package com.dmgproductions.amp.ui.theme

import androidx.compose.ui.graphics.Color

// ── A.M.P neon brand palette ─────────────────────────────────────────────
// The app's identity is neon-on-dark: a hot orange paired with electric cyan.
val NeonOrange = Color(0xFFFF7A1A)
val NeonOrangeBright = Color(0xFFFF9D4D)
val NeonOrangeDeep = Color(0xFFC44A00)
val NeonCyan = Color(0xFF35E0FF)
val NeonCyanDeep = Color(0xFF0086A8)

// Activity accents (used to tint the UI by detected motion state).
val ActivityIdle = Color(0xFF8E9BB3)
val ActivityWalking = NeonCyan
val ActivityRunning = NeonOrange

// Dark surfaces — near-black with a faint warm lift.
val InkBlack = Color(0xFF080809)
val InkSurface = Color(0xFF121316)
val InkSurfaceHigh = Color(0xFF1C1D22)
val InkOutline = Color(0xFF34363D)

// Light surfaces for the light scheme fallback.
val Cloud = Color(0xFFFBF8F6)
val CloudSurface = Color(0xFFFFFFFF)
val CloudOutline = Color(0xFFD9D2CC)

val TextWhite = Color(0xFFF3F1EF)
