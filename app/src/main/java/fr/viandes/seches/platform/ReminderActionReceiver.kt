package fr.viandes.seches.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import fr.viandes.seches.MainActivity
import fr.viandes.seches.ViandesApp
import kotlinx.coroutines.launch

/** Notification buttons "Fait" and "Reporter 1 jour" run the same use cases as the UI (R13). */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val runId = intent.getLongExtra(ReminderReceiver.EXTRA_RUN_ID, -1)
        val pieceId = intent.getLongExtra(MainActivity.EXTRA_PIECE_ID, -1)
        if (runId < 0 || pieceId < 0) return
        val app = context.applicationContext as ViandesApp
        val pending = goAsync()
        app.appScope.launch {
            try {
                val container = app.container
                // Ignore stale notifications: only act while this exact run is still the open one.
                val summary = container.pieces.getSummary(pieceId)
                if (summary?.openRun?.id == runId) {
                    when (intent.action) {
                        ACTION_DONE -> {
                            val seasoning = container.recipes.get(summary.piece.recipeId)?.fridgeSeasoningDefault
                            container.pieceActions.finishStage(pieceId, seasoning)
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

    companion object {
        const val ACTION_DONE = "fr.viandes.seches.ACTION_DONE"
        const val ACTION_POSTPONE = "fr.viandes.seches.ACTION_POSTPONE"
    }
}
