package com.GrupoB.FutbolApi.model.dto

import com.GrupoB.FutbolApi.model.Match
import java.time.LocalDate

class MatchDTO(
    val homeTeam: TeamDTO,
    val awayTeam: TeamDTO,
    val date: LocalDate
) {
    companion object {
        fun fromModel(match: Match): MatchDTO {
            return MatchDTO(
                homeTeam = TeamDTO.fromModel(match.homeTeam),
                awayTeam = TeamDTO.fromModel(match.awayTeam),
                date = match.date
            )
        }
    }
}