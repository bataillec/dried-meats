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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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
        val recipe = recipes.save(Recipe(0, "Classique", BigDecimal("25"), BigDecimal("5.5"), BigDecimal("2")))
        val clock = FakeClock(1_000_000_000_000)
        val actions =
            PieceActions(
                pieces,
                recipes,
                meats,
                FakeSettingsStore(),
                ReminderPlanner(pieces, FakeAlarmScheduler(), clock),
                clock,
            )
        actions.createPiece("Bœuf 1", recipe, meat, 1250, BigDecimal("12"))
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

    private val v1 =
        """
        {
          "format": "meat-me-later-backup",
          "version": 1,
          "meatTypes": [
            {"id": 1, "name": "Bœuf", "archived": false},
            {"id": 2, "name": "Porc", "archived": false}
          ],
          "recipes": [
            {"id": 1, "meatTypeId": 1, "name": "Classique", "saltGPerKg": "25", "sugarGPerKg": "5", "pepperGPerKg": "2",
             "fridgeSeasoningDefault": null, "vacuumDivisor": "2", "vacuumOffsetDays": "1", "fridgeMinDays": 14, "archived": false},
            {"id": 2, "meatTypeId": 2, "name": "Jambon", "saltGPerKg": "30", "sugarGPerKg": "2", "pepperGPerKg": "1",
             "fridgeSeasoningDefault": null, "vacuumDivisor": "2", "vacuumOffsetDays": "1", "fridgeMinDays": 14, "archived": false}
          ],
          "pieces": [
            {"id": 1, "label": "Bœuf 1", "recipeId": 1, "weightG": 1250, "diameterCm": "12", "saltG": "31.3", "sugarG": "6.3",
             "pepperG": "2.5", "fridgeSeasoning": null, "status": "VACUUM", "createdAt": 1000},
            {"id": 2, "label": "Porc 1", "recipeId": 2, "weightG": 900, "diameterCm": "9", "saltG": "27", "sugarG": "1.8",
             "pepperG": "0.9", "fridgeSeasoning": null, "status": "CANCELLED", "createdAt": 2000}
          ],
          "stageRuns": [
            {"id": 1, "pieceId": 1, "stage": "VACUUM", "startedAt": 1000, "plannedEndAt": 9000, "endedAt": null,
             "postponedDays": 0, "note": null},
            {"id": 2, "pieceId": 2, "stage": "VACUUM", "startedAt": 2000, "plannedEndAt": 9000, "endedAt": 3000,
             "postponedDays": 0, "note": null}
          ]
        }
        """.trimIndent()

    @Test
    fun `R21 a version 1 backup is imported and each piece takes its recipe's meat type`() =
        runBlocking {
            backup.import(v1)
            assertEquals(
                mapOf(1L to 1L, 2L to 2L),
                db.pieceDao().all().associate { it.id to it.meatTypeId },
            )
            assertEquals(2, db.recipeDao().all().size)
            assertEquals(2, db.stageRunDao().all().size)
            val exported = backup.export()
            assertTrue(exported.contains("\"version\": 2"))
            val recipesSection = exported.substringAfter("\"recipes\"").substringBefore("\"pieces\"")
            assertFalse(recipesSection.contains("meatTypeId"))
            backup.import(exported)
            assertEquals(exported, backup.export())
        }

    @Test
    fun `R21 a version 1 backup with a piece whose recipe is missing is rejected`() =
        runBlocking {
            val before = populate()
            assertThrows(BackupFormatException::class.java) {
                runBlocking { backup.import(v1.replace("\"recipeId\": 2", "\"recipeId\": 99")) }
            }
            assertEquals(before, backup.export())
        }

    @Test
    fun `R21 a version 2 backup whose piece points to an unknown meat type is rejected`() =
        runBlocking {
            val json = populate()
            val bad = json.replace("\"meatTypeId\": 1", "\"meatTypeId\": 42")
            assertThrows(BackupFormatException::class.java) { runBlocking { backup.import(bad) } }
            assertEquals(json, backup.export())
        }

    @Test
    fun `R21 unknown versions are rejected`() =
        runBlocking {
            val json = populate()
            assertThrows(BackupFormatException::class.java) {
                runBlocking { backup.import(json.replace("\"version\": 2", "\"version\": 3")) }
            }
            assertThrows(BackupFormatException::class.java) {
                runBlocking { backup.import(json.replace("\"version\": 2", "\"version\": 0")) }
            }
            Unit
        }
}
