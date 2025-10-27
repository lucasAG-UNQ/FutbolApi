package com.grupob.futbolapi.unit.model.builder

import com.grupob.futbolapi.model.Match
import com.grupob.futbolapi.model.Team
import java.time.LocalDate

class MatchBuilder {
    private var id: Long? = null
    private var homeTeam: Team = TeamBuilder().withId(1L).withName("Home Team").build()
    private var awayTeam: Team = TeamBuilder().withId(2L).withName("Away Team").build()
    private var date: LocalDate = LocalDate.now()
    private var tournament: String? = "A Tournament"

    fun withId(id: Long?): MatchBuilder {
        this.id = id
        return this
    }

    fun withHomeTeam(team: Team): MatchBuilder {
        this.homeTeam = team
        return this
    }

    fun withAwayTeam(team: Team): MatchBuilder {
        this.awayTeam = team
        return this
    }

    fun withDate(date: LocalDate): MatchBuilder {
        this.date = date
        return this
    }

    fun withTournament(tournament: String?): MatchBuilder {
        this.tournament = tournament
        return this
    }

    fun build(): Match {
        return Match(
            id = this.id,
            homeTeam = this.homeTeam,
            awayTeam = this.awayTeam,
            date = this.date,
            tournament = this.tournament
        )
    }
}