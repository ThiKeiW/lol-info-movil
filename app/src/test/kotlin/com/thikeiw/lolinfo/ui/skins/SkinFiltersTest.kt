package com.thikeiw.lolinfo.ui.skins

import com.thikeiw.lolinfo.domain.model.Skin
import org.junit.Assert.assertEquals
import org.junit.Test

class SkinFiltersTest {

  private fun skin(
    id: Int,
    name: String,
    championName: String,
    rarity: String? = null,
    isLegacy: Boolean = false,
  ): Skin =
    Skin(
      id = id,
      championId = championName.lowercase(),
      num = id % 1000,
      name = name,
      rarity = rarity,
      isLegacy = isLegacy,
      skinLines = emptyList(),
      splashUrl = "",
      loadingUrl = "",
      championName = championName,
    )

  private val skins =
    listOf(
      skin(103001, "Ahri Dinastía", "Ahri", rarity = "kEpic"),
      skin(103016, "Ahri Estrella Oscura", "Ahri", rarity = "kLegendary"),
      skin(266001, "Aatrox Justo", "Aatrox", rarity = "kEpic", isLegacy = true),
      skin(432001, "Karma Django", "Karma", isLegacy = true),
    )

  @Test
  fun `returns all skins when no filters are active`() {
    val result = applySkinFilters(skins, query = "", selectedRarities = emptySet(), legacyOnly = false)
    assertEquals(skins.size, result.size)
  }

  @Test
  fun `filters by skin name ignoring case`() {
    val result =
      applySkinFilters(skins, query = "estrella", selectedRarities = emptySet(), legacyOnly = false)
    assertEquals(listOf(103016), result.map { it.id })
  }

  @Test
  fun `filters by champion name ignoring case`() {
    val result =
      applySkinFilters(skins, query = "ahri", selectedRarities = emptySet(), legacyOnly = false)
    assertEquals(listOf(103001, 103016), result.map { it.id })
  }

  @Test
  fun `filters by selected rarities`() {
    val result =
      applySkinFilters(skins, query = "", selectedRarities = setOf("kLegendary"), legacyOnly = false)
    assertEquals(listOf(103016), result.map { it.id })
  }

  @Test
  fun `filters by legacy only`() {
    val result = applySkinFilters(skins, query = "", selectedRarities = emptySet(), legacyOnly = true)
    assertEquals(listOf(266001, 432001), result.map { it.id })
  }

  @Test
  fun `combines text, rarity and legacy filters`() {
    val result =
      applySkinFilters(skins, query = "aatrox", selectedRarities = setOf("kEpic"), legacyOnly = true)
    assertEquals(listOf(266001), result.map { it.id })
  }

  @Test
  fun `sorts results by champion name then skin name`() {
    val result = applySkinFilters(skins, query = "", selectedRarities = emptySet(), legacyOnly = false)
    assertEquals(listOf(266001, 103001, 103016, 432001), result.map { it.id })
  }
}
