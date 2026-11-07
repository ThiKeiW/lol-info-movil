package com.thikeiw.lolinfo.data.remote

import com.thikeiw.lolinfo.data.remote.dto.ChampionDetailResponse
import com.thikeiw.lolinfo.data.remote.dto.ChampionListResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface DataDragonService {
  @GET("api/versions.json")
  suspend fun versions(): List<String>

  /** Data Dragon usa carpetas tipo es_MX/en_US: Data Dragon Locale (es_mx → es_MX). */
  @GET("cdn/{version}/data/{locale}/champion.json")
  suspend fun champions(
    @Path("version") version: String,
    @Path("locale") locale: String,
  ): ChampionListResponse

  @GET("cdn/{version}/data/{locale}/champion/{id}.json")
  suspend fun championDetail(
    @Path("version") version: String,
    @Path("locale") locale: String,
    @Path("id") id: String,
  ): ChampionDetailResponse
}
