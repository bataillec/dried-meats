package fr.viandes.seches.domain

import java.time.ZoneId

/** Injected time source. Domain code never reads the system clock directly (CLAUDE.md). */
interface Clock {
    /** UTC epoch milliseconds. */
    fun nowMillis(): Long

    fun zone(): ZoneId
}
