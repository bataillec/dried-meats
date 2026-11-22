package fr.viandes.seches.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import fr.viandes.seches.domain.AlarmScheduler

/**
 * Exact alarms (setExactAndAllowWhileIdle), request code = stage run id (R12, N5).
 * The set of scheduled ids and "already notified" markers live in SharedPreferences,
 * because the AlarmManager cannot be enumerated.
 */
class AndroidAlarmScheduler(
    private val context: Context,
) : AlarmScheduler {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = context.getSharedPreferences("alarms", Context.MODE_PRIVATE)
    private val lock = Any()

    private fun pending(runId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            runId.toInt(),
            Intent(context, ReminderReceiver::class.java)
                .setAction(ReminderReceiver.ACTION_FIRE)
                .putExtra(ReminderReceiver.EXTRA_RUN_ID, runId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    override fun schedule(
        runId: Long,
        triggerAtMillis: Long,
    ) {
        val intent = pending(runId)
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, intent)
        } else {
            // Without the exact-alarm permission fall back to an inexact but Doze-aware alarm.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, intent)
        }
        synchronized(lock) {
            prefs.edit().putStringSet(KEY_IDS, scheduledIds().plus(runId).map { it.toString() }.toSet()).commit()
        }
    }

    override fun cancel(runId: Long) {
        alarmManager.cancel(pending(runId))
        synchronized(lock) {
            prefs.edit().putStringSet(KEY_IDS, scheduledIds().minus(runId).map { it.toString() }.toSet()).commit()
        }
    }

    override fun scheduledIds(): Set<Long> =
        prefs
            .getStringSet(KEY_IDS, emptySet())
            .orEmpty()
            .mapNotNull {
                it.toLongOrNull()
            }.toSet()

    override fun lastFiredPlannedEnd(runId: Long): Long? = prefs.getLong("fired_$runId", NONE).takeIf { it != NONE }

    override fun recordFired(
        runId: Long,
        plannedEndAt: Long,
    ) {
        synchronized(lock) {
            prefs
                .edit()
                .putLong("fired_$runId", plannedEndAt)
                .putStringSet(KEY_IDS, scheduledIds().minus(runId).map { it.toString() }.toSet())
                .commit()
        }
    }

    override fun clearFired(runId: Long) {
        prefs.edit().remove("fired_$runId").apply()
    }

    override fun reset() {
        scheduledIds().forEach { alarmManager.cancel(pending(it)) }
        synchronized(lock) { prefs.edit().clear().commit() }
    }

    override fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private companion object {
        const val KEY_IDS = "scheduled_ids"
        const val NONE = Long.MIN_VALUE
    }
}
