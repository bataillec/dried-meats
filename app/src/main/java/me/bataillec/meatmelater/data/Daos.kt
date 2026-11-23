package me.bataillec.meatmelater.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MeatTypeDao {
    @Query("SELECT * FROM meat_type ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<MeatTypeEntity>>

    @Query("SELECT * FROM meat_type WHERE id = :id")
    suspend fun get(id: Long): MeatTypeEntity?

    @Insert
    suspend fun insert(entity: MeatTypeEntity): Long

    @Update
    suspend fun update(entity: MeatTypeEntity)

    @Query("DELETE FROM meat_type WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM meat_type")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM piece WHERE meat_type_id = :id")
    suspend fun pieceCount(id: Long): Int

    @Query("SELECT * FROM meat_type")
    suspend fun all(): List<MeatTypeEntity>

    @Query("DELETE FROM meat_type")
    suspend fun deleteAll()
}

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipe ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipe WHERE id = :id")
    fun observe(id: Long): Flow<RecipeEntity?>

    @Query("SELECT * FROM recipe WHERE id = :id")
    suspend fun get(id: Long): RecipeEntity?

    @Insert
    suspend fun insert(entity: RecipeEntity): Long

    @Update
    suspend fun update(entity: RecipeEntity)

    @Query("UPDATE recipe SET archived = :archived WHERE id = :id")
    suspend fun setArchived(
        id: Long,
        archived: Boolean,
    )

    @Query("DELETE FROM recipe WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM recipe")
    suspend fun all(): List<RecipeEntity>

    @Query("DELETE FROM recipe")
    suspend fun deleteAll()
}

/** Row of a piece joined with its recipe and meat names. */
data class PieceRow(
    val id: Long,
    val label: String,
    val recipeId: Long,
    val meatTypeId: Long,
    val weightG: Int,
    val diameterCm: String,
    val saltG: String,
    val sugarG: String,
    val pepperG: String,
    val fridgeSeasoning: String?,
    val status: String,
    val createdAt: Long,
    val recipeName: String,
    val meatTypeName: String,
)

private const val PIECE_ROW_SELECT =
    "SELECT p.id AS id, p.label AS label, p.recipe_id AS recipeId, p.meat_type_id AS meatTypeId, " +
        "p.weight_g AS weightG, p.diameter_cm AS diameterCm, p.salt_g AS saltG, p.sugar_g AS sugarG, " +
        "p.pepper_g AS pepperG, p.fridge_seasoning AS fridgeSeasoning, p.status AS status, " +
        "p.created_at AS createdAt, r.name AS recipeName, m.name AS meatTypeName " +
        "FROM piece p JOIN recipe r ON r.id = p.recipe_id " +
        "JOIN meat_type m ON m.id = p.meat_type_id "

@Dao
interface PieceDao {
    @Query(PIECE_ROW_SELECT + "WHERE p.status IN ('VACUUM', 'FRIDGE') ORDER BY p.created_at DESC")
    fun observeActive(): Flow<List<PieceRow>>

    @Query(PIECE_ROW_SELECT + "WHERE p.status = 'DONE' ORDER BY p.created_at DESC")
    fun observeFinished(): Flow<List<PieceRow>>

    @Query(PIECE_ROW_SELECT + "WHERE p.id = :id")
    fun observeRow(id: Long): Flow<PieceRow?>

    @Query(PIECE_ROW_SELECT + "WHERE p.id = :id")
    suspend fun getRow(id: Long): PieceRow?

    @Query("SELECT * FROM piece WHERE id = :id")
    suspend fun getEntity(id: Long): PieceEntity?

    @Insert
    suspend fun insert(entity: PieceEntity): Long

    @Update
    suspend fun update(entity: PieceEntity)

    @Query("DELETE FROM piece WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM piece WHERE recipe_id = :recipeId")
    suspend fun countByRecipe(recipeId: Long): Int

    @Query("SELECT * FROM piece")
    suspend fun all(): List<PieceEntity>

    @Query("SELECT label FROM piece")
    suspend fun labels(): List<String>

    @Query("DELETE FROM piece")
    suspend fun deleteAll()

    @Query("UPDATE piece SET status = :status, fridge_seasoning = :fridgeSeasoning WHERE id = :id")
    suspend fun setStatus(
        id: Long,
        status: String,
        fridgeSeasoning: String?,
    )

    @Query("UPDATE piece SET fridge_seasoning = :seasoning WHERE id = :id")
    suspend fun setFridgeSeasoning(
        id: Long,
        seasoning: String?,
    )
}

@Dao
interface StageRunDao {
    @Insert
    suspend fun insert(entity: StageRunEntity): Long

    @Update
    suspend fun update(entity: StageRunEntity)

    @Query("SELECT * FROM stage_run WHERE piece_id = :pieceId ORDER BY started_at, id")
    fun observeForPiece(pieceId: Long): Flow<List<StageRunEntity>>

    @Query("SELECT * FROM stage_run WHERE piece_id = :pieceId ORDER BY started_at, id")
    suspend fun forPiece(pieceId: Long): List<StageRunEntity>

    @Query("SELECT * FROM stage_run WHERE ended_at IS NULL")
    fun observeOpen(): Flow<List<StageRunEntity>>

    @Query("SELECT * FROM stage_run WHERE ended_at IS NULL")
    suspend fun openRuns(): List<StageRunEntity>

    @Query("SELECT * FROM stage_run")
    fun observeAll(): Flow<List<StageRunEntity>>

    @Query("SELECT * FROM stage_run")
    suspend fun all(): List<StageRunEntity>

    @Query("SELECT * FROM stage_run WHERE id = :id")
    suspend fun get(id: Long): StageRunEntity?

    @Query("UPDATE stage_run SET note = :note WHERE id = :id")
    suspend fun setNote(
        id: Long,
        note: String?,
    )

    /** Closes a run only if it is still open; returns the number of rows changed (0 or 1). */
    @Query("UPDATE stage_run SET ended_at = :endedAt, note = :note WHERE id = :id AND ended_at IS NULL")
    suspend fun closeIfOpen(
        id: Long,
        endedAt: Long,
        note: String?,
    ): Int

    @Query("DELETE FROM stage_run")
    suspend fun deleteAll()
}
