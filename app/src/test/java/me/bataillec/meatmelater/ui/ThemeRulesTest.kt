package me.bataillec.meatmelater.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import me.bataillec.meatmelater.domain.AccentColor
import me.bataillec.meatmelater.domain.AppSettings
import me.bataillec.meatmelater.domain.ThemeMode
import me.bataillec.meatmelater.ui.theme.resolveColorScheme
import me.bataillec.meatmelater.ui.theme.usesDarkBars
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeRulesTest {
    @Test
    fun `R24 Clair on a dark phone needs dark bar icons (regression, invisible status bar)`() {
        assertFalse(usesDarkBars(ThemeMode.LIGHT, systemDark = true))
        assertTrue(usesDarkBars(ThemeMode.DARK, systemDark = false))
        assertTrue(usesDarkBars(ThemeMode.OLED, systemDark = false))
        assertTrue(usesDarkBars(ThemeMode.SYSTEM, systemDark = true))
        assertFalse(usesDarkBars(ThemeMode.SYSTEM, systemDark = false))
    }

    @Test
    fun `R24 Système never resolves to OLED and OLED is pure black`() {
        val oled = resolveColorScheme(ThemeMode.OLED, systemDark = true)
        assertEquals(androidx.compose.ui.graphics.Color.Black, oled.background)
        assertNotEquals(oled, resolveColorScheme(ThemeMode.SYSTEM, systemDark = true))
        assertEquals(resolveColorScheme(ThemeMode.DARK, false), resolveColorScheme(ThemeMode.SYSTEM, true))
        assertEquals(resolveColorScheme(ThemeMode.LIGHT, true), resolveColorScheme(ThemeMode.SYSTEM, false))
    }

    private fun contrast(
        a: Color,
        b: Color,
    ): Double {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun assertAa(
        label: String,
        scheme: ColorScheme,
    ) {
        val text =
            listOf(
                scheme.onPrimary to scheme.primary,
                scheme.onPrimaryContainer to scheme.primaryContainer,
                scheme.onSecondary to scheme.secondary,
                scheme.onSecondaryContainer to scheme.secondaryContainer,
                scheme.onTertiary to scheme.tertiary,
                scheme.onTertiaryContainer to scheme.tertiaryContainer,
                scheme.onSurface to scheme.surface,
                scheme.onBackground to scheme.background,
                scheme.onSurfaceVariant to scheme.surfaceVariant,
            )
        text.forEachIndexed { i, (fg, bg) -> assertTrue("$label text pair $i", contrast(fg, bg) >= 4.5) }
        listOf(
            scheme.primary to scheme.background,
            scheme.primary to scheme.surface,
            scheme.outline to scheme.background,
        ).forEachIndexed { i, (fg, bg) -> assertTrue("$label component pair $i", contrast(fg, bg) >= 3.0) }
    }

    @Test
    fun `R27 N4 every accent keeps AA contrast in every mode`() {
        for (accent in AccentColor.entries) {
            for (mode in ThemeMode.entries) {
                for (systemDark in listOf(false, true)) {
                    assertAa("$accent $mode dark=$systemDark", resolveColorScheme(mode, systemDark, accent))
                }
            }
        }
    }

    @Test
    fun `R27 OLED stays pure black for every accent and accents differ`() {
        for (accent in AccentColor.entries) {
            val oled = resolveColorScheme(ThemeMode.OLED, systemDark = false, accent)
            assertEquals(Color.Black, oled.background)
            assertEquals(Color.Black, oled.surface)
            assertEquals(
                resolveColorScheme(ThemeMode.DARK, false, accent),
                resolveColorScheme(ThemeMode.SYSTEM, true, accent),
            )
        }
        val primaries = AccentColor.entries.map { resolveColorScheme(ThemeMode.LIGHT, false, it).primary }
        assertEquals(AccentColor.entries.size, primaries.toSet().size)
    }

    @Test
    fun `R27 default accent is the original palette and alert colours never follow the accent`() {
        assertEquals(Color(0xFF8B2E1F), resolveColorScheme(ThemeMode.LIGHT, false).primary)
        assertEquals(
            AccentColor.VIANDE_SECHEE,
            AppSettings().accent,
        )
        for (mode in ThemeMode.entries) {
            val reference = resolveColorScheme(mode, true, AccentColor.VIANDE_SECHEE)
            for (accent in AccentColor.entries) {
                val scheme = resolveColorScheme(mode, true, accent)
                assertEquals(reference.tertiary, scheme.tertiary)
                assertEquals(reference.tertiaryContainer, scheme.tertiaryContainer)
                assertEquals(reference.error, scheme.error)
            }
        }
    }

    @Test
    fun `R27 stage chips stay distinguishable from each other for every accent`() {
        for (accent in AccentColor.entries) {
            for (mode in ThemeMode.entries) {
                val scheme = resolveColorScheme(mode, true, accent)
                assertNotEquals("$accent $mode", scheme.primaryContainer, scheme.secondaryContainer)
            }
        }
    }
}
