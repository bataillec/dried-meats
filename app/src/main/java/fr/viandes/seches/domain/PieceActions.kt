package fr.viandes.seches.domain

import java.math.BigDecimal

/**
 * Use cases that change pieces and stage runs. Every mutation ends with
 * [ReminderPlanner.reconcile] (CLAUDE.md, R12).
 */
class PieceActions(
    private val pieces: PieceStore,
    private val recipes: RecipeStore,
    private val settings: SettingsStore,
    private val planner: ReminderPlanner,
    private val clock: Clock,
) {
    private suspend fun machine(): PieceStateMachine =
        PieceStateMachine(clock, PlannedEndCalculator(clock.zone(), settings.current().reminderTime))

    /** R6, R7: snapshots doses, starts VACUUM. Returns the new piece id, or null on unusable input. */
    suspend fun createPiece(
        label: String,
        recipeId: Long,
        weightG: Int,
        diameterCm: BigDecimal,
        vacuumDaysOverride: Int? = null,
    ): Long? {
        val recipe = recipes.get(recipeId) ?: return null
        val plan = PieceFactory.plan(weightG, diameterCm, recipe, vacuumDaysOverride) ?: return null
        val finalLabel = label.trim().ifEmpty { return null }
        val piece =
            Piece(
                id = 0,
                label = finalLabel,
                recipeId = recipeId,
                weightG = weightG,
                diameterCm = diameterCm,
                doses = plan.doses,
                fridgeSeasoning = null,
                status = PieceStatus.VACUUM,
                createdAt = clock.nowMillis(),
            )
        val id = pieces.create(piece, machine().startVacuum(0, plan.vacuumDays))
        planner.reconcile()
        return id
    }

    /** R8, R9: closes the open run now and moves on. [fridgeDaysOverride] replaces the recipe minimum. */
    suspend fun finishStage(
        pieceId: Long,
        fridgeSeasoning: String? = null,
        fridgeDaysOverride: Int? = null,
    ): Boolean {
        val summary = pieces.getSummary(pieceId) ?: return false
        val run = summary.openRun ?: return false
        val recipe = recipes.get(summary.piece.recipeId)
        val fridgeDays = fridgeDaysOverride ?: recipe?.fridgeMinDays ?: Recipe.DEFAULT_FRIDGE_MIN_DAYS
        val transition = machine().finish(summary.piece.status, run, fridgeSeasoning, fridgeDays)
        pieces.applyTransition(pieceId, transition)
        planner.reconcile()
        return true
    }

    /** R13, R23: "Reporter 1 jour". */
    suspend fun postponeOneDay(pieceId: Long): Boolean {
        val run = pieces.getSummary(pieceId)?.openRun ?: return false
        pieces.updateRun(machine().postponeOneDay(run))
        planner.reconcile()
        return true
    }

    /** R10: new duration in days from the run start (>= 1). */
    suspend fun changeDuration(
        pieceId: Long,
        days: Int,
    ): Boolean {
        if (days < 1) return false
        val run = pieces.getSummary(pieceId)?.openRun ?: return false
        pieces.updateRun(machine().changeDuration(run, days))
        planner.reconcile()
        return true
    }

    /** R11 */
    suspend fun cancel(pieceId: Long): Boolean {
        val summary = pieces.getSummary(pieceId) ?: return false
        val transition = machine().cancel(summary.piece.status, summary.openRun)
        pieces.applyTransition(pieceId, transition)
        planner.reconcile()
        return true
    }

    /** R11: removes the piece and its history; its reminder is cancelled by the reconcile. */
    suspend fun delete(pieceId: Long) {
        pieces.delete(pieceId)
        planner.reconcile()
    }

    /** R23 */
    suspend fun saveNote(
        run: StageRun,
        note: String?,
    ) {
        pieces.updateRun(run.copy(note = note?.trim()?.takeIf { it.isNotEmpty() }))
    }
}
