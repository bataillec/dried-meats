package fr.viandes.seches.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import fr.viandes.seches.AppContainer
import fr.viandes.seches.ViandesApp
import fr.viandes.seches.domain.MeatType
import fr.viandes.seches.domain.Recipe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

@Composable
fun rememberContainer(): AppContainer = (LocalContext.current.applicationContext as ViandesApp).container

/** Recipes joined with their meat type names; archived ones only when [includeArchived]. */
fun AppContainer.recipeOptions(includeArchived: Boolean = false): Flow<List<RecipeOption>> =
    combine(recipes.observeAll(), meatTypes.observeAll()) { rs: List<Recipe>, ms: List<MeatType> ->
        val names = ms.associate { it.id to it.name }
        rs
            .filter { includeArchived || !it.archived }
            .map { RecipeOption(it, names[it.meatTypeId] ?: "?") }
            .sortedWith(compareBy({ it.meatName.lowercase() }, { it.recipe.name.lowercase() }))
    }

/** Emits the current time now and every minute (remaining-time labels, overdue detection). */
fun AppContainer.ticker(): Flow<Long> =
    flow {
        while (true) {
            emit(clock.nowMillis())
            delay(TICK_MILLIS)
        }
    }

private const val TICK_MILLIS = 30_000L

fun <T> Flow<T>.asState(
    scope: CoroutineScope,
    initial: T,
) = stateIn(scope, SharingStarted.WhileSubscribed(5_000), initial)
