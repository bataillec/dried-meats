package fr.viandes.seches.ui.pieces

import android.content.ActivityNotFoundException
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.viandes.seches.AppContainer
import fr.viandes.seches.R
import fr.viandes.seches.domain.PieceDetail
import fr.viandes.seches.domain.PieceStatus
import fr.viandes.seches.domain.Recipe
import fr.viandes.seches.domain.Stage
import fr.viandes.seches.domain.StageRun
import fr.viandes.seches.domain.Statistics
import fr.viandes.seches.platform.CalendarIntent
import fr.viandes.seches.ui.common.ConfirmDialog
import fr.viandes.seches.ui.common.DoseLine
import fr.viandes.seches.ui.common.SectionTitle
import fr.viandes.seches.ui.common.formatDateTime
import fr.viandes.seches.ui.common.formatWeight
import fr.viandes.seches.ui.common.fr
import fr.viandes.seches.ui.common.frFixed
import fr.viandes.seches.ui.common.remainingText
import fr.viandes.seches.ui.common.rememberContainer
import fr.viandes.seches.ui.common.ticker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

data class DetailUiState(
    val detail: PieceDetail? = null,
    val recipe: Recipe? = null,
    val nowMillis: Long = 0,
    val loaded: Boolean = false,
)

class PieceDetailViewModel(
    private val container: AppContainer,
    private val pieceId: Long,
) : ViewModel() {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val state: StateFlow<DetailUiState> =
        combine(
            container.pieces.observeDetail(pieceId).flatMapLatest { d ->
                if (d ==
                    null
                ) {
                    flowOf(null to null)
                } else {
                    container.recipes.observe(d.summary.piece.recipeId).let { r ->
                        combine(flowOf(d), r) { dd, rr ->
                            dd to
                                rr
                        }
                    }
                }
            },
            container.ticker(),
        ) { (detail, recipe), now -> DetailUiState(detail, recipe, now, true) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, DetailUiState())

    fun finish(
        seasoning: String?,
        fridgeDays: Int?,
    ) = viewModelScope.launch { container.pieceActions.finishStage(pieceId, seasoning, fridgeDays) }

    fun postpone() = viewModelScope.launch { container.pieceActions.postponeOneDay(pieceId) }

    fun changeDuration(days: Int) = viewModelScope.launch { container.pieceActions.changeDuration(pieceId, days) }

    fun cancelPiece() = viewModelScope.launch { container.pieceActions.cancel(pieceId) }

    fun delete(onDone: () -> Unit) =
        viewModelScope.launch {
            container.pieceActions.delete(pieceId)
            onDone()
        }

    fun saveNote(
        runId: Long,
        note: String,
    ) = viewModelScope.launch { container.pieceActions.saveNote(runId, note) }
}

private enum class DetailDialog { FINISH, DURATION, CANCEL, DELETE }

@Composable
fun PieceDetailScreen(
    pieceId: Long,
    onOpenCalculator: (Long) -> Unit,
    onGone: () -> Unit,
) {
    val container = rememberContainer()
    val vm: PieceDetailViewModel = viewModel(key = "piece-$pieceId") { PieceDetailViewModel(container, pieceId) }
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<DetailDialog?>(null) }
    var noteRun by remember { mutableStateOf<StageRun?>(null) }
    val detail = state.detail
    if (detail == null) {
        if (state.loaded) Text(stringResource(R.string.piece_not_found), Modifier.padding(16.dp))
        return
    }
    val summary = detail.summary
    val piece = summary.piece
    val openRun = summary.openRun
    val active = piece.status == PieceStatus.VACUUM || piece.status == PieceStatus.FRIDGE

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(piece.label, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            when (piece.status) {
                PieceStatus.VACUUM -> StageChip(Stage.VACUUM)
                PieceStatus.FRIDGE -> StageChip(Stage.FRIDGE)
                PieceStatus.DONE ->
                    Text(
                        stringResource(R.string.status_done),
                        color = MaterialTheme.colorScheme.primary,
                    )
                PieceStatus.CANCELLED ->
                    Text(
                        stringResource(R.string.status_cancelled),
                        color = MaterialTheme.colorScheme.error,
                    )
            }
        }
        Text("${summary.meatTypeName} · ${summary.recipeName}", style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(R.string.weight_diameter, formatWeight(piece.weightG), piece.diameterCm.fr()),
            style = MaterialTheme.typography.bodyMedium,
        )

        if (openRun != null) {
            CurrentStageCard(openRun, state.nowMillis, vm, onFinish = { dialog = DetailDialog.FINISH }, onDuration = {
                dialog =
                    DetailDialog.DURATION
            }) {
                openCalendar(
                    context,
                    "${piece.label} · " + stageAction(openRun.stage, context),
                    openRun,
                    summary.meatTypeName,
                    summary.recipeName,
                    piece.fridgeSeasoning,
                )
            }
        }

        SectionTitle(stringResource(R.string.seasoning_stage1))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                DoseLine(
                    stringResource(R.string.salt),
                    stringResource(R.string.grams_format, piece.doses.saltG.frFixed()),
                )
                DoseLine(
                    stringResource(R.string.sugar),
                    stringResource(R.string.grams_format, piece.doses.sugarG.frFixed()),
                )
                DoseLine(
                    stringResource(R.string.pepper),
                    stringResource(R.string.grams_format, piece.doses.pepperG.frFixed()),
                )
            }
        }
        SectionTitle(stringResource(R.string.seasoning_stage2))
        Text(
            piece.fridgeSeasoning ?: stringResource(R.string.none_seasoning),
            style = MaterialTheme.typography.bodyLarge,
        )

        SectionTitle(stringResource(R.string.history))
        detail.runs.forEach { run -> RunCard(run, onNote = { noteRun = run }) }

        OutlinedButton(onClick = { onOpenCalculator(piece.id) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.open_in_calculator))
        }
        if (active) {
            OutlinedButton(onClick = { dialog = DetailDialog.CANCEL }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cancel_piece))
            }
        }
        OutlinedButton(onClick = { dialog = DetailDialog.DELETE }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
        }
    }

    when (dialog) {
        DetailDialog.FINISH ->
            if (piece.status == PieceStatus.VACUUM) {
                FinishVacuumDialog(state.recipe, onConfirm = { seasoning, days ->
                    vm.finish(seasoning, days)
                    dialog = null
                }, onDismiss = { dialog = null })
            } else {
                ConfirmDialog(
                    title = stringResource(R.string.finish_fridge_title),
                    text = stringResource(R.string.finish_fridge_text),
                    confirmLabel = stringResource(R.string.done_action),
                    onConfirm = {
                        vm.finish(null, null)
                        dialog = null
                    },
                    onDismiss = { dialog = null },
                )
            }
        DetailDialog.DURATION -> {
            val run = openRun
            if (run != null) {
                DurationDialog(currentDays(run, container.clock.zone()), onConfirm = {
                    vm.changeDuration(it)
                    dialog = null
                }, onDismiss = { dialog = null })
            }
        }
        DetailDialog.CANCEL ->
            ConfirmDialog(
                title = stringResource(R.string.cancel_piece),
                text = stringResource(R.string.cancel_piece_text),
                confirmLabel = stringResource(R.string.cancel_piece),
                onConfirm = {
                    vm.cancelPiece()
                    dialog = null
                },
                onDismiss = { dialog = null },
            )
        DetailDialog.DELETE ->
            ConfirmDialog(
                title = stringResource(R.string.delete_piece_title),
                text = stringResource(R.string.delete_piece_text),
                confirmLabel = stringResource(R.string.delete),
                onConfirm = {
                    dialog = null
                    vm.delete(onGone)
                },
                onDismiss = { dialog = null },
            )
        null -> Unit
    }
    noteRun?.let { run ->
        NoteDialog(run.note.orEmpty(), onConfirm = {
            vm.saveNote(run.id, it)
            noteRun = null
        }, onDismiss = { noteRun = null })
    }
}

private fun currentDays(
    run: StageRun,
    zone: java.time.ZoneId,
): Int {
    val start = Instant.ofEpochMilli(run.startedAt).atZone(zone).toLocalDate()
    val end = Instant.ofEpochMilli(run.plannedEndAt).atZone(zone).toLocalDate()
    return ChronoUnit.DAYS
        .between(start, end)
        .toInt()
        .coerceAtLeast(1)
}

private fun stageAction(
    stage: Stage,
    context: Context,
): String = context.getString(if (stage == Stage.VACUUM) R.string.action_leave_vacuum else R.string.action_leave_fridge)

private fun openCalendar(
    context: Context,
    title: String,
    run: StageRun,
    meat: String,
    recipe: String,
    seasoning: String?,
) {
    val description =
        listOfNotNull(
            meat,
            recipe,
            seasoning?.let {
                context.getString(R.string.calendar_seasoning, it)
            },
        ).joinToString(" · ")
    try {
        context.startActivity(CalendarIntent.insertEvent(title, run.plannedEndAt, description))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.no_calendar_app, Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun CurrentStageCard(
    run: StageRun,
    now: Long,
    vm: PieceDetailViewModel,
    onFinish: () -> Unit,
    onDuration: () -> Unit,
    onCalendar: () -> Unit,
) {
    val overdue = run.isOverdue(now)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(if (run.stage == Stage.VACUUM) R.string.current_vacuum else R.string.current_fridge),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                remainingText(run.plannedEndAt, now),
                style = MaterialTheme.typography.headlineSmall,
                color = if (overdue) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                stringResource(R.string.planned_end, formatDateTime(run.plannedEndAt)),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (run.stage == Stage.FRIDGE) {
                Text(stringResource(R.string.fridge_check_hint), style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (run.stage == Stage.VACUUM) R.string.leave_vacuum else R.string.leave_fridge))
            }
            OutlinedButton(onClick = vm::postpone, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.postpone_one_day))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDuration,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.edit_duration)) }
                OutlinedButton(
                    onClick = onCalendar,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.add_to_calendar)) }
            }
        }
    }
}

@Composable
private fun RunCard(
    run: StageRun,
    onNote: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(if (run.stage == Stage.VACUUM) R.string.stage_vacuum else R.string.stage_fridge),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(stringResource(R.string.started_at, formatDateTime(run.startedAt)))
            Text(stringResource(R.string.planned_end, formatDateTime(run.plannedEndAt)))
            val ended = run.endedAt
            if (ended != null) {
                Text(stringResource(R.string.ended_at, formatDateTime(ended)))
                Statistics.actualDays(run)?.let { Text(stringResource(R.string.actual_days, it.fr())) }
            }
            if (run.postponedDays > 0) {
                Text(
                    androidx.compose.ui.res.pluralStringResource(
                        R.plurals.postponements,
                        run.postponedDays,
                        run.postponedDays,
                    ),
                )
            }
            if (!run.note.isNullOrBlank()) {
                Text(
                    stringResource(R.string.note_prefix, run.note),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            TextButton(onClick = onNote) {
                Text(stringResource(if (run.note.isNullOrBlank()) R.string.add_note else R.string.edit_note))
            }
        }
    }
}

/** R8: seasoning for stage 2, prefilled from the recipe, may be left empty. */
@Composable
private fun FinishVacuumDialog(
    recipe: Recipe?,
    onConfirm: (String?, Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var seasoning by remember { mutableStateOf(recipe?.fridgeSeasoningDefault.orEmpty()) }
    var days by remember { mutableStateOf((recipe?.fridgeMinDays ?: Recipe.DEFAULT_FRIDGE_MIN_DAYS).toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.finish_vacuum_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.finish_vacuum_text))
                OutlinedTextField(
                    seasoning,
                    { seasoning = it },
                    label = { Text(stringResource(R.string.fridge_seasoning)) },
                    supportingText = { Text(stringResource(R.string.fridge_seasoning_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    days,
                    { days = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.fridge_min_days)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(seasoning.ifBlank { null }, days.toIntOrNull()?.takeIf { it >= 1 }) }) {
                Text(stringResource(R.string.leave_vacuum))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun DurationDialog(
    current: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var days by remember { mutableStateOf(current.toString()) }
    val value = days.toIntOrNull()?.takeIf { it >= 1 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_duration)) },
        text = {
            OutlinedTextField(
                days,
                { days = it.filter(Char::isDigit) },
                label = { Text(stringResource(R.string.duration_days)) },
                isError = value == null,
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { value?.let(onConfirm) },
                enabled = value != null,
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun NoteDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var note by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_note)) },
        text = {
            OutlinedTextField(note, {
                note = it
            }, label = { Text(stringResource(R.string.note_hint)) }, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = { TextButton(onClick = { onConfirm(note) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
