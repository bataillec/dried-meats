package me.bataillec.meatmelater.ui.stats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.bataillec.meatmelater.AppContainer
import me.bataillec.meatmelater.R
import me.bataillec.meatmelater.domain.FinishedPiece
import me.bataillec.meatmelater.domain.Stage
import me.bataillec.meatmelater.domain.Statistics
import me.bataillec.meatmelater.ui.common.SectionTitle
import me.bataillec.meatmelater.ui.common.formatDate
import me.bataillec.meatmelater.ui.common.fr
import me.bataillec.meatmelater.ui.common.rememberContainer
import java.math.BigDecimal

/** Average actual duration per stage for one recipe, from finished runs (R19). */
data class RecipeStats(
    val meatName: String,
    val recipeName: String,
    val count: Int,
    val vacuumAvg: BigDecimal?,
    val fridgeAvg: BigDecimal?,
)

data class StatsUiState(
    val perRecipe: List<RecipeStats> = emptyList(),
    val finished: List<FinishedPiece> = emptyList(),
)

fun computeStats(finished: List<FinishedPiece>): StatsUiState {
    val perRecipe =
        finished
            .groupBy { it.summary.meatTypeName to it.summary.recipeName }
            .map { (key, pieces) ->
                val runs = pieces.flatMap { it.runs }
                RecipeStats(
                    meatName = key.first,
                    recipeName = key.second,
                    count = pieces.size,
                    vacuumAvg = Statistics.averageDays(runs.filter { it.stage == Stage.VACUUM }),
                    fridgeAvg = Statistics.averageDays(runs.filter { it.stage == Stage.FRIDGE }),
                )
            }.sortedWith(compareBy({ it.meatName.lowercase() }, { it.recipeName.lowercase() }))
    return StatsUiState(perRecipe, finished.sortedByDescending { it.runs.maxOfOrNull { r -> r.endedAt ?: 0L } ?: 0L })
}

class StatsViewModel(
    container: AppContainer,
) : ViewModel() {
    val state: StateFlow<StatsUiState> =
        container.pieces
            .observeFinished()
            .map(
                ::computeStats,
            ).stateIn(viewModelScope, SharingStarted.Eagerly, StatsUiState())
}

@Composable
fun StatsScreen(onOpenPiece: (Long) -> Unit) {
    val container = rememberContainer()
    val vm: StatsViewModel = viewModel { StatsViewModel(container) }
    val state by vm.state.collectAsState()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding =
            androidx.compose.foundation.layout
                .PaddingValues(16.dp),
    ) {
        if (state.finished.isEmpty()) {
            item { Text(stringResource(R.string.stats_empty), style = MaterialTheme.typography.bodyLarge) }
        }
        if (state.perRecipe.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.stats_averages)) }
            items(state.perRecipe, key = { "${it.meatName}/${it.recipeName}" }) { s ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("${s.meatName} · ${s.recipeName}", style = MaterialTheme.typography.titleMedium)
                        Text(
                            androidx.compose.ui.res
                                .pluralStringResource(R.plurals.finished_pieces, s.count, s.count),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(stringResource(R.string.stats_vacuum_avg, s.vacuumAvg?.fr() ?: "–"))
                        Text(stringResource(R.string.stats_fridge_avg, s.fridgeAvg?.fr() ?: "–"))
                    }
                }
            }
        }
        if (state.finished.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.stats_finished)) }
            items(state.finished, key = { it.summary.piece.id }) { f ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onOpenPiece(f.summary.piece.id) }) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                f.summary.piece.label,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            f.runs.maxOfOrNull { it.endedAt ?: 0L }?.let {
                                Text(
                                    formatDate(it),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        Text(
                            "${f.summary.meatTypeName} · ${f.summary.recipeName}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        val parts =
                            f.runs.sortedBy { it.startedAt }.mapNotNull { r ->
                                Statistics.actualDays(r)?.let { actual ->
                                    val plannedDays =
                                        java.math
                                            .BigDecimal(
                                                r.plannedEndAt - r.startedAt,
                                            ).divide(
                                                java.math.BigDecimal(86_400_000L),
                                                1,
                                                java.math.RoundingMode.HALF_UP,
                                            )
                                    Triple(r.stage, plannedDays, actual)
                                }
                            }
                        parts.forEach { (stage, planned, actual) ->
                            Text(
                                stringResource(
                                    R.string.stats_planned_vs_actual,
                                    stringResource(
                                        if (stage ==
                                            Stage.VACUUM
                                        ) {
                                            R.string.stage_vacuum
                                        } else {
                                            R.string.stage_fridge
                                        },
                                    ),
                                    planned.fr(),
                                    actual.fr(),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}
