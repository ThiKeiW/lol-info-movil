package com.thikeiw.lolinfo.data.remote

import com.thikeiw.lolinfo.data.remote.dto.CdragonSkinDto
import com.thikeiw.lolinfo.data.remote.dto.CdragonSkinLineDto
import retrofit2.http.GET

interface CommunityDragonService {
  @GET("latest/plugins/rcp-be-lol-game-data/global/default/v1/skins.json")
  suspend fun skins(): Map<String, CdragonSkinDto>

  @GET("latest/plugins/rcp-be-lol-game-data/global/default/v1/skinlines.json")
  suspend fun skinLines(): List<CdragonSkinLineDto>
}
