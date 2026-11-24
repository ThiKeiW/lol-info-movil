package com.thikeiw.lolinfo.domain.model

data class Champion(
  val id: String,
  val key: Int,
  val name: String,
  val title: String,
  val blurb: String,
  val lore: String = "",
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
  val priceRp: Int? = null,
  val priceMe: Int? = null,
  /** Nombre del campeón dueño de la skin (para listados globales); se completa al observar. */
  val championName: String = "",
  /** Marcas del usuario; se resuelven desde la tabla skin_flags. */
  val isFavorite: Boolean = false,
  val isOwned: Boolean = false,
) {
  val isBase: Boolean get() = num == 0
}
