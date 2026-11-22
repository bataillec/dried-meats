package fr.viandes.seches.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class StageDurationRuleTest {
    private val default = StageDurationRule()

    @Test
    fun `R7 12 cm with defaults gives 7 days`() {
        assertEquals(7, default.days(BigDecimal("12")))
    }

    @Test
    fun `R7 9 cm rounds 5_5 up to 6 days`() {
        assertEquals(6, default.days(BigDecimal("9")))
    }

    @Test
    fun `R7 never rounds down`() {
        // 8.2 / 2 + 1 = 5.1 -> 6
        assertEquals(6, default.days(BigDecimal("8.2")))
    }

    @Test
    fun `R7 tiny diameter is at least one day`() {
        assertEquals(2, default.days(BigDecimal("0.5"))) // 1.25 -> 2
        assertEquals(1, StageDurationRule(BigDecimal("2"), BigDecimal("-5")).days(BigDecimal("1")))
    }

    @Test
    fun `R7 large diameter`() {
        assertEquals(26, default.days(BigDecimal("50")))
    }

    @Test
    fun `R25 custom divisor and offset`() {
        val rule = StageDurationRule(BigDecimal("3"), BigDecimal("2"))
        assertEquals(6, rule.days(BigDecimal("10"))) // 3.33 + 2 = 5.33 -> 6
    }

    @Test
    fun `R25 raw value for the live example`() {
        assertEquals(0, BigDecimal("5.5").compareTo(default.rawDays(BigDecimal("9"))))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `R25 divisor must be positive`() {
        StageDurationRule(BigDecimal.ZERO, BigDecimal.ONE)
    }
}
