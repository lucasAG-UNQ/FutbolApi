package com.grupob.futbolapi.model.dto

import com.grupob.futbolapi.model.Team

class SimpleTeamDTO(
    val teamID: Long,
    val teamName: String
) {
    companion object {
        fun fromModel(team: Team): SimpleTeamDTO {
            return SimpleTeamDTO(
                teamID = team.id!!,
                teamName = team.name
            )
        }
    }
}
