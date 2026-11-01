package com.thikeiw.lolinfo.domain.model

data class Champion(
  val id: String,
  val key: Int,
  val name: String,
  val title: String,
  val blurb: String,
  val tags: List<String>,
  val partype: String,
  val attack: Int,
  val defense: Int,
  val magic: Int,
  val difficulty: Int,
  val imageUrl: String,
)

data class Skin(
  val id: Int,
  val championId: String,
  val num: Int,
  val name: String,
  val rarity: String?,
  val isLegacy: Boolean,
  val skinLines: List<String>,
  val splashUrl: String,
  val loadingUrl: String,
) {
  val isBase: Boolean get() = num == 0
}
