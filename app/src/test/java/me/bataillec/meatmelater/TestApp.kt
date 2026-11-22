package me.bataillec.meatmelater

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import me.bataillec.meatmelater.data.AppDatabase
import me.bataillec.meatmelater.data.OneOpenRunGuard
import me.bataillec.meatmelater.domain.FakeClock

/** Shared fake clock of the Robolectric tests; reset by [AppTestBase]. */
object TestClock {
    const val START = 1_798_000_000_000L // 2026-12-23, UTC
    val clock = FakeClock(START)
}

/** Robolectric application: in-memory Room, fake clock, real Android alarm scheduler (shadowed). */
class TestApp : MeatMeLaterApp() {
    override val runStartupWork: Boolean = false

    override fun createContainer(): AppContainer =
        AppContainer(
            this,
            databaseFactory = { ctx: Context ->
                Room
                    .inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
                    .setDriver(AndroidSQLiteDriver())
                    .addCallback(OneOpenRunGuard)
                    .build()
            },
            clock = TestClock.clock,
        )
}
