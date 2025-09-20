package com.grupob.futbolapi.services

import com.grupob.futbolapi.model.Team
import com.grupob.futbolapi.repositories.TeamRepository
import org.springframework.stereotype.Service

@Service
class TeamService(private val teamRepository: TeamRepository) {
    fun getTeam(teamName: String): Team? {
        return teamRepository.findByNameWithPlayers(teamName)
    }
    fun getTeamWithPlayers(teamName: String): Team? {
        return teamRepository.findByNameWithPlayers(teamName)
    }
}