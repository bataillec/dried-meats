package fr.viandes.seches

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

open class ViandesApp : Application() {
    lateinit var container: AppContainer
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Overridden by the Robolectric test application to inject an in-memory database and a fake clock. */
    protected open fun createContainer(): AppContainer = AppContainer(this)

    /** False in tests, which seed and reconcile explicitly. */
    protected open val runStartupWork: Boolean = true

    override fun onCreate() {
        super.onCreate()
        container = createContainer()
        if (runStartupWork) {
            appScope.launch {
                container.seeder.seedIfNeeded()
                container.reminderPlanner.reconcile()
            }
        }
    }
}
