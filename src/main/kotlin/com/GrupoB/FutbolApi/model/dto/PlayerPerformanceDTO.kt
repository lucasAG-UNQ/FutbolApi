package com.grupob.futbolapi.model.dto

import com.grupob.futbolapi.model.Player

data class PlayerPerformanceDTO(
    val name: String,
    val position: String?,
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
    companion object {
        fun fromModel(player: Player): PlayerPerformanceDTO {
            return PlayerPerformanceDTO(
                name = player.name,
                position = player.position,
                tournament = player.tournament,
                season = player.season,
                apps = player.apps,
                goals = player.goals,
                assists = player.assists,
                rating = player.rating,
                minutes = player.minutes,
                yellowCards = player.yellowCards,
                redCards = player.redCards,
                age = player.age
            )
        }
    }
}
