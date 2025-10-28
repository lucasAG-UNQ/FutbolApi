package com.grupob.futbolapi.services

import com.grupob.futbolapi.model.Team

interface ITeamService {
    fun getTeamWithPlayers(teamName: String): Team?
    fun getTeamWithPlayers(teamId: Long): Team?
    fun predictMatch(teamA: Long, teamB: Long): Team?
}