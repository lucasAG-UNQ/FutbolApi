package com.grupob.futbolapi.model.dto

data class TeamStatsDTO(
    val teamId: Long,
    val teamName: String,
    val totalMatches: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val winPercentage: Double,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val averageGoalsFor: Double,
    val averageGoalsAgainst: Double,
    val longestWinStreak: Int,
    val founded: Int?,
    val venue: String?,
    val clubColors: String?,
    val mvp: PlayerDTO?
)
