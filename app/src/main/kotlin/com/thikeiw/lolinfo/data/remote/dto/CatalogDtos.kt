package com.thikeiw.lolinfo.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChampionListResponse(val data: Map<String, ChampionSummaryDto> = emptyMap())

@Serializable
data class ChampionSummaryDto(
  val id: String,
  val key: String,
  val name: String,
  val title: String = "",
  val blurb: String = "",
  val info: ChampionInfoDto = ChampionInfoDto(),
  val tags: List<String> = emptyList(),
  val partype: String = "",
)

@Serializable
data class ChampionInfoDto(
  val attack: Int = 0,
  val defense: Int = 0,
  val magic: Int = 0,
  val difficulty: Int = 0,
)

@Serializable
data class ChampionDetailResponse(val data: Map<String, ChampionDetailDto> = emptyMap())

@Serializable
data class ChampionDetailDto(
  val id: String,
  val lore: String = "",
  val skins: List<ChampionSkinDto> = emptyList(),
)

@Serializable
data class ChampionSkinDto(val num: Int, val name: String)

@Serializable
data class CdragonSkinDto(
  val id: Int,
  val name: String = "",
  val isBase: Boolean = false,
  val skinClassification: String = "",
  val rarity: String = "",
  val isLegacy: Boolean = false,
  val skinLines: List<CdragonSkinLineRef>? = null,
)

@Serializable data class CdragonSkinLineRef(val id: Int = 0)

@Serializable data class CdragonSkinLineDto(val id: Int = 0, val name: String = "")
