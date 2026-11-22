package me.bataillec.meatmelater.data

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL

@Database(
    entities = [MeatTypeEntity::class, RecipeEntity::class, PieceEntity::class, StageRunEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun meatTypeDao(): MeatTypeDao

    abstract fun recipeDao(): RecipeDao

    abstract fun pieceDao(): PieceDao

    abstract fun stageRunDao(): StageRunDao

    companion object {
        const val NAME = "meat-me-later.db"

        fun build(context: Context): AppDatabase =
            Room
                .databaseBuilder(context, AppDatabase::class.java, NAME)
                .setDriver(BundledSQLiteDriver())
                .addCallback(OneOpenRunGuard)
                .build()
    }
}

/**
 * design.md section 3: at most one open stage run per piece. Room annotations cannot express a
 * partial unique index, and an extra index would fail Room's schema validation, so the rule is a
 * trigger created on every open (idempotent). Triggers are not part of Room's schema check.
 */
object OneOpenRunGuard : RoomDatabase.Callback() {
    private const val SQL =
        "CREATE TRIGGER IF NOT EXISTS one_open_run_per_piece BEFORE INSERT ON stage_run " +
            "WHEN NEW.ended_at IS NULL AND EXISTS " +
            "(SELECT 1 FROM stage_run WHERE piece_id = NEW.piece_id AND ended_at IS NULL) " +
            "BEGIN SELECT RAISE(ABORT, 'one open stage run per piece'); END"

    override suspend fun onOpen(connection: SQLiteConnection) {
        connection.execSQL(SQL)
    }
}
