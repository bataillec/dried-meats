package fr.viandes.seches

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import fr.viandes.seches.data.AppDatabase
import fr.viandes.seches.data.OneOpenRunGuard
import fr.viandes.seches.domain.FakeClock

/** Shared fake clock of the Robolectric tests; reset by [AppTestBase]. */
object TestClock {
    const val START = 1_798_000_000_000L // 2026-12-23, UTC
    val clock = FakeClock(START)
}

/** Robolectric application: in-memory Room, fake clock, real Android alarm scheduler (shadowed). */
class TestApp : ViandesApp() {
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
