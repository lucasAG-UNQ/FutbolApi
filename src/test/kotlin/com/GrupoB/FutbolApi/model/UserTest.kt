package com.grupob.futbolapi.model

import com.grupob.futbolapi.model.builder.UserBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("User Model Tests")
class UserTest {

    @Test
    fun `a User can be created with valid data`() {
        val user = UserBuilder()
            .withId(1L)
            .withUsername("testuser")
            .withPassword("hashedpassword")
            .build()

        assertEquals(1L, user.id)
        assertEquals("testuser", user.username)
        assertEquals("hashedpassword", user.password_hash)
    }

    @Test
    fun `a User can be created with a null id`() {
        val newUser = UserBuilder()
            .withId(null)
            .withUsername("newuser")
            .withPassword("newpass")
            .build()

        assertNull(newUser.id, "A new user should have a null ID before being persisted")
        assertEquals("newuser", newUser.username)
        assertEquals("newpass", newUser.password_hash)
    }

    @Nested
    @DisplayName("Equality and HashCode Tests")
    inner class EqualityTests {

        @Test
        fun `two User instances with different ids are not equal`() {
            val user1 = UserBuilder().withId(1L).build()
            val user2 = UserBuilder().withId(2L).build()

            assertNotEquals(user1, user2, "Users with different IDs should not be equal")
        }

        @Test
        fun `a User is not equal to an object of a different type`() {
            val user = UserBuilder().withId(1L).build()
            val otherObject = Any()

            assertNotEquals(user, otherObject, "User should not be equal to a different type")
        }

        @Test
        fun `a User is not equal to null`() {
            val user = UserBuilder().withId(1L).build()

            assertNotEquals(null, user, "User should not be equal to null")
        }

        @Test
        fun `a User is equal to itself`() {
            val user = UserBuilder().withId(1L).build()

            assertEquals(user, user, "User should be equal to itself")
        }
    }
}