package me.bataillec.meatmelater

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import me.bataillec.meatmelater.domain.MeatType
import me.bataillec.meatmelater.domain.PieceStatus
import me.bataillec.meatmelater.domain.Recipe
import me.bataillec.meatmelater.platform.ReminderReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

/** R8, R13: the vacuum notification button opens the seasoning prompt; nothing moves until it is confirmed. */
@RunWith(AndroidJUnit4::class)
class NotificationFinishVacuumTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val container get() = (context.applicationContext as MeatMeLaterApp).container

    private fun str(id: Int) = context.getString(id)

    private fun waitForText(text: String) =
        compose.waitUntil(15_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    private fun status(id: Long) =
        runBlocking {
            container.pieces
                .getSummary(id)!!
                .piece.status
        }

    private fun createPiece(): Long =
        runBlocking {
            val meat = container.meatTypes.save(MeatType(0, "Test"))
            val recipe =
                container.recipes.save(
                    Recipe(
                        0,
                        "Classique",
                        BigDecimal("25"),
                        BigDecimal("5"),
                        BigDecimal("2"),
                        fridgeSeasoningDefault = "herbes",
                    ),
                )
            container.pieceActions.createPiece("Pièce ${System.nanoTime()}", recipe, meat, 1250, BigDecimal("12"))!!
        }

    private fun launchFromNotification(id: Long): ActivityScenario<MainActivity> {
        val runId =
            runBlocking {
                container.pieces
                    .getSummary(id)!!
                    .openRun!!
                    .id
            }
        val intent =
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_PIECE_ID, id)
                .putExtra(ReminderReceiver.EXTRA_RUN_ID, runId)
                .putExtra(MainActivity.EXTRA_FINISH_VACUUM, true)
        runBlocking { container.settings.update { it.copy(batteryHintShown = true) } }
        return ActivityScenario.launch(intent)
    }

    @Test
    fun r8NotificationActionPromptsPrefilledAndCancelLeavesPieceInVacuum() {
        val id = createPiece()
        launchFromNotification(id).use {
            waitForText(str(R.string.finish_vacuum_text))
            compose.onNodeWithText("herbes").assertExists()
            compose.onNodeWithText(str(R.string.cancel)).performClick()
            compose.waitForIdle()
            assertEquals(PieceStatus.VACUUM, status(id))
            assertEquals(
                PieceStatus.VACUUM,
                runBlocking {
                    container.pieces
                        .getSummary(id)!!
                        .openRun!!
                        .let { PieceStatus.VACUUM }
                },
            )
        }
    }

    @Test
    fun r8NotificationActionConfirmWithEmptySeasoningSavesNone() {
        val id = createPiece()
        launchFromNotification(id).use {
            waitForText(str(R.string.finish_vacuum_text))
            compose.onNodeWithText("herbes").performTextClearance()
            compose.onAllNodesWithText(str(R.string.leave_vacuum)).onLast().performClick()
            compose.waitUntil(15_000) { status(id) == PieceStatus.FRIDGE }
            assertNull(
                runBlocking {
                    container.pieces
                        .getSummary(id)!!
                        .piece.fridgeSeasoning
                },
            )
        }
    }
}
