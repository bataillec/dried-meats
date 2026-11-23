package me.bataillec.meatmelater.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import me.bataillec.meatmelater.domain.AccentColor
import me.bataillec.meatmelater.domain.ThemeMode

/** R27: the alert orange is semantic (overdue cards, warnings): it never follows the accent. */
private val AlertLight =
    listOf(Color(0xFF9A4F00), Color(0xFFFFFFFF), Color(0xFFFFDCBE), Color(0xFF301500))
private val AlertDark =
    listOf(Color(0xFFFFB873), Color(0xFF4D2700), Color(0xFF6F3A00), Color(0xFFFFDCBE))

private fun lightScheme(roles: AccentRoles): ColorScheme =
    lightColorScheme(
        primary = roles.primary,
        onPrimary = roles.onPrimary,
        primaryContainer = roles.primaryContainer,
        onPrimaryContainer = roles.onPrimaryContainer,
        secondary = roles.secondary,
        onSecondary = roles.onSecondary,
        secondaryContainer = roles.secondaryContainer,
        onSecondaryContainer = roles.onSecondaryContainer,
        tertiary = AlertLight[0],
        onTertiary = AlertLight[1],
        tertiaryContainer = AlertLight[2],
        onTertiaryContainer = AlertLight[3],
        background = roles.background,
        onBackground = roles.onBackground,
        surface = roles.surface,
        onSurface = roles.onSurface,
        surfaceVariant = roles.surfaceVariant,
        onSurfaceVariant = roles.onSurfaceVariant,
        outline = roles.outline,
    )

private fun darkScheme(roles: AccentRoles): ColorScheme =
    darkColorScheme(
        primary = roles.primary,
        onPrimary = roles.onPrimary,
        primaryContainer = roles.primaryContainer,
        onPrimaryContainer = roles.onPrimaryContainer,
        secondary = roles.secondary,
        onSecondary = roles.onSecondary,
        secondaryContainer = roles.secondaryContainer,
        onSecondaryContainer = roles.onSecondaryContainer,
        tertiary = AlertDark[0],
        onTertiary = AlertDark[1],
        tertiaryContainer = AlertDark[2],
        onTertiaryContainer = AlertDark[3],
        background = roles.background,
        onBackground = roles.onBackground,
        surface = roles.surface,
        onSurface = roles.onSurface,
        surfaceVariant = roles.surfaceVariant,
        onSurfaceVariant = roles.onSurfaceVariant,
        outline = roles.outline,
    )

/** R24: pure black background, very dark grey cards/dividers/chips; only the accent containers differ per accent. */
private fun oledScheme(palette: AccentPalette): ColorScheme =
    darkScheme(palette.dark).copy(
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
        primaryContainer = palette.oledPrimaryContainer,
        secondaryContainer = palette.oledSecondaryContainer,
        tertiaryContainer = Color(0xFF3A1E00),
    )

/** R24: SYSTEM resolves to LIGHT or DARK only, never OLED. R27: the accent only swaps the accent roles. */
fun resolveColorScheme(
    mode: ThemeMode,
    systemDark: Boolean,
    accent: AccentColor = AccentColor.VIANDE_SECHEE,
): ColorScheme {
    val kind =
        when (mode) {
            ThemeMode.LIGHT -> SchemeKind.LIGHT
            ThemeMode.DARK -> SchemeKind.DARK
            ThemeMode.OLED -> SchemeKind.OLED
            ThemeMode.SYSTEM -> if (systemDark) SchemeKind.DARK else SchemeKind.LIGHT
        }
    return schemes.getValue(accent to kind)
}

private enum class SchemeKind { LIGHT, DARK, OLED }

/** Built once so that equal inputs return the same instance (ColorScheme has no structural equality). */
private val schemes: Map<Pair<AccentColor, SchemeKind>, ColorScheme> by lazy {
    buildMap {
        for (accent in AccentColor.entries) {
            val palette = paletteOf(accent)
            put(accent to SchemeKind.LIGHT, lightScheme(palette.light))
            put(accent to SchemeKind.DARK, darkScheme(palette.dark))
            put(accent to SchemeKind.OLED, oledScheme(palette))
        }
    }
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
    accent: AccentColor = AccentColor.VIANDE_SECHEE,
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = resolveColorScheme(mode, isSystemInDarkTheme(), accent), content = content)
}
