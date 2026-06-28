package com.dmgproductions.amp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Brand fallback schemes — used when dynamic color is unavailable (pre-S) or
// disabled. The neon orange stays the primary so A.M.P keeps its identity.
private val AmpDarkColors = darkColorScheme(
    primary = NeonOrange,
    onPrimary = InkBlack,
    primaryContainer = NeonOrangeDeep,
    onPrimaryContainer = NeonOrangeBright,
    secondary = NeonCyan,
    onSecondary = InkBlack,
    secondaryContainer = NeonCyanDeep,
    onSecondaryContainer = NeonCyan,
    tertiary = NeonOrangeBright,
    background = InkBlack,
    onBackground = TextWhite,
    surface = InkSurface,
    onSurface = TextWhite,
    surfaceVariant = InkSurfaceHigh,
    onSurfaceVariant = ActivityIdle,
    outline = InkOutline,
)

private val AmpLightColors = lightColorScheme(
    primary = NeonOrangeDeep,
    onPrimary = CloudSurface,
    primaryContainer = NeonOrangeBright,
    onPrimaryContainer = InkBlack,
    secondary = NeonCyanDeep,
    onSecondary = CloudSurface,
    tertiary = NeonOrange,
    background = Cloud,
    onBackground = InkBlack,
    surface = CloudSurface,
    onSurface = InkBlack,
    surfaceVariant = Cloud,
    onSurfaceVariant = InkSurfaceHigh,
    outline = CloudOutline,
)

/**
 * A.M.P's Material 3 theme.
 *
 * Uses Material You **dynamic color** as the base on Android 12+, so the palette
 * adapts to the user's wallpaper (and, later, album art). On older devices, or
 * when dynamic color is off, it falls back to the neon brand scheme. Either way
 * the brand orange remains available as a persistent accent (see [AmpAccents]).
 */
@Composable
fun AmpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> AmpDarkColors
        else -> AmpLightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AmpTypography,
        shapes = AmpShapes,
        content = content,
    )
}
