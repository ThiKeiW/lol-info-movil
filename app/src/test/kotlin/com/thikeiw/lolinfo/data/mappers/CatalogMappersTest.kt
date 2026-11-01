package com.thikeiw.lolinfo.data.mappers

import com.thikeiw.lolinfo.data.local.ChampionEntity
import com.thikeiw.lolinfo.data.remote.dto.CdragonSkinDto
import com.thikeiw.lolinfo.data.remote.dto.CdragonSkinLineRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogMappersTest {

  private val aatrox =
    ChampionEntity(
      id = "Aatrox",
      key = 266,
      name = "Aatrox",
      title = "la Espada Darkin",
      blurb = "",
      tags = listOf("Luchador"),
      partype = "Sangre",
      attack = 8,
      defense = 4,
      magic = 3,
      difficulty = 4,
      imageUrl = "icon",
      version = "16.21.1",
    )

  private val pyke =
    aatrox.copy(id = "Pyke", key = 555, name = "Pyke", tags = listOf("Asesino"))

  private fun skin(id: Int, name: String, rarity: String, classification: String = "kChampion") =
    CdragonSkinDto(
      id = id,
      name = name,
      skinClassification = classification,
      rarity = rarity,
      isLegacy = false,
      skinLines = listOf(CdragonSkinLineRef(33)),
    )

  @Test
  fun `splits skin id into champion key and skin number`() {
    assertEquals(266, championKeyOf(266001))
    assertEquals(1, skinNumOf(266001))
    assertEquals(266, championKeyOf(266000))
    assertEquals(0, skinNumOf(266000))
  }

  @Test
  fun `maps community dragon rarities to spanish labels`() {
    assertEquals("Legendaria", rarityLabel("kLegendary"))
    assertEquals("Mítica", rarityLabel("kMythic"))
    assertEquals("Épica", rarityLabel("kEpic"))
    assertNull(rarityLabel("kNoRarity"))
    assertNull(rarityLabel(""))
  }

  @Test
  fun `builds base skin with localized champion name`() {
    val skins =
      buildSkinEntities(
        champions = listOf(aatrox),
        cdragonSkins = mapOf("266000" to skin(266000, "Aatrox", "kNoRarity")),
        skinLineNames = mapOf(33 to "Justicieros"),
      )

    val base = skins.single()
    assertEquals("Aatrox", base.name)
    assertEquals(0, base.num)
    assertTrue(base.splashUrl.endsWith("/Aatrox_0.jpg"))
    assertNull(base.rarity)
  }

  @Test
  fun `keeps real skins with rarity and theme names`() {
    val skins =
      buildSkinEntities(
        champions = listOf(aatrox),
        cdragonSkins = mapOf("266001" to skin(266001, "Aatrox Justiciero", "kEpic")),
        skinLineNames = mapOf(33 to "Justicieros"),
      )

    val result = skins.single()
    assertEquals("Aatrox Justiciero", result.name)
    assertEquals("Épica", result.rarity)
    assertEquals(listOf("Justicieros"), result.skinLines)
  }

  @Test
  fun `drops skins that do not belong to a known champion`() {
    val skins =
      buildSkinEntities(
        champions = listOf(aatrox),
        cdragonSkins = mapOf("60001301" to skin(60001301, "Classic Annie", "kNoRarity")),
        skinLineNames = emptyMap(),
      )

    assertTrue(skins.isEmpty())
  }

  @Test
  fun `keeps generic base skins for real champions but not generic extras`() {
    val skins =
      buildSkinEntities(
        champions = listOf(pyke),
        cdragonSkins =
          mapOf(
            "555000" to skin(555000, "Pyke", "kNoRarity", classification = "kGeneric"),
            "555001" to skin(555001, "Pyke Sand", "kNoRarity", classification = "kGeneric"),
          ),
        skinLineNames = emptyMap(),
      )

    assertEquals(listOf(555000), skins.map { it.id })
  }

  @Test
  fun `does not duplicate skins with the same id`() {
    val duplicate = skin(266001, "Aatrox Justiciero", "kEpic")
    val skins =
      buildSkinEntities(
        champions = listOf(aatrox),
        cdragonSkins = mapOf("a" to duplicate, "b" to duplicate),
        skinLineNames = emptyMap(),
      )

    assertEquals(1, skins.size)
  }
}
