package com.grupob.futbolapi.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "requests")
class Request(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val endpoint: String,

    val timestamp: LocalDateTime,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User
)
