package me.bataillec.meatmelater.ui.pieces

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.bataillec.meatmelater.R
import me.bataillec.meatmelater.domain.Doses
import me.bataillec.meatmelater.ui.common.DoseLine
import me.bataillec.meatmelater.ui.common.RecipeOption
import me.bataillec.meatmelater.ui.common.fr
import me.bataillec.meatmelater.ui.common.frFixed

/** R4: doses (g of salt, sugar, pepper) for the weight and recipe chosen on the new-piece screen. */
@Composable
fun DosesCard(
    doses: Doses?,
    option: RecipeOption?,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            if (doses == null) {
                Text(stringResource(R.string.doses_hint), style = MaterialTheme.typography.bodyMedium)
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
