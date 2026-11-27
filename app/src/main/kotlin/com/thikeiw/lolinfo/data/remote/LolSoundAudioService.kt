package com.thikeiw.lolinfo.data.remote

import com.thikeiw.lolinfo.data.remote.dto.LolSoundChampionDetailDto
import com.thikeiw.lolinfo.data.remote.dto.LolSoundChampionDto
import com.thikeiw.lolinfo.data.remote.dto.LolSoundLineDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface LolSoundAudioService {
  @GET("api/v1/champions")
  suspend fun champions(): List<LolSoundChampionDto>

  @GET("api/v1/champions/{code}")
  suspend fun champion(@Path("code") code: String): LolSoundChampionDetailDto

  @GET("api/v1/champions/{code}/skins/{skinCode}/sounds")
  suspend fun sounds(
    @Path("code") code: String,
    @Path("skinCode") skinCode: String,
    @Query("language") language: String,
  ): List<LolSoundLineDto>
}
