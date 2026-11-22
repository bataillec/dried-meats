package me.bataillec.meatmelater.domain

import java.math.BigDecimal

/** What a new piece looks like before it is stored (R6, R7). */
data class NewPiecePlan(
    val doses: Doses,
    val vacuumDays: Int,
)

object PieceFactory {
    /** @return null when weight or diameter is not usable. Doses are snapshotted by the caller (R6). */
    fun plan(
        weightG: Int,
        diameterCm: BigDecimal,
        recipe: Recipe,
        vacuumDaysOverride: Int? = null,
    ): NewPiecePlan? {
        if (diameterCm.signum() <= 0) return null
        val doses = DosageCalculator.doses(weightG, recipe) ?: return null
        val days = vacuumDaysOverride?.coerceAtLeast(1) ?: recipe.durationRule.days(diameterCm)
        return NewPiecePlan(doses, days)
    }
}
