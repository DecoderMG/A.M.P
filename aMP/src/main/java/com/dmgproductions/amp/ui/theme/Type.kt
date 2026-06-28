package com.dmgproductions.amp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Expressive type scale: large, confident display sizes with tight tracking,
// and a slightly heavier body for legibility on dark surfaces.
private val Default = FontFamily.SansSerif

val AmpTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Black,
        fontSize = 57.sp, lineHeight = 60.sp, letterSpacing = (-1).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-0.5).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Bold,
        fontSize = 34.sp, lineHeight = 40.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Bold,
        fontSize = 30.sp, lineHeight = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Bold,
        fontSize = 25.sp, lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp, lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.3.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.5.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Default, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
    ),
)
