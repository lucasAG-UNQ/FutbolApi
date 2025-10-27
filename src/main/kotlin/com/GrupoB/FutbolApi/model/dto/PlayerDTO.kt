package com.grupob.futbolapi.model.dto

import com.grupob.futbolapi.model.Player

class PlayerDTO(
    val id: Long?,
    val name: String,
    val position: String?,
    val team: SimpleTeamDTO?,
    val tournament: String?,
    val season: String?,
    val apps: Int?,
    val goals: Int?,
    val assists: Int?,
    val rating: Double?,
    val minutes: Int?,
    val yellowCards: Int?,
    val redCards: Int?,
    val age: Int?
) {
    fun toModel(): Player {
        return Player(
            id = this.id,
            name = this.name,
            position = this.position,
            team = null, // The team is set in the service layer
            tournament = this.tournament,
            season = this.season,
            apps = this.apps,
            goals = this.goals,
            assists = this.assists,
            rating = this.rating,
            minutes = this.minutes,
            yellowCards = this.yellowCards,
            redCards = this.redCards,
            age = this.age
        )
    }

    companion object {
        fun fromModel(player: Player): PlayerDTO {
            return PlayerDTO(
                player.id,
                player.name,
                player.position,
                team = SimpleTeamDTO(player.team!!.id!!,player.team!!.name),
                player.tournament,
                player.season,
                player.apps,
                player.goals,
                player.assists,
                player.rating,
                player.minutes,
                player.yellowCards,
                player.redCards,
                player.age
            )
        }
    }
}