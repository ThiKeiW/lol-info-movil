package com.thikeiw.lolinfo.data

import com.thikeiw.lolinfo.data.local.CatalogDao
import com.thikeiw.lolinfo.data.local.CatalogPreferences
import com.thikeiw.lolinfo.data.mappers.buildSkinEntities
import com.thikeiw.lolinfo.data.mappers.toDomain
import com.thikeiw.lolinfo.data.mappers.toEntity
import com.thikeiw.lolinfo.data.remote.CommunityDragonService
import com.thikeiw.lolinfo.data.remote.DataDragonService
import com.thikeiw.lolinfo.domain.model.Champion
import com.thikeiw.lolinfo.domain.model.Skin
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface CatalogRepository {
  fun observeChampions(): Flow<List<Champion>>

  fun observeChampion(id: String): Flow<Champion?>

  fun observeSkins(championId: String): Flow<List<Skin>>

  fun observeCatalogVersion(): Flow<String?>

  suspend fun refresh(force: Boolean = false): Result<Unit>
}

@Singleton
class DefaultCatalogRepository
@Inject
constructor(
  private val dataDragon: DataDragonService,
  private val communityDragon: CommunityDragonService,
  private val dao: CatalogDao,
  private val preferences: CatalogPreferences,
) : CatalogRepository {

  override fun observeChampions(): Flow<List<Champion>> =
    dao.observeChampions().map { champions -> champions.map { it.toDomain() } }

  override fun observeChampion(id: String): Flow<Champion?> =
    dao.observeChampion(id).map { it?.toDomain() }

  override fun observeSkins(championId: String): Flow<List<Skin>> =
    dao.observeSkins(championId).map { skins -> skins.map { it.toDomain() } }

  override fun observeCatalogVersion(): Flow<String?> = preferences.catalogVersion

  override suspend fun refresh(force: Boolean): Result<Unit> =
    try {
      val versions = dataDragon.versions()
      val latest = versions.firstOrNull() ?: error("Data Dragon no devolvió versiones")

      val storedVersion = preferences.catalogVersion.first()
      val alreadySynced = storedVersion == latest && dao.countChampions() > 0
      if (!force && alreadySynced) {
        Result.success(Unit)
      } else {
        val championDtos = dataDragon.champions(latest).data.values
        val cdragonSkins = communityDragon.skins()
        val skinLineNames =
          runCatching { communityDragon.skinLines() }
            .getOrDefault(emptyList())
            .associate { it.id to it.name }

        val champions = championDtos.map { it.toEntity(latest) }
        val skins = buildSkinEntities(champions, cdragonSkins, skinLineNames)

        dao.replaceCatalog(champions, skins)
        preferences.setCatalogVersion(latest, System.currentTimeMillis())
        Result.success(Unit)
      }
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (error: Exception) {
      Result.failure(error)
    }
}
