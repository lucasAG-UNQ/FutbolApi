package com.grupob.futbolapi.services

import com.grupob.futbolapi.model.Player
import com.grupob.futbolapi.model.Team
import com.grupob.futbolapi.repositories.PlayerRepository
import com.grupob.futbolapi.repositories.TeamRepository
import io.github.bonigarcia.wdm.WebDriverManager
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import org.openqa.selenium.By
import org.openqa.selenium.PageLoadStrategy
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait


import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.Calendar
import java.util.concurrent.Executors


@Service
class WhoScoredScraperService(
    private val teamRepository: TeamRepository,
    private val playerRepository: PlayerRepository
) {

    val baseURL="https://www.whoscored.com"

    @Transactional
    fun getTeam(teamID: Long): Team{
        println("${Calendar.getInstance().time} - entering getTeam")

        val client = OkHttpClient()

        val url = "$baseURL/statisticsfeed/1/getplayerstatistics?category=summary&subcategory=all&statsAccumulationType=0&isCurrent=true&teamIds=$teamID&sortBy=Rating&sortAscending=&field=Overall&isMinApp=false&includeZeroValues=true"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .header("Referer", "https://www.whoscored.com/Teams/$teamID")
            .build()

        println("${Calendar.getInstance().time} - After request send")

        val response = client.newCall(request).execute()
        val body = response.body?.string()
        val json = JSONObject(body)
        val playersJSON = json.getJSONArray("playerTableStats")

        val firstPlayer = playersJSON.getJSONObject(0)
        val teamName=firstPlayer.getString("teamName")
        val teamCountry= firstPlayer.getString("teamRegionName")
        val teamID=firstPlayer.getLong("teamId")

        val retTeam= Team(teamID,teamName,teamCountry)

        println("${Calendar.getInstance().time} - before mapping")
        val players = (0 until playersJSON.length()).map { i ->
            val p = playersJSON.getJSONObject(i)
            Player(
                id = p.getLong("playerId"),
                name = p.getString("name"),
                position = p.optString("positionText"),
                tournament = p.getString("tournamentName"),
                season = p.getString("seasonName"),
                apps = p.optInt("apps"),
                goals = p.optInt("goal"),
                assists = p.optInt("assistTotal"),
                rating = p.optDouble("rating"),
                minutes = p.optInt("minsPlayed"),
                yellowCards = p.optInt("yellowCard"),
                redCards = p.optInt("redCard"),
                age = p.optInt("age"),
                team=retTeam
            )
        }.toMutableList()

        retTeam.players=players

        println("${Calendar.getInstance().time} - After player mapping")

        return retTeam
    }

    @Transactional
    fun searchTeams(searchParam: String): List<Map<String, Any>>{
        val titleText = "Teams:"

        val client = OkHttpClient()

        // Build the URL with the search query
        val url = "$baseURL/search/?t=$searchParam"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                println("${Calendar.getInstance().time} - Failed to fetch page")
                return emptyList()
            }

            val html = response.body?.string() ?: return emptyList()
            val doc = Jsoup.parse(html)

            // Locate the table that follows the H2 with the given text
            val table = doc.selectFirst("h2:matchesOwn(^$titleText\$) + table")
                ?: return emptyList()

            val rows = table.select("tr").drop(1) // skip header row

            val res = ArrayList<Map<String, Any>>()

            for (row in rows) {
                val firstTd = row.selectFirst("td") ?: continue
                val link = firstTd.selectFirst("a") ?: continue

                val teamName = link.text()
                val href = link.attr("href") // e.g., /teams/346/show/argentina-argentina
                val teamID = Regex("/teams/(\\d+)/").find(href)?.groupValues?.get(1)?.toLongOrNull() ?: continue

                res.add(mapOf("id" to teamID, "name" to teamName))
            }

            return res
        }
    }

    @Transactional
    fun searchTeams2(searchParam: String): List<Map<String, Any>> {
        println(Calendar.getInstance().time.toString()+"-Search Start")
        val driver = initFirefox()
        println(Calendar.getInstance().time.toString()+"-After driver init")
        val wait = WebDriverWait(driver, Duration.ofSeconds(5))

        driver.get("${baseURL}/search/?t=${searchParam}")

        println(Calendar.getInstance().time.toString()+"-After page get")
        val titleText = "Teams:"

        // Then wait for the table to appear
        val table: WebElement = try{
            wait.until(
                ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//h2[normalize-space(text())='$titleText']/following-sibling::table[1]")
                )
            )
        }catch (e: TimeoutException){
            driver.quit()
            println(Calendar.getInstance().time.toString()+"-Table not found")
            return emptyList()
        }

        println(Calendar.getInstance().time.toString()+"-After wait")
        // Now get rows
        var rows = table.findElements(By.tagName("tr"))
        rows = rows.subList(1,rows.size)

        val res:ArrayList<Map<String,Any>> = ArrayList<Map<String, Any>>()

        for (row in rows){
            val teamElement:List<WebElement> = row.findElements(By.tagName("td"))
            val teamName:String = teamElement[0].findElement(By.xpath("./*")).text
            val teamLink = teamElement[0].findElement(By.xpath("./*")).getDomProperty("href")!!
            val teamID= Regex("/teams/(\\d+)/").find(teamLink)!!.groupValues[1]

            res.add(mapOf("id" to teamID.toLong(), "name" to teamName))
        }

        println(Calendar.getInstance().time.toString()+"-After team mapping")

//        val teams: ArrayList<Team> = ArrayList<Team>()
//
//        for (row in rows){
//
//            val teamElement:List<WebElement> = row.findElements(By.tagName("td"))
//            val teamName:String = teamElement[0].findElement(By.xpath("./*")).text
//            val teamLink = teamElement[0].getDomProperty("href")!!
//            val teamID= Regex("/teams/(\\d+)/").find(teamLink)!!
//            val teamCountry = teamElement[1].findElement(By.xpath("./*")).text
//
//            val actualTeam:Team = Team(id = teamID.value.toLong(),name = teamName, country = teamCountry)
//
//            searchAndAddPlayers(driver, teamLink)
//
//            teams.add(actualTeam)
//        }
        val executor = Executors.newSingleThreadExecutor()
        executor.submit {
            try {
                driver.quit()
            } catch (e: Exception) {
                println("Error closing driver: ${e.message}")
            }
        }

        println(Calendar.getInstance().time.toString()+"-Driver quit")
        return res
    }

    private fun searchAndAddPlayers(driver: WebDriver, teamLink: String):List<Player> {
        driver.get("${baseURL}$teamLink")
        val wait = WebDriverWait(driver, Duration.ofSeconds(10))

        wait.until(
            ExpectedConditions.presenceOfElementLocated(
                By.id("statistics-table-summary")
            )
        )


        return emptyList()
    }

    private fun initFirefox(): WebDriver {
        // Setup GeckoDriver automatically
        WebDriverManager.firefoxdriver().setup()

        val options = FirefoxOptions()

//        if (headless) {
//            options.addArguments("--headless")
//        }

        options.addPreference("permissions.default.image", 2)        // 2 = block all images
        options.addPreference("media.autoplay.default", 1)            // block autoplay
        options.addPreference("media.autoplay.blocking_policy", 2)
        options.addPreference("permissions.default.stylesheet", 2)    // optional: disable CSS (page may look weird)
        options.addPreference("permissions.default.fonts", 2)
        options.addPreference("permissions.default.object", 2)
        options.addPreference("permissions.default.script", 1)        // careful: 1=allow, 2=block JS (usually keep 1)

// --- Tracking / ads ---
        options.addPreference("privacy.trackingprotection.enabled", true)
        options.addPreference("dom.ipc.plugins.enabled.libflashplayer.so", false)

// --- Disable notifications / popups ---
        options.addPreference("dom.webnotifications.enabled", false)
        options.addPreference("dom.push.enabled", false)

// --- Headless mode ---
        options.addArguments("--headless") // run without GUI
        options.addArguments("--disable-gpu")
        options.addArguments("--no-sandbox")

// --- Faster page load ---
        options.setPageLoadStrategy(PageLoadStrategy.NONE) // don't wait for images/scripts

        // Common flags
        options.addArguments("--width=800")
        options.addArguments("--height=600")

        // Optional: set custom user agent
        options.addPreference(
            "general.useragent.override",
            "Mozilla/5.0 (X11; Linux x86_64; rv:120.0) Gecko/20100101 Firefox/120.0"
        )

        return FirefoxDriver(options)
    }

//    @Transactional
//    fun scrapeAndSaveData() {
//        Playwright.create().use { playwright ->
//            val browser = playwright.chromium().launch(
//                BrowserType.LaunchOptions()
//                    .setHeadless(false) // Set to true for production
//            )
//            val page = browser.newPage()
//            try {
//                page.navigate("https://www.whoscored.com/statistics", com.microsoft.playwright.Page.NavigateOptions().setTimeout(60000.0))
//                page.waitForLoadState(LoadState.NETWORKIDLE)
//
//                //// Handle cookie consent dialog if it appears
//                val cookieButton = page.locator("//button[contains(text(), 'Aceptar todo')]")
//                if (cookieButton.isVisible) {
//                    cookieButton.click()
//                }
//
//                val allTeamData = mutableListOf<Pair<String, String>>()
//
//                // Loop through all pages of the team statistics table
//                //while (true) {
//                    page.waitForSelector("#top-team-stats-summary-content > tr")
//                    val teamRows = page.querySelectorAll("#top-team-stats-summary-content > tr")
//
//                    teamRows.mapNotNull { row ->
//                        val linkElement = row.querySelector("a.team-link")
//                        if (linkElement != null) {
//                            // The first 3 characters are the rank, e.g., "1. ", so we remove them.
//                            val teamName = linkElement.innerText().substring(3)
//                            val teamUrl = "https://www.whoscored.com" + linkElement.getAttribute("href")
//                            allTeamData.add(Pair(teamName, teamUrl))
//                        }
//                    }
//
//                    val nextButton = page.locator("#statistics-team-paging-summary > div > dl > dd > a#next")
//                    // Check if the next button is disabled (it gets a class 'disabled')
//                    val isNextDisabled = nextButton.evaluate("node => node.classList.contains('disabled')") as Boolean
//                //    if (isNextDisabled) {
//                //        break // Exit loop if on the last page
//                //    } else {
//                //        nextButton.click()
//                //        page.waitForLoadState(LoadState.NETWORKIDLE)
//                //    }
//                //}
//                var count=0;
//                // Now, scrape players for each collected team
//                for ((teamName, teamUrl) in allTeamData) {
//                    var team = teamRepository.findByName(teamName)
//                    if (team == null) {
//                        team = Team(name = teamName)
//                    }
//                    count++
//                    println(count)
//                    println(team)
//                    println(teamName)
//                    page.navigate(teamUrl, com.microsoft.playwright.Page.NavigateOptions().setTimeout(60000.0))
//                    page.waitForLoadState(LoadState.DOMCONTENTLOADED)
//                    page.waitForSelector("#top-player-stats-summary-grid > tbody > tr > td")
//
//
//                    val playerRows = try {
//                        page.querySelectorAll("#top-player-stats-summary-grid tbody tr")
//                    } catch (e: PlaywrightException) {
//                        println("Rows not found: ${e.message}")
//                        emptyList()
//                    }
//
//                    for (playerRow in playerRows) {
//                        println(playerRow)
//                        val playerElementID = playerRow.querySelector("td:nth-child(1)")
//                        val playerName = playerElementID.querySelector("a.player-link > span")?.innerText()
//                        val position = playerElementID.querySelector("span > span:nth-child(1)")?.innerText() + playerElementID.querySelector("span > span:nth-child(2)")?.innerText()
//                        println(playerName)
//                        if (playerName != null && playerName.isNotEmpty()) {
//                            val playerExists = team.players.any { it.name == playerName }
//                            if (!playerExists) {
//                                val newPlayer = Player(name = playerName, position = position ?: "N/A", team = team)
//                                team.players.add(newPlayer)
//                            }
//                        }
//                    }
//                    teamRepository.save(team)
//                }
//            } finally {
//                browser.close()
//            }
//        }
//    }
}
