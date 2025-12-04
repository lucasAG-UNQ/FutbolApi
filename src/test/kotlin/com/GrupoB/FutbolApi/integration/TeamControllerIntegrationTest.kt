package com.grupob.futbolapi.integration

import com.grupob.futbolapi.model.Team
import com.grupob.futbolapi.repositories.TeamRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Roll back database changes after each test
@DisplayName("TeamController Integration Tests")
class TeamControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var teamRepository: TeamRepository

    private lateinit var savedTeam: Team


    @BeforeEach
    fun setUp() {
        // Clear the repository to ensure a clean state for each test
        teamRepository.deleteAll()

        // Arrange: Save a team directly to the H2 database
        val teamToSave = Team(id = 1,name = "Integration Test Team", country = "Testland")
        savedTeam = teamRepository.save(teamToSave)
    }

    @Test
    @WithMockUser // Run this test with a mock authenticated user
    fun getShouldReturn200OKAndTheCorrectTeamDataFromTheDatabase() {
        // Act & Assert
        mockMvc.perform(get("/api/teams/{teamID}", savedTeam.id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(savedTeam.id!!))
            .andExpect(jsonPath("$.name").value(savedTeam.name))
            .andExpect(jsonPath("$.country").value(savedTeam.country))
            .andExpect(jsonPath("$.players").isEmpty())
    }

    @Test
    @WithMockUser // Also secure this test endpoint
    fun getShouldReturn404NotFoundForANonExistentTeamID() {
        // Arrange
        val nonExistentId = -1L

        // Act & Assert
        mockMvc.perform(get("/api/teams/{teamID}", nonExistentId))
            .andExpect(status().isNotFound)
    }

    @Test
    fun getShouldReturn401UnauthorizedWithoutAUser() {
        // Act & Assert
        // This test verifies that the endpoint is indeed protected
        mockMvc.perform(get("/api/teams/{teamID}", savedTeam.id))
            .andExpect(status().isUnauthorized)
    }
}