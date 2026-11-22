package me.bataillec.meatmelater.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToString
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.bataillec.meatmelater.AppTestBase
import me.bataillec.meatmelater.R
import me.bataillec.meatmelater.domain.PieceStatus
import me.bataillec.meatmelater.domain.ThemeMode
import me.bataillec.meatmelater.ui.calculator.CalculatorScreen
import me.bataillec.meatmelater.ui.pieces.NewPieceScreen
import me.bataillec.meatmelater.ui.pieces.PieceDetailScreen
import me.bataillec.meatmelater.ui.pieces.PiecesScreen
import me.bataillec.meatmelater.ui.recipes.RecipeEditScreen
import me.bataillec.meatmelater.ui.recipes.RecipesScreen
import me.bataillec.meatmelater.ui.settings.SettingsScreen
import me.bataillec.meatmelater.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h2400dp-xhdpi")
class UiFlowsTest : AppTestBase() {
    @get:Rule
    val compose = createComposeRule()

    private fun show(content: @androidx.compose.runtime.Composable () -> Unit) =
        compose.setContent {
            AppTheme(ThemeMode.LIGHT, content)
        }

    private fun awaitUi(condition: () -> Boolean) =
        try {
            awaitUntil {
                if (compose.mainClock.autoAdvance) {
                    compose.waitForIdle()
                } else {
                    compose.mainClock.advanceTimeBy(50)
                }
                condition()
            }
        } catch (e: IllegalStateException) {
            println("SEMANTICS DUMP:\n" + compose.onRoot().printToString())
            throw e
        }

    private fun waitForText(text: String) =
        awaitUi { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun `R6 R7 new piece shows live doses and planned duration then creates the piece`() {
        seedRecipe()
        var created: Long? = null
        show { NewPieceScreen(onCreated = { created = it }) }
        waitForText("Bœuf · Classique")
        compose.onNodeWithText(string(R.string.weight)).performTextInput("1,25")
        compose.onNodeWithText(string(R.string.diameter)).performTextInput("12")
        waitForText(string(R.string.grams_format, "31,3"))
        compose.onNodeWithText(string(R.string.grams_format, "6,3")).assertIsDisplayed() // sugar 5 g/kg
        compose.onNodeWithText("7 jours").assertIsDisplayed()
        compose.onNodeWithText(string(R.string.start_piece)).performClick()
        awaitUi { created != null }
        val piece = runBlocking { container.pieces.getSummary(created!!) }!!
        assertEquals(PieceStatus.VACUUM, piece.piece.status)
        assertEquals(
            "31.3",
            piece.piece.doses.saltG
                .toPlainString(),
        )
        assertEquals("Bœuf 1", piece.piece.label)
    }

    @Test
    fun `R7 a duration override replaces the computed one`() {
        seedRecipe()
        var created: Long? = null
        show { NewPieceScreen(onCreated = { created = it }) }
        waitForText("Bœuf · Classique")
        compose.onNodeWithText(string(R.string.weight)).performTextInput("1000")
        compose.onNodeWithText(string(R.string.unit_g)).performClick()
        compose.onNodeWithText(string(R.string.diameter)).performTextInput("9")
        waitForText("6 jours")
        compose.onNodeWithText(string(R.string.override_days)).performTextInput("10")
        compose.onNodeWithText(string(R.string.start_piece)).performClick()
        awaitUi { created != null }
        val run = runBlocking { container.pieces.getSummary(created!!) }!!.openRun!!
        assertTrue(run.plannedEndAt > clock.millis + 9 * 86_400_000L)
    }

    @Ignore(DIALOG_IDLE)
    @Test
    fun `R8 leaving the vacuum stage asks for the seasoning prefilled from the recipe`() {
        val id = createPiece(seedRecipe())
        show { PieceDetailScreen(id, onOpenCalculator = {}, onGone = {}) }
        waitForText(string(R.string.leave_vacuum))
        compose.onAllNodesWithText(string(R.string.leave_vacuum)).onLast().performClick() // card button
        // The dialog's focused field blinks forever, which would never let Compose go idle.
        compose.mainClock.autoAdvance = false
        waitForText(string(R.string.finish_vacuum_text))
        compose.onNodeWithText("herbes").assertIsDisplayed() // prefilled
        compose.onAllNodesWithText(string(R.string.leave_vacuum)).onLast().performClick() // dialog confirm
        compose.waitUntil(
            10_000,
        ) {
            runBlocking {
                container.pieces
                    .getSummary(id)!!
                    .piece.status
            } == PieceStatus.FRIDGE
        }
        val piece = runBlocking { container.pieces.getSummary(id)!!.piece }
        assertEquals("herbes", piece.fridgeSeasoning)
    }

    @Ignore(DIALOG_IDLE)
    @Test
    fun `R8 the seasoning can be emptied (no seasoning)`() {
        val id = createPiece(seedRecipe())
        show { PieceDetailScreen(id, onOpenCalculator = {}, onGone = {}) }
        waitForText(string(R.string.leave_vacuum))
        compose.onAllNodesWithText(string(R.string.leave_vacuum)).onLast().performClick()
        // The dialog's focused field blinks forever, which would never let Compose go idle.
        compose.mainClock.autoAdvance = false
        waitForText(string(R.string.finish_vacuum_text))
        compose.onNodeWithText("herbes").performTextClearance()
        compose.onAllNodesWithText(string(R.string.leave_vacuum)).onLast().performClick()
        compose.waitUntil(
            10_000,
        ) {
            runBlocking {
                container.pieces
                    .getSummary(id)!!
                    .piece.status
            } == PieceStatus.FRIDGE
        }
        assertEquals(
            null,
            runBlocking {
                container.pieces
                    .getSummary(id)!!
                    .piece.fridgeSeasoning
            },
        )
    }

    @Test
    fun `R13 R23 postponing from the detail screen counts the postponement`() {
        val id = createPiece(seedRecipe())
        show { PieceDetailScreen(id, onOpenCalculator = {}, onGone = {}) }
        waitForText(string(R.string.postpone_one_day))
        compose.onNodeWithText(string(R.string.postpone_one_day)).performClick()
        awaitUi {
            runBlocking {
                container.pieces
                    .getSummary(id)!!
                    .openRun!!
                    .postponedDays
            } == 1
        }
    }

    @Test
    fun `R15 overdue pieces are pinned above running ones`() {
        val recipe = seedRecipe()
        createPiece(recipe, "Long", days = 10)
        createPiece(recipe, "Court", days = 1)
        clock.millis += 3 * 86_400_000L
        show { PiecesScreen(onOpen = {}, onNew = {}) }
        waitForText(string(R.string.overdue_section))
        waitForText("Long")
        val overdueTop = compose.onNodeWithText(string(R.string.overdue_section)).getBoundsInRoot().top
        val runningTop = compose.onNodeWithText(string(R.string.running_section)).getBoundsInRoot().top
        val courtTop = compose.onNodeWithText("Court").getBoundsInRoot().top
        val longTop = compose.onNodeWithText("Long").getBoundsInRoot().top
        assertTrue(overdueTop < courtTop && courtTop < runningTop && runningTop < longTop)
    }

    @Test
    fun `R24 choosing Sombre OLED in Réglages is stored`() {
        show { SettingsScreen() }
        waitForText(string(R.string.theme_oled))
        compose.onNodeWithText(string(R.string.theme_oled)).performClick()
        awaitUi { runBlocking { container.settings.current().theme } == ThemeMode.OLED }
        compose.onNodeWithText(string(R.string.theme_light)).performClick()
        awaitUi { runBlocking { container.settings.current().theme } == ThemeMode.LIGHT }
    }

    @Test
    fun `R25 steppers change the recipe doses and show the live duration example`() {
        val recipe = seedRecipe()
        show { SettingsScreen() }
        waitForText("Bœuf · Classique")
        compose.onAllNodesWithContentDescription(string(R.string.increase)).onFirst().performClick()
        compose.waitUntil(
            10_000,
        ) {
            runBlocking {
                container.recipes
                    .get(recipe)!!
                    .saltGPerKg
                    .toPlainString()
            } == "25.5"
        }
        compose.onNodeWithText("Ø 9 cm ÷ 2 + 1 = 5,5 → 6 jours").assertExists()
    }

    @Test
    fun `R2 R3 recipes list and edit form create a recipe`() {
        runBlocking {
            container.meatTypes.save(
                me.bataillec.meatmelater.domain
                    .MeatType(0, "Porc"),
            )
        }
        var done = false
        show { RecipeEditScreen(0, onDone = { done = true }) }
        waitForText(string(R.string.recipe_name))
        compose.onNodeWithText(string(R.string.recipe_name)).performTextInput("Jambon")
        compose.onNodeWithText(string(R.string.salt)).performTextInput("30")
        compose.onNodeWithText(string(R.string.sugar)).performTextInput("2,5")
        compose.onNodeWithText(string(R.string.pepper)).performTextInput("1")
        compose.onNodeWithText(string(R.string.save)).performClick()
        awaitUi { done }
        val saved = runBlocking { container.recipes.observeAll().first() }.single()
        assertEquals("Jambon", saved.name)
        assertEquals("2.5", saved.sugarGPerKg.toPlainString())
        assertEquals(14, saved.fridgeMinDays)
    }

    @Test
    fun `R2 recipes screen lists recipes grouped by meat`() {
        seedRecipe("Canard", "Magret")
        show { RecipesScreen(onEdit = {}) }
        waitForText("Magret")
        compose.onNodeWithText("Canard").assertIsDisplayed()
    }

    @Test
    fun `R2 an invalid recipe form is not saved`() {
        runBlocking {
            container.meatTypes.save(
                me.bataillec.meatmelater.domain
                    .MeatType(0, "Porc"),
            )
        }
        var done = false
        show { RecipeEditScreen(0, onDone = { done = true }) }
        waitForText(string(R.string.recipe_name))
        compose.onNodeWithText(string(R.string.save)).performClick()
        compose.waitForIdle()
        assertTrue(!done)
        assertNotNull(runBlocking { container.recipes.observeAll().first() }.takeIf { it.isEmpty() })
    }

    @Test
    fun `R4 R5 calculator computes doses for the selected recipe`() {
        seedRecipe()
        show { CalculatorScreen(null) }
        waitForText("Bœuf · Classique")
        compose.onNodeWithText(string(R.string.weight)).performTextInput("1,25")
        waitForText(string(R.string.grams_format, "31,3"))
        compose.onNodeWithText(string(R.string.grams_format, "2,5")).assertIsDisplayed() // pepper 2 g/kg * 1.25 kg
    }

    @Test
    fun `R4 calculator with empty weight shows the hint and no crash`() {
        seedRecipe()
        show { CalculatorScreen(null) }
        waitForText(string(R.string.calculator_hint))
    }

    private companion object {
        const val DIALOG_IDLE =
            "AlertDialog never reaches idle under Robolectric; covered by the instrumented test " +
                "app/src/androidTest/.../LeaveVacuumDialogTest and by PieceActions domain tests."
    }
}
