package com.grupob.futbolapi.model.builder

import com.grupob.futbolapi.model.Player
import com.grupob.futbolapi.model.Team

class TeamBuilder {
    private var id: Long? = null
    private var name: String = "A Team"
    private var country: String = "A Country"
    private var players: MutableList<Player> = mutableListOf()

    fun withId(id: Long?): TeamBuilder {
        this.id = id
        return this
    }

    fun withName(name: String): TeamBuilder {
        this.name = name
        return this
    }

    fun withCountry(country: String): TeamBuilder {
        this.country = country
        return this
    }

    fun withPlayer(player: Player): TeamBuilder {
        this.players.add(player)
        return this
    }

    fun withPlayers(players: List<Player>): TeamBuilder {
        this.players.addAll(players)
        return this
    }

    fun build(): Team {
        val team = Team(
            id = this.id,
            name = this.name,
            country = this.country
        )
        team.players = this.players
        // Ensure bidirectional relationship is set
        this.players.forEach { it.team = team }
        return team
    }
}