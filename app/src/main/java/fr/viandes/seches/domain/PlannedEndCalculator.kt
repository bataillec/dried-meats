package fr.viandes.seches.domain

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Computes planned end timestamps (R12, R26): the start date plus N calendar days, at the
 * configured reminder time of day, in the given zone. Result is an absolute UTC epoch millis.
 */
class PlannedEndCalculator(
    private val zone: ZoneId,
    private val reminderTime: LocalTime = DEFAULT_REMINDER_TIME,
) {
    fun plannedEnd(
        startMillis: Long,
        durationDays: Int,
    ): Long =
        Instant
            .ofEpochMilli(startMillis)
            .atZone(zone)
            .toLocalDate()
            .plusDays(durationDays.toLong())
            .atTime(reminderTime)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    /** "Reporter 1 jour" (R13): same time of day, one calendar day later. */
    fun postponedByOneDay(plannedEndMillis: Long): Long =
        Instant
            .ofEpochMilli(plannedEndMillis)
            .atZone(zone)
            .plusDays(1)
            .toInstant()
            .toEpochMilli()

    companion object {
        val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.of(18, 0)
    }
}
