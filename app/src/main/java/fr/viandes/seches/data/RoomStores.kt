package fr.viandes.seches.data

import androidx.room3.withWriteTransaction
import fr.viandes.seches.domain.FinishedPiece
import fr.viandes.seches.domain.MeatType
import fr.viandes.seches.domain.MeatTypeStore
import fr.viandes.seches.domain.Piece
import fr.viandes.seches.domain.PieceDetail
import fr.viandes.seches.domain.PieceStore
import fr.viandes.seches.domain.PieceSummary
import fr.viandes.seches.domain.Recipe
import fr.viandes.seches.domain.RecipeStore
import fr.viandes.seches.domain.StageRun
import fr.viandes.seches.domain.Transition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoomMeatTypeStore(
    private val db: AppDatabase,
) : MeatTypeStore {
    private val dao = db.meatTypeDao()

    override fun observeAll(): Flow<List<MeatType>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun get(id: Long): MeatType? = dao.get(id)?.toDomain()

    override suspend fun save(meatType: MeatType): Long =
        if (meatType.id == 0L) {
            dao.insert(meatType.toEntity())
        } else {
            dao.update(meatType.toEntity())
            meatType.id
        }

    override suspend fun delete(id: Long): Boolean =
        db.withWriteTransaction {
            if (dao.recipeCount(id) > 0) {
                false
            } else {
                dao.delete(id)
                true
            }
        }

    override suspend fun count(): Int = dao.count()
}

class RoomRecipeStore(
    private val db: AppDatabase,
) : RecipeStore {
    private val dao = db.recipeDao()

    override fun observeAll(): Flow<List<Recipe>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observe(id: Long): Flow<Recipe?> = dao.observe(id).map { it?.toDomain() }

    override suspend fun get(id: Long): Recipe? = dao.get(id)?.toDomain()

    override suspend fun save(recipe: Recipe): Long =
        if (recipe.id == 0L) {
            dao.insert(recipe.toEntity())
        } else {
            dao.update(recipe.toEntity())
            recipe.id
        }

    override suspend fun setArchived(
        id: Long,
        archived: Boolean,
    ) = dao.setArchived(id, archived)

    override suspend fun delete(id: Long): Boolean =
        db.withWriteTransaction {
            if (db.pieceDao().countByRecipe(id) > 0) {
                false
            } else {
                dao.delete(id)
                true
            }
        }
}

class RoomPieceStore(
    private val db: AppDatabase,
) : PieceStore {
    private val pieces = db.pieceDao()
    private val runs = db.stageRunDao()

    override fun observeActive(): Flow<List<PieceSummary>> =
        combine(pieces.observeActive(), runs.observeOpen()) { rows, open ->
            val byPiece = open.associateBy { it.pieceId }
            rows.map { row -> row.toSummary(byPiece[row.id]?.toDomain()) }
        }

    override fun observeDetail(id: Long): Flow<PieceDetail?> =
        combine(pieces.observeRow(id), runs.observeForPiece(id)) { row, rs ->
            row?.let {
                val domainRuns = rs.map { r -> r.toDomain() }
                PieceDetail(it.toSummary(domainRuns.firstOrNull { r -> r.isOpen }), domainRuns)
            }
        }

    override fun observeFinished(): Flow<List<FinishedPiece>> =
        combine(pieces.observeFinished(), runs.observeAll()) { rows, all ->
            val byPiece = all.groupBy { it.pieceId }
            rows.map { row -> FinishedPiece(row.toSummary(null), byPiece[row.id].orEmpty().map { it.toDomain() }) }
        }

    override suspend fun getSummary(id: Long): PieceSummary? {
        val row = pieces.getRow(id) ?: return null
        val open = runs.forPiece(id).firstOrNull { it.endedAt == null }
        return row.toSummary(open?.toDomain())
    }

    override suspend fun openRuns(): List<Pair<StageRun, PieceSummary>> =
        runs.openRuns().mapNotNull { run ->
            pieces.getRow(run.pieceId)?.let { run.toDomain() to it.toSummary(run.toDomain()) }
        }

    override suspend fun create(
        piece: Piece,
        firstRun: StageRun,
    ): Long =
        db.withWriteTransaction {
            val id = pieces.insert(piece.toEntity().copy(id = 0))
            runs.insert(firstRun.copy(pieceId = id).toEntity().copy(id = 0))
            id
        }

    override suspend fun applyTransition(
        pieceId: Long,
        transition: Transition,
    ) = db.withWriteTransaction {
        runs.update(transition.closedRun.toEntity())
        transition.nextRun?.let { runs.insert(it.copy(pieceId = pieceId).toEntity().copy(id = 0)) }
        pieces.setStatus(pieceId, transition.status.name, transition.fridgeSeasoning)
    }

    override suspend fun updateRun(run: StageRun) = runs.update(run.toEntity())

    override suspend fun updateFridgeSeasoning(
        pieceId: Long,
        seasoning: String?,
    ) = pieces.setFridgeSeasoning(pieceId, seasoning)

    override suspend fun delete(pieceId: Long) = pieces.delete(pieceId)

    override suspend fun countByRecipe(recipeId: Long): Int = pieces.countByRecipe(recipeId)
}
