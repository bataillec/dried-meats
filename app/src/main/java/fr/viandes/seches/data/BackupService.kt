package fr.viandes.seches.data

import androidx.room3.withWriteTransaction
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val meatTypes: List<BackupMeatType>,
    val recipes: List<BackupRecipe>,
    val pieces: List<BackupPiece>,
    val stageRuns: List<BackupRun>,
) {
    companion object {
        const val FORMAT = "viandes-seches-backup"
        const val VERSION = 1
    }
}

@Serializable
data class BackupMeatType(
    val id: Long,
    val name: String,
    val archived: Boolean,
)

@Serializable
data class BackupRecipe(
    val id: Long,
    val meatTypeId: Long,
    val name: String,
    val saltGPerKg: String,
    val sugarGPerKg: String,
    val pepperGPerKg: String,
    val fridgeSeasoningDefault: String?,
    val vacuumDivisor: String,
    val vacuumOffsetDays: String,
    val fridgeMinDays: Int,
    val archived: Boolean,
)

@Serializable
data class BackupPiece(
    val id: Long,
    val label: String,
    val recipeId: Long,
    val weightG: Int,
    val diameterCm: String,
    val saltG: String,
    val sugarG: String,
    val pepperG: String,
    val fridgeSeasoning: String?,
    val status: String,
    val createdAt: Long,
)

@Serializable
data class BackupRun(
    val id: Long,
    val pieceId: Long,
    val stage: String,
    val startedAt: Long,
    val plannedEndAt: Long,
    val endedAt: Long?,
    val postponedDays: Int,
    val note: String?,
)

class BackupFormatException(
    message: String,
) : Exception(message)

/** R21: full JSON export / import of the local database (backup, new phone). */
class BackupService(
    private val db: AppDatabase,
) {
    private val json = Json { prettyPrint = true }

    suspend fun export(): String {
        val file =
            db.withWriteTransaction {
                BackupFile(
                    meatTypes = db.meatTypeDao().all().map { BackupMeatType(it.id, it.name, it.archived) },
                    recipes =
                        db.recipeDao().all().map {
                            BackupRecipe(
                                it.id,
                                it.meatTypeId,
                                it.name,
                                it.saltGPerKg,
                                it.sugarGPerKg,
                                it.pepperGPerKg,
                                it.fridgeSeasoningDefault,
                                it.vacuumDivisor,
                                it.vacuumOffsetDays,
                                it.fridgeMinDays,
                                it.archived,
                            )
                        },
                    pieces =
                        db.pieceDao().all().map {
                            BackupPiece(
                                it.id,
                                it.label,
                                it.recipeId,
                                it.weightG,
                                it.diameterCm,
                                it.saltG,
                                it.sugarG,
                                it.pepperG,
                                it.fridgeSeasoning,
                                it.status,
                                it.createdAt,
                            )
                        },
                    stageRuns =
                        db.stageRunDao().all().map {
                            BackupRun(
                                it.id,
                                it.pieceId,
                                it.stage,
                                it.startedAt,
                                it.plannedEndAt,
                                it.endedAt,
                                it.postponedDays,
                                it.note,
                            )
                        },
                )
            }
        return json.encodeToString(BackupFile.serializer(), file)
    }

    /**
     * Replaces ALL data with the file's content, atomically: any invalid row rolls everything back.
     * @throws BackupFormatException for files that are not a valid backup of this app.
     */
    suspend fun import(text: String) {
        val file =
            try {
                json.decodeFromString(BackupFile.serializer(), text)
            } catch (e: kotlinx.serialization.SerializationException) {
                throw BackupFormatException("Fichier illisible: ${e.message}")
            } catch (e: IllegalArgumentException) {
                throw BackupFormatException("Fichier illisible: ${e.message}")
            }
        if (file.format != BackupFile.FORMAT || file.version != BackupFile.VERSION) {
            throw BackupFormatException("Format ou version non pris en charge")
        }
        validate(file)
        db.withWriteTransaction {
            db.stageRunDao().deleteAll()
            db.pieceDao().deleteAll()
            db.recipeDao().deleteAll()
            db.meatTypeDao().deleteAll()
            insertAll(file)
        }
    }

    private suspend fun insertAll(file: BackupFile) {
        file.meatTypes.forEach { db.meatTypeDao().insert(MeatTypeEntity(it.id, it.name, it.archived)) }
        file.recipes.forEach {
            db.recipeDao().insert(
                RecipeEntity(
                    it.id,
                    it.meatTypeId,
                    it.name,
                    it.saltGPerKg,
                    it.sugarGPerKg,
                    it.pepperGPerKg,
                    it.fridgeSeasoningDefault,
                    it.vacuumDivisor,
                    it.vacuumOffsetDays,
                    it.fridgeMinDays,
                    it.archived,
                ),
            )
        }
        file.pieces.forEach {
            db.pieceDao().insert(
                PieceEntity(
                    it.id,
                    it.label,
                    it.recipeId,
                    it.weightG,
                    it.diameterCm,
                    it.saltG,
                    it.sugarG,
                    it.pepperG,
                    it.fridgeSeasoning,
                    it.status,
                    it.createdAt,
                ),
            )
        }
        file.stageRuns.forEach {
            db.stageRunDao().insert(
                StageRunEntity(
                    it.id,
                    it.pieceId,
                    it.stage,
                    it.startedAt,
                    it.plannedEndAt,
                    it.endedAt,
                    it.postponedDays,
                    it.note,
                ),
            )
        }
    }

    private fun validate(file: BackupFile) {
        val meatIds = file.meatTypes.map { it.id }.toSet()
        val recipeIds = file.recipes.map { it.id }.toSet()
        val pieceIds = file.pieces.map { it.id }.toSet()
        try {
            require(file.recipes.all { it.meatTypeId in meatIds })
            require(file.pieces.all { it.recipeId in recipeIds })
            require(file.stageRuns.all { it.pieceId in pieceIds })
            file.recipes.forEach {
                java.math.BigDecimal(it.saltGPerKg)
                java.math.BigDecimal(it.sugarGPerKg)
                java.math.BigDecimal(it.pepperGPerKg)
                java.math.BigDecimal(it.vacuumDivisor)
                java.math.BigDecimal(it.vacuumOffsetDays)
            }
            file.pieces.forEach {
                java.math.BigDecimal(it.diameterCm)
                java.math.BigDecimal(it.saltG)
                java.math.BigDecimal(it.sugarG)
                java.math.BigDecimal(it.pepperG)
                fr.viandes.seches.domain.PieceStatus
                    .valueOf(it.status)
            }
            file.stageRuns.forEach {
                fr.viandes.seches.domain.Stage
                    .valueOf(it.stage)
            }
            // At most one open run per piece (design.md section 3).
            require(
                file.stageRuns
                    .filter { it.endedAt == null }
                    .groupBy { it.pieceId }
                    .values
                    .all { it.size == 1 },
            )
        } catch (e: IllegalArgumentException) {
            throw BackupFormatException("Données incohérentes dans le fichier")
        }
    }
}
