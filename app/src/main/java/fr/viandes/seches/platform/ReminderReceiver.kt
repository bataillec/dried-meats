package fr.viandes.seches.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import fr.viandes.seches.ViandesApp
import kotlinx.coroutines.launch

/** Fires at a run's planned end (R12): posts the notification and records that it was shown. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val runId = intent.getLongExtra(EXTRA_RUN_ID, -1)
        if (runId < 0) return
        val app = context.applicationContext as ViandesApp
        val pending = goAsync()
        app.appScope.launch {
            try {
                val container = app.container
                val open = container.pieces.openRuns().firstOrNull { it.first.id == runId }
                if (open != null && open.first.plannedEndAt <= container.clock.nowMillis() + EARLY_TOLERANCE_MILLIS) {
                    // Only mark as notified when the notification really went out, so that granting the
                    // permission later lets the next reconcile re-post it.
                    if (NotificationFactory(context).show(open.first, open.second)) {
                        container.alarmScheduler.recordFired(runId, open.first.plannedEndAt)
                    }
                } else {
                    // Run closed or moved since the alarm was set: rebuild the alarm cache.
                    container.reminderPlanner.reconcile()
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "fr.viandes.seches.FIRE"
        const val EXTRA_RUN_ID = "run_id"
        private const val EARLY_TOLERANCE_MILLIS = 60_000L
    }
}
