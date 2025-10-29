package com.grupob.futbolapi.services.implementation

import com.grupob.futbolapi.exceptions.TeamNotFoundException
import com.grupob.futbolapi.model.dto.MatchDTO
import com.grupob.futbolapi.model.dto.PlayerDTO
import com.grupob.futbolapi.model.dto.SimpleTeamDTO
import com.grupob.futbolapi.model.dto.TeamDTO
import com.grupob.futbolapi.services.IWhoScoredScraperService
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class WhoScoredScraperService(
    private val client: OkHttpClient,
    private val baseURL: String = "https://www.whoscored.com"
) : IWhoScoredScraperService {

    private val logger = LoggerFactory.getLogger(WhoScoredScraperService::class.java)

    @Transactional
    override fun getTeam(teamID: Long): TeamDTO {
        val url = "$baseURL/statisticsfeed/1/getplayerstatistics?category=summary&subcategory=all&statsAccumulationType=0&isCurrent=true&teamIds=$teamID&sortBy=Rating&sortAscending=&field=Overall&isMinApp=false&includeZeroValues=true"

        val request = buildRequest(url)

        val response = client.newCall(request).execute()
        val body = response.body?.string()
        if(body==null || !response.isSuccessful)throw TeamNotFoundException("Team with id $teamID doesn't seems to exist")
        val json = JSONObject(body)
        val playersJSON = try {
            json.getJSONArray("playerTableStats")
        } catch (e: JSONException) {
            throw TeamNotFoundException("Team with id $teamID doesn't seems to exist")
        }

        if (playersJSON.length() == 0) throw TeamNotFoundException("Team with id $teamID doesn't seems to exist")

        val firstPlayer = playersJSON.getJSONObject(0) ?: throw TeamNotFoundException("Team with id $teamID doesn't seems to exist")
        val teamName = firstPlayer.getString("teamName")
        val teamCountry = firstPlayer.getString("teamRegionName")
        val teamIDFromJson = firstPlayer.getLong("teamId")


        val players = (0 until playersJSON.length()).map { i ->
            val p = playersJSON.getJSONObject(i)
            PlayerDTO(
                id = p.getLong("playerId"),
                name = p.optString("name",null),
                position = p.optString("positionText",null),
                team = SimpleTeamDTO(teamIDFromJson,teamName),
                tournament = p.optString("tournamentName",null),
                season = p.optString("seasonName",null),
                apps = p.opt("apps") as? Int,
                goals = p.opt("goal") as? Int,
                assists = p.opt("assistTotal") as? Int,
                rating = p.opt("rating") as? Double,
                minutes = p.opt("minsPlayed") as? Int,
                yellowCards = p.opt("yellowCard") as? Int,
                redCards = p.opt("redCard") as? Int,
                age = p.opt("age") as? Int
            )
        }.toMutableList()

        return TeamDTO(
            teamIDFromJson, teamName, teamCountry, players
        )
    }

    @Transactional
    override fun searchTeams(searchParam: String): List<SimpleTeamDTO> {
        val titleText = "Teams:"
        val url = "$baseURL/search/?t=$searchParam"

        val request = buildRequest(url)

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
                val firstTd = row.selectFirst("td")
                val link = firstTd!!.selectFirst("a")

                val teamName = link!!.text()
                val href = link.attr("href")
                val teamID = Regex("/teams/(\\d+)/").find(href)?.groupValues?.get(1)?.toLong()

                res.add(SimpleTeamDTO(teamID!!, teamName))
            }

            return res
        }
    }

    override fun getNextTeamMatches(teamId: Long): List<MatchDTO> {
        logger.debug("Starting getNextTeamMatches for teamId: {}", teamId)
        val url = "$baseURL/teams/$teamId/fixtures"
        logger.debug("Requesting URL: {}", url)

        val request = buildRequest(url)

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

            return parseFixtures("[$arrayText]")

        }
    }

    private fun parseFixtures(rawJsonArray: String): List<MatchDTO> {
        val root = JSONArray(rawJsonArray)
        val fixtures = mutableListOf<MatchDTO>()

        for (i in 0 until root.length()) {
            val arr = root.getJSONArray(i)

            val date = LocalDate.parse(arr.optString(2), DateTimeFormatter.ofPattern("dd-MM-yy"))

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
    private fun buildRequest(url: String): Request = Request.Builder()
        .url(url)
        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36")
        .header("Referer", "https://www.google.com/")
        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        .header("Accept-Language", "en-US,en;q=0.9")
        .build()
}

