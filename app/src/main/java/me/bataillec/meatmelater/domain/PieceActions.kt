package me.bataillec.meatmelater.domain

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    /** Serialises read-modify-write actions (UI taps racing with notification buttons). */
    private val lock = Mutex()

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
        return lock.withLock {
            val summary = pieces.getSummary(pieceId) ?: return@withLock false
            val run = summary.openRun ?: return@withLock false
            val recipe = recipes.get(summary.piece.recipeId)
            val fridgeDays = fridgeDaysOverride ?: recipe?.fridgeMinDays ?: Recipe.DEFAULT_FRIDGE_MIN_DAYS
            val transition = machine().finish(summary.piece.status, run, fridgeSeasoning, fridgeDays)
            val applied = pieces.applyTransition(pieceId, transition)
            planner.reconcile()
            return@withLock applied
        }
    }

    /** R13, R23: "Reporter 1 jour". */
    suspend fun postponeOneDay(pieceId: Long): Boolean {
        return lock.withLock {
            val run = pieces.getSummary(pieceId)?.openRun ?: return@withLock false
            pieces.updateRun(machine().postponeOneDay(run))
            planner.reconcile()
            return@withLock true
        }
    }

    /** R10: new duration in days from the run start (>= 1). */
    suspend fun changeDuration(
        pieceId: Long,
        days: Int,
    ): Boolean {
        return lock.withLock {
            if (days < 1) return@withLock false
            val run = pieces.getSummary(pieceId)?.openRun ?: return@withLock false
            pieces.updateRun(machine().changeDuration(run, days))
            planner.reconcile()
            return@withLock true
        }
    }

    /** R11 */
    suspend fun cancel(pieceId: Long): Boolean {
        return lock.withLock {
            val summary = pieces.getSummary(pieceId) ?: return@withLock false
            val transition = machine().cancel(summary.piece.status, summary.openRun)
            val applied = pieces.applyTransition(pieceId, transition)
            planner.reconcile()
            return@withLock applied
        }
    }

    /** R11: removes the piece and its history; its reminder is cancelled by the reconcile. */
    suspend fun delete(pieceId: Long) {
        pieces.delete(pieceId)
        planner.reconcile()
    }

    /** R23: updates only the note column; reconcile keeps the "every run change reconciles" rule uniform. */
    suspend fun saveNote(
        runId: Long,
        note: String?,
    ) {
        pieces.updateNote(runId, note?.trim()?.takeIf { it.isNotEmpty() })
        planner.reconcile()
    }
}
