package me.bataillec.meatmelater.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import me.bataillec.meatmelater.domain.AccentColor
import me.bataillec.meatmelater.domain.AppSettings
import me.bataillec.meatmelater.domain.SettingsStore
import me.bataillec.meatmelater.domain.ThemeMode

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("settings")

/** R24-R27: theme, accent, reminder time and global defaults, persisted in DataStore. */
class DataStoreSettingsStore(
    context: Context,
) : SettingsStore {
    private val store = context.applicationContext.settingsDataStore

    override fun observe(): Flow<AppSettings> = store.data.map { it.toAppSettings() }

    override suspend fun current(): AppSettings = observe().first()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs ->
            val next = transform(prefs.toAppSettings())
            prefs[THEME] = next.theme.name
            prefs[ACCENT] = next.accent.name
            prefs[HOUR] = next.reminderHour
            prefs[MINUTE] = next.reminderMinute
            prefs[FRIDGE_DAYS] = next.defaultFridgeMinDays
            prefs[DIVISOR] = next.defaultVacuumDivisor.toPlainString()
            prefs[OFFSET] = next.defaultVacuumOffsetDays.toPlainString()
            prefs[SEEDED] = next.seeded
            prefs[BATTERY_HINT] = next.batteryHintShown
        }
    }
}

private val THEME = stringPreferencesKey("theme")
private val ACCENT = stringPreferencesKey("accent")
private val HOUR = intPreferencesKey("reminder_hour")
private val MINUTE = intPreferencesKey("reminder_minute")
private val FRIDGE_DAYS = intPreferencesKey("default_fridge_min_days")
private val DIVISOR = stringPreferencesKey("default_vacuum_divisor")
private val OFFSET = stringPreferencesKey("default_vacuum_offset_days")
private val SEEDED = booleanPreferencesKey("seeded")
private val BATTERY_HINT = booleanPreferencesKey("battery_hint_shown")

internal fun Preferences.toAppSettings(): AppSettings {
    val defaults = AppSettings()
    return AppSettings(
        theme = this[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: defaults.theme,
        accent = this[ACCENT]?.let { runCatching { AccentColor.valueOf(it) }.getOrNull() } ?: defaults.accent,
        reminderHour = this[HOUR] ?: defaults.reminderHour,
        reminderMinute = this[MINUTE] ?: defaults.reminderMinute,
        defaultFridgeMinDays = this[FRIDGE_DAYS] ?: defaults.defaultFridgeMinDays,
        defaultVacuumDivisor = this[DIVISOR]?.toBigDecimalOrNull() ?: defaults.defaultVacuumDivisor,
        defaultVacuumOffsetDays = this[OFFSET]?.toBigDecimalOrNull() ?: defaults.defaultVacuumOffsetDays,
        seeded = this[SEEDED] ?: false,
        batteryHintShown = this[BATTERY_HINT] ?: false,
    )
}
