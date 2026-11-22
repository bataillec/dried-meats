package fr.viandes.seches.platform

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import fr.viandes.seches.R

/** What the system currently lets the app do for reliable reminders (R16). */
data class ReminderPermissions(
    val notifications: Boolean,
    val exactAlarms: Boolean,
    val batteryExempt: Boolean,
) {
    val allGranted: Boolean get() = notifications && exactAlarms && batteryExempt

    companion object {
        fun read(context: Context): ReminderPermissions {
            val notif =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            val alarms = context.getSystemService(AlarmManager::class.java)
            val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
            val power = context.getSystemService(PowerManager::class.java)
            return ReminderPermissions(notif, exact, power.isIgnoringBatteryOptimizations(context.packageName))
        }
    }
}

object PermissionActions {
    fun exactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    // Sideloaded APK (intent.md section 6): the Play policy on this intent does not apply.
    @SuppressLint("BatteryLife")
    fun batteryExemption(context: Context) {
        val request =
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(request)
        } catch (_: android.content.ActivityNotFoundException) {
            context.startActivity(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

@Composable
fun rememberReminderPermissions(): ReminderPermissions {
    val context = LocalContext.current
    var state by remember { mutableStateOf(ReminderPermissions.read(context)) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event ==
                    Lifecycle.Event.ON_RESUME
                ) {
                    state = ReminderPermissions.read(context)
                }
            }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return state
}

/** Home-screen warning listing every missing permission with a button to fix it (R16). */
@Composable
fun PermissionBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val permissions = rememberReminderPermissions()
    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    if (permissions.allGranted) return
    Card(
        modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.permissions_warning_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                stringResource(R.string.permissions_warning_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(vertical = 4.dp),
            )
            if (!permissions.notifications) {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >=
                            Build.VERSION_CODES.TIRAMISU
                        ) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                ) { Text(stringResource(R.string.allow_notifications)) }
            }
            if (!permissions.exactAlarms) {
                Button(onClick = {
                    PermissionActions.exactAlarmSettings(context)
                }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(stringResource(R.string.allow_exact_alarms))
                }
            }
            if (!permissions.batteryExempt) {
                Button(onClick = {
                    PermissionActions.batteryExemption(context)
                }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(stringResource(R.string.allow_battery))
                }
            }
        }
    }
}

/**
 * R16: first-launch explanation of why the app should be exempt from battery optimisation, with a
 * deep link to the system dialog. Shown once (the flag is stored in settings), and never when the
 * exemption is already granted.
 */
@Composable
fun BatteryOnboardingDialog(
    shouldShow: Boolean,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val permissions = rememberReminderPermissions()
    if (!batteryWizardVisible(shouldShow, permissions.batteryExempt)) return
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onFinished,
        title = { Text(stringResource(R.string.battery_wizard_title)) },
        text = { Text(stringResource(R.string.battery_wizard_text)) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                PermissionActions.batteryExemption(context)
                onFinished()
            }) { Text(stringResource(R.string.battery_wizard_open)) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(
                onClick = onFinished,
            ) { Text(stringResource(R.string.battery_wizard_later)) }
        },
    )
}

/** Pure rule behind the first-launch wizard: only when not yet shown and not already exempt. */
fun batteryWizardVisible(
    notYetShown: Boolean,
    alreadyExempt: Boolean,
): Boolean = notYetShown && !alreadyExempt
