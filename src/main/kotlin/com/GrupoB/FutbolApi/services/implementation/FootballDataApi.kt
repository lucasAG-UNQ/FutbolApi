package com.grupob.futbolapi.services.implementation

import com.grupob.futbolapi.services.IFootballDataApi
import me.xdrop.fuzzywuzzy.FuzzySearch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional
@Service
class FootballDataApi(private val client: OkHttpClient) : IFootballDataApi {

    val baseUrl = "https://api.football-data.org/v4"
    @Value("\${FOOTBALL_DATA_API_KEY}")
    private lateinit var apiKey: String

    override fun getTeam(query : String) : JSONObject?{
        var teamsCount = Int.MAX_VALUE
        val threshold = 75


        var bestTeam: JSONObject? = null
        var bestScore = 0
        var offset = 0
        while (teamsCount != 0) {

            val (teams, count) = fetchTeamsPage(offset)
            teamsCount = count

            for (i in 0 until teams.length()) {
                val team = teams.getJSONObject(i)
                val name = team.getString("name")

                val score = similarity(query, name)

                // Track best result so far
                if (score > bestScore) {
                    bestScore = score
                    bestTeam = team
                }

                // EARLY STOP: found good enough match
                if (bestScore >= threshold) {
                    return bestTeam
                }
            }

            offset+=500
        }

        // Return best found only if above threshold, if the func gets off the loop, there is no team match
        return null
    }

    fun getTeamById(teamId: Long): JSONObject? {
        val request = Request.Builder()
            .url("$baseUrl/teams/$teamId")
            .addHeader("X-Auth-Token", apiKey)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            return null
        }
        val body = response.body?.string() ?: return null
        return JSONObject(body)
    }

    fun similarity(a: String, b: String): Int {
        return FuzzySearch.ratio(a.lowercase(), b.lowercase())
    }

    fun fetchTeamsPage(offset: Int): Pair<JSONArray, Int> {
        val request = Request.Builder()
            .url("$baseUrl/teams?limit=500&offset=$offset")
            .addHeader("X-Auth-Token", apiKey)
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty body")

        val json = JSONObject(body)
        val teams = json.getJSONArray("teams")
        val teamsCount = json.getInt("count")

        return teams to teamsCount
    }
}
