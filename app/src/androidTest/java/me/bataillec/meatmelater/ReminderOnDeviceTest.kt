package me.bataillec.meatmelater

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.runBlocking
import me.bataillec.meatmelater.domain.MeatType
import me.bataillec.meatmelater.domain.Recipe
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

/** N5 / R12 on a real AlarmManager: an alarm due in a few seconds posts the notification promptly. */
@RunWith(AndroidJUnit4::class)
class ReminderOnDeviceTest {
    @get:Rule
    val permission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @Test
    fun alarmDueInSecondsPostsNotificationWithinSixtySeconds() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = (context as MeatMeLaterApp).container
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancelAll()
        val id =
            runBlocking {
                val meat = container.meatTypes.save(MeatType(0, "Device"))
                val recipe =
                    container.recipes.save(
                        Recipe(0, "Rappel", BigDecimal("25"), BigDecimal("5"), BigDecimal("2")),
                    )
                container.pieceActions.createPiece(
                    "Rappel ${System.nanoTime()}",
                    recipe,
                    meat,
                    1000,
                    BigDecimal("10"),
                )!!
            }
        // Move the planned end a few seconds ahead, then rebuild the alarm from the database.
        val dueIn = 5_000L
        val triggerAt = container.clock.nowMillis() + dueIn
        runBlocking {
            val run = container.pieces.getSummary(id)!!.openRun!!
            container.pieces.updateRun(run.copy(plannedEndAt = triggerAt))
            container.reminderPlanner.reconcile()
        }
        val deadline = triggerAt + 60_000
        while (manager.activeNotifications.isEmpty() && container.clock.nowMillis() < deadline) Thread.sleep(500)
        val delay = container.clock.nowMillis() - triggerAt
        assertTrue("no notification", manager.activeNotifications.isNotEmpty())
        assertTrue("notification came ${delay}ms after the planned time", delay < 60_000)
    }
}
