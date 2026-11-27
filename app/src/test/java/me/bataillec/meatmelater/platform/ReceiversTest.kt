package me.bataillec.meatmelater.platform

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import me.bataillec.meatmelater.AppTestBase
import me.bataillec.meatmelater.MainActivity
import me.bataillec.meatmelater.domain.PieceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@Suppress("DEPRECATION") // ShadowAlarmManager.ScheduledAlarm exposes plain fields
class ReceiversTest : AppTestBase() {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val notifications get() = context.getSystemService(NotificationManager::class.java)
    private val alarms get() = context.getSystemService(AlarmManager::class.java)

    private fun grantNotifications(granted: Boolean) {
        if (granted) {
            shadowOf(
                app,
            ).grantPermissions(
                Manifest.permission.POST_NOTIFICATIONS,
            )
        } else {
            shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun broadcast(
        receiver: Class<*>,
        action: String?,
        runId: Long? = null,
        pieceId: Long? = null,
    ) {
        val intent = Intent(context, receiver)
        action?.let { intent.action = it }
        runId?.let { intent.putExtra(ReminderReceiver.EXTRA_RUN_ID, it) }
        pieceId?.let { intent.putExtra(MainActivity.EXTRA_PIECE_ID, it) }
        context.sendBroadcast(intent)
        shadowOf(android.os.Looper.getMainLooper()).idle()
    }

    private fun openRun(pieceId: Long) = runBlocking { container.pieces.getSummary(pieceId)!!.openRun!! }

    private fun scheduledCount() = shadowOf(alarms).scheduledAlarms.size

    @Test
    fun `R12 creating a piece registers one exact alarm at the planned end`() {
        val id = createPiece(seedRecipe(), days = 3)
        val run = openRun(id)
        assertEquals(1, scheduledCount())
        assertEquals(run.plannedEndAt, shadowOf(alarms).scheduledAlarms.single().triggerAtTime)
    }

    @Test
    fun `R14 boot completed rebuilds alarms that were lost`() {
        val id = createPiece(seedRecipe())
        shadowOf(alarms).scheduledAlarms.toList().forEach { it.operation?.let { op -> alarms.cancel(op) } }
        container.alarmScheduler.reset()
        assertEquals(0, scheduledCount())
        broadcast(BootReceiver::class.java, Intent.ACTION_BOOT_COMPLETED)
        awaitUntil { scheduledCount() == 1 }
        assertEquals(openRun(id).plannedEndAt, shadowOf(alarms).scheduledAlarms.single().triggerAtTime)
    }

    @Test
    fun `R14 time set timezone changed and package replaced all reconcile`() {
        createPiece(seedRecipe())
        val actions =
            listOf(Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED)
        actions.forEach { action ->
            container.alarmScheduler.reset()
            shadowOf(alarms).scheduledAlarms.toList().forEach { it.operation?.let { op -> alarms.cancel(op) } }
            broadcast(BootReceiver::class.java, action)
            awaitUntil { scheduledCount() == 1 }
        }
    }

    @Test
    fun `R14 unrelated broadcasts are ignored by the boot receiver`() {
        createPiece(seedRecipe())
        shadowOf(alarms).scheduledAlarms.toList().forEach { it.operation?.let { op -> alarms.cancel(op) } }
        container.alarmScheduler.reset()
        broadcast(BootReceiver::class.java, Intent.ACTION_BATTERY_LOW)
        Thread.sleep(300)
        shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(0, scheduledCount())
    }

    @Test
    fun `R13 the alarm posts a notification and records it as notified`() {
        grantNotifications(true)
        val id = createPiece(seedRecipe(), days = 1)
        val run = openRun(id)
        clock.millis = run.plannedEndAt + 1_000
        broadcast(ReminderReceiver::class.java, null, runId = run.id)
        awaitUntil { shadowOf(notifications).allNotifications.isNotEmpty() }
        val n = shadowOf(notifications).allNotifications.single()
        assertEquals(2, n.actions.size)
        // the receiver records the notification right after posting it, on its background thread
        awaitUntil { container.alarmScheduler.lastFiredPlannedEnd(run.id) == run.plannedEndAt }
    }

    @Test
    fun `R16 without notification permission nothing is posted and the run stays un-notified`() {
        grantNotifications(false)
        val id = createPiece(seedRecipe(), days = 1)
        val run = openRun(id)
        clock.millis = run.plannedEndAt + 1_000
        broadcast(ReminderReceiver::class.java, null, runId = run.id)
        Thread.sleep(500)
        shadowOf(android.os.Looper.getMainLooper()).idle()
        assertTrue(shadowOf(notifications).allNotifications.isEmpty())
        assertNull(container.alarmScheduler.lastFiredPlannedEnd(run.id))
        // after the user grants the permission, the next reconcile fires it immediately
        grantNotifications(true)
        runBlocking { container.reminderPlanner.reconcile() }
        assertEquals(clock.millis, shadowOf(alarms).scheduledAlarms.single().triggerAtTime)
    }

    @Test
    fun `R12 an alarm for a run that was moved meanwhile does not notify`() {
        grantNotifications(true)
        val id = createPiece(seedRecipe(), days = 2)
        val run = openRun(id)
        // the alarm fires but the planned end is still two days away
        broadcast(ReminderReceiver::class.java, null, runId = run.id)
        Thread.sleep(500)
        shadowOf(android.os.Looper.getMainLooper()).idle()
        assertTrue(shadowOf(notifications).allNotifications.isEmpty())
    }

    @Test
    fun `R8 R13 the vacuum Fait action never moves the piece from the receiver`() {
        val id = createPiece(seedRecipe())
        val run = openRun(id)
        broadcast(ReminderActionReceiver::class.java, ReminderActionReceiver.ACTION_DONE, run.id, id)
        Thread.sleep(500)
        shadowOf(android.os.Looper.getMainLooper()).idle()
        val summary = runBlocking { container.pieces.getSummary(id)!! }
        assertEquals(PieceStatus.VACUUM, summary.piece.status)
        assertEquals(run.id, summary.openRun!!.id)
        assertNull(summary.piece.fridgeSeasoning)
        assertEquals(1, scheduledCount()) // the vacuum reminder is still pending
    }

    @Test
    fun `R8 R13 the vacuum notification button opens the app on the piece and the fridge one is a broadcast`() {
        grantNotifications(true)
        val id = createPiece(seedRecipe(), days = 1)
        val run = openRun(id)
        clock.millis = run.plannedEndAt + 1_000
        broadcast(ReminderReceiver::class.java, null, runId = run.id)
        awaitUntil { shadowOf(notifications).allNotifications.isNotEmpty() }
        val vacuumDone = shadowOf(notifications).allNotifications.single().actions[0]
        val vacuumIntent = shadowOf(vacuumDone.actionIntent)
        assertTrue(vacuumIntent.isActivityIntent)
        assertEquals(MainActivity::class.java.name, vacuumIntent.savedIntent.component!!.className)
        assertTrue(vacuumIntent.savedIntent.getBooleanExtra(MainActivity.EXTRA_FINISH_VACUUM, false))
        assertEquals(id, vacuumIntent.savedIntent.getLongExtra(MainActivity.EXTRA_PIECE_ID, -1))
        assertEquals(run.id, vacuumIntent.savedIntent.getLongExtra(ReminderReceiver.EXTRA_RUN_ID, -1))

        runBlocking { container.pieceActions.finishStage(id) }
        val fridgeRun = openRun(id)
        clock.millis = fridgeRun.plannedEndAt + 1_000
        broadcast(ReminderReceiver::class.java, null, runId = fridgeRun.id)
        awaitUntil { shadowOf(notifications).allNotifications.size == 2 }
        val fridgeDone = shadowOf(notifications).allNotifications.last().actions[0]
        assertTrue(shadowOf(fridgeDone.actionIntent).isBroadcastIntent)
    }

    @Test
    fun `R13 Fait on a fridge notification finishes the piece and clears the alarm`() {
        val id = createPiece(seedRecipe())
        runBlocking { container.pieceActions.finishStage(id) }
        val run = openRun(id)
        broadcast(ReminderActionReceiver::class.java, ReminderActionReceiver.ACTION_DONE, run.id, id)
        awaitUntil {
            runBlocking {
                container.pieces
                    .getSummary(id)!!
                    .piece.status
            } == PieceStatus.DONE
        }
        awaitUntil { scheduledCount() == 0 }
    }

    @Test
    fun `R13 R23 Reporter 1 jour postpones by one day and counts it`() {
        val id = createPiece(seedRecipe())
        val run = openRun(id)
        broadcast(ReminderActionReceiver::class.java, ReminderActionReceiver.ACTION_POSTPONE, run.id, id)
        awaitUntil { openRun(id).postponedDays == 1 }
        assertTrue(openRun(id).plannedEndAt > run.plannedEndAt)
        awaitUntil { shadowOf(alarms).scheduledAlarms.singleOrNull()?.triggerAtTime == openRun(id).plannedEndAt }
    }

    @Test
    fun `R13 a stale notification action is ignored`() {
        val id = createPiece(seedRecipe())
        val staleRun = openRun(id)
        runBlocking { container.pieceActions.finishStage(id) } // now in FRIDGE, the old run is closed
        broadcast(ReminderActionReceiver::class.java, ReminderActionReceiver.ACTION_DONE, staleRun.id, id)
        Thread.sleep(500)
        shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(
            PieceStatus.FRIDGE,
            runBlocking {
                container.pieces
                    .getSummary(id)!!
                    .piece.status
            },
        )
        assertFalse(runBlocking { container.pieces.getSummary(id)!!.openRun == null })
        assertNotNull(container.alarmScheduler)
    }
}
