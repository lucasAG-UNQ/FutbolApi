package com.grupob.futbolapi.services

import com.grupob.futbolapi.model.Team
import com.grupob.futbolapi.model.dto.MatchDTO
import com.grupob.futbolapi.model.dto.SimpleTeamDTO

interface IWhoScoredScraperService {
    fun getTeam(teamID: Long): Team
    fun searchTeams(searchParam: String): List<SimpleTeamDTO>
    fun getNextTeamMatches(teamId: Long): List<MatchDTO>
}