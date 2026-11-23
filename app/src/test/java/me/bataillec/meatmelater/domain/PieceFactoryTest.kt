package me.bataillec.meatmelater.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class PieceFactoryTest {
    private val recipe = Recipe(1, "Boeuf", BigDecimal("25"), BigDecimal("5"), BigDecimal("2"))

    @Test
    fun `R6 R7 plan has doses and duration`() {
        val plan = PieceFactory.plan(1250, BigDecimal("12"), recipe)!!
        assertEquals(BigDecimal("31.3"), plan.doses.saltG)
        assertEquals(7, plan.vacuumDays)
    }

    @Test
    fun `R7 override replaces the computed duration but stays at least 1`() {
        assertEquals(10, PieceFactory.plan(1000, BigDecimal("12"), recipe, 10)!!.vacuumDays)
        assertEquals(1, PieceFactory.plan(1000, BigDecimal("12"), recipe, 0)!!.vacuumDays)
    }

    @Test
    fun `R6 invalid weight or diameter gives no plan`() {
        assertNull(PieceFactory.plan(0, BigDecimal("12"), recipe))
        assertNull(PieceFactory.plan(1000, BigDecimal.ZERO, recipe))
    }

    @Test
    fun `R6 plan is a snapshot independent of later recipe edits`() {
        val plan = PieceFactory.plan(1000, BigDecimal("12"), recipe)!!
        val edited = recipe.copy(saltGPerKg = BigDecimal("40"))
        assertEquals(BigDecimal("25.0"), plan.doses.saltG)
        assertEquals(BigDecimal("40.0"), PieceFactory.plan(1000, BigDecimal("12"), edited)!!.doses.saltG)
    }
}
