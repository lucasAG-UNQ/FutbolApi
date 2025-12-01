package com.grupob.futbolapi.model.dto

import com.grupob.futbolapi.model.Match
import java.time.LocalDate

class MatchDTO(
    val id:Long?,
    val homeTeam: SimpleTeamDTO,
    val awayTeam: SimpleTeamDTO,
    val date: LocalDate,
    val tournament: String?,
    val homeScore: Int?,
    val awayScore: Int?
) {
    fun toModel(): Match {
        return Match(
            id = this.id,
            homeTeam = null, //set in the service layer
            awayTeam = null,
            date = this.date,
            tournament = tournament,
            homeScore = homeScore,
            awayScore = awayScore
        )
    }

    companion object {
        fun fromModel(match: Match): MatchDTO {
            return MatchDTO(
                id = match.id,
                homeTeam = SimpleTeamDTO(match.homeTeam!!.id!!, match.homeTeam!!.name),
                awayTeam = SimpleTeamDTO(match.awayTeam!!.id!!, match.awayTeam!!.name),
                date = match.date,
                tournament = match.tournament,
                homeScore = match.homeScore,
                awayScore = match.awayScore
            )
        }
    }
}