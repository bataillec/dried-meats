package fr.viandes.seches.ui.recipes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.viandes.seches.AppContainer
import fr.viandes.seches.R
import fr.viandes.seches.domain.MeatType
import fr.viandes.seches.ui.common.RecipeOption
import fr.viandes.seches.ui.common.SectionTitle
import fr.viandes.seches.ui.common.fr
import fr.viandes.seches.ui.common.recipeOptions
import fr.viandes.seches.ui.common.rememberContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecipesUiState(
    val options: List<RecipeOption> = emptyList(),
    val meatTypes: List<MeatType> = emptyList(),
)

class RecipesViewModel(
    private val container: AppContainer,
) : ViewModel() {
    val state: StateFlow<RecipesUiState> =
        combine(container.recipeOptions(includeArchived = true), container.meatTypes.observeAll()) { o, m ->
            RecipesUiState(o, m)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, RecipesUiState())

    /** R1: returns false when the meat type is still used by recipes. */
    fun saveMeat(
        id: Long,
        name: String,
    ) = viewModelScope.launch { if (name.isNotBlank()) container.meatTypes.save(MeatType(id, name.trim())) }

    fun deleteMeat(
        id: Long,
        onRefused: () -> Unit,
    ) = viewModelScope.launch { if (!container.meatTypes.delete(id)) onRefused() }
}

/** R2, R3: recipes grouped by meat type; meat types are managed from a dialog (R1). */
@Composable
fun RecipesScreen(onEdit: (Long) -> Unit) {
    val container = rememberContainer()
    val vm: RecipesViewModel = viewModel { RecipesViewModel(container) }
    val state by vm.state.collectAsState()
    var showMeats by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.recipes), style = MaterialTheme.typography.headlineSmall)
                    TextButton(onClick = { showMeats = true }) { Text(stringResource(R.string.meat_types)) }
                }
            }
            if (state.options.isEmpty()) {
                item { Text(stringResource(R.string.recipes_empty), modifier = Modifier.padding(vertical = 16.dp)) }
            }
            state.options.groupBy { it.meatName }.forEach { (meat, options) ->
                item { SectionTitle(meat) }
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
        }
        FloatingActionButton(onClick = {
            onEdit(0)
        }, modifier = Modifier.align(androidx.compose.ui.Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_recipe))
        }
    }
    if (showMeats) MeatTypesDialog(state.meatTypes, vm) { showMeats = false }
}

@Composable
private fun MeatTypesDialog(
    meatTypes: List<MeatType>,
    vm: RecipesViewModel,
    onClose: () -> Unit,
) {
    var newName by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<MeatType?>(null) }
    var editName by remember { mutableStateOf("") }
    var refused by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.meat_types)) },
        text = {
            Column {
                meatTypes.forEach { meat ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        if (editing?.id == meat.id) {
                            OutlinedTextField(
                                editName,
                                { editName = it },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = {
                                vm.saveMeat(meat.id, editName)
                                editing = null
                            }) { Text(stringResource(R.string.save)) }
                        } else {
                            Text(meat.name, Modifier.weight(1f))
                            IconButton(onClick = {
                                editing = meat
                                editName = meat.name
                            }) { Icon(Icons.Filled.Edit, stringResource(R.string.edit)) }
                            IconButton(onClick = { vm.deleteMeat(meat.id) { refused = true } }) {
                                Icon(Icons.Filled.Delete, stringResource(R.string.delete))
                            }
                        }
                    }
                }
                if (refused) {
                    Text(
                        stringResource(R.string.meat_in_use),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        newName,
                        { newName = it },
                        label = { Text(stringResource(R.string.new_meat_type)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        vm.saveMeat(0, newName)
                        newName = ""
                    }) { Text(stringResource(R.string.add)) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.close)) } },
    )
}
