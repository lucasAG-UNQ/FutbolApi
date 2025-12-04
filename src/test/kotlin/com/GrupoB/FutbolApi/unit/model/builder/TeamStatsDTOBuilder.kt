package com.grupob.futbolapi.unit.model.builder

import com.grupob.futbolapi.model.dto.PlayerDTO
import com.grupob.futbolapi.model.dto.TeamStatsDTO

class TeamStatsDTOBuilder {
    private var teamId: Long = 1L
    private var teamName: String = "Test Team"
    private var totalMatches: Int = 0
    private var wins: Int = 0
    private var draws: Int = 0
    private var losses: Int = 0
    private var winPercentage: Double = 0.0
    private var goalsFor: Int = 0
    private var goalsAgainst: Int = 0
    private var averageGoalsFor: Double = 0.0
    private var averageGoalsAgainst: Double = 0.0
    private var longestWinStreak: Int = 0
    private var founded: Int? = null
    private var venue: String? = null
    private var clubColors: String? = null
    private var mvp: PlayerDTO? = null

    fun withTeamId(teamId: Long): TeamStatsDTOBuilder {
        this.teamId = teamId
        return this
    }

    fun withTeamName(teamName: String): TeamStatsDTOBuilder {
        this.teamName = teamName
        return this
    }

    fun build(): TeamStatsDTO {
        return TeamStatsDTO(
            teamId = teamId,
            teamName = teamName,
            totalMatches = totalMatches,
            wins = wins,
            draws = draws,
            losses = losses,
            winPercentage = winPercentage,
            goalsFor = goalsFor,
            goalsAgainst = goalsAgainst,
            averageGoalsFor = averageGoalsFor,
            averageGoalsAgainst = averageGoalsAgainst,
            longestWinStreak = longestWinStreak,
            founded = founded,
            venue = venue,
            clubColors = clubColors,
            mvp = mvp
        )
    }
}
