package me.bataillec.meatmelater.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch
import me.bataillec.meatmelater.MainActivity
import me.bataillec.meatmelater.MeatMeLaterApp
import me.bataillec.meatmelater.domain.Stage

/** Notification buttons "Fait" (fridge stage) and "Reporter 1 jour" run the same use cases as the UI (R13). */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val runId = intent.getLongExtra(ReminderReceiver.EXTRA_RUN_ID, -1)
        val pieceId = intent.getLongExtra(MainActivity.EXTRA_PIECE_ID, -1)
        if (runId < 0 || pieceId < 0) return
        val app = context.applicationContext as MeatMeLaterApp
        val pending = goAsync()
        app.appScope.launch {
            try {
                val container = app.container
                // Ignore stale notifications: only act while this exact run is still the open one.
                val summary = container.pieces.getSummary(pieceId)
                if (summary?.openRun?.id == runId) {
                    when (intent.action) {
                        // R8: leaving VACUUM needs the seasoning prompt, which lives in the app; only FRIDGE is one tap.
                        ACTION_DONE ->
                            if (isFridge(
                                    summary.openRun.stage,
                                )
                            ) {
                                container.pieceActions.finishStage(pieceId, null)
                            }
                        ACTION_POSTPONE -> container.pieceActions.postponeOneDay(pieceId)
                    }
                }
                NotificationFactory(context).dismiss(runId)
            } finally {
                pending.finish()
            }
        }
    }

    private fun isFridge(stage: Stage) = stage == Stage.FRIDGE

    companion object {
        const val ACTION_DONE = "me.bataillec.meatmelater.ACTION_DONE"
        const val ACTION_POSTPONE = "me.bataillec.meatmelater.ACTION_POSTPONE"
    }
}
