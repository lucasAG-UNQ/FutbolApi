package com.grupob.futbolapi.services

import com.grupob.futbolapi.model.dto.MatchDTO
import com.grupob.futbolapi.model.dto.TeamComparisonDTO
import com.grupob.futbolapi.model.dto.TeamStatsDTO

interface IMatchService {
    fun getNextMatches(teamID: Long): List<MatchDTO>
    fun getFinishedMatches(teamID: Long): List<MatchDTO>
    fun getTeamStats(teamID: Long): TeamStatsDTO
    fun compareTeams(teamAId: Long, teamBId: Long): TeamComparisonDTO
}