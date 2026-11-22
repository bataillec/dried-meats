package me.bataillec.meatmelater.data

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "meat_type")
data class MeatTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val archived: Boolean = false,
)

/** g/kg values and the divisor/offset are stored as exact decimal text (BigDecimal.toPlainString). */
@Entity(
    tableName = "recipe",
    foreignKeys = [ForeignKey(entity = MeatTypeEntity::class, parentColumns = ["id"], childColumns = ["meat_type_id"])],
    indices = [Index("meat_type_id")],
)
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "meat_type_id") val meatTypeId: Long,
    val name: String,
    @ColumnInfo(name = "salt_g_per_kg") val saltGPerKg: String,
    @ColumnInfo(name = "sugar_g_per_kg") val sugarGPerKg: String,
    @ColumnInfo(name = "pepper_g_per_kg") val pepperGPerKg: String,
    @ColumnInfo(name = "fridge_seasoning_default") val fridgeSeasoningDefault: String?,
    @ColumnInfo(name = "vacuum_divisor") val vacuumDivisor: String,
    @ColumnInfo(name = "vacuum_offset_days") val vacuumOffsetDays: String,
    @ColumnInfo(name = "fridge_min_days") val fridgeMinDays: Int,
    val archived: Boolean = false,
)

@Entity(
    tableName = "piece",
    foreignKeys = [ForeignKey(entity = RecipeEntity::class, parentColumns = ["id"], childColumns = ["recipe_id"])],
    indices = [Index("recipe_id")],
)
data class PieceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    @ColumnInfo(name = "recipe_id") val recipeId: Long,
    @ColumnInfo(name = "weight_g") val weightG: Int,
    @ColumnInfo(name = "diameter_cm") val diameterCm: String,
    @ColumnInfo(name = "salt_g") val saltG: String,
    @ColumnInfo(name = "sugar_g") val sugarG: String,
    @ColumnInfo(name = "pepper_g") val pepperG: String,
    @ColumnInfo(name = "fridge_seasoning") val fridgeSeasoning: String?,
    val status: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "stage_run",
    foreignKeys = [
        ForeignKey(
            entity = PieceEntity::class,
            parentColumns = ["id"],
            childColumns = ["piece_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("piece_id")],
)
data class StageRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "piece_id") val pieceId: Long,
    val stage: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "planned_end_at") val plannedEndAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long?,
    @ColumnInfo(name = "postponed_days") val postponedDays: Int,
    val note: String?,
)
