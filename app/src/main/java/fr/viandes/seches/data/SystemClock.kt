package fr.viandes.seches.data

import fr.viandes.seches.domain.Clock
import java.time.ZoneId

/** The only place allowed to read the system clock (CLAUDE.md). */
class SystemClock : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun zone(): ZoneId = ZoneId.systemDefault()
}
