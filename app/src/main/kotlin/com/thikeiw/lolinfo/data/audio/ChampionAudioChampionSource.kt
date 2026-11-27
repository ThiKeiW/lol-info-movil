package com.thikeiw.lolinfo.data.audio

import com.thikeiw.lolinfo.data.local.CatalogDao
import com.thikeiw.lolinfo.data.mappers.toDomain
import com.thikeiw.lolinfo.domain.model.Skin
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

data class ChampionAudioChampion(
  val id: String,
  val key: Int,
  val name: String,
  val version: String,
)

interface ChampionAudioChampionSource {
  suspend fun champion(id: String): ChampionAudioChampion?

  suspend fun skins(championId: String): List<Skin>
}

@Singleton
class DefaultChampionAudioChampionSource @Inject constructor(private val dao: CatalogDao) :
  ChampionAudioChampionSource {
  override suspend fun champion(id: String): ChampionAudioChampion? =
    dao.getChampion(id)?.let { entity ->
      ChampionAudioChampion(
        id = entity.id,
        key = entity.key,
        name = entity.name,
        version = entity.version,
      )
    }

  override suspend fun skins(championId: String): List<Skin> =
    dao.observeSkins(championId).first().map { it.toDomain() }
}
