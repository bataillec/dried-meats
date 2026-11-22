package fr.viandes.seches.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class PlannedEndCalculatorTest {
    private val paris = ZoneId.of("Europe/Paris")

    private fun millis(
        y: Int,
        m: Int,
        d: Int,
        h: Int,
        min: Int = 0,
    ) = LocalDateTime
        .of(y, m, d, h, min)
        .atZone(paris)
        .toInstant()
        .toEpochMilli()

    @Test
    fun `R26 planned end uses the reminder time of day`() {
        val calc = PlannedEndCalculator(paris, LocalTime.of(18, 0))
        assertEquals(millis(2026, 11, 29, 18), calc.plannedEnd(millis(2026, 11, 22, 9, 30), 7))
    }

    @Test
    fun `R26 custom reminder hour`() {
        val calc = PlannedEndCalculator(paris, LocalTime.of(8, 15))
        assertEquals(millis(2026, 12, 6, 8, 15), calc.plannedEnd(millis(2026, 11, 22, 20), 14))
    }

    @Test
    fun `R13 postponing keeps the time of day across the DST change`() {
        val calc = PlannedEndCalculator(paris, LocalTime.of(18, 0))
        // Europe/Paris changes to winter time on 2026-10-25
        val end = millis(2026, 10, 24, 18)
        assertEquals(millis(2026, 10, 25, 18), calc.postponedByOneDay(end))
    }

    @Test
    fun `R14 planned end is an absolute instant`() {
        val calc = PlannedEndCalculator(paris, LocalTime.of(18, 0))
        val stored = calc.plannedEnd(millis(2026, 11, 22, 9), 3)
        // the same stored instant, read in another zone, is still the same instant
        assertEquals(
            stored,
            java.time.Instant
                .ofEpochMilli(stored)
                .toEpochMilli(),
        )
    }
}
