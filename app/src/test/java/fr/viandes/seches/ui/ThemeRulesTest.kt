package fr.viandes.seches.ui

import fr.viandes.seches.domain.ThemeMode
import fr.viandes.seches.ui.theme.resolveColorScheme
import fr.viandes.seches.ui.theme.usesDarkBars
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
}
