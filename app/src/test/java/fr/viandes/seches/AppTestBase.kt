package fr.viandes.seches

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.viandes.seches.domain.MeatType
import fr.viandes.seches.domain.Recipe
import kotlinx.coroutines.runBlocking
import org.junit.Before
import java.math.BigDecimal

abstract class AppTestBase {
    protected val app: TestApp get() = ApplicationProvider.getApplicationContext<Context>() as TestApp
    protected val container: AppContainer get() = app.container
    protected val clock get() = TestClock.clock

    @Before
    fun resetClock() {
        clock.millis = TestClock.START
    }

    protected fun string(id: Int): String = app.getString(id)

    protected fun string(
        id: Int,
        vararg args: Any,
    ): String = app.getString(id, *args)

    /** Seeds one meat type and one recipe (salt 25, sugar 5, pepper 2 g/kg, fridge default "herbes"). */
    protected fun seedRecipe(
        meat: String = "Bœuf",
        name: String = "Classique",
    ): Long =
        runBlocking {
            val meatId = container.meatTypes.save(MeatType(0, meat))
            container.recipes.save(
                Recipe(
                    0,
                    meatId,
                    name,
                    BigDecimal("25"),
                    BigDecimal("5"),
                    BigDecimal("2"),
                    fridgeSeasoningDefault = "herbes",
                ),
            )
        }

    protected fun createPiece(
        recipeId: Long,
        label: String = "Bœuf 1",
        days: Int? = null,
    ): Long = runBlocking { container.pieceActions.createPiece(label, recipeId, 1250, BigDecimal("12"), days)!! }

    /** Polls [condition] for up to [timeoutMs] (receivers and Room work on background dispatchers). */
    protected fun awaitUntil(
        timeoutMs: Long = 10_000,
        condition: () -> Boolean,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "condition not met within ${timeoutMs}ms" }
            org.robolectric.shadows.ShadowLooper
                .idleMainLooper()
            Thread.sleep(25)
        }
    }
}
