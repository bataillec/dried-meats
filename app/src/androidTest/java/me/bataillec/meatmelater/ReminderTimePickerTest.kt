package me.bataillec.meatmelater

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import me.bataillec.meatmelater.ui.settings.ReminderTimeRow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** R26: one `HH:mm` row opening a time picker; confirm saves both values, cancel changes nothing. */
@RunWith(AndroidJUnit4::class)
class ReminderTimePickerTest {
    @get:Rule
    val compose = createComposeRule()

    private fun str(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private var stored = 18 to 0
    private var confirmCount = 0

    private fun show(fontScale: Float = 1f) {
        compose.setContent {
            var time by remember { mutableStateOf(stored) }
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                ReminderTimeRow(time.first, time.second) { h, m ->
                    stored = h to m
                    time = stored
                    confirmCount++
                }
            }
        }
    }

    @Test
    fun r26RowShowsHhMmWithSpokenLabel() {
        stored = 7 to 5
        show()
        compose.onNodeWithText("07:05").assertIsDisplayed()
        val description =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
                .getString(R.string.reminder_time_description, "07:05")
        compose.onNodeWithContentDescription(description).assertExists()
    }

    @Test
    fun r26CancelLeavesTimeUntouched() {
        stored = 18 to 0
        show()
        compose.onNodeWithText("18:00").performClick()
        compose.onNodeWithText(str(R.string.reminder_time_title)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.cancel)).performClick()
        assertEquals(18 to 0, stored)
        assertEquals(0, confirmCount)
    }

    @Test
    fun r26ConfirmWithTypedTimeNotMultipleOfFiveSavesHourAndMinuteOnce() {
        stored = 18 to 0
        confirmCount = 0
        show(fontScale = 2f) // large font: the dialog opens directly in keyboard mode
        compose.onNodeWithText("18:00").performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("07")
        compose.onAllNodes(hasSetTextAction())[1].performTextReplacement("37")
        compose.onNodeWithText(str(R.string.validate)).assertIsDisplayed().performClick()
        assertEquals(7 to 37, stored)
        assertEquals(1, confirmCount)
        compose.onNodeWithText("07:37").assertIsDisplayed()
    }

    @Test
    fun r26DialogButtonsStayReachableAt200PercentFont() {
        show(fontScale = 2f)
        compose.onNodeWithText("18:00").performClick()
        compose.onNodeWithText(str(R.string.validate)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.cancel)).assertIsDisplayed()
    }

    @Test
    fun r26AtNormalFontDialOpensFirstAndToggleSwitchesToKeyboard() {
        stored = 18 to 0
        confirmCount = 0
        show()
        compose.onNodeWithText("18:00").performClick()
        compose.onNodeWithContentDescription(str(R.string.reminder_time_use_keyboard)).performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("06")
        compose.onAllNodes(hasSetTextAction())[1].performTextReplacement("45")
        compose.onNodeWithText(str(R.string.validate)).performClick()
        assertEquals(6 to 45, stored)
        assertEquals(1, confirmCount)
    }

    @Test
    fun r26RowStaysFullyVisibleAt200PercentFont() {
        stored = 18 to 0
        show(fontScale = 2f)
        compose.onNodeWithText(str(R.string.reminder_time)).assertIsDisplayed()
        compose.onNodeWithText("18:00").assertIsDisplayed()
    }
}
