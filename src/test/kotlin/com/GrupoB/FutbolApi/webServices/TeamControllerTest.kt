package com.grupob.futbolapi.webServices

import com.fasterxml.jackson.databind.ObjectMapper
import com.grupob.futbolapi.model.builder.PlayerBuilder
import com.grupob.futbolapi.model.builder.TeamBuilder
import com.grupob.futbolapi.services.ITeamService
import com.grupob.futbolapi.services.IWhoScoredScraperService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders


@ExtendWith(MockitoExtension::class)
@DisplayName("TeamController Unit Tests")
class TeamControllerTest {


    // Using @MockitoBean as requested to mock the service layer
    @Mock
    private lateinit var teamService: ITeamService

    @Mock
    private lateinit var scraperService: IWhoScoredScraperService


    @InjectMocks
    lateinit var teamController: TeamController

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(teamController).build()
    }

    @Nested
    @DisplayName("GET /api/teams/{teamID}")
    inner class GetTeamById {

        @Test
        fun `should return 200 OK with team data when team is found`() {
            // Arrange
            val teamId = 10L
            val player = PlayerBuilder().withId(1L).withName("Jude Bellingham").withPosition("Midfielder").build()
            val team = TeamBuilder().withId(teamId).withName("Real Madrid").withPlayer(player).build()

            `when`(teamService.getTeamWithPlayers(teamId)).thenReturn(team)

            // Act & Assert
            mockMvc.perform(get("/api/teams/{teamID}", teamId))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(teamId))
                .andExpect(jsonPath("$.name").value("Real Madrid"))
                .andExpect(jsonPath("$.players[0].name").value("Jude Bellingham"))
        }

        @Test
        fun `should return 404 Not Found when team is not found`() {
            // Arrange
            val teamId = 99L
            `when`(teamService.getTeamWithPlayers(teamId)).thenReturn(null)

            // Act & Assert
            mockMvc.perform(get("/api/teams/{teamID}", teamId))
                .andExpect(status().isNotFound)
        }
    }
}