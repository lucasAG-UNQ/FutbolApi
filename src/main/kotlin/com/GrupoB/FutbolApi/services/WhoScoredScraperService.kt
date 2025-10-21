package com.grupob.futbolapi.services

import com.grupob.futbolapi.model.Match
import com.grupob.futbolapi.model.Player
import com.grupob.futbolapi.model.Team
import com.grupob.futbolapi.model.dto.MatchDTO
import com.grupob.futbolapi.model.dto.SimpleTeamDTO
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
import java.util.Calendar
import java.util.Locale

@Service
class WhoScoredScraperService {

    private val logger = LoggerFactory.getLogger(WhoScoredScraperService::class.java)
    private val baseURL = "https://www.whoscored.com"

    @Transactional
    fun getTeam(teamID: Long): Team {
        // ... (rest of the getTeam method)
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
    fun searchTeams(searchParam: String): List<SimpleTeamDTO> {
        // ... (rest of the searchTeams method)
        val titleText = "Teams:"
        val client = OkHttpClient()
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

                res.add(SimpleTeamDTO(teamID,teamName))
            }

            return res
        }
    }

    fun getNextTeamMatches(teamId: Long): List<MatchDTO> {
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

            val html = response.body?.string() ?: ""
            val jsoupDoc = Jsoup.parse(html)
            val raw = jsoupDoc.select("script")
                .first { it.html().contains("fixtureMatches") }
                .html()
            val arrayText = raw.substringAfter("fixtureMatches: [")
                .substringBefore("];")
                .replace("'", "\"")
                .replace(", ,", ", null,")


            return parseFixtures("[$arrayText]")


//
//            if (html.isEmpty()) {
//                logger.warn("Response body is null or empty for teamId: {}", teamId)
//                return emptyList()
//            }
//
//            logger.debug("Received HTML response of length: {}", html.length)
//
//            val doc = Jsoup.parse(html)
//            val fixtures = mutableListOf<MatchDTO>()
//            val formatter = DateTimeFormatter.ofPattern("dd-MM-yy", Locale.ENGLISH)
//            val rows = doc.select("div.divtable-row")
//            logger.debug("Found {} potential match rows.", rows.size)
//
//            for ((index, row) in rows.withIndex()) {
//                logger.trace("Processing row {}: {}", index, row.html())
//
//                val homeScore = row.selectFirst(".home-score")?.text()?.trim().orEmpty()
//                val awayScore = row.selectFirst(".away-score")?.text()?.trim().orEmpty()
//
//                if (homeScore.isNotEmpty() || awayScore.isNotEmpty()) {
//                    logger.trace("Skipping row {} because it has a score (match already played).", index)
//                    continue
//                }
//
//                val matchId = row.attr("data-id").toLongOrNull()
//                val dateString = row.selectFirst(".fourth-col-date")?.text()?.trim()
//                val tournament = row.selectFirst(".tournament-link")?.text()?.trim().orEmpty()
//                val teams = row.select(".team-link")
//                val homeTeamLink = teams.getOrNull(0)?.attr("href")
//                val awayTeamLink = teams.getOrNull(1)?.attr("href")
//                val homeTeamName = teams.getOrNull(0)?.text()?.trim()
//                val awayTeamName = teams.getOrNull(1)?.text()?.trim()
//
//                logger.trace("Row {} data: matchId={}, dateString={}, tournament={}, homeTeamName={}, awayTeamName={}", index, matchId, dateString, tournament, homeTeamName, awayTeamName)
//
//                if (matchId == null || dateString.isNullOrEmpty() || homeTeamLink.isNullOrEmpty() || awayTeamLink.isNullOrEmpty() || homeTeamName.isNullOrEmpty() || awayTeamName.isNullOrEmpty()) {
//                    logger.warn("Skipping row {} due to missing essential data.", index)
//                    continue
//                }
//
//                val date = LocalDate.parse(dateString, formatter)
//                val homeTeamId = Regex("/teams/(\\d+)/").find(homeTeamLink)?.groupValues?.get(1)?.toLongOrNull() ?: continue
//                val awayTeamId = Regex("/teams/(\\d+)/").find(awayTeamLink)?.groupValues?.get(1)?.toLongOrNull() ?: continue
//
//                val homeTeam = SimpleTeamDTO(homeTeamId, homeTeamName)
//                val awayTeam = SimpleTeamDTO(awayTeamId, awayTeamName)
//
//                val match = MatchDTO(matchId, homeTeam, awayTeam, date, tournament)
//                fixtures.add(match)
//                logger.trace("Successfully created and added MatchDTO: {}", match)
//            }
//
//            logger.debug("Finished scraping. Found {} upcoming matches for teamId: {}.", fixtures.size, teamId)
//            logger.trace("Final list of matches: {}", fixtures)
//            return fixtures
        }
    }

    fun parseFixtures(raw: String): List<MatchDTO> {
        val arrayText = raw.substringAfter("fixtureMatches: [")
            .substringBefore("];")
            .replace("'", "\"")
            .replace(", ,", ", null,")
        val root = JSONArray(raw)
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
                tournament = arr.getString(17),
            )
            fixtures.add(matchDto)
        }

        return fixtures
    }
}