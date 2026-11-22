package fr.viandes.seches

import android.content.Context
import fr.viandes.seches.data.AppDatabase
import fr.viandes.seches.data.BackupService
import fr.viandes.seches.data.DataStoreSettingsStore
import fr.viandes.seches.data.RoomMeatTypeStore
import fr.viandes.seches.data.RoomPieceStore
import fr.viandes.seches.data.RoomRecipeStore
import fr.viandes.seches.data.SystemClock
import fr.viandes.seches.domain.AlarmScheduler
import fr.viandes.seches.domain.Clock
import fr.viandes.seches.domain.MeatTypeStore
import fr.viandes.seches.domain.PieceActions
import fr.viandes.seches.domain.PieceStore
import fr.viandes.seches.domain.RecipeStore
import fr.viandes.seches.domain.ReminderPlanner
import fr.viandes.seches.domain.Seeder
import fr.viandes.seches.domain.SettingsStore
import fr.viandes.seches.platform.AndroidAlarmScheduler

/** Manual dependency injection (design.md section 1). */
class AppContainer(
    val context: Context,
    databaseFactory: (Context) -> AppDatabase = AppDatabase::build,
    val clock: Clock = SystemClock(),
    alarmFactory: (Context) -> AlarmScheduler = ::AndroidAlarmScheduler,
) {
    val database: AppDatabase by lazy { databaseFactory(context) }
    val settings: SettingsStore by lazy { DataStoreSettingsStore(context) }
    val meatTypes: MeatTypeStore by lazy { RoomMeatTypeStore(database) }
    val recipes: RecipeStore by lazy { RoomRecipeStore(database) }
    val pieces: PieceStore by lazy { RoomPieceStore(database) }
    val alarmScheduler: AlarmScheduler by lazy { alarmFactory(context) }
    val reminderPlanner: ReminderPlanner by lazy { ReminderPlanner(pieces, alarmScheduler, clock) }
    val pieceActions: PieceActions by lazy { PieceActions(pieces, recipes, settings, reminderPlanner, clock) }
    val seeder: Seeder by lazy { Seeder(settings, meatTypes) }
    val backup: BackupService by lazy { BackupService(database) }
}
