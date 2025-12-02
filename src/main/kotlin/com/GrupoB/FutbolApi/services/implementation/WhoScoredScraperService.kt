package com.grupob.futbolapi.services.implementation

import com.grupob.futbolapi.exceptions.PlayerNotFoundException
import com.grupob.futbolapi.exceptions.TeamNotFoundException
import com.grupob.futbolapi.model.Player
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

@Transactional(noRollbackFor = [TeamNotFoundException::class])
@Service
class WhoScoredScraperService(
    private val client: OkHttpClient,
    private val baseURL: String = "https://www.whoscored.com"
) : IWhoScoredScraperService {

    private val logger = LoggerFactory.getLogger(WhoScoredScraperService::class.java)

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

        if (playersJSON.length() == 0) {
            val teamReq= client.newCall(buildRequest("${baseURL}/teams/$teamID")).execute()
            val teamName = Jsoup.parse(teamReq.body?.string() ?: "").selectFirst("span.team-header-name")
            if (teamName == null) throw TeamNotFoundException("Team with id $teamID doesn't seems to exist")
            return TeamDTO(teamID, teamName.text(), "", emptyList())
        }
        val firstPlayer = playersJSON.getJSONObject(0) ?: throw TeamNotFoundException("Team with id $teamID doesn't seems to exist")
        val teamName = firstPlayer.getString("teamName")
        val teamCountry = firstPlayer.getString("teamRegionName")
        val teamIDFromJson = firstPlayer.getLong("teamId")

        val players = (0 until playersJSON.length()).map { i ->
            val p = playersJSON.getJSONObject(i)

            val pid = p.getLong("playerId")
            val pname = p.optString("name",null)
            val pposition = p.optString("positionText",null)
            val pteam = SimpleTeamDTO(teamIDFromJson,teamName)
            val ptournament = p.optString("tournamentName",null)
            val pseason = p.optString("seasonName",null)
            val papps = p.optInt("apps", 0)
            val pgoals = p.optInt("goal", 0)
            val passists = p.optInt("assistTotal", 0)
            val prating = p.optDouble("rating", 0.0)
            val pminutes = p.optInt("minsPlayed", 0)
            val pyellowCards = p.optInt("yellowCard", 0)
            val predCards = p.optInt("redCard", 0)
            val page = p.optInt("age", 0)

            PlayerDTO(
                id = pid,
                name = pname,
                position = pposition,
                team = pteam,
                tournament = ptournament,
                season = pseason,
                apps = papps,
                goals = pgoals,
                assists = passists,
                rating = prating,
                minutes = pminutes,
                yellowCards = pyellowCards,
                redCards = predCards,
                age = page
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

    override fun getPlayerById(playerId: Long): Player {
        val url = "$baseURL/statisticsfeed/1/getplayerstatistics?category=summary&subcategory=all&statsAccumulationType=0&isCurrent=true&playerId=${playerId}&teamIds=&matchId=&stageId=&tournamentOptions=&sortBy=Rating&sortAscending=&age=&ageComparisonType=&appearances=&appearancesComparisonType=&field=Overall&nationality=&positionOptions=&timeOfTheGameEnd=&timeOfTheGameStart=&isMinApp=false&page=&includeZeroValues=true&numberOfPlayersToPick=&incPens="
        val request = buildRequest(url)
        val response = client.newCall(request).execute()
        val body = response.body?.string()
        if (body == null || !response.isSuccessful) {
            throw PlayerNotFoundException("Player with id $playerId doesn't seem to exist")
        }

        val json = JSONObject(body)
        val playerStats = json.getJSONArray("playerTableStats")

        if (playerStats.length() == 0) {
            throw PlayerNotFoundException("Player with id $playerId doesn't seem to exist")
        }

        val firstPlayer = playerStats.getJSONObject(0)
        val name = firstPlayer.optString("name")
        val position = firstPlayer.optString("positionText")
        val age = firstPlayer.optInt("age")

        var totalApps = 0
        var totalMinutes = 0
        var totalGoals = 0
        var totalAssists = 0
        var totalYellowCards = 0
        var totalRedCards = 0
        var weightedRatingSum = 0.0
        val tournamentNames = mutableSetOf<String>()
        val seasonNames = mutableSetOf<String>()

        for (i in 0 until playerStats.length()) {
            val p = playerStats.getJSONObject(i)
            val minutes = p.optInt("minsPlayed")
            totalApps += p.optInt("apps")
            totalMinutes += minutes
            totalGoals += p.optInt("goal")
            totalAssists += p.optInt("assistTotal")
            totalYellowCards += p.optInt("yellowCard")
            totalRedCards += p.optInt("redCard")
            weightedRatingSum += p.optDouble("rating") * minutes
            tournamentNames.add(p.optString("tournamentName"))
            seasonNames.add(p.optString("seasonName"))
        }

        val rating = if (totalMinutes > 0) weightedRatingSum / totalMinutes else 0.0

        return Player(
            id = playerId,
            name = name,
            position = position,
            age = age,
            apps = totalApps,
            minutes = totalMinutes,
            goals = totalGoals,
            assists = totalAssists,
            yellowCards = totalYellowCards,
            redCards = totalRedCards,
            rating = rating,
            tournament = tournamentNames.joinToString(", "),
            season = seasonNames.joinToString(", ")
        )
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
                homeScore = arr.optString(31, null)?.filter { it.isDigit() }?.toIntOrNull(),
                awayScore = arr.optString(32, null)?.filter { it.isDigit() }?.toIntOrNull(),
                tournament = arr.getString(16),//22 tournament code, 16 for the name of the tournament
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
