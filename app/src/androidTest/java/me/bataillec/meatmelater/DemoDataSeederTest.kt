package me.bataillec.meatmelater

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import me.bataillec.meatmelater.domain.MeatType
import me.bataillec.meatmelater.domain.Recipe
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

/**
 * Developer utility, skipped unless run with `-e seedDemo true`
 * (`adb shell am instrument -w -e seedDemo true [-e dueInSeconds 90] -e class me.bataillec.meatmelater.DemoDataSeederTest ...`).
 * Fills the app with a few pieces so screenshots and manual checks have something to show.
 */
@RunWith(AndroidJUnit4::class)
class DemoDataSeederTest {
    @Test
    fun seed() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("seedDemo") == "true")
        val container = (ApplicationProvider.getApplicationContext<Context>() as MeatMeLaterApp).container
        runBlocking {
            container.seeder.seedIfNeeded()
            val meat = container.meatTypes.save(MeatType(0, "Bœuf (démo)"))
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
            val overdue = container.pieceActions.createPiece("Bœuf démo 1", recipe, 1250, BigDecimal("12"))!!
            container.pieceActions.createPiece("Bœuf démo 2", recipe, 900, BigDecimal("9"))
            val fridge = container.pieceActions.createPiece("Bœuf démo 3", recipe, 2000, BigDecimal("15"))!!
            container.pieceActions.finishStage(fridge, "poivre concassé")
            val run = container.pieces.getSummary(overdue)!!.openRun!!
            container.pieces.updateRun(run.copy(plannedEndAt = container.clock.nowMillis() - 2 * 86_400_000L))
            // Optional: a piece whose reminder is due in N seconds (Doze, reboot and time-zone checks).
            InstrumentationRegistry.getArguments().getString("dueInSeconds")?.toLongOrNull()?.let { seconds ->
                val due = container.pieceActions.createPiece("Rappel dans $seconds s", recipe, 1000, BigDecimal("10"))!!
                val dueRun = container.pieces.getSummary(due)!!.openRun!!
                container.pieces.updateRun(dueRun.copy(plannedEndAt = container.clock.nowMillis() + seconds * 1000))
            }
            container.reminderPlanner.reconcile()
        }
    }
}
