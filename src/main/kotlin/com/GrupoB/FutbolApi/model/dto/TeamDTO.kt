package com.GrupoB.FutbolApi.model.dto

import com.GrupoB.FutbolApi.model.Team

class TeamDTO(
    val id: Long?,
    val name: String,
    val country: String,
    val players: List<PlayerDTO>
) {
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