package com.thikeiw.lolinfo.data.remote

import com.thikeiw.lolinfo.data.remote.dto.CdragonSkinDto
import com.thikeiw.lolinfo.data.remote.dto.CdragonSkinLineDto
import retrofit2.http.GET
import retrofit2.http.Path

interface CommunityDragonService {
  @GET("latest/plugins/rcp-be-lol-game-data/global/{locale}/v1/skins.json")
  suspend fun skins(@Path("locale") locale: String): Map<String, CdragonSkinDto>

  @GET("latest/plugins/rcp-be-lol-game-data/global/{locale}/v1/skinlines.json")
  suspend fun skinLines(@Path("locale") locale: String): List<CdragonSkinLineDto>
}
