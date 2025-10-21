package com.grupob.futbolapi.model

import jakarta.persistence.*

@Entity
@Table(name = "teams")
class Team(
    @Id
    var id: Long? = null,

    @Column(unique = true, nullable = false)
    var name: String,

    @Column(nullable = false)
    var country: String,

    @OneToMany(mappedBy = "team", cascade = [CascadeType.ALL], orphanRemoval = true)
    var players: MutableList<Player> = mutableListOf()
)
