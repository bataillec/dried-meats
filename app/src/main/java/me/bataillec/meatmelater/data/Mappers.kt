package me.bataillec.meatmelater.data

import me.bataillec.meatmelater.domain.Doses
import me.bataillec.meatmelater.domain.MeatType
import me.bataillec.meatmelater.domain.Piece
import me.bataillec.meatmelater.domain.PieceStatus
import me.bataillec.meatmelater.domain.PieceSummary
import me.bataillec.meatmelater.domain.Recipe
import me.bataillec.meatmelater.domain.Stage
import me.bataillec.meatmelater.domain.StageRun
import java.math.BigDecimal

internal fun MeatTypeEntity.toDomain() = MeatType(id, name, archived)

internal fun MeatType.toEntity() = MeatTypeEntity(id, name, archived)

internal fun RecipeEntity.toDomain() =
    Recipe(
        id = id,
        name = name,
        saltGPerKg = BigDecimal(saltGPerKg),
        sugarGPerKg = BigDecimal(sugarGPerKg),
        pepperGPerKg = BigDecimal(pepperGPerKg),
        fridgeSeasoningDefault = fridgeSeasoningDefault,
        vacuumDivisor = BigDecimal(vacuumDivisor),
        vacuumOffsetDays = BigDecimal(vacuumOffsetDays),
        fridgeMinDays = fridgeMinDays,
        archived = archived,
    )

internal fun Recipe.toEntity() =
    RecipeEntity(
        id = id,
        name = name,
        saltGPerKg = saltGPerKg.toPlainString(),
        sugarGPerKg = sugarGPerKg.toPlainString(),
        pepperGPerKg = pepperGPerKg.toPlainString(),
        fridgeSeasoningDefault = fridgeSeasoningDefault,
        vacuumDivisor = vacuumDivisor.toPlainString(),
        vacuumOffsetDays = vacuumOffsetDays.toPlainString(),
        fridgeMinDays = fridgeMinDays,
        archived = archived,
    )

internal fun PieceRow.toPiece() =
    Piece(
        id = id,
        label = label,
        recipeId = recipeId,
        meatTypeId = meatTypeId,
        weightG = weightG,
        diameterCm = BigDecimal(diameterCm),
        doses = Doses(BigDecimal(saltG), BigDecimal(sugarG), BigDecimal(pepperG)),
        fridgeSeasoning = fridgeSeasoning,
        status = PieceStatus.valueOf(status),
        createdAt = createdAt,
    )

internal fun PieceRow.toSummary(openRun: StageRun?) = PieceSummary(toPiece(), recipeName, meatTypeName, openRun)

internal fun PieceEntity.toDomain() =
    Piece(
        id = id,
        label = label,
        recipeId = recipeId,
        meatTypeId = meatTypeId,
        weightG = weightG,
        diameterCm = BigDecimal(diameterCm),
        doses = Doses(BigDecimal(saltG), BigDecimal(sugarG), BigDecimal(pepperG)),
        fridgeSeasoning = fridgeSeasoning,
        status = PieceStatus.valueOf(status),
        createdAt = createdAt,
    )

internal fun Piece.toEntity() =
    PieceEntity(
        id = id,
        label = label,
        recipeId = recipeId,
        meatTypeId = meatTypeId,
        weightG = weightG,
        diameterCm = diameterCm.toPlainString(),
        saltG = doses.saltG.toPlainString(),
        sugarG = doses.sugarG.toPlainString(),
        pepperG = doses.pepperG.toPlainString(),
        fridgeSeasoning = fridgeSeasoning,
        status = status.name,
        createdAt = createdAt,
    )

internal fun StageRunEntity.toDomain() =
    StageRun(id, pieceId, Stage.valueOf(stage), startedAt, plannedEndAt, endedAt, postponedDays, note)

internal fun StageRun.toEntity() =
    StageRunEntity(id, pieceId, stage.name, startedAt, plannedEndAt, endedAt, postponedDays, note)
