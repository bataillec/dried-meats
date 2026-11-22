package fr.viandes.seches.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import fr.viandes.seches.domain.AppSettings
import fr.viandes.seches.domain.SettingsStore
import fr.viandes.seches.domain.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("settings")

/** R24-R26: theme, reminder time and global defaults, persisted in DataStore. */
class DataStoreSettingsStore(
    context: Context,
) : SettingsStore {
    private val store = context.applicationContext.settingsDataStore

    override fun observe(): Flow<AppSettings> = store.data.map { it.toSettings() }

    override suspend fun current(): AppSettings = observe().first()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[THEME] = next.theme.name
            prefs[HOUR] = next.reminderHour
            prefs[MINUTE] = next.reminderMinute
            prefs[FRIDGE_DAYS] = next.defaultFridgeMinDays
            prefs[DIVISOR] = next.defaultVacuumDivisor.toPlainString()
            prefs[OFFSET] = next.defaultVacuumOffsetDays.toPlainString()
            prefs[SEEDED] = next.seeded
            prefs[BATTERY_HINT] = next.batteryHintShown
        }
    }

    private fun Preferences.toSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            theme = this[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: defaults.theme,
            reminderHour = this[HOUR] ?: defaults.reminderHour,
            reminderMinute = this[MINUTE] ?: defaults.reminderMinute,
            defaultFridgeMinDays = this[FRIDGE_DAYS] ?: defaults.defaultFridgeMinDays,
            defaultVacuumDivisor = this[DIVISOR]?.toBigDecimalOrNull() ?: defaults.defaultVacuumDivisor,
            defaultVacuumOffsetDays = this[OFFSET]?.toBigDecimalOrNull() ?: defaults.defaultVacuumOffsetDays,
            seeded = this[SEEDED] ?: false,
            batteryHintShown = this[BATTERY_HINT] ?: false,
        )
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val HOUR = intPreferencesKey("reminder_hour")
        val MINUTE = intPreferencesKey("reminder_minute")
        val FRIDGE_DAYS = intPreferencesKey("default_fridge_min_days")
        val DIVISOR = stringPreferencesKey("default_vacuum_divisor")
        val OFFSET = stringPreferencesKey("default_vacuum_offset_days")
        val SEEDED = booleanPreferencesKey("seeded")
        val BATTERY_HINT = booleanPreferencesKey("battery_hint_shown")
    }
}
