package me.bataillec.meatmelater.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class StatisticsTest {
    private val day = 86_400_000L

    private fun run(
        start: Long,
        end: Long?,
    ) = StageRun(1, 1, Stage.VACUUM, start, start + 7 * day, end)

    @Test
    fun `R18 actual days has one decimal`() {
        assertEquals(BigDecimal("6.5"), Statistics.actualDays(run(0, 6 * day + day / 2)))
    }

    @Test
    fun `R19 average over finished runs ignores open ones`() {
        val avg = Statistics.averageDays(listOf(run(0, 6 * day), run(0, 8 * day), run(0, null)))
        assertEquals(BigDecimal("7.0"), avg)
    }

    @Test
    fun `R19 no finished runs gives null`() {
        assertNull(Statistics.averageDays(listOf(run(0, null))))
        assertNull(Statistics.averageDays(emptyList()))
    }
}
