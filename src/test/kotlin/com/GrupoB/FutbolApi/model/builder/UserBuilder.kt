package com.grupob.futbolapi.model.builder

import com.grupob.futbolapi.model.User

class UserBuilder {
    private var id: Long? = null
    private var username: String = "aUsername"
    private var password_hash: String = "aPassword"

    fun withId(id: Long?): UserBuilder {
        this.id = id
        return this
    }

    fun withUsername(username: String): UserBuilder {
        this.username = username
        return this
    }

    fun withPassword(password: String): UserBuilder {
        this.password_hash = password
        return this
    }

    fun build(): User {
        return User(
            id = this.id,
            username = this.username,
            password_hash = this.password_hash
        )
    }
}