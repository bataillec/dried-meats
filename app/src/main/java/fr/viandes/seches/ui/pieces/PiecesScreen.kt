package fr.viandes.seches.ui.pieces

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.viandes.seches.AppContainer
import fr.viandes.seches.R
import fr.viandes.seches.domain.PieceSummary
import fr.viandes.seches.domain.Stage
import fr.viandes.seches.platform.PermissionBanner
import fr.viandes.seches.ui.common.SectionTitle
import fr.viandes.seches.ui.common.formatDateTime
import fr.viandes.seches.ui.common.remainingText
import fr.viandes.seches.ui.common.rememberContainer
import fr.viandes.seches.ui.common.ticker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class PiecesUiState(
    val overdue: List<PieceSummary> = emptyList(),
    val running: List<PieceSummary> = emptyList(),
    val nowMillis: Long = 0,
    val loaded: Boolean = false,
)

/** R15: overdue stages are pinned on top, then running pieces by next deadline. */
class PiecesViewModel(
    container: AppContainer,
) : ViewModel() {
    val state: StateFlow<PiecesUiState> =
        combine(container.pieces.observeActive(), container.ticker()) { pieces, now ->
            val withRun = pieces.filter { it.openRun != null }
            val (overdue, running) = withRun.partition { it.openRun!!.isOverdue(now) }
            PiecesUiState(
                overdue = overdue.sortedBy { it.openRun!!.plannedEndAt },
                running = running.sortedBy { it.openRun!!.plannedEndAt },
                nowMillis = now,
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, PiecesUiState())
}

@Composable
fun PiecesScreen(
    onOpen: (Long) -> Unit,
    onNew: () -> Unit,
) {
    val container = rememberContainer()
    val vm: PiecesViewModel = viewModel { PiecesViewModel(container) }
    val state by vm.state.collectAsState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp)) {
            item { PermissionBanner() }
            if (state.loaded && state.overdue.isEmpty() && state.running.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.pieces_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
            if (state.overdue.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.overdue_section)) }
                items(state.overdue, key = { "o${it.piece.id}" }) { PieceRow(it, state.nowMillis, true, onOpen) }
            }
            if (state.running.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.running_section)) }
                items(state.running, key = { "r${it.piece.id}" }) { PieceRow(it, state.nowMillis, false, onOpen) }
            }
        }
        FloatingActionButton(onClick = onNew, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_piece))
        }
    }
}

@Composable
private fun PieceRow(
    summary: PieceSummary,
    now: Long,
    overdue: Boolean,
    onOpen: (Long) -> Unit,
) {
    val run = summary.openRun ?: return
    val colors =
        if (overdue) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onOpen(summary.piece.id) }, colors = colors) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(summary.piece.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                StageChip(run.stage)
            }
            Text("${summary.meatTypeName} · ${summary.recipeName}", style = MaterialTheme.typography.bodyMedium)
            Text(
                remainingText(run.plannedEndAt, now) + " · " + formatDateTime(run.plannedEndAt),
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (overdue) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
    }
}

@Composable
fun StageChip(stage: Stage) {
    val (label, color) =
        when (stage) {
            Stage.VACUUM -> stringResource(R.string.stage_vacuum) to MaterialTheme.colorScheme.secondaryContainer
            Stage.FRIDGE -> stringResource(R.string.stage_fridge) to MaterialTheme.colorScheme.primaryContainer
        }
    androidx.compose.material3.Surface(color = color, shape = MaterialTheme.shapes.small) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
