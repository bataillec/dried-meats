package me.bataillec.meatmelater.domain

import java.math.BigDecimal

enum class Stage { VACUUM, FRIDGE }

enum class PieceStatus { VACUUM, FRIDGE, DONE, CANCELLED }

data class MeatType(
    val id: Long,
    val name: String,
    val archived: Boolean = false,
)

data class Recipe(
    val id: Long,
    val name: String,
    val saltGPerKg: BigDecimal,
    val sugarGPerKg: BigDecimal,
    val pepperGPerKg: BigDecimal,
    val fridgeSeasoningDefault: String? = null,
    val vacuumDivisor: BigDecimal = DEFAULT_VACUUM_DIVISOR,
    val vacuumOffsetDays: BigDecimal = DEFAULT_VACUUM_OFFSET_DAYS,
    val fridgeMinDays: Int = DEFAULT_FRIDGE_MIN_DAYS,
    val archived: Boolean = false,
) {
    val durationRule: StageDurationRule get() = StageDurationRule(vacuumDivisor, vacuumOffsetDays)

    companion object {
        val DEFAULT_VACUUM_DIVISOR: BigDecimal = BigDecimal("2")
        val DEFAULT_VACUUM_OFFSET_DAYS: BigDecimal = BigDecimal("1")
        const val DEFAULT_FRIDGE_MIN_DAYS = 14
    }
}

/** Grams of salt, sugar and pepper for one piece, rounded to 0.1 g (R4). */
data class Doses(
    val saltG: BigDecimal,
    val sugarG: BigDecimal,
    val pepperG: BigDecimal,
)

data class Piece(
    val id: Long,
    val label: String,
    val recipeId: Long,
    val meatTypeId: Long,
    val weightG: Int,
    val diameterCm: BigDecimal,
    val doses: Doses,
    val fridgeSeasoning: String?,
    val status: PieceStatus,
    val createdAt: Long,
)

data class StageRun(
    val id: Long,
    val pieceId: Long,
    val stage: Stage,
    val startedAt: Long,
    val plannedEndAt: Long,
    val endedAt: Long? = null,
    val postponedDays: Int = 0,
    val note: String? = null,
) {
    val isOpen: Boolean get() = endedAt == null

    fun isOverdue(nowMillis: Long): Boolean = isOpen && plannedEndAt <= nowMillis
}
