package me.bataillec.meatmelater.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.bataillec.meatmelater.AppContainer
import me.bataillec.meatmelater.MeatMeLaterApp
import me.bataillec.meatmelater.domain.Recipe

@Composable
fun rememberContainer(): AppContainer = (LocalContext.current.applicationContext as MeatMeLaterApp).container

/** Recipes as picker options, sorted by name; archived ones only when [includeArchived]. */
fun AppContainer.recipeOptions(includeArchived: Boolean = false): Flow<List<RecipeOption>> =
    recipes.observeAll().map { rs: List<Recipe> ->
        rs
            .filter { includeArchived || !it.archived }
            .map { RecipeOption(it) }
            .sortedBy { it.recipe.name.lowercase() }
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
