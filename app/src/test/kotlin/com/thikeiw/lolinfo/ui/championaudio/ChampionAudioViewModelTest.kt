package com.thikeiw.lolinfo.ui.championaudio

import com.thikeiw.lolinfo.data.audio.ChampionAudioRepository
import com.thikeiw.lolinfo.domain.model.AudioForm
import com.thikeiw.lolinfo.domain.model.AudioLine
import com.thikeiw.lolinfo.domain.model.AudioSection
import com.thikeiw.lolinfo.domain.model.ChampionAudioCatalog
import com.thikeiw.lolinfo.domain.model.SkinAudioGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChampionAudioViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @Before fun setUp() { Dispatchers.setMain(dispatcher) }
  @After fun tearDown() { Dispatchers.resetMain() }

  @Test
  fun `load selects requested skin group`() = runTest(dispatcher) {
    val line = AudioLine("1", "Aatrox Select.ogg", listOf("u"), "Pick", "Champion Select")
    val catalog = ChampionAudioCatalog(
      championId = "Aatrox",
      championName = "Aatrox",
      pageTitle = "Aatrox/LoL/Audio",
      hasPage = true,
      groups = mapOf(0 to SkinAudioGroup(0, "Aatrox", listOf(AudioForm("base", null, listOf(AudioSection("Champion Select", listOf(line))))))),
    )
    val vm = ChampionAudioViewModel(FakeAudioRepo(catalog), FakePlayer())

    vm.load("Aatrox", 0, "Aatrox")
    advanceUntilIdle()

    assertEquals(false, vm.uiState.value.isLoading)
    assertEquals(0, vm.uiState.value.selectedSkinNum)
  }

  private class FakeAudioRepo(private val catalog: ChampionAudioCatalog) : ChampionAudioRepository {
    override suspend fun load(championId: String, skinNum: Int) = Result.success(catalog)
    override fun clearMemory() {}
  }

  private class FakePlayer : AudioPlayer {
    override fun setListener(listener: AudioPlayer.Listener?) {}
    override fun play(urls: List<String>, startPositionMs: Long) {}
    override fun pause() {}
    override fun resume() {}
    override fun seekTo(positionMs: Long) {}
    override fun stop() {}
    override fun release() {}
  }
}
