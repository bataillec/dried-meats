package me.bataillec.meatmelater.platform

import kotlinx.coroutines.runBlocking
import me.bataillec.meatmelater.AppTestBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BatteryWizardTest : AppTestBase() {
    @Test
    fun `R16 wizard shows only when not shown yet and not exempt`() {
        assertTrue(batteryWizardVisible(notYetShown = true, alreadyExempt = false))
        assertFalse(batteryWizardVisible(notYetShown = false, alreadyExempt = false))
        assertFalse(batteryWizardVisible(notYetShown = true, alreadyExempt = true))
    }

    @Test
    fun `R16 the shown flag is persisted so the wizard appears once`() =
        runBlocking {
            assertFalse(container.settings.current().batteryHintShown)
            container.settings.update { it.copy(batteryHintShown = true) }
            assertTrue(container.settings.current().batteryHintShown)
        }

    @Test
    fun `R16 permission state reflects the battery exemption`() {
        val before = ReminderPermissions.read(app)
        assertFalse(before.batteryExempt)
        org.robolectric.Shadows
            .shadowOf(app.getSystemService(android.os.PowerManager::class.java))
            .setIgnoringBatteryOptimizations(app.packageName, true)
        assertTrue(ReminderPermissions.read(app).batteryExempt)
        assertEquals(false, ReminderPermissions.read(app).allGranted && !before.batteryExempt)
    }
}
