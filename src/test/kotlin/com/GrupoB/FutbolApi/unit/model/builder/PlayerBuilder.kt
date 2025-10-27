package com.grupob.futbolapi.unit.model.builder

import com.grupob.futbolapi.model.Player
import com.grupob.futbolapi.model.Team

class PlayerBuilder {
    private var id: Long? = null
    private var name: String = "A Player"
    private var position: String? = "A Position"
    private var team: Team? = null
    private var tournament: String? = "A Tournament"
    private var season: String? = "A Season"
    private var apps: Int? = 0
    private var goals: Int? = 0
    private var assists: Int? = 0
    private var rating: Double? = 0.0
    private var minutes: Int? = 0
    private var yellowCards: Int? = 0
    private var redCards: Int? = 0
    private var age: Int? = 20

    fun withId(id: Long?): PlayerBuilder {
        this.id = id
        return this
    }

    fun withName(name: String): PlayerBuilder {
        this.name = name
        return this
    }

    fun withPosition(position: String?): PlayerBuilder {
        this.position = position
        return this
    }

    fun withTeam(team: Team?): PlayerBuilder {
        this.team = team
        return this
    }

    fun withTournament(tournament: String?): PlayerBuilder {
        this.tournament = tournament
        return this
    }

    fun withSeason(season: String?): PlayerBuilder {
        this.season = season
        return this
    }

    fun withApps(apps: Int?): PlayerBuilder {
        this.apps = apps
        return this
    }

    fun withGoals(goals: Int?): PlayerBuilder {
        this.goals = goals
        return this
    }

    fun withAssists(assists: Int?): PlayerBuilder {
        this.assists = assists
        return this
    }

    fun withRating(rating: Double?): PlayerBuilder {
        this.rating = rating
        return this
    }

    fun withMinutes(minutes: Int?): PlayerBuilder {
        this.minutes = minutes
        return this
    }

    fun withYellowCards(yellowCards: Int?): PlayerBuilder {
        this.yellowCards = yellowCards
        return this
    }

    fun withRedCards(redCards: Int?): PlayerBuilder {
        this.redCards = redCards
        return this
    }

    fun withAge(age: Int?): PlayerBuilder {
        this.age = age
        return this
    }

    fun build(): Player {
        return Player(
            id = this.id,
            name = this.name,
            position = this.position,
            team = this.team,
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
}