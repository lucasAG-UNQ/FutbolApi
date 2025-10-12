package com.grupob.futbolapi.services

import com.grupob.futbolapi.model.Player
import com.grupob.futbolapi.model.Team
import com.grupob.futbolapi.repositories.PlayerRepository
import com.grupob.futbolapi.repositories.TeamRepository
import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.nio.file.Files
import java.time.Duration
import java.util.concurrent.TimeoutException

@Service
class ScraperService(
    private val teamRepository: TeamRepository,
    private val playerRepository: PlayerRepository
) {

    val baseUrl = "https://www.whoscored.com"

    @Transactional
    fun findTeamAndSave(searchParam: String){
        val driver = initFirefox()

        val tableTitleText = "Teams:"
        val wait = WebDriverWait(driver, Duration.ofSeconds(10))

        val h2: WebElement? = try {
            wait.until(
                ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//h2[normalize-space(text())='$tableTitleText']/following-sibling::table[1]")
                )
            )
        } catch (e: TimeoutException) {
            println("⚠️ Header '$tableTitleText' not found within timeout.")
            null
        }

        // Then wait for the table after it
        val table: WebElement = wait.until(
            ExpectedConditions.presenceOfElementLocated(
                By.xpath("//h2[normalize-space(text())='$tableTitleText']/following-sibling::table[1]")
            )
        )

        // Now safely get rows
        val rows = table.findElements(By.tagName("tr"))
        for (row in rows) {
            val cols = row.findElements(By.tagName("td")).map { it.text.trim() }
            println(cols)
        }
    }


    private fun initFirefox(headless: Boolean = true): WebDriver {
        // Setup GeckoDriver automatically
        WebDriverManager.firefoxdriver().setup()

        val options = FirefoxOptions()

        if (headless) {
            options.addArguments("--headless")
        }

        // Common flags
        options.addArguments("--width=1280")
        options.addArguments("--height=800")
        options.addPreference("permissions.default.image", 2)

        // Optional: set custom user agent
        options.addPreference(
            "general.useragent.override",
            "Mozilla/5.0 (X11; Linux x86_64; rv:120.0) Gecko/20100101 Firefox/120.0"
        )

        return FirefoxDriver(options)
    }

    @Transactional
    fun scrapeAndSaveData() {
        val driver = initFirefox()
        val wait = WebDriverWait(driver, Duration.ofSeconds(20))
        val statisticsUrl = "$baseUrl/statistics"

        try {
            driver.get(statisticsUrl)

            // Handle cookie consent
            val acceptButton = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.xpath("//button[contains(., 'Aceptar todo')]"))
            )
            try {
                acceptButton.click()
            } catch (e: Exception) {
                (driver as JavascriptExecutor).executeScript("arguments[0].click()", acceptButton)
            }

            val screenshot = (driver as TakesScreenshot).getScreenshotAs(OutputType.FILE)

            // Save it somewhere
            val destination = File("page_screenshot.png")
            Files.copy(screenshot.toPath(), destination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)

            println("✅ Screenshot saved to: ${destination.absolutePath}")


            // The team statistics table is inside an iframe
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("top-team-stats")))

            val teamUrls = mutableListOf<Pair<String, String>>()
            var count =0
            // Loop through pagination
            while (true) {
                count++
                println(count)
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("top-team-stats-summary-grid")))
                val table = driver.findElement(By.id("top-team-stats-summary-grid"))
                val rows = table.findElements(By.cssSelector("tbody > tr"))

                for (row in rows) {
                    val linkElement = row.findElement(By.xpath(".//td/a[contains(@class, 'team-link')]"))
                    val teamName = linkElement.text
                    val teamUrl = linkElement.getDomAttribute("href")
                    if (teamName.isNotEmpty() && teamUrl.isNotEmpty()) {
                        teamUrls.add(Pair(teamName, teamUrl))
                    }
                }

                val nextButton = driver.findElement(By.cssSelector("a#next"))
                val nextClass = nextButton.getDomAttribute("class");
                if (nextClass != null && nextClass.contains("disabled")) {
                    break // Last page
                }
                nextButton.click()
            }

            // Switch back to the main content from the iframe
            driver.switchTo().defaultContent()

            // Now, scrape players for each team
            for ((teamName, teamUrl) in teamUrls.distinctBy { it.first }) { // Use distinct to avoid duplicates from pagination issues
                driver.get(baseUrl + teamUrl)
                var team = teamRepository.findByName(teamName)
                if (team == null) {
                    team = Team(name = teamName)
                }

                wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("player-table-statistics-body")))
                val playerRows = driver.findElements(By.cssSelector("table#player-table-statistics-body > tbody > tr"))

                for (playerRow in playerRows) {
                    try {
                        val playerName = playerRow.findElement(By.cssSelector("td.pn > a.player-link")).text
                        val position = playerRow.findElement(By.cssSelector("td.pos")).text

                        if (playerName.isNotEmpty()) {
                            val playerExists = team.players.any { it.name == playerName }
                            if (!playerExists) {
                                val newPlayer = Player(name = playerName, position = position, team = team)
                                team.players.add(newPlayer)
                            }
                        }
                    } catch (e: Exception) {
                        // Player row might be empty or malformed, just skip it
                        println("Could not parse player row. Skipping.")
                    }
                }
                teamRepository.save(team)
            }

        } finally {
            driver.quit()
        }
    }
}

