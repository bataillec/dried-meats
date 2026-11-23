package me.bataillec.meatmelater.data

import androidx.room3.withReadTransaction
import androidx.room3.withWriteTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import me.bataillec.meatmelater.domain.FinishedPiece
import me.bataillec.meatmelater.domain.MeatType
import me.bataillec.meatmelater.domain.MeatTypeStore
import me.bataillec.meatmelater.domain.Piece
import me.bataillec.meatmelater.domain.PieceDetail
import me.bataillec.meatmelater.domain.PieceStore
import me.bataillec.meatmelater.domain.PieceSummary
import me.bataillec.meatmelater.domain.Recipe
import me.bataillec.meatmelater.domain.RecipeStore
import me.bataillec.meatmelater.domain.StageRun
import me.bataillec.meatmelater.domain.Transition

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
            if (dao.pieceCount(id) > 0) {
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
        db.withReadTransaction {
            runs.openRuns().mapNotNull { run ->
                pieces.getRow(run.pieceId)?.let { run.toDomain() to it.toSummary(run.toDomain()) }
            }
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
    ): Boolean =
        db.withWriteTransaction {
            val closed = transition.closedRun
            val endedAt = checkNotNull(closed.endedAt) { "closed run must have an end" }
            if (runs.closeIfOpen(closed.id, endedAt, closed.note) == 0) {
                false
            } else {
                transition.nextRun?.let { runs.insert(it.copy(pieceId = pieceId).toEntity().copy(id = 0)) }
                pieces.setStatus(pieceId, transition.status.name, transition.fridgeSeasoning)
                true
            }
        }

    override suspend fun updateNote(
        runId: Long,
        note: String?,
    ) = runs.setNote(runId, note)

    override suspend fun updateRun(run: StageRun) = runs.update(run.toEntity())

    override suspend fun updateFridgeSeasoning(
        pieceId: Long,
        seasoning: String?,
    ) = pieces.setFridgeSeasoning(pieceId, seasoning)

    override suspend fun delete(pieceId: Long) = pieces.delete(pieceId)

    override suspend fun countByRecipe(recipeId: Long): Int = pieces.countByRecipe(recipeId)

    override suspend fun labels(): List<String> = pieces.labels()
}
