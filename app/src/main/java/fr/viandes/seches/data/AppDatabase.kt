package fr.viandes.seches.data

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

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
        const val NAME = "viandes-seches.db"

        fun build(context: Context): AppDatabase =
            Room
                .databaseBuilder(context, AppDatabase::class.java, NAME)
                .setDriver(BundledSQLiteDriver())
                .build()
    }
}
