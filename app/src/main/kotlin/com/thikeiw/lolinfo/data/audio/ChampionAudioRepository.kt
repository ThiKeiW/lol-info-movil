package com.thikeiw.lolinfo.data.audio

import com.thikeiw.lolinfo.domain.model.ChampionAudioCatalog

interface ChampionAudioRepository {
  suspend fun load(championId: String, skinNum: Int): Result<ChampionAudioCatalog>

  fun clearMemory()
}
