package me.bataillec.meatmelater.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch
import me.bataillec.meatmelater.MeatMeLaterApp

/** R14: rebuilds the alarm cache after reboot, app update, clock or time-zone change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action !in HANDLED_ACTIONS) return
        val app = context.applicationContext as MeatMeLaterApp
        val pending = goAsync()
        app.appScope.launch {
            try {
                app.container.reminderPlanner.reconcile()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        val HANDLED_ACTIONS =
            setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
            )
    }
}
