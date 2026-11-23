package me.bataillec.meatmelater.data

import android.content.Context
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** Issue #5: meat type moves from recipe to piece (database 1 -> 2). */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            File(ApplicationProvider.getApplicationContext<Context>().cacheDir, "migration-test.db"),
            AndroidSQLiteDriver(),
            AppDatabase::class,
        )

    private fun rows(
        connection: SQLiteConnection,
        sql: String,
    ): List<List<String>> =
        connection.prepare(sql).use { stmt ->
            buildList {
                while (stmt.step()) {
                    add(List(stmt.getColumnCount()) { if (stmt.isNull(it)) "null" else stmt.getText(it) })
                }
            }
        }

    private fun seedVersion1(connection: SQLiteConnection) {
        connection.execSQL(
            "INSERT INTO meat_type (id, name, archived) VALUES (1, 'Bœuf', 0), (2, 'Porc', 0), (3, 'Canard', 0)",
        )
        val recipe = "INSERT INTO recipe VALUES (%d, %d, '%s', '25', '5', '2', %s, '2', '1', 14, 0)"
        connection.execSQL(recipe.format(1, 1, "Classique bœuf", "'herbes'"))
        connection.execSQL(recipe.format(2, 2, "Jambon", "NULL"))
        val piece = "INSERT INTO piece VALUES (%d, '%s', %d, 1250, '12', '31.3', '6.3', '2.5', %s, '%s', 1000)"
        connection.execSQL(piece.format(1, "Bœuf 1", 1, "NULL", "VACUUM"))
        connection.execSQL(piece.format(2, "Porc 1", 2, "'poivre'", "FRIDGE"))
        connection.execSQL(piece.format(3, "Bœuf 2", 1, "NULL", "DONE"))
        val run = "INSERT INTO stage_run VALUES (%d, %d, '%s', 1000, 2000, %s, 1, %s)"
        connection.execSQL(run.format(1, 1, "VACUUM", "NULL", "NULL"))
        connection.execSQL(run.format(2, 2, "VACUUM", "1500", "'souple'"))
        connection.execSQL(run.format(3, 2, "FRIDGE", "NULL", "NULL"))
        connection.execSQL(run.format(4, 3, "VACUUM", "1500", "NULL"))
        connection.execSQL(run.format(5, 3, "FRIDGE", "1900", "NULL"))
    }

    @Test
    fun `R1 R6 migration 1 to 2 backfills each piece with its recipe's meat type and keeps all data`() =
        runBlocking {
            helper.createDatabase(1).also {
                seedVersion1(it)
                it.close()
            }
            val db = helper.runMigrationsAndValidate(2, listOf(MIGRATION_1_2))

            assertEquals(
                listOf(listOf("1", "1"), listOf("2", "2"), listOf("3", "1")),
                rows(db, "SELECT id, meat_type_id FROM piece ORDER BY id"),
            )
            assertEquals(
                listOf("Bœuf 1", "Porc 1", "Bœuf 2"),
                rows(db, "SELECT label FROM piece ORDER BY id").map { it.single() },
            )
            // Snapshots and status untouched (R6).
            assertEquals(
                listOf("1", "Bœuf 1", "1", "1", "1250", "12", "31.3", "6.3", "2.5", "null", "VACUUM", "1000"),
                rows(db, "SELECT * FROM piece WHERE id = 1").single(),
            )
            assertEquals("poivre", rows(db, "SELECT fridge_seasoning FROM piece WHERE id = 2").single().single())

            // Recipes keep everything but the meat type.
            assertFalse(rows(db, "PRAGMA table_info(recipe)").any { it[1] == "meat_type_id" })
            assertEquals(
                listOf(listOf("1", "Classique bœuf", "herbes"), listOf("2", "Jambon", "null")),
                rows(db, "SELECT id, name, fridge_seasoning_default FROM recipe ORDER BY id"),
            )

            // The cascade must not have eaten the stage runs while piece was rebuilt.
            assertEquals(
                listOf(
                    listOf("1", "1", "VACUUM", "null", "null"),
                    listOf("2", "2", "VACUUM", "1500", "souple"),
                    listOf("3", "2", "FRIDGE", "null", "null"),
                    listOf("4", "3", "VACUUM", "1500", "null"),
                    listOf("5", "3", "FRIDGE", "1900", "null"),
                ),
                rows(db, "SELECT id, piece_id, stage, ended_at, note FROM stage_run ORDER BY id"),
            )

            // Indices are back and the foreign keys are consistent.
            val pieceIndices = rows(db, "PRAGMA index_list(piece)").map { it[1] }
            assertTrue(pieceIndices.containsAll(listOf("index_piece_recipe_id", "index_piece_meat_type_id")))
            assertTrue(rows(db, "PRAGMA index_list(stage_run)").any { it[1] == "index_stage_run_piece_id" })
            assertTrue(rows(db, "PRAGMA foreign_key_check").isEmpty())
            db.close()
        }

    @Test
    fun `R7 migrated database still accepts new rows and cascades piece deletes to runs`() =
        runBlocking {
            helper.createDatabase(1).also {
                seedVersion1(it)
                it.close()
            }
            val db = helper.runMigrationsAndValidate(2, listOf(MIGRATION_1_2))
            db.execSQL("PRAGMA foreign_keys = ON")
            db.execSQL(
                "INSERT INTO piece VALUES (4, 'Canard 1', 2, 3, 1000, '10', '1', '1', '1', NULL, 'VACUUM', 3000)",
            )
            assertEquals("3", rows(db, "SELECT meat_type_id FROM piece WHERE id = 4").single().single())
            db.execSQL("DELETE FROM piece WHERE id = 2")
            assertTrue(rows(db, "SELECT id FROM stage_run WHERE piece_id = 2").isEmpty())
            assertEquals(3, rows(db, "SELECT id FROM stage_run").size)
            db.close()
        }
}
