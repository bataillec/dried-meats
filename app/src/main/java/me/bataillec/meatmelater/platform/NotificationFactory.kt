package me.bataillec.meatmelater.platform

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import me.bataillec.meatmelater.MainActivity
import me.bataillec.meatmelater.R
import me.bataillec.meatmelater.domain.PieceSummary
import me.bataillec.meatmelater.domain.Stage
import me.bataillec.meatmelater.domain.StageRun

/** R13: one notification per piece with "Fait" and "Reporter 1 jour". */
class NotificationFactory(
    private val context: Context,
) {
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { description = context.getString(R.string.channel_description) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // guarded by hasPermission() above
    fun show(
        run: StageRun,
        summary: PieceSummary,
    ): Boolean {
        ensureChannel()
        if (!hasPermission()) return false
        val action =
            context.getString(
                if (run.stage ==
                    Stage.VACUUM
                ) {
                    R.string.action_leave_vacuum
                } else {
                    R.string.action_leave_fridge
                },
            )
        val body =
            if (run.stage == Stage.FRIDGE) {
                context.getString(R.string.notification_fridge_body)
            } else {
                action
            }
        val open =
            PendingIntent.getActivity(
                context,
                run.id.toInt(),
                Intent(context, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_PIECE_ID, summary.piece.id)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("${summary.piece.label} · $action")
                .setContentText("${summary.meatTypeName} · ${summary.recipeName}")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText("${summary.meatTypeName} · ${summary.recipeName}\n$body"),
                ).setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(open)
                .setAutoCancel(true)
                .addAction(
                    0,
                    context.getString(R.string.notification_done),
                    actionIntent(ReminderActionReceiver.ACTION_DONE, run, summary),
                ).addAction(
                    0,
                    context.getString(R.string.notification_postpone),
                    actionIntent(ReminderActionReceiver.ACTION_POSTPONE, run, summary),
                ).build()
        return try {
            NotificationManagerCompat.from(context).notify(run.id.toInt(), notification)
            true
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call: the home list still pins the overdue stage (R15).
            false
        }
    }

    fun dismiss(runId: Long) = NotificationManagerCompat.from(context).cancel(runId.toInt())

    private fun actionIntent(
        action: String,
        run: StageRun,
        summary: PieceSummary,
    ): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            run.id.toInt() * 10 + if (action == ReminderActionReceiver.ACTION_DONE) 1 else 2,
            Intent(context, ReminderActionReceiver::class.java)
                .setAction(action)
                .putExtra(ReminderReceiver.EXTRA_RUN_ID, run.id)
                .putExtra(MainActivity.EXTRA_PIECE_ID, summary.piece.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val CHANNEL_ID = "stage_reminders"
    }
}
