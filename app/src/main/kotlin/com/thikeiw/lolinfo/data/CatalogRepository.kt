package com.thikeiw.lolinfo.data

import com.thikeiw.lolinfo.data.local.CatalogDao
import com.thikeiw.lolinfo.data.local.CatalogLocales
import com.thikeiw.lolinfo.data.local.CatalogPreferences
import com.thikeiw.lolinfo.data.local.SkinPriceCatalog
import com.thikeiw.lolinfo.data.mappers.buildSkinEntities
import com.thikeiw.lolinfo.data.mappers.toDomain
import com.thikeiw.lolinfo.data.mappers.toEntity
import com.thikeiw.lolinfo.data.remote.CommunityDragonService
import com.thikeiw.lolinfo.data.remote.DataDragonService
import com.thikeiw.lolinfo.domain.model.Champion
import com.thikeiw.lolinfo.domain.model.Skin
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

interface CatalogRepository {
  fun observeChampions(): Flow<List<Champion>>

  fun observeChampion(id: String): Flow<Champion?>

  fun observeSkins(championId: String): Flow<List<Skin>>

  fun observeCatalogVersion(): Flow<String?>

  /** Idioma elegido por el usuario para el contenido del catálogo. */
  fun observeLocale(): Flow<String>

  suspend fun setLocale(locale: String)

  /**
   * Nombre en inglés de una skin (Data Dragon) para búsquedas de video en YouTube, donde el
   * canal de SkinSpotlights nomina siempre en inglés. Devuelve null si no se pudo obtener.
   */
  suspend fun englishSkinName(championId: String, skinNum: Int): String?

  suspend fun refresh(force: Boolean = false): Result<Unit>

  /** Descarga el lore completo del campeón (Data Dragon) y lo guarda en Room. */
  suspend fun fetchChampionLore(championId: String): Result<String>
}

@Singleton
class DefaultCatalogRepository
@Inject
constructor(
  private val dataDragon: DataDragonService,
  private val communityDragon: CommunityDragonService,
  private val dao: CatalogDao,
  private val preferences: CatalogPreferences,
  private val skinPrices: SkinPriceCatalog,
) : CatalogRepository {

  override fun observeChampions(): Flow<List<Champion>> =
    dao.observeChampions().map { champions -> champions.map { it.toDomain() } }

  override fun observeChampion(id: String): Flow<Champion?> =
    dao.observeChampion(id).map { it?.toDomain() }

  override fun observeSkins(championId: String): Flow<List<Skin>> =
    dao.observeSkins(championId).map { skins -> skins.map { it.toDomain() } }

  override fun observeCatalogVersion(): Flow<String?> = preferences.catalogVersion

  override fun observeLocale(): Flow<String> = preferences.catalogLocale

  override suspend fun setLocale(locale: String) = preferences.setLocale(locale)

  private val englishSkinNameCache = ConcurrentHashMap<String, Map<Int, String>>()

  private val prefetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var prefetchJob: Job? = null

  override suspend fun englishSkinName(championId: String, skinNum: Int): String? =
    try {
      val names =
        englishSkinNameCache.getOrPut(championId) {
          val version = dao.getChampion(championId)?.version ?: return null
          dataDragon
            .championDetail(version, CatalogLocales.EN_US.toDdragonLocale(), championId)
            .data.values
            .firstOrNull()
            ?.skins
            .orEmpty()
            .associate { skin -> skin.num to skin.name }
        }
      names[skinNum]?.takeIf { it.isNotBlank() }
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (error: Exception) {
      null
    }

  override suspend fun refresh(force: Boolean): Result<Unit> =
    try {
      val versions = dataDragon.versions()
      val latest = versions.firstOrNull() ?: error("Data Dragon no devolvió versiones")

      val locale = preferences.catalogLocale.first()
      val storedVersion = preferences.catalogVersion.first()
      val storedSchema = preferences.catalogSchema.first()
      val storedLocale = preferences.catalogSyncedLocale.first()
      val alreadySynced =
        storedVersion == latest &&
          storedSchema == CatalogPreferences.CATALOG_SCHEMA &&
          storedLocale == locale &&
          dao.countChampions() > 0
      if (!force && alreadySynced) {
        Result.success(Unit)
      } else {
        val championDtos = dataDragon.champions(latest, locale.toDdragonLocale()).data.values
        val cdragonLocale =
          if (locale == CatalogLocales.EN_US) CatalogLocales.COMMUNITY_DRAGON_DEFAULT else locale
        val cdragonSkins = communityDragon.skins(cdragonLocale)
        val skinLineNames =
          runCatching { communityDragon.skinLines(cdragonLocale) }
            .getOrDefault(emptyList())
            .associate { it.id to it.name }

        val champions = championDtos.map { it.toEntity(latest) }
        val skins = buildSkinEntities(champions, cdragonSkins, skinLineNames, skinPrices.prices())

        dao.replaceCatalog(champions, skins)
        preferences.setCatalogVersion(latest, System.currentTimeMillis(), locale)
        prefetchLore(latest, locale.toDdragonLocale())
        return Result.success(Unit)
      }
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (error: Exception) {
      Result.failure(error)
    }

  override suspend fun fetchChampionLore(championId: String): Result<String> =
    try {
      val entity = dao.getChampion(championId) ?: error("Campeón no sincronizado: $championId")
      val detail =
        dataDragon.championDetail(entity.version, preferences.catalogLocale.first().toDdragonLocale(), entity.id)
      val lore = detail.data.values.firstOrNull()?.lore.orEmpty()
      if (lore.isNotBlank()) {
        dao.updateLore(entity.id, lore)
        Result.success(lore)
      } else {
        Result.failure(IllegalStateException("Data Dragon no devuelve lore para $championId"))
      }
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (error: Exception) {
      Result.failure(error)
    }

  /**
   * Después de sincronizar, descarga el lore completo de todos los campeones en segundo plano
   * (Data Dragon por Campeón); el re-sincronizado borra el lore guardado, así que la descripción
   * en detalle usaba el blurb corto terminado en "…" hasta visitar cada campeón.
   */
  private fun prefetchLore(
    version: String,
    ddragonLocale: String,
  ) {
    if (prefetchJob?.isActive == true) return
    prefetchJob =
      prefetchScope.launch {
        val champions = dao.observeChampions().first()
        for (champion in champions) {
          if (champion.lore.isNotBlank()) continue
          try {
            val detail = dataDragon.championDetail(version, ddragonLocale, champion.id)
            val lore = detail.data.values.firstOrNull()?.lore.orEmpty()
            if (lore.isNotBlank()) dao.updateLore(champion.id, lore)
          } catch (cancellation: CancellationException) {
            throw cancellation
          } catch (error: Exception) {
            continue
          }
        }
      }
  }
}

/** es_mx/en_us → es_MX/en_US (formato de carpeta de Data Dragon). */
private fun String.toDdragonLocale(): String =
  split("_")
    .mapIndexed { index, part -> if (index == 0) part else part.uppercase() }
    .joinToString("_")
