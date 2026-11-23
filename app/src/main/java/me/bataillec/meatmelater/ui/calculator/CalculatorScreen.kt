package me.bataillec.meatmelater.ui.calculator

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import me.bataillec.meatmelater.AppContainer
import me.bataillec.meatmelater.R
import me.bataillec.meatmelater.domain.DosageCalculator
import me.bataillec.meatmelater.domain.Doses
import me.bataillec.meatmelater.domain.WeightParser
import me.bataillec.meatmelater.domain.WeightUnit
import me.bataillec.meatmelater.ui.common.DecimalField
import me.bataillec.meatmelater.ui.common.DoseLine
import me.bataillec.meatmelater.ui.common.RecipeOption
import me.bataillec.meatmelater.ui.common.RecipePicker
import me.bataillec.meatmelater.ui.common.WeightUnitToggle
import me.bataillec.meatmelater.ui.common.fr
import me.bataillec.meatmelater.ui.common.frFixed
import me.bataillec.meatmelater.ui.common.recipeOptions
import me.bataillec.meatmelater.ui.common.rememberContainer

data class CalculatorUiState(
    val options: List<RecipeOption> = emptyList(),
    val recipeId: Long? = null,
    val weightText: String = "",
    val unit: WeightUnit = WeightUnit.KILOGRAMS,
    val doses: Doses? = null,
    val weightInvalid: Boolean = false,
)

/** R4, R5: standalone calculator. */
class CalculatorViewModel(
    private val container: AppContainer,
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

    fun selectRecipe(id: Long) = input.update { it.copy(recipeId = id) }

    fun setWeight(text: String) = input.update { it.copy(weightText = text) }

    fun setUnit(unit: WeightUnit) = input.update { it.copy(unit = unit) }
}

@Composable
fun CalculatorScreen() {
    val container = rememberContainer()
    val vm: CalculatorViewModel = viewModel { CalculatorViewModel(container) }
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
