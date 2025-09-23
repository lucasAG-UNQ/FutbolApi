package com.grupob.futbolapi.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class UserTest {

    @Test
    fun `User can be created with valid data`() {
        val user = User(id = 1L, username = "testuser", password_hash = "hashedpassword")
        assertEquals(1L, user.id)
        assertEquals("testuser", user.username)
        assertEquals("hashedpassword", user.password_hash)
    }
}