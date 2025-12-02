package com.grupob.futbolapi.unit.webServices

import com.grupob.futbolapi.unit.model.builder.PlayerBuilder
import com.grupob.futbolapi.services.IPlayerService
import com.grupob.futbolapi.webServices.PlayerController
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

private const val playerEndpoint = "/api/players"

@ExtendWith(MockitoExtension::class)
@DisplayName("PlayerController Unit Tests")
class PlayerControllerTest {

    @Mock
    private lateinit var playerService: IPlayerService

    @InjectMocks
    private lateinit var playerController: PlayerController

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(playerController).build()
    }

    @Nested
    @DisplayName("GET /api/players/{playerID}/performance")
    inner class GetPlayerPerformance {

        private val playerId = 1L

        @Test
        fun shouldReturn200OKWithPlayerPerformanceWhenPlayerIsFound() {
            val player = PlayerBuilder()
                .withId(playerId)
                .withName("Test Player")
                .withPosition("Midfielder")
                .withAge(25)
                .withApps(10)
                .withGoals(5)
                .withAssists(3)
                .withRating(7.5)
                .build()

            `when`(playerService.findByPlayerId(playerId)).thenReturn(player)

            mockMvc.perform(get("$playerEndpoint/{playerID}/performance", playerId))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Test Player"))
                .andExpect(jsonPath("$.position").value("Midfielder"))
                .andExpect(jsonPath("$.age").value(25))
                .andExpect(jsonPath("$.apps").value(10))
                .andExpect(jsonPath("$.goals").value(5))
                .andExpect(jsonPath("$.assists").value(3))
                .andExpect(jsonPath("$.rating").value(7.5))
        }

        @Test
        fun shouldReturn404NotFoundWhenPlayerIsNotFound() {
            `when`(playerService.findByPlayerId(playerId)).thenReturn(null)

            mockMvc.perform(get("$playerEndpoint/{playerID}/performance", playerId))
                .andExpect(status().isNotFound)
        }
    }
}
