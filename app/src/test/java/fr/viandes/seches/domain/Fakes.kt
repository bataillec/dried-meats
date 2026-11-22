package fr.viandes.seches.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeAlarmScheduler : AlarmScheduler {
    val alarms = mutableMapOf<Long, Long>()
    private val fired = mutableMapOf<Long, Long>()
    var exactAllowed = true

    override fun schedule(
        runId: Long,
        triggerAtMillis: Long,
    ) {
        alarms[runId] = triggerAtMillis
    }

    override fun cancel(runId: Long) {
        alarms.remove(runId)
    }

    override fun scheduledIds(): Set<Long> = alarms.keys.toSet()

    override fun lastFiredPlannedEnd(runId: Long): Long? = fired[runId]

    override fun recordFired(
        runId: Long,
        plannedEndAt: Long,
    ) {
        fired[runId] = plannedEndAt
        alarms.remove(runId)
    }

    override fun clearFired(runId: Long) {
        fired.remove(runId)
    }

    override fun canScheduleExact(): Boolean = exactAllowed
}

class FakeSettingsStore(
    initial: AppSettings = AppSettings(),
) : SettingsStore {
    private val state = MutableStateFlow(initial)

    override fun observe(): Flow<AppSettings> = state

    override suspend fun current(): AppSettings = state.value

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        state.value = transform(state.value)
    }
}

class FakeMeatTypeStore : MeatTypeStore {
    val items = MutableStateFlow<List<MeatType>>(emptyList())
    private var next = 1L
    var recipeCounts = mutableMapOf<Long, Int>()

    override fun observeAll(): Flow<List<MeatType>> = items

    override suspend fun get(id: Long): MeatType? = items.value.firstOrNull { it.id == id }

    override suspend fun save(meatType: MeatType): Long {
        val id = if (meatType.id == 0L) next++ else meatType.id
        items.value = items.value.filterNot { it.id == id } + meatType.copy(id = id)
        return id
    }

    override suspend fun delete(id: Long): Boolean {
        if ((recipeCounts[id] ?: 0) > 0) return false
        items.value = items.value.filterNot { it.id == id }
        return true
    }

    override suspend fun count(): Int = items.value.size
}

class FakeRecipeStore : RecipeStore {
    val items = MutableStateFlow<List<Recipe>>(emptyList())
    private var next = 1L
    var pieceCountOf: (Long) -> Int = { 0 }

    override fun observeAll(): Flow<List<Recipe>> = items

    override fun observe(id: Long): Flow<Recipe?> = items.map { l -> l.firstOrNull { it.id == id } }

    override suspend fun get(id: Long): Recipe? = items.value.firstOrNull { it.id == id }

    override suspend fun save(recipe: Recipe): Long {
        val id = if (recipe.id == 0L) next++ else recipe.id
        items.value = items.value.filterNot { it.id == id } + recipe.copy(id = id)
        return id
    }

    override suspend fun setArchived(
        id: Long,
        archived: Boolean,
    ) {
        items.value = items.value.map { if (it.id == id) it.copy(archived = archived) else it }
    }

    override suspend fun delete(id: Long): Boolean {
        if (pieceCountOf(id) > 0) return false
        items.value = items.value.filterNot { it.id == id }
        return true
    }
}

class FakePieceStore(
    private val recipes: FakeRecipeStore,
) : PieceStore {
    val pieces = MutableStateFlow<Map<Long, Piece>>(emptyMap())
    val runs = MutableStateFlow<List<StageRun>>(emptyList())
    private var nextPiece = 1L
    private var nextRun = 1L

    private suspend fun summary(piece: Piece): PieceSummary {
        val recipe = recipes.get(piece.recipeId)
        return PieceSummary(
            piece,
            recipe?.name ?: "?",
            "Bœuf",
            runs.value.firstOrNull {
                it.pieceId == piece.id &&
                    it.isOpen
            },
        )
    }

    override fun observeActive(): Flow<List<PieceSummary>> =
        pieces.map { m ->
            m.values
                .filter { it.status == PieceStatus.VACUUM || it.status == PieceStatus.FRIDGE }
                .map { p -> PieceSummary(p, "", "", runs.value.firstOrNull { it.pieceId == p.id && it.isOpen }) }
        }

    override fun observeDetail(id: Long): Flow<PieceDetail?> =
        pieces.map { m ->
            m[id]?.let { p ->
                val rs = runs.value.filter { it.pieceId == id }
                PieceDetail(PieceSummary(p, "", "", rs.firstOrNull { it.isOpen }), rs)
            }
        }

    override fun observeFinished(): Flow<List<FinishedPiece>> =
        pieces.map { m ->
            m.values.filter { it.status == PieceStatus.DONE }.map { p ->
                FinishedPiece(PieceSummary(p, "", "", null), runs.value.filter { it.pieceId == p.id })
            }
        }

    override suspend fun getSummary(id: Long): PieceSummary? = pieces.value[id]?.let { summary(it) }

    override suspend fun openRuns(): List<Pair<StageRun, PieceSummary>> =
        runs.value.filter { it.isOpen }.mapNotNull { r -> pieces.value[r.pieceId]?.let { r to summary(it) } }

    override suspend fun create(
        piece: Piece,
        firstRun: StageRun,
    ): Long {
        val id = nextPiece++
        pieces.value = pieces.value + (id to piece.copy(id = id))
        runs.value = runs.value + firstRun.copy(id = nextRun++, pieceId = id)
        return id
    }

    override suspend fun applyTransition(
        pieceId: Long,
        transition: Transition,
    ) {
        runs.value = runs.value.map { if (it.id == transition.closedRun.id) transition.closedRun else it }
        transition.nextRun?.let { runs.value = runs.value + it.copy(id = nextRun++, pieceId = pieceId) }
        val p = pieces.value.getValue(pieceId)
        pieces.value =
            pieces.value + (pieceId to p.copy(status = transition.status, fridgeSeasoning = transition.fridgeSeasoning))
    }

    override suspend fun updateRun(run: StageRun) {
        runs.value = runs.value.map { if (it.id == run.id) run else it }
    }

    override suspend fun updateFridgeSeasoning(
        pieceId: Long,
        seasoning: String?,
    ) {
        pieces.value = pieces.value + (pieceId to pieces.value.getValue(pieceId).copy(fridgeSeasoning = seasoning))
    }

    override suspend fun delete(pieceId: Long) {
        pieces.value = pieces.value - pieceId
        runs.value = runs.value.filterNot { it.pieceId == pieceId }
    }

    override suspend fun countByRecipe(recipeId: Long): Int = pieces.value.values.count { it.recipeId == recipeId }

    override suspend fun labels(): List<String> = pieces.value.values.map { it.label }
}
