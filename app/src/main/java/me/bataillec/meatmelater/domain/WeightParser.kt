package me.bataillec.meatmelater.domain

import java.math.BigDecimal
import java.math.RoundingMode

enum class WeightUnit { GRAMS, KILOGRAMS }

/** Parses user-typed weights (French or English decimal separator). Weights are stored as integer grams. */
object WeightParser {
    /** @return grams, or null for empty / invalid / non-positive input. */
    fun parseGrams(
        text: String,
        unit: WeightUnit,
    ): Int? {
        val value = parseDecimal(text) ?: return null
        val grams =
            when (unit) {
                WeightUnit.GRAMS -> value
                WeightUnit.KILOGRAMS -> value.multiply(BigDecimal(1000))
            }.setScale(0, RoundingMode.HALF_UP)
        return if (grams.signum() > 0 && grams <= MAX_GRAMS) grams.toInt() else null
    }

    fun parseDecimal(text: String): BigDecimal? = text.trim().replace(',', '.').toBigDecimalOrNull()

    private val MAX_GRAMS = BigDecimal(100_000)
}
