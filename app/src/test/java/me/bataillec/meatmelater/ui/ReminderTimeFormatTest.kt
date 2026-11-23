package me.bataillec.meatmelater.ui

import kotlinx.coroutines.test.runTest
import me.bataillec.meatmelater.domain.FakeAlarmScheduler
import me.bataillec.meatmelater.domain.FakeClock
import me.bataillec.meatmelater.domain.FakeMeatTypeStore
import me.bataillec.meatmelater.domain.FakePieceStore
import me.bataillec.meatmelater.domain.FakeRecipeStore
import me.bataillec.meatmelater.domain.FakeSettingsStore
import me.bataillec.meatmelater.domain.MeatType
import me.bataillec.meatmelater.domain.PieceActions
import me.bataillec.meatmelater.domain.Recipe
import me.bataillec.meatmelater.domain.ReminderPlanner
import me.bataillec.meatmelater.ui.settings.formatReminderTime
import me.bataillec.meatmelater.ui.settings.saveReminderTime
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderTimeFormatTest {
    @Test
    fun `R26 reminder time is formatted HH-mm in 24 h`() {
        assertEquals("07:05", formatReminderTime(7, 5))
        assertEquals("18:00", formatReminderTime(18, 0))
        assertEquals("23:59", formatReminderTime(23, 59))
        assertEquals("00:00", formatReminderTime(0, 0))
    }

    @Test
    fun `R26 saving the reminder time stores hour and minute together and reschedules the alarms`() =
        runTest {
            val paris = ZoneId.of("Europe/Paris")
            val now =
                LocalDateTime
                    .of(2026, 11, 1, 9, 0)
                    .atZone(paris)
                    .toInstant()
                    .toEpochMilli()
            val clock = FakeClock(now)
            val recipes = FakeRecipeStore()
            val pieces = FakePieceStore(recipes)
            val scheduler = FakeAlarmScheduler()
            val planner = ReminderPlanner(pieces, scheduler, clock)
            val settings = FakeSettingsStore()
            val meats = FakeMeatTypeStore()
            val meat = meats.save(MeatType(0, "Bœuf"))
            val recipe = recipes.save(Recipe(0, "Classique", BigDecimal("25"), BigDecimal("5"), BigDecimal("2")))
            PieceActions(pieces, recipes, meats, settings, planner, clock)
                .createPiece("P", recipe, meat, 1250, BigDecimal("12"))
            scheduler.reset()
            assertEquals(emptySet<Long>(), scheduler.scheduledIds())

            saveReminderTime(settings, planner, 7, 37)

            assertEquals(7 to 37, settings.current().let { it.reminderHour to it.reminderMinute })
            assertEquals(1, scheduler.scheduledIds().size)
        }
}
