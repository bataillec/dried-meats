package me.bataillec.meatmelater.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class PieceStateMachineTest {
    private val paris = ZoneId.of("Europe/Paris")

    private fun at(
        d: Int,
        h: Int,
    ) = LocalDateTime
        .of(2026, 11, d, h, 0)
        .atZone(paris)
        .toInstant()
        .toEpochMilli()

    private val clock = FakeClock(at(1, 9))
    private val machine = PieceStateMachine(clock, PlannedEndCalculator(paris, LocalTime.of(18, 0)))

    @Test
    fun `R7 new piece starts in vacuum with planned end at reminder time`() {
        val run = machine.startVacuum(1, 7)
        assertEquals(Stage.VACUUM, run.stage)
        assertEquals(at(1, 9), run.startedAt)
        assertEquals(at(8, 18), run.plannedEndAt)
        assertTrue(run.isOpen)
    }

    @Test
    fun `R8 finishing vacuum opens the fridge run and stores trimmed seasoning`() {
        val run = machine.startVacuum(1, 7)
        clock.millis = at(5, 10)
        val t = machine.finish(PieceStatus.VACUUM, run, "  herbes  ", 14)
        assertEquals(PieceStatus.FRIDGE, t.status)
        assertEquals("herbes", t.fridgeSeasoning)
        assertEquals(Stage.FRIDGE, t.nextRun!!.stage)
        assertEquals(at(19, 18), t.nextRun.plannedEndAt)
    }

    @Test
    fun `R8 empty seasoning means none`() {
        val run = machine.startVacuum(1, 7)
        assertNull(machine.finish(PieceStatus.VACUUM, run, "   ", 14).fridgeSeasoning)
        assertNull(machine.finish(PieceStatus.VACUUM, run, null, 14).fridgeSeasoning)
    }

    @Test
    fun `R9 early finish records actual end and next stage starts at that moment`() {
        val run = machine.startVacuum(1, 7)
        clock.millis = at(3, 12)
        val t = machine.finish(PieceStatus.VACUUM, run, null, 14)
        assertEquals(at(3, 12), t.closedRun.endedAt)
        assertEquals(at(3, 12), t.nextRun!!.startedAt)
    }

    @Test
    fun `R9 late finish also uses the actual time`() {
        val run = machine.startVacuum(1, 2)
        clock.millis = at(20, 7)
        val t = machine.finish(PieceStatus.VACUUM, run, null, 14)
        assertEquals(at(20, 7), t.closedRun.endedAt)
        assertEquals(at(20, 7), t.nextRun!!.startedAt)
    }

    @Test
    fun `R8 finishing fridge ends the piece`() {
        val vacuum = machine.finish(PieceStatus.VACUUM, machine.startVacuum(1, 7), null, 14)
        clock.millis = at(25, 9)
        val done = machine.finish(PieceStatus.FRIDGE, vacuum.nextRun!!, "x", 14)
        assertEquals(PieceStatus.DONE, done.status)
        assertNull(done.nextRun)
        assertEquals(at(25, 9), done.closedRun.endedAt)
    }

    @Test(expected = IllegalStateException::class)
    fun `R8 a done piece cannot be finished again`() {
        val run = machine.startVacuum(1, 7).copy(endedAt = 1L)
        machine.finish(PieceStatus.DONE, run.copy(endedAt = null), null, 14)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `R8 a closed run cannot be finished`() {
        val run = machine.startVacuum(1, 7).copy(endedAt = 5L)
        machine.finish(PieceStatus.VACUUM, run, null, 14)
    }

    @Test
    fun `R11 cancel closes the run`() {
        val run = machine.startVacuum(1, 7)
        clock.millis = at(2, 9)
        val t = machine.cancel(PieceStatus.VACUUM, run)
        assertEquals(PieceStatus.CANCELLED, t.status)
        assertEquals(at(2, 9), t.closedRun.endedAt)
        assertNull(t.nextRun)
    }

    @Test(expected = IllegalStateException::class)
    fun `R11 a finished piece cannot be cancelled`() {
        machine.cancel(PieceStatus.DONE, null)
    }

    @Test
    fun `R10 changing the duration recomputes from the run start`() {
        val run = machine.startVacuum(1, 7)
        clock.millis = at(4, 9)
        assertEquals(at(11, 18), machine.changeDuration(run, 10).plannedEndAt)
        assertEquals(at(2, 18), machine.changeDuration(run, 1).plannedEndAt)
    }

    @Test
    fun `R13 R23 postponing adds a day and counts it`() {
        val run = machine.startVacuum(1, 7)
        val once = machine.postponeOneDay(run)
        val twice = machine.postponeOneDay(once)
        assertEquals(at(9, 18), once.plannedEndAt)
        assertEquals(at(10, 18), twice.plannedEndAt)
        assertEquals(2, twice.postponedDays)
    }

    @Test
    fun `R15 overdue detection`() {
        val run = machine.startVacuum(1, 7)
        assertFalse(run.isOverdue(at(8, 17)))
        assertTrue(run.isOverdue(at(8, 18)))
        assertFalse(run.copy(endedAt = at(8, 19)).isOverdue(at(9, 9)))
    }
}
