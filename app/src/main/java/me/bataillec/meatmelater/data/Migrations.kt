package me.bataillec.meatmelater.data

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * 1 -> 2 (issue #5): the meat type moves from the recipe to the piece.
 *
 * SQLite cannot drop a column that is part of a foreign key, and `piece.meat_type_id` must be NOT NULL
 * with a foreign key, so the three tables are rebuilt. Order matters: `stage_run` is copied aside and
 * dropped first, so that dropping `piece` cannot cascade-delete stage runs if enforcement were on. Pieces take the meat
 * type of their recipe, so nothing visible changes for existing data. Row ids are preserved.
 *
 * Dependency: this rebuild assumes SQLite foreign-key enforcement is OFF during the migration, which is the
 * case today (Room never issues `PRAGMA foreign_keys=ON` and the app does not either). If enforcement is ever
 * enabled, rewrite the migration (rebuild `recipe` first, or turn enforcement off around it).
 */
val MIGRATION_1_2: Migration =
    object : Migration(1, 2) {
        override suspend fun migrate(connection: SQLiteConnection) {
            STATEMENTS.forEach { connection.execSQL(it) }
        }
    }

private val STATEMENTS =
    listOf(
        "PRAGMA defer_foreign_keys = ON",
        // 1. stage_run aside (also drops its one-open-run trigger, recreated by OneOpenRunGuard on open).
        "CREATE TABLE `stage_run_old` AS SELECT * FROM `stage_run`",
        "DROP TABLE `stage_run`",
        // 2. piece with its own meat type, backfilled from the recipe.
        "CREATE TABLE `piece_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `label` TEXT NOT NULL, " +
            "`recipe_id` INTEGER NOT NULL, `meat_type_id` INTEGER NOT NULL, `weight_g` INTEGER NOT NULL, " +
            "`diameter_cm` TEXT NOT NULL, `salt_g` TEXT NOT NULL, `sugar_g` TEXT NOT NULL, " +
            "`pepper_g` TEXT NOT NULL, `fridge_seasoning` TEXT, `status` TEXT NOT NULL, " +
            "`created_at` INTEGER NOT NULL, " +
            "FOREIGN KEY(`recipe_id`) REFERENCES `recipe`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION , " +
            "FOREIGN KEY(`meat_type_id`) REFERENCES `meat_type`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )",
        "INSERT INTO `piece_new` (`id`, `label`, `recipe_id`, `meat_type_id`, `weight_g`, `diameter_cm`, " +
            "`salt_g`, `sugar_g`, `pepper_g`, `fridge_seasoning`, `status`, `created_at`) " +
            "SELECT p.`id`, p.`label`, p.`recipe_id`, " +
            "(SELECT r.`meat_type_id` FROM `recipe` r WHERE r.`id` = p.`recipe_id`), p.`weight_g`, " +
            "p.`diameter_cm`, p.`salt_g`, p.`sugar_g`, p.`pepper_g`, p.`fridge_seasoning`, p.`status`, " +
            "p.`created_at` FROM `piece` p",
        "DROP TABLE `piece`",
        "ALTER TABLE `piece_new` RENAME TO `piece`",
        "CREATE INDEX IF NOT EXISTS `index_piece_recipe_id` ON `piece` (`recipe_id`)",
        "CREATE INDEX IF NOT EXISTS `index_piece_meat_type_id` ON `piece` (`meat_type_id`)",
        // 3. recipe without its meat type.
        "CREATE TABLE `recipe_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
            "`salt_g_per_kg` TEXT NOT NULL, `sugar_g_per_kg` TEXT NOT NULL, `pepper_g_per_kg` TEXT NOT NULL, " +
            "`fridge_seasoning_default` TEXT, `vacuum_divisor` TEXT NOT NULL, " +
            "`vacuum_offset_days` TEXT NOT NULL, `fridge_min_days` INTEGER NOT NULL, `archived` INTEGER NOT NULL)",
        "INSERT INTO `recipe_new` (`id`, `name`, `salt_g_per_kg`, `sugar_g_per_kg`, `pepper_g_per_kg`, " +
            "`fridge_seasoning_default`, `vacuum_divisor`, `vacuum_offset_days`, `fridge_min_days`, `archived`) " +
            "SELECT `id`, `name`, `salt_g_per_kg`, `sugar_g_per_kg`, `pepper_g_per_kg`, " +
            "`fridge_seasoning_default`, `vacuum_divisor`, `vacuum_offset_days`, `fridge_min_days`, `archived` " +
            "FROM `recipe`",
        "DROP TABLE `recipe`",
        "ALTER TABLE `recipe_new` RENAME TO `recipe`",
        // 4. stage_run back, with its cascade and index.
        "CREATE TABLE `stage_run` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`piece_id` INTEGER NOT NULL, `stage` TEXT NOT NULL, `started_at` INTEGER NOT NULL, " +
            "`planned_end_at` INTEGER NOT NULL, `ended_at` INTEGER, `postponed_days` INTEGER NOT NULL, " +
            "`note` TEXT, FOREIGN KEY(`piece_id`) REFERENCES `piece`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "INSERT INTO `stage_run` SELECT * FROM `stage_run_old`",
        "DROP TABLE `stage_run_old`",
        "CREATE INDEX IF NOT EXISTS `index_stage_run_piece_id` ON `stage_run` (`piece_id`)",
    )
