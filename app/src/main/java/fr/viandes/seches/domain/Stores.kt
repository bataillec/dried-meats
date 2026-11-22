package fr.viandes.seches.domain

import kotlinx.coroutines.flow.Flow

/** A piece together with what the lists and notifications need to display. */
data class PieceSummary(
    val piece: Piece,
    val recipeName: String,
    val meatTypeName: String,
    val openRun: StageRun?,
)

data class PieceDetail(
    val summary: PieceSummary,
    val runs: List<StageRun>,
)

/** A finished piece for the statistics screen (R19). */
data class FinishedPiece(
    val summary: PieceSummary,
    val runs: List<StageRun>,
)

interface MeatTypeStore {
    fun observeAll(): Flow<List<MeatType>>

    suspend fun get(id: Long): MeatType?

    suspend fun save(meatType: MeatType): Long

    /** R1: delete is refused (returns false) while recipes still use the meat type. */
    suspend fun delete(id: Long): Boolean

    suspend fun count(): Int
}

interface RecipeStore {
    fun observeAll(): Flow<List<Recipe>>

    fun observe(id: Long): Flow<Recipe?>

    suspend fun get(id: Long): Recipe?

    suspend fun save(recipe: Recipe): Long

    suspend fun setArchived(
        id: Long,
        archived: Boolean,
    )

    /** R3: returns false (and deletes nothing) while pieces use the recipe. */
    suspend fun delete(id: Long): Boolean
}

interface PieceStore {
    /** Pieces that are not DONE/CANCELLED, with their open run. */
    fun observeActive(): Flow<List<PieceSummary>>

    fun observeDetail(id: Long): Flow<PieceDetail?>

    fun observeFinished(): Flow<List<FinishedPiece>>

    suspend fun getSummary(id: Long): PieceSummary?

    suspend fun openRuns(): List<Pair<StageRun, PieceSummary>>

    /** Inserts the piece and its first run atomically (R6, R7); returns the piece id. */
    suspend fun create(
        piece: Piece,
        firstRun: StageRun,
    ): Long

    /** Applies a state-machine transition atomically (R8, R9, R11). */
    suspend fun applyTransition(
        pieceId: Long,
        transition: Transition,
    )

    suspend fun updateRun(run: StageRun)

    suspend fun updateFridgeSeasoning(
        pieceId: Long,
        seasoning: String?,
    )

    /** R11: deletes the piece and its runs. */
    suspend fun delete(pieceId: Long)

    suspend fun countByRecipe(recipeId: Long): Int

    suspend fun labels(): List<String>
}

enum class ThemeMode { LIGHT, DARK, OLED, SYSTEM }

/** R24-R26: global defaults and appearance. */
data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val reminderHour: Int = 18,
    val reminderMinute: Int = 0,
    val defaultFridgeMinDays: Int = Recipe.DEFAULT_FRIDGE_MIN_DAYS,
    val defaultVacuumDivisor: java.math.BigDecimal = Recipe.DEFAULT_VACUUM_DIVISOR,
    val defaultVacuumOffsetDays: java.math.BigDecimal = Recipe.DEFAULT_VACUUM_OFFSET_DAYS,
    val seeded: Boolean = false,
    val batteryHintShown: Boolean = false,
) {
    val reminderTime: java.time.LocalTime get() = java.time.LocalTime.of(reminderHour, reminderMinute)
}

interface SettingsStore {
    fun observe(): Flow<AppSettings>

    suspend fun current(): AppSettings

    suspend fun update(transform: (AppSettings) -> AppSettings)
}
