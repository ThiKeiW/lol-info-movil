package com.thikeiw.lolinfo.data.mappers

import com.thikeiw.lolinfo.data.local.ChampionEntity
import com.thikeiw.lolinfo.data.local.SkinEntity
import com.thikeiw.lolinfo.data.local.SkinPrice
import com.thikeiw.lolinfo.data.remote.dto.CdragonSkinDto
import com.thikeiw.lolinfo.data.remote.dto.ChampionSummaryDto
import com.thikeiw.lolinfo.domain.model.Champion
import com.thikeiw.lolinfo.domain.model.Skin

const val DDRAGON_CDN = "https://ddragon.leagueoflegends.com/cdn"

fun championKeyOf(skinId: Int): Int = skinId / 1000

fun skinNumOf(skinId: Int): Int = skinId % 1000

fun splashUrl(championId: String, num: Int): String = "$DDRAGON_CDN/img/champion/splash/${championId}_$num.jpg"

fun loadingUrl(championId: String, num: Int): String = "$DDRAGON_CDN/img/champion/loading/${championId}_$num.jpg"

fun championImageUrl(version: String, championId: String): String =
  "$DDRAGON_CDN/$version/img/champion/$championId.png"

/**
 * Los tags y raridades de Data Dragon/CommunityDragon siempre llegan en inglés; se guardan como
 * código crudo en Room y la UI los localiza con recursos (tag_fighter, skin_rarity_*), así cambian
 * con el idioma de la app. Códigos desconocidos (kNoRarity) no se muestran.
 */
fun rarityLabel(rarity: String): String? =
  when (rarity) {
    "kLegendary",
    "kMythic",
    "kEpic",
    "kRare",
    "kTranscendent",
    "kUltimate",
    "kExalted" -> rarity
    else -> null
  }

fun ChampionSummaryDto.toEntity(version: String): ChampionEntity =
  ChampionEntity(
    id = id,
    key = key.toIntOrNull() ?: 0,
    name = name,
    title = title,
    blurb = blurb,
    lore = "",
    tags = tags,
    partype = partype,
    attack = info.attack,
    defense = info.defense,
    magic = info.magic,
    difficulty = info.difficulty,
    imageUrl = championImageUrl(version, id),
    version = version,
  )

fun ChampionEntity.toDomain(): Champion =
  Champion(
    id = id,
    key = key,
    name = name,
    title = title,
    blurb = blurb,
    lore = lore,
    tags = tags,
    partype = partype,
    attack = attack,
    defense = defense,
    magic = magic,
    difficulty = difficulty,
    imageUrl = imageUrl,
  )

fun SkinEntity.toDomain(): Skin =
  Skin(
    id = id,
    championId = championId,
    num = num,
    name = name,
    rarity = rarity,
    isLegacy = isLegacy,
    skinLines = skinLines,
    splashUrl = splashUrl,
    loadingUrl = loadingUrl,
    priceRp = priceRp,
    priceMe = priceMe,
  )

/**
 * Builds the skin catalog from CommunityDragon data. Skin ids follow Riot's convention
 * `championKey * 1000 + skinNum`, so no extra per-champion request is needed.
 */
fun buildSkinEntities(
  champions: List<ChampionEntity>,
  cdragonSkins: Map<String, CdragonSkinDto>,
  skinLineNames: Map<Int, String>,
  prices: Map<Int, SkinPrice> = emptyMap(),
): List<SkinEntity> {
  val championsByKey = champions.associateBy { it.key }

  return cdragonSkins.values
    .mapNotNull { dto ->
      val champion = championsByKey[championKeyOf(dto.id)] ?: return@mapNotNull null
      val num = skinNumOf(dto.id)
      val isRealSkin = dto.skinClassification == "kChampion"
      if (!isRealSkin && num != 0) return@mapNotNull null

      val displayName =
        if (num == 0) champion.name else dto.name.ifBlank { "${champion.name} #$num" }

      SkinEntity(
        id = dto.id,
        championId = champion.id,
        num = num,
        name = displayName,
        rarity = rarityLabel(dto.rarity),
        isLegacy = dto.isLegacy,
        skinLines =
          dto.skinLines.orEmpty()
            .mapNotNull { skinLineNames[it.id] }
            .filter { it.isNotBlank() }
            .distinct(),
        splashUrl = splashUrl(champion.id, num),
        loadingUrl = loadingUrl(champion.id, num),
        priceRp = if (num == 0) null else prices[dto.id]?.rp,
        priceMe = if (num == 0) null else prices[dto.id]?.me,
      )
    }
    .distinctBy { it.id }
    .sortedWith(compareBy({ it.championId }, { it.num }))
}
