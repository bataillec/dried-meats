package me.bataillec.meatmelater

import android.content.Context
import me.bataillec.meatmelater.data.AppDatabase
import me.bataillec.meatmelater.data.BackupService
import me.bataillec.meatmelater.data.DataStoreSettingsStore
import me.bataillec.meatmelater.data.RoomMeatTypeStore
import me.bataillec.meatmelater.data.RoomPieceStore
import me.bataillec.meatmelater.data.RoomRecipeStore
import me.bataillec.meatmelater.data.SystemClock
import me.bataillec.meatmelater.domain.AlarmScheduler
import me.bataillec.meatmelater.domain.Clock
import me.bataillec.meatmelater.domain.MeatTypeStore
import me.bataillec.meatmelater.domain.PieceActions
import me.bataillec.meatmelater.domain.PieceStore
import me.bataillec.meatmelater.domain.RecipeStore
import me.bataillec.meatmelater.domain.ReminderPlanner
import me.bataillec.meatmelater.domain.Seeder
import me.bataillec.meatmelater.domain.SettingsStore
import me.bataillec.meatmelater.platform.AndroidAlarmScheduler

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
