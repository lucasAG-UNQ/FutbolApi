package com.grupob.futbolapi.unit.webServices

import com.grupob.futbolapi.model.Team
import com.grupob.futbolapi.model.dto.MatchDTO
import com.grupob.futbolapi.model.dto.PredictionDTO
import com.grupob.futbolapi.model.dto.SimpleTeamDTO
import com.grupob.futbolapi.services.IMatchService
import com.grupob.futbolapi.unit.model.builder.PlayerBuilder
import com.grupob.futbolapi.unit.model.builder.TeamBuilder
import com.grupob.futbolapi.services.ITeamService
import com.grupob.futbolapi.services.IWhoScoredScraperService
import com.grupob.futbolapi.webServices.TeamController
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate

private const val teamEndpoint = "/api/teams"

@ExtendWith(MockitoExtension::class)
@DisplayName("TeamController Unit Tests")
class TeamControllerTest {

    @Mock
    private lateinit var teamService: ITeamService

    @Mock
    private lateinit var scraperService: IWhoScoredScraperService

    @Mock
    private lateinit var matchService: IMatchService

    @InjectMocks
    private lateinit var teamController: TeamController

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(teamController).build()
    }

    @Nested
    @DisplayName("GET /api/teams/{teamID}")
    inner class GetTeamById {

        private var teamId: Long = 10L
        private lateinit var team: Team

        @BeforeEach
        fun setUp() {
            val player = PlayerBuilder().withId(1L).withName("Jude Bellingham").withPosition("Midfielder").build()
            team = TeamBuilder().withId(teamId).withName("Real Madrid").withPlayer(player).build()
        }

        @Test
        fun shouldReturn200OKWithTeamDataWhenTeamIsFound() {
            `when`(teamService.getTeamWithPlayers(teamId)).thenReturn(team)

            mockMvc.perform(get("$teamEndpoint/{teamID}", teamId))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(teamId))
                .andExpect(jsonPath("$.name").value("Real Madrid"))
                .andExpect(jsonPath("$.players[0].name").value("Jude Bellingham"))
        }

        @Test
        fun shouldReturn404NotFoundWhenTeamIsNotFound() {
            `when`(teamService.getTeamWithPlayers(teamId)).thenThrow(com.grupob.futbolapi.exceptions.TeamNotFoundException("Team not found"))

            mockMvc.perform(get("$teamEndpoint/{teamID}", teamId))
                .andExpect(status().isNotFound)
        }
    }

    @Nested
    @DisplayName("GET /api/teams/{teamID}/nextMatches")
    inner class GetNextMatches {

        private val teamId = 10L

        @Test
        fun shouldReturn200OKWithListOfMatches() {
            val matches = listOf(
                MatchDTO(1, SimpleTeamDTO(10, "Team A"), SimpleTeamDTO(11, "Team B"), LocalDate.now(), "La Liga", 1, 0),
                MatchDTO(2, SimpleTeamDTO(12, "Team C"), SimpleTeamDTO(10, "Team A"), LocalDate.now().plusDays(7), "Copa del Rey", 0, 0)
            )
            `when`(matchService.getNextMatches(teamId)).thenReturn(matches)

            mockMvc.perform(get("$teamEndpoint/{teamID}/nextMatches", teamId))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].tournament").value("Copa del Rey"))
                .andExpect(jsonPath("$.size()").value(2))
        }

        @Test
        fun shouldReturn200OKWithEmptyListWhenNoMatchesAreFound() {
            `when`(matchService.getNextMatches(teamId)).thenReturn(emptyList())

            mockMvc.perform(get("$teamEndpoint/{teamID}/nextMatches", teamId))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.size()").value(0))
        }
    }

    @Nested
    @DisplayName("GET /api/teams/search/{searchParam}")
    inner class SearchTeams {

        private val searchParam = "Madrid"

        @Test
        fun shouldReturn200OKWithListOfTeamsWhenSearchIsSuccessful() {
            val teams = listOf(
                SimpleTeamDTO(10, "Real Madrid"),
                SimpleTeamDTO(15, "Atletico Madrid")
            )
            `when`(scraperService.searchTeams(searchParam)).thenReturn(teams)

            mockMvc.perform(get("$teamEndpoint/search/{searchParam}", searchParam))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].teamName").value("Real Madrid"))
                .andExpect(jsonPath("$[1].teamID").value(15))
        }

        @Test
        fun shouldReturn404NotFoundWhenSearchReturnsNoTeams() {
            `when`(scraperService.searchTeams(searchParam)).thenReturn(emptyList())

            mockMvc.perform(get("$teamEndpoint/search/{searchParam}", searchParam))
                .andExpect(status().isNotFound)
        }
    }

    @Nested
    @DisplayName("GET /api/teams/predict/{teamA}/{teamB}")
    inner class PredictMatch {

        private val teamAId = 1L
        private val teamBId = 2L

        @Test
        fun shouldReturn200OKWithPrediction() {
            val teamA = SimpleTeamDTO(teamAId, "Team A")
            val teamB = SimpleTeamDTO(teamBId, "Team B")
            val prediction = PredictionDTO(teamA, teamB, 0.5, 0.3, 0.2, teamA)
            `when`(teamService.predictMatch(teamAId, teamBId)).thenReturn(prediction)

            mockMvc.perform(get("$teamEndpoint/predict/{teamA}/{teamB}", teamAId, teamBId))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.homeTeam.teamName").value("Team A"))
                .andExpect(jsonPath("$.awayTeam.teamName").value("Team B"))
                .andExpect(jsonPath("$.predictedWinner.teamName").value("Team A"))
        }
    }
}
