package me.bataillec.meatmelater.ui.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.bataillec.meatmelater.R
import me.bataillec.meatmelater.domain.MeatType
import me.bataillec.meatmelater.domain.Recipe
import me.bataillec.meatmelater.domain.WeightParser
import me.bataillec.meatmelater.ui.common.ConfirmDialog
import me.bataillec.meatmelater.ui.common.DecimalField
import me.bataillec.meatmelater.ui.common.SectionTitle
import me.bataillec.meatmelater.ui.common.fr
import me.bataillec.meatmelater.ui.common.rememberContainer

/** R2, R3: create / edit / duplicate / archive / delete a recipe. [recipeId] 0 = new. */
@Composable
fun RecipeEditScreen(
    recipeId: Long,
    onDone: () -> Unit,
) {
    val container = rememberContainer()
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    var meatTypes by remember { mutableStateOf(emptyList<MeatType>()) }
    var original by remember { mutableStateOf<Recipe?>(null) }
    var name by remember { mutableStateOf("") }
    var meatId by remember { mutableStateOf<Long?>(null) }
    var salt by remember { mutableStateOf("") }
    var sugar by remember { mutableStateOf("") }
    var pepper by remember { mutableStateOf("") }
    var fridgeNote by remember { mutableStateOf("") }
    var divisor by remember { mutableStateOf("") }
    var offset by remember { mutableStateOf("") }
    var fridgeDays by remember { mutableStateOf("") }
    var usedByPieces by remember { mutableStateOf(0) }
    var showDelete by remember { mutableStateOf(false) }
    var meatMenu by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(recipeId) {
        meatTypes = container.meatTypes.observeAll().first()
        val settings = container.settings.current()
        val recipe = if (recipeId != 0L) container.recipes.get(recipeId) else null
        original = recipe
        name = recipe?.name.orEmpty()
        meatId = recipe?.meatTypeId ?: meatTypes.firstOrNull()?.id
        salt = recipe?.saltGPerKg?.fr().orEmpty()
        sugar = recipe?.sugarGPerKg?.fr().orEmpty()
        pepper = recipe?.pepperGPerKg?.fr().orEmpty()
        fridgeNote = recipe?.fridgeSeasoningDefault.orEmpty()
        divisor = (recipe?.vacuumDivisor ?: settings.defaultVacuumDivisor).fr()
        offset = (recipe?.vacuumOffsetDays ?: settings.defaultVacuumOffsetDays).fr()
        fridgeDays = (recipe?.fridgeMinDays ?: settings.defaultFridgeMinDays).toString()
        usedByPieces = if (recipe != null) container.pieces.countByRecipe(recipe.id) else 0
        loaded = true
    }
    if (!loaded) return

    val saltV = WeightParser.parseDecimal(salt)?.takeIf { it.signum() >= 0 }
    val sugarV = WeightParser.parseDecimal(sugar)?.takeIf { it.signum() >= 0 }
    val pepperV = WeightParser.parseDecimal(pepper)?.takeIf { it.signum() >= 0 }
    val divisorV = WeightParser.parseDecimal(divisor)?.takeIf { it.signum() > 0 }
    val offsetV = WeightParser.parseDecimal(offset)
    val daysV = fridgeDays.trim().toIntOrNull()?.takeIf { it >= 1 }
    val valid =
        name.isNotBlank() &&
            meatId != null &&
            saltV != null &&
            sugarV != null &&
            pepperV != null &&
            divisorV != null &&
            offsetV != null &&
            daysV != null

    fun build(id: Long): Recipe? {
        if (!valid) return null
        return Recipe(
            id = id,
            meatTypeId = meatId!!,
            name = name.trim(),
            saltGPerKg = saltV,
            sugarGPerKg = sugarV,
            pepperGPerKg = pepperV,
            fridgeSeasoningDefault = fridgeNote.trim().ifEmpty { null },
            vacuumDivisor = divisorV,
            vacuumOffsetDays = offsetV,
            fridgeMinDays = daysV,
            archived = original?.archived ?: false,
        )
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            name,
            { name = it },
            label = { Text(stringResource(R.string.recipe_name)) },
            singleLine = true,
            isError = submitted && name.isBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
        Column {
            OutlinedButton(onClick = { meatMenu = true }, modifier = Modifier.fillMaxWidth()) {
                Text(meatTypes.firstOrNull { it.id == meatId }?.name ?: stringResource(R.string.pick_meat))
            }
            DropdownMenu(meatMenu, { meatMenu = false }) {
                meatTypes.forEach { meat ->
                    DropdownMenuItem(text = { Text(meat.name) }, onClick = {
                        meatId = meat.id
                        meatMenu = false
                    })
                }
            }
        }
        SectionTitle(stringResource(R.string.vacuum_doses))
        DecimalField(
            salt,
            { salt = it },
            stringResource(R.string.salt),
            Modifier.fillMaxWidth(),
            suffix = "g/kg",
            isError =
                submitted && saltV == null,
        )
        DecimalField(
            sugar,
            { sugar = it },
            stringResource(R.string.sugar),
            Modifier.fillMaxWidth(),
            suffix = "g/kg",
            isError =
                submitted && sugarV == null,
        )
        DecimalField(
            pepper,
            { pepper = it },
            stringResource(R.string.pepper),
            Modifier.fillMaxWidth(),
            suffix = "g/kg",
            isError =
                submitted && pepperV == null,
        )
        SectionTitle(stringResource(R.string.vacuum_duration))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DecimalField(
                divisor,
                { divisor = it },
                stringResource(R.string.divisor),
                Modifier.weight(1f),
                isError =
                    submitted && divisorV == null,
            )
            DecimalField(
                offset,
                { offset = it },
                stringResource(R.string.added_days),
                Modifier.weight(1f),
                isError =
                    submitted && offsetV == null,
            )
        }
        SectionTitle(stringResource(R.string.fridge))
        DecimalField(
            fridgeDays,
            {
                fridgeDays = it.filter(Char::isDigit)
            },
            stringResource(R.string.fridge_min_days),
            Modifier.fillMaxWidth(),
            isError =
                submitted && daysV == null,
        )
        OutlinedTextField(
            fridgeNote,
            { fridgeNote = it },
            label = { Text(stringResource(R.string.fridge_seasoning_default)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                submitted = true
                build(recipeId)?.let { recipe ->
                    scope.launch {
                        container.recipes.save(recipe)
                        onDone()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text(stringResource(R.string.save)) }
        if (original != null) {
            OutlinedButton(
                onClick = {
                    build(0)?.let { r ->
                        scope.launch {
                            container.recipes.save(r.copy(name = r.name + " (copie)", archived = false))
                            onDone()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.duplicate)) }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        container.recipes.setArchived(recipeId, !(original?.archived ?: false))
                        onDone()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(if (original?.archived == true) R.string.unarchive else R.string.archive)) }
            if (usedByPieces > 0) {
                Text(
                    stringResource(
                        R.string.recipe_in_use,
                        androidx.compose.ui.res
                            .pluralStringResource(R.plurals.pieces_count, usedByPieces, usedByPieces),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                OutlinedButton(onClick = { showDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (showDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_recipe_title),
            text = stringResource(R.string.delete_recipe_text),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                scope.launch {
                    container.recipes.delete(recipeId)
                    onDone()
                }
            },
            onDismiss = { showDelete = false },
        )
    }
}
