package me.bataillec.meatmelater.data

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.bataillec.meatmelater.domain.FakeAlarmScheduler
import me.bataillec.meatmelater.domain.FakeClock
import me.bataillec.meatmelater.domain.FakeSettingsStore
import me.bataillec.meatmelater.domain.MeatType
import me.bataillec.meatmelater.domain.PieceActions
import me.bataillec.meatmelater.domain.PieceStatus
import me.bataillec.meatmelater.domain.Recipe
import me.bataillec.meatmelater.domain.ReminderPlanner
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
class RoomStoresTest {
    private lateinit var db: AppDatabase
    private lateinit var meats: RoomMeatTypeStore
    private lateinit var recipes: RoomRecipeStore
    private lateinit var pieces: RoomPieceStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room
                .inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .setDriver(AndroidSQLiteDriver())
                .addCallback(OneOpenRunGuard)
                .build()
        meats = RoomMeatTypeStore(db)
        recipes = RoomRecipeStore(db)
        pieces = RoomPieceStore(db)
    }

    @After
    fun tearDown() = db.close()

    private var meatId = 0L

    private fun actions(clock: FakeClock): PieceActions =
        PieceActions(
            pieces,
            recipes,
            meats,
            FakeSettingsStore(),
            ReminderPlanner(pieces, FakeAlarmScheduler(), clock),
            clock,
        )

    /** Seeds one meat type (kept in [meatId]) and one recipe; the recipe has no meat type. */
    private suspend fun seedRecipe(): Long {
        meatId = meats.save(MeatType(0, "Bœuf"))
        return recipes.save(Recipe(0, "Classique", BigDecimal("25"), BigDecimal("5.5"), BigDecimal("2")))
    }

    @Test
    fun `R2 recipe round trips exact decimals`() =
        runBlocking {
            val id = seedRecipe()
            val r = recipes.get(id)!!
            assertEquals(BigDecimal("5.5"), r.sugarGPerKg)
            assertEquals(14, r.fridgeMinDays)
        }

    @Test
    fun `R3 recipe with pieces cannot be deleted but can be archived`() =
        runBlocking {
            val recipeId = seedRecipe()
            val clock = FakeClock(1_000_000_000_000)
            val actions = actions(clock)
            actions.createPiece("Bœuf 1", recipeId, meatId, 1250, BigDecimal("12"))
            assertFalse(recipes.delete(recipeId))
            recipes.setArchived(recipeId, true)
            assertTrue(recipes.get(recipeId)!!.archived)
        }

    @Test
    fun `R1 meat type used by a piece cannot be deleted, an unused one can`() =
        runBlocking {
            val recipeId = seedRecipe()
            val unused = meats.save(MeatType(0, "Porc"))
            assertTrue(meats.delete(unused))
            assertTrue(meats.delete(meats.save(MeatType(0, "Canard"))))
            actions(FakeClock(1_000_000_000_000)).createPiece("Bœuf 1", recipeId, meatId, 1250, BigDecimal("12"))
            assertFalse(meats.delete(meatId))
        }

    @Test
    fun `R6 a piece keeps its own meat type and the same recipe serves any meat`() =
        runBlocking {
            val recipeId = seedRecipe()
            val pork = meats.save(MeatType(0, "Porc"))
            val actions = actions(FakeClock(1_000_000_000_000))
            val beefPiece = actions.createPiece("Bœuf 1", recipeId, meatId, 1000, BigDecimal("10"))!!
            val porkPiece = actions.createPiece("Porc 1", recipeId, pork, 1000, BigDecimal("10"))!!
            assertEquals("Bœuf", pieces.getSummary(beefPiece)!!.meatTypeName)
            assertEquals("Porc", pieces.getSummary(porkPiece)!!.meatTypeName)
            assertEquals(pork, pieces.getSummary(porkPiece)!!.piece.meatTypeId)
            assertEquals("Classique", pieces.getSummary(porkPiece)!!.recipeName)
        }

    @Test
    fun `R6 a piece needs an existing meat type`() =
        runBlocking {
            val recipeId = seedRecipe()
            val actions = actions(FakeClock(1_000_000_000_000))
            assertEquals(null, actions.createPiece("X", recipeId, 999, 1000, BigDecimal("10")))
        }

    @Test
    fun `R6 R8 R9 piece lifecycle persists with snapshot and two runs`() =
        runBlocking {
            val recipeId = seedRecipe()
            val clock = FakeClock(1_000_000_000_000)
            val scheduler = FakeAlarmScheduler()
            val actions =
                PieceActions(
                    pieces,
                    recipes,
                    meats,
                    FakeSettingsStore(),
                    ReminderPlanner(pieces, scheduler, clock),
                    clock,
                )
            val id = actions.createPiece("Bœuf 1", recipeId, meatId, 1250, BigDecimal("12"))!!
            recipes.save(recipes.get(recipeId)!!.copy(saltGPerKg = BigDecimal("99")))
            val created = pieces.getSummary(id)!!
            assertEquals(BigDecimal("31.3"), created.piece.doses.saltG)
            assertNotNull(created.openRun)
            assertEquals(1, pieces.observeActive().first().size)
            assertEquals(1, scheduler.alarms.size)

            clock.millis += 86_400_000L
            assertTrue(actions.finishStage(id, "herbes"))
            val detail = pieces.observeDetail(id).first()!!
            assertEquals(PieceStatus.FRIDGE, detail.summary.piece.status)
            assertEquals("herbes", detail.summary.piece.fridgeSeasoning)
            assertEquals(2, detail.runs.size)
            assertEquals(1, detail.runs.count { it.isOpen })

            actions.finishStage(id)
            assertEquals(PieceStatus.DONE, pieces.getSummary(id)!!.piece.status)
            assertEquals(1, pieces.observeFinished().first().size)
            assertTrue(pieces.observeActive().first().isEmpty())
            assertTrue(scheduler.alarms.isEmpty())
        }

    @Test
    fun `R11 deleting a piece cascades to its runs`() =
        runBlocking {
            val recipeId = seedRecipe()
            val clock = FakeClock(1_000_000_000_000)
            val actions = actions(clock)
            val id = actions.createPiece("Bœuf 1", recipeId, meatId, 1000, BigDecimal("10"))!!
            actions.delete(id)
            assertTrue(db.stageRunDao().all().isEmpty())
            assertEquals(0, pieces.countByRecipe(recipeId))
        }

    @Test
    fun `R7 the database refuses a second open run for the same piece`() =
        runBlocking {
            val recipeId = seedRecipe()
            val clock = FakeClock(1_000_000_000_000)
            val actions = actions(clock)
            val id = actions.createPiece("Bœuf 1", recipeId, meatId, 1000, BigDecimal("10"))!!
            val open = db.stageRunDao().openRuns().single()
            val duplicate = open.copy(id = 0)
            val failed = runCatching { db.stageRunDao().insert(duplicate) }.isFailure
            assertTrue(failed)
            assertEquals(1, db.stageRunDao().forPiece(id).size)
        }

    @Test
    fun `R23 note update touches only the note`() =
        runBlocking {
            val recipeId = seedRecipe()
            val clock = FakeClock(1_000_000_000_000)
            val actions = actions(clock)
            val id = actions.createPiece("Bœuf 1", recipeId, meatId, 1000, BigDecimal("10"))!!
            val run = pieces.getSummary(id)!!.openRun!!
            actions.postponeOneDay(id)
            actions.saveNote(run.id, "souple")
            val after = pieces.getSummary(id)!!.openRun!!
            assertEquals("souple", after.note)
            assertEquals(1, after.postponedDays)
        }
}
