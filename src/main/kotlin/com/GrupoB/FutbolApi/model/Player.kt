package com.grupob.futbolapi.model

import jakarta.persistence.*

@Entity
@Table(name = "players")
class Player(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var name: String,

    var position: String?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    var team: Team? = null,

    var tournament: String?,
    var season: String?,
    var apps: Int?,
    var goals: Int?,
    var assists: Int?,
    var rating: Double?,
    var minutes: Int?,
    var yellowCards: Int?,
    var redCards: Int?,
    var age: Int?,
)