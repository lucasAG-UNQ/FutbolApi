package com.grupob.futbolapi.services

import com.grupob.futbolapi.model.Player
import com.grupob.futbolapi.model.dto.MatchDTO
import com.grupob.futbolapi.model.dto.SimpleTeamDTO
import com.grupob.futbolapi.model.dto.TeamDTO

interface IWhoScoredScraperService {
    fun getTeam(teamID: Long): TeamDTO
    fun searchTeams(searchParam: String): List<SimpleTeamDTO>
    fun getNextTeamMatches(teamId: Long): List<MatchDTO>
    fun getPlayerById(playerId: Long): Player
}
