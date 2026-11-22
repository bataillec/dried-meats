package fr.viandes.seches.domain

import java.math.BigDecimal
import java.math.RoundingMode

/** Actual duration of one finished stage run, in days with one decimal (R18, R19). */
object Statistics {
    private const val MILLIS_PER_DAY = 86_400_000L

    fun actualDays(run: StageRun): BigDecimal? {
        val end = run.endedAt ?: return null
        return BigDecimal(end - run.startedAt).divide(BigDecimal(MILLIS_PER_DAY), 1, RoundingMode.HALF_UP)
    }

    /** Average actual duration over finished runs, or null when there are none. */
    fun averageDays(runs: List<StageRun>): BigDecimal? {
        val durations = runs.mapNotNull { actualDays(it) }
        if (durations.isEmpty()) return null
        return durations.reduce(BigDecimal::add).divide(BigDecimal(durations.size), 1, RoundingMode.HALF_UP)
    }
}
