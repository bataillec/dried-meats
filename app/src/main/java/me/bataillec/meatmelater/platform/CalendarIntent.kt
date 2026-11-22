package me.bataillec.meatmelater.platform

import android.content.Intent
import android.provider.CalendarContract

/**
 * R22: opens the calendar app's own "new event" screen. No calendar permission, no network,
 * nothing is created without the user's confirmation. The event is a one-off copy.
 */
object CalendarIntent {
    private const val EVENT_LENGTH_MILLIS = 30 * 60 * 1000L

    fun insertEvent(
        title: String,
        startMillis: Long,
        description: String,
    ): Intent =
        Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, title)
            .putExtra(CalendarContract.Events.DESCRIPTION, description)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startMillis + EVENT_LENGTH_MILLIS)
}
