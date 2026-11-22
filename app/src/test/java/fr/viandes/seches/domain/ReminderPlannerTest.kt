package fr.viandes.seches.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderPlannerTest {
    private val paris = ZoneId.of("Europe/Paris")

    private fun at(
        d: Int,
        h: Int,
    ) = LocalDateTime
        .of(2026, 11, d, h, 0)
        .atZone(paris)
        .toInstant()
        .toEpochMilli()

    private val clock = FakeClock(at(1, 9))
    private val recipes = FakeRecipeStore()
    private val store = FakePieceStore(recipes)
    private val scheduler = FakeAlarmScheduler()
    private val planner = ReminderPlanner(store, scheduler, clock)
    private val actions = PieceActions(store, recipes, FakeSettingsStore(), planner, clock)
    private var recipeId = 0L

    @Before
    fun setUp() =
        runTest {
            recipeId = recipes.save(Recipe(0, 1, "Boeuf", BigDecimal("25"), BigDecimal("5"), BigDecimal("2")))
        }

    private suspend fun newPiece(): Long = actions.createPiece("Boeuf 1", recipeId, 1250, BigDecimal("12"))!!

    @Test
    fun `R12 creating a piece schedules one alarm at the planned end`() =
        runTest {
            newPiece()
            assertEquals(mapOf(1L to at(8, 18)), scheduler.alarms)
        }

    @Test
    fun `R10 editing the duration reschedules`() =
        runTest {
            val id = newPiece()
            actions.changeDuration(id, 10)
            assertEquals(at(11, 18), scheduler.alarms[1L])
        }

    @Test
    fun `R13 postponing reschedules one day later`() =
        runTest {
            val id = newPiece()
            actions.postponeOneDay(id)
            assertEquals(at(9, 18), scheduler.alarms[1L])
        }

    @Test
    fun `R8 finishing vacuum replaces the alarm with the fridge one`() =
        runTest {
            val id = newPiece()
            clock.millis = at(3, 10)
            actions.finishStage(id, "herbes")
            assertEquals(setOf(2L), scheduler.alarms.keys)
            assertEquals(at(17, 18), scheduler.alarms[2L]) // 14 days after day 3
        }

    @Test
    fun `R11 cancelling and deleting remove the alarm`() =
        runTest {
            val a = newPiece()
            actions.cancel(a)
            assertTrue(scheduler.alarms.isEmpty())
            val b = newPiece()
            assertEquals(1, scheduler.alarms.size)
            actions.delete(b)
            assertTrue(scheduler.alarms.isEmpty())
        }

    @Test
    fun `R14 reconcile rebuilds alarms lost after reboot`() =
        runTest {
            newPiece()
            scheduler.alarms.clear()
            planner.reconcile()
            assertEquals(at(8, 18), scheduler.alarms[1L])
        }

    @Test
    fun `R14 reconcile after a clock or timezone change keeps absolute timestamps`() =
        runTest {
            newPiece()
            val before = scheduler.alarms.toMap()
            clock.millis = at(2, 3) // clock moved
            planner.reconcile()
            assertEquals(before, scheduler.alarms)
        }

    @Test
    fun `R14 overdue never-notified run fires immediately, notified one does not`() =
        runTest {
            val id = newPiece()
            scheduler.alarms.clear()
            clock.millis = at(9, 8)
            planner.reconcile()
            assertEquals(at(9, 8), scheduler.alarms[1L])
            scheduler.recordFired(1L, at(8, 18))
            planner.reconcile()
            assertFalse(scheduler.alarms.containsKey(1L))
            // postponing moves the planned end: a new alarm is scheduled again
            actions.postponeOneDay(id)
            assertEquals(at(9, 18), scheduler.alarms[1L])
        }

    @Test
    fun `R12 stale alarms of unknown runs are cancelled`() =
        runTest {
            scheduler.schedule(99L, at(5, 18))
            planner.reconcile()
            assertTrue(scheduler.alarms.isEmpty())
        }

    @Test
    fun `R6 a recipe edit does not alter an existing piece snapshot`() =
        runTest {
            val id = newPiece()
            recipes.save(recipes.get(recipeId)!!.copy(saltGPerKg = BigDecimal("99")))
            assertEquals(
                BigDecimal("31.3"),
                store
                    .getSummary(id)!!
                    .piece.doses.saltG,
            )
        }

    @Test
    fun `R23 saving a note does not revert a concurrent postponement`() =
        runTest {
            val id = newPiece()
            val staleRun = store.getSummary(id)!!.openRun!!
            actions.postponeOneDay(id)
            actions.saveNote(staleRun.id, "  ferme  ")
            val run = store.getSummary(id)!!.openRun!!
            assertEquals("ferme", run.note)
            assertEquals(1, run.postponedDays)
            assertEquals(at(9, 18), run.plannedEndAt)
            assertEquals(at(9, 18), scheduler.alarms[1L])
        }

    @Test
    fun `R13 postponing an overdue run lands after now instead of refiring at once`() =
        runTest {
            val id = newPiece() // planned end day 8, 18:00
            clock.millis = at(12, 9)
            actions.postponeOneDay(id)
            val planned = store.getSummary(id)!!.openRun!!.plannedEndAt
            assertEquals(at(12, 18), planned)
            assertEquals(planned, scheduler.alarms[1L])
        }

    @Test
    fun `R8 R9 a second finish on the same stage is refused and creates no second run`() =
        runTest {
            val id = newPiece()
            assertTrue(actions.finishStage(id, "a"))
            // the piece is now in FRIDGE: finishing again moves it to DONE, never a second open run
            assertTrue(actions.finishStage(id))
            assertFalse(actions.finishStage(id))
            assertEquals(0, store.runs.value.count { it.isOpen })
            assertEquals(2, store.runs.value.size)
        }
}
