package fr.viandes.seches.domain

import java.math.BigDecimal
import java.math.RoundingMode

/** R4: `grams = weight_kg x dose_g_per_kg`, rounded half-up to 0.1 g. Pure and BigDecimal-only. */
object DosageCalculator {
    private val GRAMS_PER_KG = BigDecimal(1000)

    /** @return null when the weight is not strictly positive (no result, no crash). */
    fun doses(
        weightG: Int,
        recipe: Recipe,
    ): Doses? = doses(weightG, recipe.saltGPerKg, recipe.sugarGPerKg, recipe.pepperGPerKg)

    fun doses(
        weightG: Int,
        saltGPerKg: BigDecimal,
        sugarGPerKg: BigDecimal,
        pepperGPerKg: BigDecimal,
    ): Doses? {
        if (weightG <= 0) return null
        return Doses(
            saltG = dose(weightG, saltGPerKg),
            sugarG = dose(weightG, sugarGPerKg),
            pepperG = dose(weightG, pepperGPerKg),
        )
    }

    fun dose(
        weightG: Int,
        gPerKg: BigDecimal,
    ): BigDecimal =
        BigDecimal(weightG)
            .multiply(gPerKg)
            .divide(GRAMS_PER_KG, 1, RoundingMode.HALF_UP)
}
