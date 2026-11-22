package fr.viandes.seches.domain

/** Platform alarm facade. A fake is used in tests; the Android one is in `platform`. */
interface AlarmScheduler {
    /** (Re)schedules the single exact alarm of a stage run (request code = run id). */
    fun schedule(
        runId: Long,
        triggerAtMillis: Long,
    )

    fun cancel(runId: Long)

    /** Run ids that currently have an alarm registered by this app. */
    fun scheduledIds(): Set<Long>

    /** Planned end for which a notification has already been posted, if any. */
    fun lastFiredPlannedEnd(runId: Long): Long?

    fun recordFired(
        runId: Long,
        plannedEndAt: Long,
    )

    fun clearFired(runId: Long)

    fun canScheduleExact(): Boolean
}

/**
 * R12, R14: the database is the source of truth; alarms are a rebuildable cache.
 * Call [reconcile] on app start, boot, package replaced, time/timezone change and after every edit.
 */
class ReminderPlanner(
    private val store: PieceStore,
    private val scheduler: AlarmScheduler,
    private val clock: Clock,
) {
    suspend fun reconcile() {
        val open = store.openRuns().map { it.first }
        val openIds = open.map { it.id }.toSet()
        (scheduler.scheduledIds() - openIds).forEach { scheduler.cancel(it) }
        val now = clock.nowMillis()
        for (run in open) {
            if (run.plannedEndAt > now) {
                // A moved planned end invalidates an earlier "already notified" record.
                scheduler.clearFired(run.id)
                scheduler.schedule(run.id, run.plannedEndAt)
            } else if (scheduler.lastFiredPlannedEnd(run.id) != run.plannedEndAt) {
                // Overdue and never notified (alarm lost across reboot/update): fire now.
                scheduler.schedule(run.id, now)
            }
            // Overdue and already notified: nothing to do, the home list pins it (R15).
        }
    }
}
