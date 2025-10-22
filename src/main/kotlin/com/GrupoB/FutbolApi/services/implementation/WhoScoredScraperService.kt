package com.grupob.futbolapi.services.implementation

import com.grupob.futbolapi.exceptions.TeamNotFoundException
import com.grupob.futbolapi.model.Player
import com.grupob.futbolapi.model.Team
import com.grupob.futbolapi.model.dto.MatchDTO
import com.grupob.futbolapi.model.dto.SimpleTeamDTO
import com.grupob.futbolapi.services.IWhoScoredScraperService
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class WhoScoredScraperService : IWhoScoredScraperService {

    private val logger = LoggerFactory.getLogger(WhoScoredScraperService::class.java)
    private val baseURL = "https://www.whoscored.com"

    @Transactional
    override fun getTeam(teamID: Long): Team {
        val client = OkHttpClient()
        val url = "$baseURL/statisticsfeed/1/getplayerstatistics?category=summary&subcategory=all&statsAccumulationType=0&isCurrent=true&teamIds=$teamID&sortBy=Rating&sortAscending=&field=Overall&isMinApp=false&includeZeroValues=true"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .header("Referer", "https://www.whoscored.com/Teams/$teamID")
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string()
        val json = JSONObject(body)
        val playersJSON = json.getJSONArray("playerTableStats")

        if (playersJSON.length() == 0) throw TeamNotFoundException("Team with id $teamID doesn't seems to exist")

        val firstPlayer = playersJSON.getJSONObject(0)
        val teamName = firstPlayer.getString("teamName")
        val teamCountry = firstPlayer.getString("teamRegionName")
        val teamIDFromJson = firstPlayer.getLong("teamId")

        val retTeam = Team(teamIDFromJson, teamName, teamCountry)

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
                team = retTeam
            )
        }.toMutableList()

        retTeam.players = players

        return retTeam
    }

    @Transactional
    override fun searchTeams(searchParam: String): List<SimpleTeamDTO> {
        val titleText = "Teams:"
        val client = OkHttpClient()
        val url = "$baseURL/search/?t=$searchParam"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                logger.error("Failed to fetch search page for param: {}. Status: {}", searchParam, response.code)
                return emptyList()
            }

            val html = response.body?.string() ?: return emptyList()
            val doc = Jsoup.parse(html)

            val table = doc.selectFirst("h2:matchesOwn(^$titleText\$) + table")
                ?: return emptyList()

            val rows = table.select("tr").drop(1) // skip header row
            val res = ArrayList<SimpleTeamDTO>()

            for (row in rows) {
                val firstTd = row.selectFirst("td") ?: continue
                val link = firstTd.selectFirst("a") ?: continue

                val teamName = link.text()
                val href = link.attr("href")
                val teamID = Regex("/teams/(\\d+)/").find(href)?.groupValues?.get(1)?.toLongOrNull() ?: continue

                res.add(SimpleTeamDTO(teamID, teamName))
            }

            return res
        }
    }

    override fun getNextTeamMatches(teamId: Long): List<MatchDTO> {
        logger.debug("Starting getNextTeamMatches for teamId: {}", teamId)
        val client = OkHttpClient()
        val url = "$baseURL/teams/$teamId/fixtures"
        logger.debug("Requesting URL: {}", url)

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                logger.error("Failed to fetch page for teamId: {}. Status: {}", teamId, response.code)
                return emptyList()
            }

            val html = response.body?.string()
            if (html.isNullOrEmpty()) {
                logger.warn("Response body is null or empty for teamId: {}", teamId)
                return emptyList()
            }

            val rawScript = Jsoup.parse(html).select("script")
                .firstOrNull { it.html().contains("fixtureMatches") }?.html() ?: return emptyList()
            
            val arrayText = rawScript.substringAfter("fixtureMatches: [")
                .substringBefore("];")
                .replace("'", "\"")
                .replace(", ,", ", null,")

            return try {
                parseFixtures("[$arrayText]")
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun parseFixtures(rawJsonArray: String): List<MatchDTO> {
        val root = JSONArray(rawJsonArray)
        val fixtures = mutableListOf<MatchDTO>()

        for (i in 0 until root.length()) {
            val arr = root.getJSONArray(i)

            val date = LocalDate.parse(arr.getString(2), DateTimeFormatter.ofPattern("dd-MM-yy"))

            val homeTeam = SimpleTeamDTO(
                teamID = arr.getLong(4),
                teamName = arr.getString(5)
            )

            val awayTeam = SimpleTeamDTO(
                teamID = arr.getLong(7),
                teamName = arr.getString(8)
            )

            val matchDto = MatchDTO(
                id = arr.getLong(0),
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                date = date,
                tournament = arr.getString(22),//22 tournament code, 16 for the name of the tournament
            )
            fixtures.add(matchDto)
        }

        return fixtures
    }
}