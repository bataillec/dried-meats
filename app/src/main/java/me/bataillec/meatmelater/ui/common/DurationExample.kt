package me.bataillec.meatmelater.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import me.bataillec.meatmelater.R
import me.bataillec.meatmelater.domain.StageDurationRule
import me.bataillec.meatmelater.ui.pieces.pluralDays
import java.math.BigDecimal

/** Diameter of the worked example of the vacuum duration rule, in Réglages (R25) and the recipe form (R28). */
val EXAMPLE_DIAMETER_CM: BigDecimal = BigDecimal(6)

/** "Ø 6 cm ÷ 2 + 1 = 4 → 4 jours": the maths come from [StageDurationRule], this only formats. */
@Composable
fun durationExampleText(rule: StageDurationRule): String =
    stringResource(
        R.string.duration_example,
        EXAMPLE_DIAMETER_CM.fr(),
        rule.divisor.fr(),
        rule.offsetDays.fr(),
        rule.rawDays(EXAMPLE_DIAMETER_CM).fr(),
        pluralDays(rule.days(EXAMPLE_DIAMETER_CM)),
    )

/**
 * R28: formula with the typed values and the 6 cm example, under the recipe form's duration fields.
 * [divisor] and [offset] are the parsed fields, null when empty or not a number: nothing is shown then.
 * Informational only; one spoken sentence for TalkBack, not a live region.
 */
@Composable
fun DurationFormulaLabel(
    divisor: BigDecimal?,
    offset: BigDecimal?,
    modifier: Modifier = Modifier,
) {
    if (divisor == null || offset == null) return
    val style = MaterialTheme.typography.bodyMedium
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    if (divisor.signum() <= 0) {
        Text(stringResource(R.string.duration_example_invalid), modifier, color = color, style = style)
        return
    }
    val rule = StageDurationRule(divisor, offset)
    val spoken =
        stringResource(
            R.string.duration_formula_spoken,
            divisor.fr(),
            offset.fr(),
            EXAMPLE_DIAMETER_CM.fr(),
            pluralDays(rule.days(EXAMPLE_DIAMETER_CM)),
        )
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = spoken }) {
        Text(stringResource(R.string.duration_formula, divisor.fr(), offset.fr()), color = color, style = style)
        Text(
            stringResource(R.string.duration_example_prefixed, durationExampleText(rule)),
            color = color,
            style = style,
        )
    }
}
