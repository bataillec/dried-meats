package fr.viandes.seches.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * R7: vacuum duration in whole days = `ceil(diameter_cm / divisor + offset)`, never below 1.
 * Always rounded up to the next whole day.
 */
data class StageDurationRule(
    val divisor: BigDecimal = Recipe.DEFAULT_VACUUM_DIVISOR,
    val offsetDays: BigDecimal = Recipe.DEFAULT_VACUUM_OFFSET_DAYS,
) {
    init {
        require(divisor.signum() > 0) { "divisor must be positive" }
    }

    /** Unrounded value, shown in the live example of the settings screen (R25). */
    fun rawDays(diameterCm: BigDecimal): BigDecimal = diameterCm.divide(divisor, MathContext.DECIMAL64).add(offsetDays)

    fun days(diameterCm: BigDecimal): Int =
        rawDays(diameterCm).setScale(0, RoundingMode.CEILING).toInt().coerceAtLeast(1)
}
