package com.grupob.futbolapi.model.dto

import com.grupob.futbolapi.model.Team

class TeamDTO(
    val id: Long?,
    val name: String,
    val country: String,
    val players: List<PlayerDTO>
) {
    fun toModel(): Team {
        val team = Team(
            id = this.id,
            name = this.name,
            country = this.country
        )
        val playerModels = this.players.map { playerDto ->
            val player = playerDto.toModel()
            player.team = team // Set the back-reference
            player
        }
        team.players = playerModels.toMutableList()
        return team
    }

    companion object {
        fun fromModel(team: Team): TeamDTO {
            return TeamDTO(
                team.id,
                team.name,
                team.country,
                team.players.map { PlayerDTO.fromModel(it) }
            )
        }
    }
}