package com.thikeiw.lolinfo.data.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LolSoundAudioCatalogBuilderTest {
  private val builder = LolSoundAudioCatalogBuilder()

  @Test
  fun `build keeps base and selected skin isolated`() {
    val catalog =
      builder.build(
        championId = "Akali",
        championName = "Akali",
        selectedSkinNum = 61,
        selectedSkinName = "Star Guardian Akali",
        baseLines = listOf(LolSoundMappedLine(1, "base.m4a", "Base line", "Attack", "https://lolsound.com/a.m4a")),
        skinForms = listOf(LolSoundSkinForm("akali_skin61", null, listOf(LolSoundMappedLine(2, "skin.m4a", "Skin line", "Attack", "https://lolsound.com/b.m4a")))),
      )

    assertEquals(setOf(0, 61), catalog.groups.keys)
    assertEquals("Base line", catalog.groups.getValue(0).forms[0].sections[0].lines[0].transcript)
    assertEquals("Skin line", catalog.groups.getValue(61).forms[0].sections[0].lines[0].transcript)
    assertFalse(catalog.groupFor(61)?.skinNum == 0)
  }

  @Test
  fun `build returns empty group when skin has no lines`() {
    val catalog =
      builder.build(
        championId = "Naafiri",
        championName = "Naafiri",
        selectedSkinNum = 1,
        selectedSkinName = "Soul Fighter Naafiri",
        baseLines = listOf(LolSoundMappedLine(1, "base.m4a", "Base", "Attack", "https://lolsound.com/a.m4a")),
        skinForms = emptyList(),
      )

    assertTrue(catalog.groups.containsKey(0))
    assertFalse(catalog.groups.containsKey(1))
  }
}
