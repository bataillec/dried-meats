package fr.viandes.seches.domain

/** R1: beef, pork and duck are pre-seeded once (guarded by a persisted flag, so deleting them sticks). */
class Seeder(
    private val settings: SettingsStore,
    private val meatTypes: MeatTypeStore,
) {
    suspend fun seedIfNeeded() {
        if (settings.current().seeded) return
        if (meatTypes.count() == 0) {
            SEED_NAMES.forEach { meatTypes.save(MeatType(0, it)) }
        }
        settings.update { it.copy(seeded = true) }
    }

    companion object {
        val SEED_NAMES = listOf("Bœuf", "Porc", "Canard")
    }
}
