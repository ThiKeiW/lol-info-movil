package com.thikeiw.lolinfo.data.remote

import com.thikeiw.lolinfo.data.remote.dto.ChampionListResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface DataDragonService {
  @GET("api/versions.json") suspend fun versions(): List<String>

  @GET("cdn/{version}/data/es_ES/champion.json")
  suspend fun champions(@Path("version") version: String): ChampionListResponse
}
