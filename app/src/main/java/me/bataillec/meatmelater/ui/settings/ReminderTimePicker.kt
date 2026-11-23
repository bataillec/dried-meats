package me.bataillec.meatmelater.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import me.bataillec.meatmelater.R
import me.bataillec.meatmelater.domain.ReminderPlanner
import me.bataillec.meatmelater.domain.SettingsStore
import java.util.Locale

/** `HH:mm`, 24 h, zero padded (R26). */
internal fun formatReminderTime(
    hour: Int,
    minute: Int,
): String = "%02d:%02d".format(Locale.ROOT, hour, minute)

/** Saves hour and minute together, then rebuilds the alarms once (R26, R12-R16). */
internal suspend fun saveReminderTime(
    settings: SettingsStore,
    planner: ReminderPlanner,
    hour: Int,
    minute: Int,
) {
    settings.update { it.copy(reminderHour = hour, reminderMinute = minute) }
    planner.reconcile()
}

/** Above this font scale the picker opens directly in keyboard mode (it fits better than the dial). */
private const val LARGE_FONT_SCALE = 1.5f

/**
 * One row showing the reminder time as `HH:mm`; a tap opens the time picker dialog.
 * [onConfirm] is called with the chosen hour and minute only when the user validates (R26).
 */
@Composable
fun ReminderTimeRow(
    hour: Int,
    minute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val label = stringResource(R.string.reminder_time)
    val time = formatReminderTime(hour, minute)
    val description = stringResource(R.string.reminder_time_description, time)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable { open = true }
            .semantics(mergeDescendants = true) {
                contentDescription = description
                role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(time, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleMedium)
        Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.padding(start = 8.dp))
    }
    if (open) {
        ReminderTimeDialog(
            hour = hour,
            minute = minute,
            onDismiss = { open = false },
            onConfirm = { h, m ->
                open = false
                onConfirm(h, m)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(
    hour: Int,
    minute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    var keyboard by rememberSaveable { mutableStateOf(false) }
    val large = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    val useKeyboard = keyboard || large
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminder_time_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (useKeyboard) TimeInput(state) else TimePicker(state)
                if (!large) {
                    val toggleLabel =
                        stringResource(
                            if (keyboard) R.string.reminder_time_use_dial else R.string.reminder_time_use_keyboard,
                        )
                    IconButton(onClick = { keyboard = !keyboard }) {
                        Icon(if (keyboard) Icons.Filled.AccessTime else Icons.Filled.Keyboard, toggleLabel)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text(stringResource(R.string.validate))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
