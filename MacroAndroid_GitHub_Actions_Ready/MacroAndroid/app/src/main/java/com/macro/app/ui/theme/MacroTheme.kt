package com.macro.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.macro.app.model.AccentTheme

object GamingColors {
    val ObsidianBg = Color(0xFF0A0B10)
    val CarbonSurface = Color(0xFF12151E)
    val ElevatedCard = Color(0xFF1A1F2C)
    val BorderSubtle = Color(0xFF282F42)

    val CrimsonRed = Color(0xFFFF2E4C)
    val CrimsonDark = Color(0xFF9E1428)
    val CyberBlue = Color(0xFF00D4FF)
    val CyberBlueDark = Color(0xFF007EA7)

    val NeonGreen = Color(0xFF00E676)
    val AmberWarn = Color(0xFFFFC107)
    val TextPrimary = Color(0xFFF2F4F8)
    val TextSecondary = Color(0xFF9AA3B2)
}

@Composable
fun MacroTheme(
    accentTheme: AccentTheme = AccentTheme.REDMAGIC_CRIMSON,
    content: @Composable () -> Unit
) {
    val primary = when (accentTheme) {
        AccentTheme.REDMAGIC_CRIMSON -> GamingColors.CrimsonRed
        AccentTheme.CYBER_BLUE -> GamingColors.CyberBlue
    }
    val secondary = when (accentTheme) {
        AccentTheme.REDMAGIC_CRIMSON -> GamingColors.CyberBlue
        AccentTheme.CYBER_BLUE -> GamingColors.CrimsonRed
    }

    val colorScheme = darkColorScheme(
        primary = primary,
        onPrimary = Color.White,
        secondary = secondary,
        onSecondary = Color.Black,
        background = GamingColors.ObsidianBg,
        onBackground = GamingColors.TextPrimary,
        surface = GamingColors.CarbonSurface,
        onSurface = GamingColors.TextPrimary,
        surfaceVariant = GamingColors.ElevatedCard,
        onSurfaceVariant = GamingColors.TextSecondary,
        outline = GamingColors.BorderSubtle,
        error = Color(0xFFFF5252)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
