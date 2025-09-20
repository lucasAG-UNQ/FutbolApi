package com.grupob.futbolapi.services

import com.grupob.futbolapi.model.Player
import com.grupob.futbolapi.model.Team
import com.grupob.futbolapi.repositories.PlayerRepository
import com.grupob.futbolapi.repositories.TeamRepository
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.LoadState
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ScraperService(
    private val teamRepository: TeamRepository,
    private val playerRepository: PlayerRepository
) {

    @Transactional
    fun scrapeAndSaveData() {
        Playwright.create().use { playwright ->
            val browser = playwright.chromium().launch(
                BrowserType.LaunchOptions()
                    .setHeadless(false) // Set to true for production
            )
            val page = browser.newPage()
            try {
                page.navigate("https://www.whoscored.com/statistics", com.microsoft.playwright.Page.NavigateOptions().setTimeout(60000.0))
                page.waitForLoadState(LoadState.NETWORKIDLE)

                //// Handle cookie consent dialog if it appears
                val cookieButton = page.locator("//button[contains(text(), 'Aceptar todo')]")
                if (cookieButton.isVisible) {
                    cookieButton.click()
                }

                val allTeamData = mutableListOf<Pair<String, String>>()

                // Loop through all pages of the team statistics table
                while (true) {
                    page.waitForSelector("#top-team-stats-summary-content > tr")
                    val teamRows = page.querySelectorAll("#top-team-stats-summary-content > tr")

                    teamRows.mapNotNull { row ->
                        val linkElement = row.querySelector("a.team-link")
                        if (linkElement != null) {
                            // The first 3 characters are the rank, e.g., "1. ", so we remove them.
                            val teamName = linkElement.innerText().substring(3)
                            val teamUrl = "https://www.whoscored.com" + linkElement.getAttribute("href")
                            allTeamData.add(Pair(teamName, teamUrl))
                        }
                    }

                    val nextButton = page.locator("#statistics-team-paging-summary > div > dl > dd > a#next")
                    // Check if the next button is disabled (it gets a class 'disabled')
                    val isNextDisabled = nextButton.evaluate("node => node.classList.contains('disabled')") as Boolean
                    if (isNextDisabled) {
                        break // Exit loop if on the last page
                    } else {
                        nextButton.click()
                        page.waitForLoadState(LoadState.NETWORKIDLE)
                    }
                }

                // Now, scrape players for each collected team
                for ((teamName, teamUrl) in allTeamData) {
                    var team = teamRepository.findByName(teamName)
                    if (team == null) {
                        team = Team(name = teamName)
                    }

                    page.navigate(teamUrl, com.microsoft.playwright.Page.NavigateOptions().setTimeout(60000.0))
                    page.waitForSelector("#top-player-stats-summary-grid > tbody > tr")

                    val playerRows = page.querySelectorAll("#top-player-stats-summary-grid > tbody > tr")


                    for (playerRow in playerRows) {
                        val playerElementID = playerRow.querySelector("td:nth-child(1)")
                        val playerName = playerElementID.querySelector("a.player-link > span")?.innerText()
                        val position = playerElementID.querySelector("span > span:nth-child(1)")?.innerText() + playerElementID.querySelector("span > span:nth-child(2)")?.innerText()

                        println(playerName)

                        if (playerName != null && playerName.isNotEmpty()) {
                            val playerExists = team.players.any { it.name == playerName }
                            if (!playerExists) {
                                val newPlayer = Player(name = playerName, position = position ?: "N/A", team = team)
                                team.players.add(newPlayer)
                            }
                        }
                    }
                    teamRepository.save(team)
                }
            } finally {
                browser.close()
            }
        }
    }
}
