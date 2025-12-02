package com.grupob.futbolapi.services

import org.json.JSONObject

interface IFootballDataApi {
    fun getTeam(query : String) : JSONObject?
}