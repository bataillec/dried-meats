package me.bataillec.meatmelater.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.bataillec.meatmelater.R
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FRENCH = Locale.FRENCH
private val DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", FRENCH)
private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy", FRENCH)

/** 31.3 -> "31,3" (French decimal comma). */
fun BigDecimal.fr(): String = stripTrailingZeros().toPlainString().replace('.', ',')

fun BigDecimal.frFixed(): String = toPlainString().replace('.', ',')

fun formatDateTime(
    millis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): String = DATE_TIME.format(Instant.ofEpochMilli(millis).atZone(zone))

fun formatDate(
    millis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): String = DATE.format(Instant.ofEpochMilli(millis).atZone(zone))

/** Grams as "1,25 kg" or "850 g". */
fun formatWeight(grams: Int): String =
    if (grams >= 1000) {
        BigDecimal(grams).divide(BigDecimal(1000)).fr() + " kg"
    } else {
        "$grams g"
    }

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE
private const val DAY = 24 * HOUR

/** "3 j 4 h", "5 h", "12 min" for a positive span. */
@Composable
fun spanText(spanMillis: Long): String {
    val span = spanMillis.coerceAtLeast(0)
    val days = (span / DAY).toInt()
    val hours = ((span % DAY) / HOUR).toInt()
    val minutes = ((span % HOUR) / MINUTE).toInt()
    return when {
        days > 0 && hours > 0 -> stringResource(R.string.span_days_hours, days, hours)
        days > 0 -> stringResource(R.string.span_days, days)
        hours > 0 -> stringResource(R.string.span_hours, hours)
        else -> stringResource(R.string.span_minutes, minutes)
    }
}

/** "dans 3 j 4 h" or "en retard de 2 j". */
@Composable
fun remainingText(
    plannedEndAt: Long,
    nowMillis: Long,
): String =
    if (plannedEndAt > nowMillis) {
        stringResource(R.string.remaining_in, spanText(plannedEndAt - nowMillis))
    } else {
        stringResource(R.string.remaining_overdue, spanText(nowMillis - plannedEndAt))
    }
