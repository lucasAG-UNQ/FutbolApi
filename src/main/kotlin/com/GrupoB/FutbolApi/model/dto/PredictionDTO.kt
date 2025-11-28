package com.grupob.futbolapi.model.dto

data class PredictionDTO(
    val homeTeam: SimpleTeamDTO,
    val awayTeam: SimpleTeamDTO,
    val winProbabilityHomeTeam: Double,
    val winProbabilityAwayTeam: Double,
    val drawProbability: Double,
    val predictedWinner: SimpleTeamDTO?
)