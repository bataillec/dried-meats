package fr.viandes.seches.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class WeightParserTest {
    @Test
    fun `R4 kilograms with comma or dot are converted to grams`() {
        assertEquals(1250, WeightParser.parseGrams("1,25", WeightUnit.KILOGRAMS))
        assertEquals(1250, WeightParser.parseGrams("1.25", WeightUnit.KILOGRAMS))
    }

    @Test
    fun `R4 grams are accepted`() {
        assertEquals(850, WeightParser.parseGrams("850", WeightUnit.GRAMS))
    }

    @Test
    fun `R4 empty zero and garbage give null`() {
        assertNull(WeightParser.parseGrams("", WeightUnit.GRAMS))
        assertNull(WeightParser.parseGrams("  ", WeightUnit.GRAMS))
        assertNull(WeightParser.parseGrams("0", WeightUnit.GRAMS))
        assertNull(WeightParser.parseGrams("-3", WeightUnit.KILOGRAMS))
        assertNull(WeightParser.parseGrams("abc", WeightUnit.GRAMS))
        assertNull(WeightParser.parseGrams("1e9", WeightUnit.GRAMS))
    }

    @Test
    fun `R2 decimal parsing accepts comma`() {
        assertEquals(BigDecimal("2.5"), WeightParser.parseDecimal("2,5"))
    }
}
