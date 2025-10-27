package com.grupob.futbolapi.unit.model.builder

import com.grupob.futbolapi.model.dto.PlayerDTO
import com.grupob.futbolapi.model.dto.TeamDTO

class TeamDTOBuilder {
    private var id: Long? = 1L
    private var name: String = "A Team Name"
    private var country: String = "A Country"
    private var players: List<PlayerDTO> = emptyList()

    fun withId(id: Long?): TeamDTOBuilder {
        this.id = id
        return this
    }

    fun withName(name: String): TeamDTOBuilder {
        this.name = name
        return this
    }

    fun withCountry(country: String): TeamDTOBuilder {
        this.country = country
        return this
    }

    fun withPlayers(players: List<PlayerDTO>): TeamDTOBuilder {
        this.players = players
        return this
    }

    fun build(): TeamDTO {
        return TeamDTO(
            id = this.id,
            name = this.name,
            country = this.country,
            players = this.players
        )
    }
}