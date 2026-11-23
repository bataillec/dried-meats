package me.bataillec.meatmelater.ui.pieces

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.bataillec.meatmelater.R
import me.bataillec.meatmelater.domain.LabelSuggester
import me.bataillec.meatmelater.domain.PieceFactory
import me.bataillec.meatmelater.domain.WeightParser
import me.bataillec.meatmelater.domain.WeightUnit
import me.bataillec.meatmelater.ui.common.DecimalField
import me.bataillec.meatmelater.ui.common.MeatTypePicker
import me.bataillec.meatmelater.ui.common.RecipePicker
import me.bataillec.meatmelater.ui.common.SectionTitle
import me.bataillec.meatmelater.ui.common.WeightUnitToggle
import me.bataillec.meatmelater.ui.common.recipeOptions
import me.bataillec.meatmelater.ui.common.rememberContainer

/** R6, R7: recipe, weight, diameter -> live doses and vacuum duration -> confirm. */
@Composable
fun NewPieceScreen(onCreated: (Long) -> Unit) {
    val container = rememberContainer()
    val scope = rememberCoroutineScope()
    val options by remember { container.recipeOptions() }.collectAsState(initial = emptyList())
    val meatTypes by remember { container.meatTypes.observeAll() }.collectAsState(initial = emptyList())
    var recipeId by remember { mutableStateOf<Long?>(null) }
    var meatTypeId by remember { mutableStateOf<Long?>(null) }
    var weightText by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(WeightUnit.KILOGRAMS) }
    var diameterText by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var labelEdited by remember { mutableStateOf(false) }
    var daysText by remember { mutableStateOf("") }
    var existingLabels by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(Unit) { existingLabels = container.pieces.labels() }
    val option = options.firstOrNull { it.recipe.id == recipeId } ?: options.firstOrNull()
    val availableMeats = meatTypes.filterNot { it.archived }
    val meat = availableMeats.firstOrNull { it.id == meatTypeId } ?: availableMeats.firstOrNull()
    LaunchedEffect(meat?.id, existingLabels) {
        if (!labelEdited && meat != null) label = LabelSuggester.suggest(meat.name, existingLabels)
    }

    val grams = WeightParser.parseGrams(weightText, unit)
    val diameter = WeightParser.parseDecimal(diameterText)?.takeIf { it.signum() > 0 }
    val daysOverride = daysText.trim().toIntOrNull()?.takeIf { it >= 1 }
    val plan =
        if (option != null &&
            grams != null &&
            diameter != null
        ) {
            PieceFactory.plan(grams, diameter, option.recipe, daysOverride)
        } else {
            null
        }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (options.isEmpty()) {
            Text(stringResource(R.string.no_recipe_hint), style = MaterialTheme.typography.bodyLarge)
        } else {
            RecipePicker(options, option?.recipe?.id, { recipeId = it }, Modifier.fillMaxWidth())
        }
        if (availableMeats.isEmpty()) {
            Text(stringResource(R.string.no_meat_type), style = MaterialTheme.typography.bodyLarge)
        } else {
            MeatTypePicker(availableMeats, meat?.id, { meatTypeId = it }, Modifier.fillMaxWidth())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DecimalField(
                weightText,
                { weightText = it },
                stringResource(R.string.weight),
                Modifier.weight(1f),
                isError =
                    weightText.isNotBlank() && grams == null,
            )
            WeightUnitToggle(unit, { unit = it }, Modifier.padding(top = 8.dp))
        }
        DecimalField(
            diameterText,
            { diameterText = it },
            stringResource(R.string.diameter),
            Modifier.fillMaxWidth(),
            suffix = "cm",
            isError = diameterText.isNotBlank() && diameter == null,
        )
        DosesCard(plan?.doses, option)
        if (plan != null) {
            SectionTitle(stringResource(R.string.vacuum_planned))
            Text(
                pluralDays(plan.vacuumDays),
                style = MaterialTheme.typography.headlineSmall,
            )
            DecimalField(
                daysText,
                { daysText = it.filter(Char::isDigit) },
                stringResource(R.string.override_days),
                Modifier.fillMaxWidth(),
            )
        }
        OutlinedTextField(
            label,
            {
                label = it
                labelEdited = true
            },
            label = { Text(stringResource(R.string.piece_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            enabled = plan != null && meat != null && label.isNotBlank(),
            onClick = {
                scope.launch {
                    container.pieceActions
                        .createPiece(label, option!!.recipe.id, meat!!.id, grams!!, diameter!!, daysOverride)
                        ?.let(onCreated)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.start_piece)) }
    }
}

@Composable
fun pluralDays(days: Int): String =
    androidx.compose.ui.res
        .pluralStringResource(R.plurals.days, days, days)
