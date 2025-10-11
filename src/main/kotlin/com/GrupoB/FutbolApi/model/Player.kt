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

    @Column(nullable = false)
    var position: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    var team: Team? = null,

    val whoscoredId: Long,
    val tournament: String,
    val season: String,
    val apps: Int,
    val goals: Int,
    val assists: Int,
    val rating: Double,
    val minutes: Int,
    val yellowCards: Int,
    val redCards: Int,
    val age: Int
)