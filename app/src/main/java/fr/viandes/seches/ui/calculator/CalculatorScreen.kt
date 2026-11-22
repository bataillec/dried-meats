package fr.viandes.seches.ui.calculator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.viandes.seches.AppContainer
import fr.viandes.seches.R
import fr.viandes.seches.domain.DosageCalculator
import fr.viandes.seches.domain.Doses
import fr.viandes.seches.domain.WeightParser
import fr.viandes.seches.domain.WeightUnit
import fr.viandes.seches.ui.common.DecimalField
import fr.viandes.seches.ui.common.DoseLine
import fr.viandes.seches.ui.common.RecipeOption
import fr.viandes.seches.ui.common.RecipePicker
import fr.viandes.seches.ui.common.WeightUnitToggle
import fr.viandes.seches.ui.common.fr
import fr.viandes.seches.ui.common.frFixed
import fr.viandes.seches.ui.common.recipeOptions
import fr.viandes.seches.ui.common.rememberContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalculatorUiState(
    val options: List<RecipeOption> = emptyList(),
    val recipeId: Long? = null,
    val weightText: String = "",
    val unit: WeightUnit = WeightUnit.KILOGRAMS,
    val doses: Doses? = null,
    val weightInvalid: Boolean = false,
)

/** R4, R5: standalone calculator; optionally prefilled from a piece. */
class CalculatorViewModel(
    private val container: AppContainer,
    pieceId: Long?,
) : ViewModel() {
    private data class Input(
        val recipeId: Long?,
        val weightText: String,
        val unit: WeightUnit,
    )

    private val input = MutableStateFlow(Input(null, "", WeightUnit.KILOGRAMS))

    val state: StateFlow<CalculatorUiState> =
        combine(container.recipeOptions(), input) { options, i ->
            val recipe = options.firstOrNull { it.recipe.id == i.recipeId } ?: options.firstOrNull()
            val grams = WeightParser.parseGrams(i.weightText, i.unit)
            CalculatorUiState(
                options = options,
                recipeId = recipe?.recipe?.id,
                weightText = i.weightText,
                unit = i.unit,
                doses = if (recipe != null && grams != null) DosageCalculator.doses(grams, recipe.recipe) else null,
                weightInvalid = i.weightText.isNotBlank() && grams == null,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, CalculatorUiState())

    init {
        if (pieceId != null) {
            viewModelScope.launch {
                container.pieces.getSummary(pieceId)?.let { summary ->
                    val p = summary.piece
                    val text =
                        if (p.weightG % 1000 ==
                            0
                        ) {
                            (p.weightG / 1000).toString()
                        } else {
                            (p.weightG / 1000.0).toString()
                        }
                    input.value = Input(p.recipeId, text, WeightUnit.KILOGRAMS)
                }
            }
        }
    }

    fun selectRecipe(id: Long) = input.update { it.copy(recipeId = id) }

    fun setWeight(text: String) = input.update { it.copy(weightText = text) }

    fun setUnit(unit: WeightUnit) = input.update { it.copy(unit = unit) }
}

@Composable
fun CalculatorScreen(pieceId: Long?) {
    val container = rememberContainer()
    val vm: CalculatorViewModel = viewModel(key = "calc-$pieceId") { CalculatorViewModel(container, pieceId) }
    val state by vm.state.collectAsState()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement =
            androidx.compose.foundation.layout.Arrangement
                .spacedBy(12.dp),
    ) {
        if (state.options.isEmpty()) {
            Text(stringResource(R.string.calculator_no_recipe), style = MaterialTheme.typography.bodyLarge)
        } else {
            RecipePicker(state.options, state.recipeId, vm::selectRecipe, Modifier.fillMaxWidth())
        }
        Row(
            horizontalArrangement =
                androidx.compose.foundation.layout.Arrangement
                    .spacedBy(12.dp),
        ) {
            DecimalField(
                value = state.weightText,
                onValueChange = vm::setWeight,
                label = stringResource(R.string.weight),
                isError = state.weightInvalid,
                modifier = Modifier.weight(1f),
            )
            WeightUnitToggle(state.unit, vm::setUnit, Modifier.padding(top = 8.dp))
        }
        DosesCard(state.doses, state.options.firstOrNull { it.recipe.id == state.recipeId })
    }
}

@Composable
fun DosesCard(
    doses: Doses?,
    option: RecipeOption?,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            if (doses == null) {
                Text(stringResource(R.string.calculator_hint), style = MaterialTheme.typography.bodyMedium)
            } else {
                DoseLine(stringResource(R.string.salt), stringResource(R.string.grams_format, doses.saltG.frFixed()))
                DoseLine(stringResource(R.string.sugar), stringResource(R.string.grams_format, doses.sugarG.frFixed()))
                DoseLine(
                    stringResource(R.string.pepper),
                    stringResource(R.string.grams_format, doses.pepperG.frFixed()),
                )
                option?.let {
                    Text(
                        stringResource(
                            R.string.dose_rates,
                            it.recipe.saltGPerKg.fr(),
                            it.recipe.sugarGPerKg.fr(),
                            it.recipe.pepperGPerKg.fr(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}
