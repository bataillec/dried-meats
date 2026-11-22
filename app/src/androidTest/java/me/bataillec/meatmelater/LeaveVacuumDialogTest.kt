package me.bataillec.meatmelater

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import me.bataillec.meatmelater.domain.MeatType
import me.bataillec.meatmelater.domain.PieceStatus
import me.bataillec.meatmelater.domain.Recipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

/** Real-device flows that Robolectric cannot run (dialog windows): R8, R16, R8 empty seasoning. */
@RunWith(AndroidJUnit4::class)
class LeaveVacuumDialogTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val container get() = (compose.activity.application as MeatMeLaterApp).container

    private fun str(id: Int) = compose.activity.getString(id)

    private fun waitForText(text: String) =
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }

    private fun dismissWizardIfShown() {
        runCatching {
            waitForText(str(R.string.battery_wizard_title))
            compose.onNodeWithText(str(R.string.battery_wizard_later)).performClick()
        }
    }

    private var label = ""

    private fun createPiece(): Long =
        runBlocking {
            label = "Pièce ${System.nanoTime()}"
            val meat = container.meatTypes.save(MeatType(0, "Test"))
            val recipe =
                container.recipes.save(
                    Recipe(
                        0,
                        meat,
                        "Classique",
                        BigDecimal("25"),
                        BigDecimal("5"),
                        BigDecimal("2"),
                        fridgeSeasoningDefault = "herbes",
                    ),
                )
            container.pieceActions.createPiece(label, recipe, 1250, BigDecimal("12"))!!
        }

    private fun openPieceAndLeaveVacuum() {
        waitForText(label)
        compose.onNodeWithText(label).performClick()
        waitForText(str(R.string.leave_vacuum))
        compose.onAllNodesWithText(str(R.string.leave_vacuum)).onLast().performClick()
        waitForText(str(R.string.finish_vacuum_text))
    }

    @Test
    fun firstLaunchBatteryWizardAppearsAndCanBeDismissed() {
        waitForText(str(R.string.battery_wizard_title))
        compose.onNodeWithText(str(R.string.battery_wizard_later)).performClick()
        compose.waitUntil(10_000) { runBlocking { container.settings.current().batteryHintShown } }
    }

    @Test
    fun leavingVacuumPrefillsRecipeSeasoningAndMovesPieceToFridge() {
        dismissWizardIfShown()
        val id = createPiece()
        openPieceAndLeaveVacuum()
        compose.onNodeWithText("herbes").assertExists()
        compose.onAllNodesWithText(str(R.string.leave_vacuum)).onLast().performClick()
        compose.waitUntil(
            15_000,
        ) {
            runBlocking {
                container.pieces
                    .getSummary(id)!!
                    .piece.status
            } == PieceStatus.FRIDGE
        }
        assertEquals(
            "herbes",
            runBlocking {
                container.pieces
                    .getSummary(id)!!
                    .piece.fridgeSeasoning
            },
        )
    }

    @Test
    fun seasoningCanBeLeftEmpty() {
        dismissWizardIfShown()
        val id = createPiece()
        openPieceAndLeaveVacuum()
        compose.onNodeWithText("herbes").performTextClearance()
        compose.onAllNodesWithText(str(R.string.leave_vacuum)).onLast().performClick()
        compose.waitUntil(
            15_000,
        ) {
            runBlocking {
                container.pieces
                    .getSummary(id)!!
                    .piece.status
            } == PieceStatus.FRIDGE
        }
        assertNull(
            runBlocking {
                container.pieces
                    .getSummary(id)!!
                    .piece.fridgeSeasoning
            },
        )
    }
}
