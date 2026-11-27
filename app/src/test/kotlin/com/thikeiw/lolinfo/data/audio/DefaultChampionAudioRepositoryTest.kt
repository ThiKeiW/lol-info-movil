package com.thikeiw.lolinfo.data.audio

import com.thikeiw.lolinfo.data.remote.LolSoundAudioService
import com.thikeiw.lolinfo.data.remote.dto.LolSoundChampionDetailDto
import com.thikeiw.lolinfo.data.remote.dto.LolSoundChampionDto
import com.thikeiw.lolinfo.data.remote.dto.LolSoundLineDto
import com.thikeiw.lolinfo.data.remote.dto.LolSoundSkinDto
import com.thikeiw.lolinfo.domain.model.Skin
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultChampionAudioRepositoryTest {
  @Test
  fun `loads base and selected skin separately`() = runTest {
    val service = FakeLolSoundService()
    val repo = DefaultChampionAudioRepository(service, FakeChampionSource(), FakeLocaleSource("es_MX"), LolSoundAudioCatalogBuilder())

    val result = repo.load("Akali", 61)

    assertTrue(result.isSuccess)
    val catalog = result.getOrThrow()
    assertEquals(setOf(0, 61), catalog.groups.keys)
    assertEquals("es_MX", service.lastLanguage)
    assertTrue(service.requestedSkins.contains("akali_base"))
    assertTrue(service.requestedSkins.contains("akali_skin61"))
  }

  @Test
  fun `skin without own lines does not fall back to base group`() = runTest {
    val service = FakeLolSoundService(skinLines = emptyList())
    val repo = DefaultChampionAudioRepository(service, FakeChampionSource(), FakeLocaleSource("en_US"), LolSoundAudioCatalogBuilder())

    val catalog = repo.load("Akali", 61).getOrThrow()

    assertTrue(catalog.groups.containsKey(0))
    assertFalse(catalog.groups.containsKey(61))
  }

  private class FakeLolSoundService(val skinLines: List<LolSoundLineDto> = listOf(LolSoundLineDto(2, "s.m4a", "Skin", "Attack", "https://lolsound.com/s.m4a"))) : LolSoundAudioService {
    val requestedSkins = mutableListOf<String>()
    var lastLanguage: String? = null
    override suspend fun champions(): List<LolSoundChampionDto> = listOf(LolSoundChampionDto(1, "akali", "Akali", "Akali"))
    override suspend fun champion(code: String): LolSoundChampionDetailDto = LolSoundChampionDetailDto(1, "akali", "Akali", "Akali", listOf(LolSoundSkinDto(10, "akali_base", "Akali", true, 0), LolSoundSkinDto(11, "akali_skin61", "Star Guardian Akali", false, 61)))
    override suspend fun sounds(code: String, skinCode: String, language: String): List<LolSoundLineDto> {
      requestedSkins += skinCode
      lastLanguage = language
      return if (skinCode == "akali_base") listOf(LolSoundLineDto(1, "b.m4a", "Base", "Attack", "https://lolsound.com/b.m4a")) else skinLines
    }
  }

  private class FakeChampionSource : ChampionAudioChampionSource {
    override suspend fun champion(id: String) = ChampionAudioChampion(id, 84, "Akali", "15.1.1")
    override suspend fun skins(championId: String) = listOf(Skin(id=84000, championId="Akali", num=0, name="Akali", rarity="kNoRarity", isLegacy=false, skinLines=emptyList(), splashUrl="", loadingUrl="", championName="Akali"), Skin(id=84061, championId="Akali", num=61, name="Star Guardian Akali", rarity="kEpic", isLegacy=false, skinLines=emptyList(), splashUrl="", loadingUrl="", championName="Akali"))
  }

  private class FakeLocaleSource(private val locale: String) : ChampionAudioLocaleSource {
    override suspend fun locale(): String = locale
  }
}
