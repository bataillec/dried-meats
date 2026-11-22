package me.bataillec.meatmelater.data

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import me.bataillec.meatmelater.domain.FakeAlarmScheduler
import me.bataillec.meatmelater.domain.FakeClock
import me.bataillec.meatmelater.domain.FakeSettingsStore
import me.bataillec.meatmelater.domain.MeatType
import me.bataillec.meatmelater.domain.PieceActions
import me.bataillec.meatmelater.domain.Recipe
import me.bataillec.meatmelater.domain.ReminderPlanner
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
class BackupServiceTest {
    private lateinit var db: AppDatabase
    private lateinit var backup: BackupService

    @Before
    fun setUp() {
        db =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
                .setDriver(AndroidSQLiteDriver())
                .addCallback(OneOpenRunGuard)
                .build()
        backup = BackupService(db)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun populate(): String {
        val meats = RoomMeatTypeStore(db)
        val recipes = RoomRecipeStore(db)
        val pieces = RoomPieceStore(db)
        val meat = meats.save(MeatType(0, "Bœuf"))
        val recipe = recipes.save(Recipe(0, meat, "Classique", BigDecimal("25"), BigDecimal("5.5"), BigDecimal("2")))
        val clock = FakeClock(1_000_000_000_000)
        val actions =
            PieceActions(
                pieces,
                recipes,
                FakeSettingsStore(),
                ReminderPlanner(pieces, FakeAlarmScheduler(), clock),
                clock,
            )
        actions.createPiece("Bœuf 1", recipe, 1250, BigDecimal("12"))
        return backup.export()
    }

    @Test
    fun `R21 export then import restores identical data`() =
        runBlocking {
            val json = populate()
            backup.import(json)
            assertEquals(json, backup.export())
        }

    @Test
    fun `R21 import replaces existing data`() =
        runBlocking {
            val json = populate()
            RoomMeatTypeStore(db).save(MeatType(0, "Extra"))
            backup.import(json)
            assertEquals(1, db.meatTypeDao().all().size)
        }

    @Test
    fun `R21 garbage and wrong format are rejected without touching data`() =
        runBlocking {
            val json = populate()
            assertThrows(BackupFormatException::class.java) { runBlocking { backup.import("not json") } }
            assertThrows(BackupFormatException::class.java) {
                runBlocking { backup.import(json.replace("meat-me-later-backup", "other")) }
            }
            assertEquals(json, backup.export())
        }

    @Test
    fun `R21 inconsistent content is rejected`() =
        runBlocking {
            val json = populate()
            val badDivisor = json.replace("\"vacuumDivisor\": \"2\"", "\"vacuumDivisor\": \"0\"")
            assertThrows(BackupFormatException::class.java) { runBlocking { backup.import(badDivisor) } }
            val orphanRun = json.replace("\"pieceId\": 1", "\"pieceId\": 99")
            assertThrows(BackupFormatException::class.java) { runBlocking { backup.import(orphanRun) } }
            val doneWithOpenRun = json.replace("\"status\": \"VACUUM\"", "\"status\": \"DONE\"")
            assertThrows(BackupFormatException::class.java) { runBlocking { backup.import(doneWithOpenRun) } }
            assertEquals(json, backup.export())
        }

    @Test
    fun `R21 backups written before the rename are still imported`() =
        runBlocking {
            val json = populate()
            val legacy = json.replace("meat-me-later-backup", "viandes-seches-backup")
            backup.import(legacy)
            assertEquals(json, backup.export())
        }
}
