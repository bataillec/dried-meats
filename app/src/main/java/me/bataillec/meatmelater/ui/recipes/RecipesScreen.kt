package me.bataillec.meatmelater.ui.recipes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.bataillec.meatmelater.R
import me.bataillec.meatmelater.ui.common.fr
import me.bataillec.meatmelater.ui.common.recipeOptions
import me.bataillec.meatmelater.ui.common.rememberContainer

/** R2, R3: recipes sorted by name, with no meat type (issue #5). */
@Composable
fun RecipesScreen(onEdit: (Long) -> Unit) {
    val container = rememberContainer()
    val options by remember { container.recipeOptions(includeArchived = true) }.collectAsState(initial = emptyList())

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
        ) {
            item {
                Text(stringResource(R.string.recipes), style = MaterialTheme.typography.headlineSmall)
            }
            if (options.isEmpty()) {
                item { Text(stringResource(R.string.recipes_empty), modifier = Modifier.padding(vertical = 16.dp)) }
            }
            items(options, key = { it.recipe.id }) { option ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onEdit(option.recipe.id) }) {
                    Column(Modifier.padding(16.dp)) {
                        val title =
                            if (option.recipe.archived) {
                                stringResource(R.string.recipe_archived_title, option.recipe.name)
                            } else {
                                option.recipe.name
                            }
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(
                                R.string.dose_rates,
                                option.recipe.saltGPerKg.fr(),
                                option.recipe.sugarGPerKg.fr(),
                                option.recipe.pepperGPerKg.fr(),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
        FloatingActionButton(onClick = {
            onEdit(0)
        }, modifier = Modifier.align(androidx.compose.ui.Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_recipe))
        }
    }
}
