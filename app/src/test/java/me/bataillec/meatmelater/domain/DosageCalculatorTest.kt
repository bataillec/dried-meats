package me.bataillec.meatmelater.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class DosageCalculatorTest {
    private fun recipe(
        salt: String,
        sugar: String = "0",
        pepper: String = "0",
    ) = Recipe(1, "test", BigDecimal(salt), BigDecimal(sugar), BigDecimal(pepper))

    @Test
    fun `R4 1250 g with salt 25 g per kg rounds half up to 31_3`() {
        val doses = DosageCalculator.doses(1250, recipe("25"))!!
        assertEquals(BigDecimal("31.3"), doses.saltG)
    }

    @Test
    fun `R4 zero weight gives no result`() {
        assertNull(DosageCalculator.doses(0, recipe("25")))
        assertNull(DosageCalculator.doses(-5, recipe("25")))
    }

    @Test
    fun `R4 all three doses are computed`() {
        val doses = DosageCalculator.doses(2000, recipe("25", "5.5", "2"))!!
        assertEquals(BigDecimal("50.0"), doses.saltG)
        assertEquals(BigDecimal("11.0"), doses.sugarG)
        assertEquals(BigDecimal("4.0"), doses.pepperG)
    }

    @Test
    fun `R4 decimals in g per kg are supported and rounded half up`() {
        // 1 kg * 2.25 g/kg = 2.25 -> 2.3 (half up, not banker's rounding)
        assertEquals(BigDecimal("2.3"), DosageCalculator.dose(1000, BigDecimal("2.25")))
        // 333 g * 7.5 g/kg = 2.4975 -> 2.5
        assertEquals(BigDecimal("2.5"), DosageCalculator.dose(333, BigDecimal("7.5")))
    }

    @Test
    fun `R4 zero dose gives zero grams`() {
        assertEquals(BigDecimal("0.0"), DosageCalculator.dose(1000, BigDecimal.ZERO))
    }
}
