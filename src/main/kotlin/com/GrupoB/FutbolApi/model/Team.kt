package com.grupob.futbolapi.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "teams")
class Team(
    @Id
    var id: Long? = null,

    @Column(unique = true, nullable = false)
    var name: String,

    @Column(nullable = false)
    var country: String,

    @OneToMany(mappedBy = "team", cascade = [CascadeType.ALL], orphanRemoval = false)
    var players: MutableList<Player> = mutableListOf(),

    var lastUpdated: LocalDateTime = LocalDateTime.now(),

    var lastUpdatedMatches: LocalDateTime? = null,

    var footballDataId: Long? = null
)
