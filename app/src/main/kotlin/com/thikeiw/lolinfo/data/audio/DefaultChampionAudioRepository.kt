package com.thikeiw.lolinfo.data.audio

import com.thikeiw.lolinfo.data.local.CatalogLocales
import com.thikeiw.lolinfo.data.remote.LolSoundAudioService
import com.thikeiw.lolinfo.data.remote.dto.LolSoundChampionDetailDto
import com.thikeiw.lolinfo.data.remote.dto.LolSoundSkinDto
import com.thikeiw.lolinfo.domain.model.ChampionAudioCatalog
import java.util.Collections
import java.util.LinkedHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Singleton
class DefaultChampionAudioRepository @Inject constructor(
  private val service: LolSoundAudioService,
  private val source: ChampionAudioChampionSource,
  private val localeSource: ChampionAudioLocaleSource,
  private val builder: LolSoundAudioCatalogBuilder,
) : ChampionAudioRepository {

  private val catalogCache: MutableMap<String, ChampionAudioCatalog> =
    Collections.synchronizedMap(
      object : LinkedHashMap<String, ChampionAudioCatalog>(24, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ChampionAudioCatalog>?): Boolean =
          size > 48
      },
    )

  private val detailCache: MutableMap<String, LolSoundChampionDetailDto> =
    Collections.synchronizedMap(
      object : LinkedHashMap<String, LolSoundChampionDetailDto>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, LolSoundChampionDetailDto>?): Boolean =
          size > 24
      },
    )

  override suspend fun load(championId: String, skinNum: Int): Result<ChampionAudioCatalog> =
    try {
      val champion = source.champion(championId) ?: error("Champion not found: $championId")
      val locale = mapLocale(localeSource.locale().ifBlank { CatalogLocales.ES_MX })
      val cacheKey = "${champion.version}:${champion.id}:$locale:$skinNum"
      catalogCache[cacheKey]?.let { return Result.success(it) }

      val detail = resolveChampionDetail(champion.id, champion.name)
      val baseSkin = detail.skins.firstOrNull { it.isBaseSkin || it.skinNum == 0 } ?: detail.skins.firstOrNull()
      val selectedSkins = resolveSkinsForNum(detail.skins, skinNum)
      val primarySelected = selectedSkins.firstOrNull()

      val (baseLines, skinForms) =
        coroutineScope {
          val baseDeferred =
            async {
              if (baseSkin == null) emptyList()
              else fetchLines(detail.code, baseSkin.code, locale)
            }
          val skinDeferred =
            async {
              if (skinNum == 0 || selectedSkins.isEmpty()) emptyList<LolSoundSkinForm>()
              else selectedSkins
                .filter { it.code != baseSkin?.code }
                .mapNotNull { skin ->
                  val lines = fetchLines(detail.code, skin.code, locale)
                  if (lines.isEmpty()) null
                  else LolSoundSkinForm(id = skin.code, label = formLabelFor(skin, selectedSkins, skinNum), lines = lines)
                }
            }
          baseDeferred.await() to skinDeferred.await()
        }

      val catalog =
        builder.build(
          championId = champion.id,
          championName = champion.name,
          selectedSkinNum = skinNum,
          selectedSkinName = primarySelected?.name?.takeIf { !it.matches(Regex(".*_skin\\d+$", RegexOption.IGNORE_CASE)) } ?: sourceSkinName(championId, skinNum) ?: champion.name,
          baseLines = baseLines,
          skinForms = skinForms,
        )
      catalogCache[cacheKey] = catalog
      Result.success(catalog)
    } catch (error: CancellationException) {
      throw error
    } catch (error: Exception) {
      Result.failure(error)
    }

  override fun clearMemory() {
    catalogCache.clear()
    detailCache.clear()
  }

  private suspend fun fetchLines(championCode: String, skinCode: String, locale: String): List<LolSoundMappedLine> =
    try {
      service.sounds(championCode, skinCode, locale).mapNotNull { dto ->
        val url = dto.url.trim()
        if (url.isBlank()) return@mapNotNull null
        val rawName = dto.name.trim().ifBlank { "sound-${dto.id}" }
        val label = dto.label.trim()
        val display = label.ifBlank { rawName.substringAfterLast('/').substringBeforeLast('.') }
        if (display.isBlank() || display.all { it.isDigit() }) return@mapNotNull null
        LolSoundMappedLine(
          id = dto.id,
          name = rawName,
          label = label,
          section = normalizeSection(dto.category),
          url = url,
        )
      }
    } catch (error: Exception) {
      if (error is CancellationException) throw error
      emptyList()
    }

  private suspend fun resolveChampionDetail(championId: String, championName: String): LolSoundChampionDetailDto {
    val key = championId.lowercase()
    detailCache[key]?.let { return it }
    val candidates = championCodes(championId, championName)
    for (code in candidates) {
      try {
        val detail = service.champion(code)
        detailCache[key] = detail
        return detail
      } catch (error: Exception) {
        if (error is CancellationException) throw error
      }
    }
    val all = try { service.champions() } catch (error: Exception) { if (error is CancellationException) throw error; emptyList() }
    val normalizedId = normalize(championId)
    val normalizedName = normalize(championName)
    val match =
      all.firstOrNull { normalize(it.code) == normalizedId || normalize(it.name) == normalizedName || normalize(it.alias.orEmpty()) == normalizedName }
    if (match != null) {
      val detail = service.champion(match.code)
      detailCache[key] = detail
      return detail
    }
    error("LolSound champion not found: $championId")
  }

  private fun championCodes(championId: String, championName: String): List<String> =
    buildList {
      add(championId.lowercase())
      add(normalize(championName))
      when (championId) {
        "MonkeyKing" -> add("wukong")
        "Nunu" -> add("nunu")
        "Renata" -> add("renata")
        "DrMundo" -> add("drmundo")
        "JarvanIV" -> add("jarvaniv")
        "MasterYi" -> add("masteryi")
        "MissFortune" -> add("missfortune")
        "TahmKench" -> add("tahmkench")
        "TwistedFate" -> add("twistedfate")
        "XinZhao" -> add("xinzhao")
        "AurelionSol" -> add("aurelionsol")
        "LeeSin" -> add("leesin")
        "KSante" -> add("ksante")
        "Kaisa" -> add("kaisa")
        "Khazix" -> add("khazix")
        "Chogath" -> add("chogath")
        "KogMaw" -> add("kogmaw")
        "RekSai" -> add("reksai")
        "Velkoz" -> add("velkoz")
        "Belveth" -> add("belveth")
        "LeBlanc" -> add("leblanc")
      }
    }.map { it.trim().lowercase() }.filter { it.isNotBlank() }.distinct()

  private fun resolveSkinsForNum(skins: List<LolSoundSkinDto>, skinNum: Int): List<LolSoundSkinDto> {
    if (skinNum == 0) return skins.filter { it.isBaseSkin || it.skinNum == 0 }.take(1)
    val exact = skins.filter { it.skinNum == skinNum }
    if (exact.isNotEmpty()) return exact
    // Known app-num -> lolsound-skinNum mismatches/aliases.
    val aliasNums = when {
      skins.any { it.code.startsWith("seraphine_skin") } && skinNum == 1 -> listOf(1, 2, 3)
      skins.any { it.code.startsWith("ahri_skin") } && skinNum == 85 -> listOf(86)
      skins.any { it.code.startsWith("kaisa_skin") } && skinNum == 70 -> listOf(71)
      else -> emptyList()
    }
    if (aliasNums.isNotEmpty()) {
      val aliased = skins.filter { it.skinNum in aliasNums }.sortedBy { it.skinNum }
      if (aliased.isNotEmpty()) return aliased
    }
    return emptyList()
  }

  private fun formLabelFor(skin: LolSoundSkinDto, selected: List<LolSoundSkinDto>, requestedSkinNum: Int): String? {
    if (selected.size <= 1) return null
    if (skin.code.startsWith("seraphine_skin")) {
      return when (skin.skinNum) {
        1 -> "Indie"
        2 -> "Rising Star"
        3 -> "Superstar"
        else -> skin.name
      }
    }
    return skin.name.takeIf { it.isNotBlank() && !it.matches(Regex(".*_skin\\d+$", RegexOption.IGNORE_CASE)) }
      ?: "Form ${skin.skinNum}"
  }

  private suspend fun sourceSkinName(championId: String, skinNum: Int): String? =
    if (skinNum == 0) source.champion(championId)?.name
    else source.skins(championId).firstOrNull { it.num == skinNum }?.name

  private fun mapLocale(locale: String): String =
    if (locale.equals(CatalogLocales.ES_MX, ignoreCase = true)) "es_MX" else "en_US"

  private fun normalizeSection(category: String): String {
    val value = category.trim().ifBlank { "Other" }
    return when (value.lowercase()) {
      "attack" -> "Attack"
      "movement" -> "Movement"
      "abilities" -> "Abilities"
      "death / respawn", "death" -> "Death / Respawn"
      "taunt / joke", "taunt", "joke" -> "Taunt / Joke"
      "interactions" -> "Interactions"
      "kills and objectives" -> "Kills and Objectives"
      "other" -> "Other"
      else -> value
    }
  }

  private fun normalize(value: String): String = value.lowercase().replace(Regex("[^a-z0-9]"), "")
}
