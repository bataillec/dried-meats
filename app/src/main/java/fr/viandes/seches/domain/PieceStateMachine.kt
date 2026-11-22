package fr.viandes.seches.domain

/** Result of a state transition: the updated closed run and, when a next stage starts, the new run to insert. */
data class Transition(
    val status: PieceStatus,
    val closedRun: StageRun,
    val nextRun: StageRun?,
    val fridgeSeasoning: String?,
)

/**
 * R8, R9, R10, R11, R23. Pure functions over immutable models; persistence and reminder
 * reconciliation happen in the use-case layer around these calls.
 *
 * VACUUM --finish--> FRIDGE --finish--> DONE; cancel from VACUUM or FRIDGE -> CANCELLED.
 */
class PieceStateMachine(
    private val clock: Clock,
    private val plannedEnd: PlannedEndCalculator,
) {
    /** Creates the first run for a new piece (R7): it starts now in VACUUM. */
    fun startVacuum(
        pieceId: Long,
        durationDays: Int,
    ): StageRun {
        require(durationDays >= 1) { "duration must be >= 1 day" }
        val now = clock.nowMillis()
        return StageRun(
            id = 0,
            pieceId = pieceId,
            stage = Stage.VACUUM,
            startedAt = now,
            plannedEndAt = plannedEnd.plannedEnd(now, durationDays),
        )
    }

    /**
     * Finishes the open run now (R9: actual end, next stage starts at that moment).
     * From VACUUM the stage-2 seasoning is chosen (null or blank = none) and a FRIDGE run of
     * [fridgeDays] is opened; from FRIDGE the piece is DONE.
     */
    fun finish(
        status: PieceStatus,
        openRun: StageRun,
        fridgeSeasoning: String?,
        fridgeDays: Int,
    ): Transition {
        require(openRun.isOpen) { "stage run is already closed" }
        val now = clock.nowMillis()
        val closed = openRun.copy(endedAt = now)
        return when (status) {
            PieceStatus.VACUUM -> {
                require(openRun.stage == Stage.VACUUM) { "open run does not match status" }
                require(fridgeDays >= 1) { "fridge duration must be >= 1 day" }
                val seasoning = fridgeSeasoning?.trim()?.takeIf { it.isNotEmpty() }
                Transition(
                    status = PieceStatus.FRIDGE,
                    closedRun = closed,
                    nextRun =
                        StageRun(
                            id = 0,
                            pieceId = openRun.pieceId,
                            stage = Stage.FRIDGE,
                            startedAt = now,
                            plannedEndAt = plannedEnd.plannedEnd(now, fridgeDays),
                        ),
                    fridgeSeasoning = seasoning,
                )
            }
            PieceStatus.FRIDGE -> {
                require(openRun.stage == Stage.FRIDGE) { "open run does not match status" }
                Transition(PieceStatus.DONE, closed, null, fridgeSeasoning)
            }
            else -> error("cannot finish a piece in status $status")
        }
    }

    /** R11: cancelling closes the open run and cancels the piece. */
    fun cancel(
        status: PieceStatus,
        openRun: StageRun?,
    ): Transition {
        check(
            status == PieceStatus.VACUUM || status == PieceStatus.FRIDGE,
        ) { "cannot cancel a piece in status $status" }
        val run = requireNotNull(openRun) { "open stage run required" }
        return Transition(PieceStatus.CANCELLED, run.copy(endedAt = clock.nowMillis()), null, null)
    }

    /** R10: recomputes the planned end from the run's start. */
    fun changeDuration(
        run: StageRun,
        durationDays: Int,
    ): StageRun {
        require(run.isOpen) { "stage run is already closed" }
        require(durationDays >= 1) { "duration must be >= 1 day" }
        return run.copy(plannedEndAt = plannedEnd.plannedEnd(run.startedAt, durationDays))
    }

    /** R13, R23: "Reporter 1 jour" adds a day and counts the postponement. */
    fun postponeOneDay(run: StageRun): StageRun {
        require(run.isOpen) { "stage run is already closed" }
        return run.copy(
            plannedEndAt = plannedEnd.postponedByOneDay(run.plannedEndAt),
            postponedDays = run.postponedDays + 1,
        )
    }
}
