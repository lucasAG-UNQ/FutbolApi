package com.grupob.futbolapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.grupob.futbolapi.model.User
import com.grupob.futbolapi.model.dto.LoginRequest
import com.grupob.futbolapi.model.dto.RegisterRequest
import com.grupob.futbolapi.repositories.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("AuthWebService Integration Tests")
class AuthWebServiceIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        userRepository.deleteAll()
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    inner class RegisterUser {

        @Test
        fun shouldRegisterUserSuccessfully() {
            // Arrange
            val registerRequest = RegisterRequest("newUser", "password123")

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk)
                .andExpect(content().string("User registered successfully"))

            // Verify user is in the database with an encoded password
            val user = userRepository.findByUsername("newUser")
            assertTrue(user != null)
            assertTrue(passwordEncoder.matches("password123", user!!.passwordHash))
        }

        @Test
        fun shouldReturnBadRequestWhenUsernameIsAlreadyTaken() {
            // Arrange: Save a user first
            userRepository.save(User(username = "existingUser", passwordHash = passwordEncoder.encode("anypass")))
            val registerRequest = RegisterRequest("existingUser", "password123")

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest)
                .andExpect(content().string("Username is already taken!"))
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    inner class LoginUser {

        @Test
        fun shouldReturnJwtWhenLoginIsSuccessful() {
            // Arrange: Register a user first
            val username = "testuser"
            val password = "password"
            userRepository.save(User(username = username, passwordHash = passwordEncoder.encode(password)))
            val loginRequest = LoginRequest(username, password)

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.token").isString)
                .andExpect(jsonPath("$.token").isNotEmpty)
        }

        @Test
        fun shouldReturn401UnauthorizedWhenCredentialsAreInvalid() {
            // Arrange
            val loginRequest = LoginRequest("wronguser", "wrongpassword")

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized)
        }
    }
}