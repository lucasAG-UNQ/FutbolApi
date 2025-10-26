package com.grupob.futbolapi.services

import com.grupob.futbolapi.model.Team
import org.springframework.http.ResponseEntity

interface ITeamService {
    fun getTeam(teamName: String): Team?
    fun getTeamWithPlayers(teamName: String): Team?
    fun getTeamWithPlayers(teamId: Long): Team?
    fun predictMatch(teamA: Long, teamB: Long): Team?
}