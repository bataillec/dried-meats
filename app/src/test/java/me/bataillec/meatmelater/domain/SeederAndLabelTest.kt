package me.bataillec.meatmelater.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SeederAndLabelTest {
    @Test
    fun `R1 seeds beef pork duck once`() =
        runTest {
            val meats = FakeMeatTypeStore()
            val settings = FakeSettingsStore()
            val seeder = Seeder(settings, meats)
            seeder.seedIfNeeded()
            seeder.seedIfNeeded()
            assertEquals(listOf("Bœuf", "Porc", "Canard"), meats.items.value.map { it.name })
            meats.items.value.forEach { meats.delete(it.id) }
            seeder.seedIfNeeded()
            assertEquals(0, meats.count())
        }

    @Test
    fun `R1 a meat type used by pieces cannot be deleted`() =
        runTest {
            val meats = FakeMeatTypeStore()
            val id = meats.save(MeatType(0, "Boeuf"))
            meats.pieceCounts[id] = 1
            assertEquals(false, meats.delete(id))
        }

    @Test
    fun `R6 label suggestion picks the first free number`() {
        assertEquals("Boeuf 1", LabelSuggester.suggest("Boeuf", emptyList()))
        assertEquals("Boeuf 3", LabelSuggester.suggest("Boeuf", listOf("boeuf 1", "Boeuf 2")))
    }

    @Test
    fun `R3 a recipe used by pieces cannot be deleted`() =
        runTest {
            val recipes = FakeRecipeStore()
            val id =
                recipes.save(
                    Recipe(0, "x", java.math.BigDecimal.ONE, java.math.BigDecimal.ONE, java.math.BigDecimal.ONE),
                )
            recipes.pieceCountOf = { 2 }
            assertEquals(false, recipes.delete(id))
            recipes.pieceCountOf = { 0 }
            assertEquals(true, recipes.delete(id))
        }
}
