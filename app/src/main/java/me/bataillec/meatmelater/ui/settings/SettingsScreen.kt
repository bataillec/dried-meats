package me.bataillec.meatmelater.ui.settings

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bataillec.meatmelater.BuildConfig
import me.bataillec.meatmelater.R
import me.bataillec.meatmelater.data.BackupFormatException
import me.bataillec.meatmelater.domain.AccentColor
import me.bataillec.meatmelater.domain.AppSettings
import me.bataillec.meatmelater.domain.Recipe
import me.bataillec.meatmelater.domain.StageDurationRule
import me.bataillec.meatmelater.domain.ThemeMode
import me.bataillec.meatmelater.platform.ExternalLinkOpener
import me.bataillec.meatmelater.platform.PermissionActions
import me.bataillec.meatmelater.platform.SupportLinks
import me.bataillec.meatmelater.platform.rememberExternalLinkOpener
import me.bataillec.meatmelater.platform.rememberReminderPermissions
import me.bataillec.meatmelater.ui.common.ConfirmDialog
import me.bataillec.meatmelater.ui.common.RecipeOption
import me.bataillec.meatmelater.ui.common.RecipePicker
import me.bataillec.meatmelater.ui.common.SectionTitle
import me.bataillec.meatmelater.ui.common.durationExampleText
import me.bataillec.meatmelater.ui.common.fr
import me.bataillec.meatmelater.ui.common.recipeOptions
import me.bataillec.meatmelater.ui.common.rememberContainer
import me.bataillec.meatmelater.ui.theme.accentSwatch
import java.math.BigDecimal

private val HALF = BigDecimal("0.5")

@Composable
fun SettingsScreen(linkOpener: ExternalLinkOpener = rememberExternalLinkOpener()) {
    val container = rememberContainer()
    val context = androidx.compose.ui.platform.LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val scope = rememberCoroutineScope()
    val settings by container.settings.observe().collectAsState(initial = AppSettings())
    val options by remember { container.recipeOptions() }.collectAsState(initial = emptyList())
    val permissions = rememberReminderPermissions()
    var recipeId by remember { mutableStateOf<Long?>(null) }
    var showMeats by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<android.net.Uri?>(null) }
    val option = options.firstOrNull { it.recipe.id == recipeId } ?: options.firstOrNull()

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                scope.launch {
                    message =
                        runCatching {
                            val text = container.backup.export()
                            withContext(
                                Dispatchers.IO,
                            ) { context.contentResolver.openOutputStream(uri)!!.use { it.write(text.toByteArray()) } }
                        }.fold(
                            { resources.getString(R.string.export_done) },
                            { resources.getString(R.string.export_failed) },
                        )
                }
            }
        }
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri !=
                null
            ) {
                pendingImport = uri
            }
        }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionTitle(stringResource(R.string.appearance))
        Column(Modifier.selectableGroup()) {
            ThemeMode.entries.forEach { mode ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(selected = settings.theme == mode, role = Role.RadioButton, onClick = {
                            scope.launch { container.settings.update { it.copy(theme = mode) } }
                        }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = settings.theme == mode, onClick = null)
                    Text(stringResource(themeLabel(mode)), Modifier.padding(start = 12.dp))
                }
            }
        }
        Text(stringResource(R.string.theme_system_note), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.accent_title), style = MaterialTheme.typography.titleSmall)
        AccentSwatches(
            settings.accent,
        ) { accent -> scope.launch { container.settings.update { it.copy(accent = accent) } } }

        SectionTitle(stringResource(R.string.vacuum_doses))
        if (option == null) {
            Text(stringResource(R.string.calculator_no_recipe))
        } else {
            RecipePicker(options, option.recipe.id, { recipeId = it }, Modifier.fillMaxWidth())
            RecipeSteppers(option, scope) { updated -> container.recipes.save(updated) }
            Text(stringResource(R.string.running_pieces_keep_doses), style = MaterialTheme.typography.bodySmall)
        }

        SectionTitle(stringResource(R.string.fridge_and_reminders))
        Stepper(stringResource(R.string.fridge_min_days_default), settings.defaultFridgeMinDays.toString(), {
            scope.launch {
                container.settings.update {
                    it.copy(
                        defaultFridgeMinDays = (it.defaultFridgeMinDays - 1).coerceAtLeast(1),
                    )
                }
            }
        }, {
            scope.launch { container.settings.update { it.copy(defaultFridgeMinDays = it.defaultFridgeMinDays + 1) } }
        })
        ReminderTimeRow(settings.reminderHour, settings.reminderMinute) { hour, minute ->
            scope.launch { saveReminderTime(container.settings, container.reminderPlanner, hour, minute) }
        }
        Text(stringResource(R.string.reminder_time_note), style = MaterialTheme.typography.bodySmall)

        SectionTitle(stringResource(R.string.permissions))
        PermissionRow(stringResource(R.string.perm_notifications), permissions.notifications)
        PermissionRow(stringResource(R.string.perm_exact_alarms), permissions.exactAlarms) {
            PermissionActions.exactAlarmSettings(context)
        }
        PermissionRow(stringResource(R.string.perm_battery), permissions.batteryExempt) {
            PermissionActions.batteryExemption(context)
        }

        SectionTitle(stringResource(R.string.meat_types))
        OutlinedButton(onClick = { showMeats = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.manage_meat_types))
        }

        SectionTitle(stringResource(R.string.data))
        Button(onClick = { exportLauncher.launch("meat-me-later.json") }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.export_json))
        }
        OutlinedButton(onClick = {
            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
        }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.import_json))
        }
        message?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        SectionTitle(stringResource(R.string.about))
        Text(
            stringResource(R.string.version_format, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(stringResource(R.string.about_text), style = MaterialTheme.typography.bodySmall)
        val noBrowser = stringResource(R.string.about_no_browser)
        val links =
            listOf(
                Triple(
                    stringResource(R.string.about_kofi),
                    stringResource(R.string.about_link_desc, stringResource(R.string.about_kofi)),
                    SupportLinks.KOFI_URL,
                ),
                Triple(
                    stringResource(R.string.about_github),
                    stringResource(R.string.about_link_desc, stringResource(R.string.about_github)),
                    SupportLinks.GITHUB_URL,
                ),
            )
        for ((label, spoken, url) in links) {
            TextButton(
                onClick = { if (!linkOpener.open(url)) message = noBrowser },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = spoken },
            ) {
                Text(label, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showMeats) {
        val meatVm: MeatTypesViewModel = viewModel { MeatTypesViewModel(container) }
        val meats by meatVm.meatTypes.collectAsState()
        MeatTypesDialog(meats, meatVm) { showMeats = false }
    }
    pendingImport?.let { uri ->
        ConfirmDialog(
            title = stringResource(R.string.import_json),
            text = stringResource(R.string.import_confirm),
            confirmLabel = stringResource(R.string.import_replace),
            onConfirm = {
                pendingImport = null
                scope.launch {
                    message =
                        importFrom(context, uri, container.backup::import, container.reminderPlanner::reconcile)
                }
            },
            onDismiss = { pendingImport = null },
        )
    }
}

private suspend fun importFrom(
    context: Context,
    uri: android.net.Uri,
    import: suspend (String) -> Unit,
    reconcile: suspend () -> Unit,
): String =
    try {
        val text =
            withContext(
                Dispatchers.IO,
            ) { context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }
        import(text)
        reconcile()
        context.getString(R.string.import_done)
    } catch (e: BackupFormatException) {
        context.getString(R.string.import_invalid, e.message.orEmpty())
    } catch (
        @Suppress("TooGenericExceptionCaught") e: Exception,
    ) {
        context.getString(R.string.import_failed)
    }

private fun themeLabel(mode: ThemeMode): Int =
    when (mode) {
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
        ThemeMode.OLED -> R.string.theme_oled
        ThemeMode.SYSTEM -> R.string.theme_system
    }

/** R25: g/kg steppers plus the duration rule with a live example (round-up shown). */
@Composable
private fun RecipeSteppers(
    option: RecipeOption,
    scope: kotlinx.coroutines.CoroutineScope,
    save: suspend (Recipe) -> Long,
) {
    val r = option.recipe

    fun change(transform: (Recipe) -> Recipe) = scope.launch { save(transform(r)) }

    fun step(
        value: BigDecimal,
        delta: BigDecimal,
        min: BigDecimal = BigDecimal.ZERO,
    ) = value.add(delta).max(min)
    Stepper(
        stringResource(R.string.salt) + " (g/kg)",
        r.saltGPerKg.fr(),
        { change { it.copy(saltGPerKg = step(it.saltGPerKg, -HALF)) } },
        { change { it.copy(saltGPerKg = step(it.saltGPerKg, HALF)) } },
    )
    Stepper(
        stringResource(R.string.sugar) + " (g/kg)",
        r.sugarGPerKg.fr(),
        { change { it.copy(sugarGPerKg = step(it.sugarGPerKg, -HALF)) } },
        { change { it.copy(sugarGPerKg = step(it.sugarGPerKg, HALF)) } },
    )
    Stepper(
        stringResource(R.string.pepper) + " (g/kg)",
        r.pepperGPerKg.fr(),
        { change { it.copy(pepperGPerKg = step(it.pepperGPerKg, -HALF)) } },
        { change { it.copy(pepperGPerKg = step(it.pepperGPerKg, HALF)) } },
    )
    SectionTitle(stringResource(R.string.vacuum_duration))
    Stepper(
        stringResource(R.string.divisor),
        r.vacuumDivisor.fr(),
        { change { it.copy(vacuumDivisor = step(it.vacuumDivisor, -HALF, HALF)) } },
        { change { it.copy(vacuumDivisor = step(it.vacuumDivisor, HALF, HALF)) } },
    )
    Stepper(
        stringResource(R.string.added_days),
        r.vacuumOffsetDays.fr(),
        { change { it.copy(vacuumOffsetDays = step(it.vacuumOffsetDays, -HALF)) } },
        { change { it.copy(vacuumOffsetDays = step(it.vacuumOffsetDays, HALF)) } },
    )
    Card(Modifier.fillMaxWidth()) {
        Text(
            durationExampleText(StageDurationRule(r.vacuumDivisor, r.vacuumOffsetDays)),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun Stepper(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        OutlinedIconButton(onClick = onMinus, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)) {
            Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.decrease))
        }
        Text(
            value,
            Modifier.widthIn(min = 56.dp).padding(horizontal = 8.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedIconButton(onClick = onPlus, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.increase))
        }
    }
}

@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    onFix: (() -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        if (granted) {
            Text(stringResource(R.string.perm_ok), color = MaterialTheme.colorScheme.primary)
        } else if (onFix != null) {
            OutlinedButton(onClick = onFix) { Text(stringResource(R.string.perm_fix)) }
        } else {
            Text(stringResource(R.string.perm_missing), color = MaterialTheme.colorScheme.error)
        }
    }
}

/** R27: one 48 dp swatch per accent, wrapping onto several lines; the selection shows a check mark and a spoken label. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentSwatches(
    selected: AccentColor,
    onSelect: (AccentColor) -> Unit,
) {
    FlowRow(
        Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AccentColor.entries.forEach { accent ->
            val name = stringResource(accentLabel(accent))
            val isSelected = accent == selected
            val description = if (isSelected) stringResource(R.string.accent_selected, name) else name
            val swatch = accentSwatch(accent)
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(accent) })
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(swatch)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = if (swatch.luminance() > 0.5f) Color.Black else Color.White,
                        )
                    }
                }
            }
        }
    }
}

private fun accentLabel(accent: AccentColor): Int =
    when (accent) {
        AccentColor.VIANDE_SECHEE -> R.string.accent_viande_sechee
        AccentColor.VERT -> R.string.accent_vert
        AccentColor.BLEU -> R.string.accent_bleu
        AccentColor.VIOLET -> R.string.accent_violet
        AccentColor.AMBRE -> R.string.accent_ambre
        AccentColor.BRUN_GRIS -> R.string.accent_brun_gris
    }
