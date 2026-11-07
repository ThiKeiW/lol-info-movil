package com.thikeiw.lolinfo.data.mappers

import com.thikeiw.lolinfo.data.local.ChampionEntity
import com.thikeiw.lolinfo.data.local.SkinPrice
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
  fun `keeps rarity codes raw for the ui to localize`() {
    assertEquals("kLegendary", rarityLabel("kLegendary"))
    assertEquals("kMythic", rarityLabel("kMythic"))
    assertEquals("kEpic", rarityLabel("kEpic"))
    assertNull(rarityLabel("kNoRarity"))
    assertNull(rarityLabel(""))
  }

  @Test
  fun `keeps champion tags raw for the ui to localize`() {
    val champion =
      aatrox.copy(tags = listOf("Fighter", "Tank", "Support", "Desconocido")).toDomain()

    assertEquals(listOf("Fighter", "Tank", "Support", "Desconocido"), champion.tags)
  }

  @Test
  fun `attaches rp and mythic essence prices to skins`() {
    val skins =
      buildSkinEntities(
        champions = listOf(aatrox),
        cdragonSkins =
          mapOf(
            "266000" to skin(266000, "Aatrox", "kNoRarity"),
            "266001" to skin(266001, "Aatrox Justiciero", "kEpic"),
            "266002" to skin(266002, "Aatrox Mítico", "kMythic"),
          ),
        skinLineNames = emptyMap(),
        prices =
          mapOf(
            266000 to SkinPrice(rp = 880),
            266001 to SkinPrice(rp = 1350),
            266002 to SkinPrice(me = 125),
          ),
      )

    val byId = skins.associateBy { it.id }
    assertNull(byId.getValue(266000).priceRp)
    assertEquals(1350, byId.getValue(266001).priceRp)
    assertEquals(125, byId.getValue(266002).priceMe)
  }

  @Test
  fun `builds base skin with champion name`() {
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
    assertEquals("kEpic", result.rarity)
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
