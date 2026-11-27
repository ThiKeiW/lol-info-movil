package com.thikeiw.lolinfo.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LolSoundChampionDto(
  val id: Int = 0,
  val code: String = "",
  val alias: String? = null,
  val name: String = "",
)

@Serializable
data class LolSoundChampionDetailDto(
  val id: Int = 0,
  val code: String = "",
  val alias: String? = null,
  val name: String = "",
  val skins: List<LolSoundSkinDto> = emptyList(),
)

@Serializable
data class LolSoundSkinDto(
  val id: Int = 0,
  val code: String = "",
  val name: String = "",
  val isBaseSkin: Boolean = false,
  val skinNum: Int = 0,
)

@Serializable
data class LolSoundLineDto(
  val id: Int = 0,
  val name: String = "",
  val label: String = "",
  val category: String = "",
  val url: String = "",
)
