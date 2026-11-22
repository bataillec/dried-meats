package me.bataillec.meatmelater.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import me.bataillec.meatmelater.domain.ThemeMode

private val LightColors =
    lightColorScheme(
        primary = Color(0xFF8B2E1F),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDAD2),
        onPrimaryContainer = Color(0xFF3A0A02),
        secondary = Color(0xFF6B4F3A),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFF3DFD0),
        onSecondaryContainer = Color(0xFF2A1708),
        tertiary = Color(0xFF9A4F00),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDCBE),
        onTertiaryContainer = Color(0xFF301500),
        background = Color(0xFFFFF8F5),
        onBackground = Color(0xFF211A18),
        surface = Color(0xFFFFF8F5),
        onSurface = Color(0xFF211A18),
        surfaceVariant = Color(0xFFF5DDD7),
        onSurfaceVariant = Color(0xFF53433F),
        outline = Color(0xFF85736E),
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFFFFB4A2),
        onPrimary = Color(0xFF561003),
        primaryContainer = Color(0xFF722719),
        onPrimaryContainer = Color(0xFFFFDAD2),
        secondary = Color(0xFFE0C0A8),
        onSecondary = Color(0xFF3F2A19),
        secondaryContainer = Color(0xFF58402D),
        onSecondaryContainer = Color(0xFFFDDCC4),
        tertiary = Color(0xFFFFB873),
        onTertiary = Color(0xFF4D2700),
        tertiaryContainer = Color(0xFF6F3A00),
        onTertiaryContainer = Color(0xFFFFDCBE),
        background = Color(0xFF1A1110),
        onBackground = Color(0xFFF1DFDB),
        surface = Color(0xFF1A1110),
        onSurface = Color(0xFFF1DFDB),
        surfaceVariant = Color(0xFF53433F),
        onSurfaceVariant = Color(0xFFD8C2BC),
        outline = Color(0xFFA08C87),
    )

/** R24: pure black background, very dark grey cards/dividers/chips, same accents as [DarkColors]. */
private val OledColors =
    DarkColors.copy(
        background = Color(0xFF000000),
        surface = Color(0xFF000000),
        surfaceVariant = Color(0xFF1A1A1A),
        surfaceContainerLowest = Color(0xFF000000),
        surfaceContainerLow = Color(0xFF0B0B0B),
        surfaceContainer = Color(0xFF121212),
        surfaceContainerHigh = Color(0xFF181818),
        surfaceContainerHighest = Color(0xFF1F1F1F),
        outline = Color(0xFF8A8A8A),
        outlineVariant = Color(0xFF2A2A2A),
        primaryContainer = Color(0xFF3A140C),
        secondaryContainer = Color(0xFF241A12),
        tertiaryContainer = Color(0xFF3A1E00),
    )

/** R24: SYSTEM resolves to LIGHT or DARK only, never OLED. */
fun resolveColorScheme(
    mode: ThemeMode,
    systemDark: Boolean,
): ColorScheme =
    when (mode) {
        ThemeMode.LIGHT -> LightColors
        ThemeMode.DARK -> DarkColors
        ThemeMode.OLED -> OledColors
        ThemeMode.SYSTEM -> if (systemDark) DarkColors else LightColors
    }

/** Whether system bar icons must be light (true) because the app background is dark. */
fun usesDarkBars(
    mode: ThemeMode,
    systemDark: Boolean,
): Boolean =
    when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.OLED -> true
        ThemeMode.SYSTEM -> systemDark
    }

@Composable
fun AppTheme(
    mode: ThemeMode,
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = resolveColorScheme(mode, isSystemInDarkTheme()), content = content)
}
