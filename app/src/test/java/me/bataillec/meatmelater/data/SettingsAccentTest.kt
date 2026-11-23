package me.bataillec.meatmelater.data

import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import me.bataillec.meatmelater.domain.AccentColor
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsAccentTest {
    private val key = stringPreferencesKey("accent")

    @Test
    fun `R27 missing accent falls back to the default so upgrading users see no change`() {
        assertEquals(AccentColor.VIANDE_SECHEE, preferencesOf().toAppSettings().accent)
    }

    @Test
    fun `R27 every accent round-trips through its stored name`() {
        for (accent in AccentColor.entries) {
            assertEquals(accent, preferencesOf(key to accent.name).toAppSettings().accent)
        }
    }

    @Test
    fun `R27 an unknown stored accent falls back to the default`() {
        assertEquals(AccentColor.VIANDE_SECHEE, preferencesOf(key to "ROSE_FLUO").toAppSettings().accent)
    }
}
