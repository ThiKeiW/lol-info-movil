package com.thikeiw.lolinfo.ui.championaudio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thikeiw.lolinfo.data.audio.ChampionAudioRepository
import com.thikeiw.lolinfo.domain.model.AudioForm
import com.thikeiw.lolinfo.domain.model.AudioLine
import com.thikeiw.lolinfo.domain.model.AudioSection
import com.thikeiw.lolinfo.domain.model.ChampionAudioCatalog
import com.thikeiw.lolinfo.domain.model.SkinAudioGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AudioPlaybackStatus {
  IDLE,
  PREPARING,
  PLAYING,
  PAUSED,
  COMPLETED,
  ERROR,
}

data class ChampionAudioUiState(
  val isLoading: Boolean = false,
  val loadError: Boolean = false,
  val catalog: ChampionAudioCatalog? = null,
  val selectedSkinNum: Int = 0,
  val selectedSkinName: String = "",
  val selectedFormId: String? = null,
  val selectedSection: String? = null,
  val currentIndex: Int = -1,
  val playbackStatus: AudioPlaybackStatus = AudioPlaybackStatus.IDLE,
  val positionMs: Long = 0,
  val durationMs: Long = 0,
) {
  val selectedGroup: SkinAudioGroup?
    get() = catalog?.groupFor(selectedSkinNum)

  val isFallback: Boolean
    get() = catalog != null && selectedGroup?.skinNum != selectedSkinNum

  val selectedForm: AudioForm?
    get() =
      selectedGroup?.forms?.firstOrNull { it.id == selectedFormId } ?: selectedGroup?.forms?.firstOrNull()

  val sections: List<AudioSection>
    get() = selectedForm?.sections.orEmpty()

  val lines: List<AudioLine>
    get() =
      if (selectedSection == null) sections.flatMap { it.lines }
      else sections.firstOrNull { it.title == selectedSection }?.lines.orEmpty()

  val currentLine: AudioLine?
    get() = lines.getOrNull(currentIndex)
}

@HiltViewModel
class ChampionAudioViewModel @Inject constructor(
  private val repository: ChampionAudioRepository,
  private val player: AudioPlayer,
) : ViewModel() {

  private val _uiState = MutableStateFlow(ChampionAudioUiState())
  val uiState: StateFlow<ChampionAudioUiState> = _uiState.asStateFlow()

  private var loadedChampionId: String? = null
  private var loadJob: Job? = null
  private val failedLineIds = mutableSetOf<String>()

  private val listener =
    object : AudioPlayer.Listener {
      override fun onPrepared(durationMs: Long) {
        _uiState.update {
          it.copy(
            playbackStatus = AudioPlaybackStatus.PLAYING,
            durationMs = durationMs.coerceAtLeast(0),
          )
        }
      }

      override fun onProgress(positionMs: Long, durationMs: Long) {
        _uiState.update {
          if (it.playbackStatus == AudioPlaybackStatus.PLAYING ||
            it.playbackStatus == AudioPlaybackStatus.PAUSED
          ) {
            it.copy(
              positionMs = positionMs.coerceAtLeast(0),
              durationMs = durationMs.coerceAtLeast(0),
            )
          } else {
            it
          }
        }
      }

      override fun onCompletion() {
        val state = _uiState.value
        val nextIndex = nextPlayableIndex(state.currentIndex + 1)
        if (nextIndex != null) {
          playAt(nextIndex)
        } else {
          _uiState.update {
            it.copy(
              playbackStatus = AudioPlaybackStatus.COMPLETED,
              positionMs = it.durationMs,
            )
          }
        }
      }

      override fun onError() {
        val state = _uiState.value
        val current = state.currentLine
        if (current != null) failedLineIds += current.id
        val nextIndex = nextPlayableIndex(state.currentIndex + 1)
        if (nextIndex != null) {
          playAt(nextIndex)
        } else {
          _uiState.update { it.copy(playbackStatus = AudioPlaybackStatus.ERROR) }
        }
      }
    }

  init {
    player.setListener(listener)
  }

  fun load(championId: String, skinNum: Int, skinName: String) {
    player.setListener(listener)
    val current = _uiState.value
    if (loadedChampionId == championId && current.catalog != null && !current.loadError && current.selectedSkinNum == skinNum) {
      return
    }
    loadedChampionId = championId
    loadJob?.cancel()
    loadJob =
      viewModelScope.launch {
        stopPlayback(resetIndex = true)
        _uiState.update {
          ChampionAudioUiState(
            isLoading = true,
            selectedSkinNum = skinNum,
            selectedSkinName = skinName,
          )
        }
        repository.load(championId, skinNum).fold(
          onSuccess = { catalog ->
            _uiState.update {
              stateForCatalog(catalog = catalog, skinNum = skinNum, skinName = skinName)
            }
          },
          onFailure = {
            _uiState.update {
              it.copy(isLoading = false, loadError = true, catalog = null)
            }
          },
        )
      }
  }

  fun retry(championId: String, skinNum: Int, skinName: String) {
    loadedChampionId = null
    repository.clearMemory()
    load(championId, skinNum, skinName)
  }

  fun selectSkin(skinNum: Int, skinName: String) {
    val state = _uiState.value
    if (state.selectedSkinNum == skinNum) return
    load(state.catalog?.championId ?: loadedChampionId ?: return, skinNum, skinName)
  }

  fun selectForm(formId: String) {
    val state = _uiState.value
    if (state.selectedFormId == formId) return
    stopPlayback(resetIndex = true)
    _uiState.update { it.copy(selectedFormId = formId, selectedSection = null, currentIndex = -1) }
  }

  fun selectSection(section: String?) {
    val state = _uiState.value
    if (state.selectedSection == section) return
    stopPlayback(resetIndex = true)
    _uiState.update { it.copy(selectedSection = section, currentIndex = -1) }
  }

  fun playLine(line: AudioLine) {
    val index = _uiState.value.lines.indexOfFirst { it.id == line.id }
    if (index >= 0) playAt(index)
  }

  fun togglePlayPause() {
    val state = _uiState.value
    when (state.playbackStatus) {
      AudioPlaybackStatus.PLAYING -> {
        player.pause()
        _uiState.update { it.copy(playbackStatus = AudioPlaybackStatus.PAUSED) }
      }
      AudioPlaybackStatus.PAUSED -> {
        player.resume()
        _uiState.update { it.copy(playbackStatus = AudioPlaybackStatus.PLAYING) }
      }
      AudioPlaybackStatus.PREPARING -> Unit
      AudioPlaybackStatus.ERROR,
      AudioPlaybackStatus.COMPLETED,
      AudioPlaybackStatus.IDLE -> {
        val index = if (state.currentIndex in state.lines.indices) state.currentIndex else 0
        playAt(index)
      }
    }
  }

  fun next() {
    val nextIndex = nextPlayableIndex(_uiState.value.currentIndex + 1) ?: return
    playAt(nextIndex)
  }

  fun previous() {
    val prevIndex = previousPlayableIndex(_uiState.value.currentIndex - 1) ?: return
    playAt(prevIndex)
  }

  fun seekTo(positionMs: Long) {
    player.seekTo(positionMs)
    _uiState.update { it.copy(positionMs = positionMs.coerceAtLeast(0)) }
  }

  fun stop() {
    stopPlayback(resetIndex = true)
  }

  private fun playAt(index: Int) {
    val state = _uiState.value
    val line = state.lines.getOrNull(index) ?: return
    if (line.id in failedLineIds) {
      val next = nextPlayableIndex(index + 1)
      if (next != null) playAt(next) else _uiState.update { it.copy(playbackStatus = AudioPlaybackStatus.ERROR) }
      return
    }
    player.play(line.urls)
    _uiState.update {
      it.copy(
        currentIndex = index,
        playbackStatus = AudioPlaybackStatus.PREPARING,
        positionMs = 0,
        durationMs = 0,
      )
    }
  }

  private fun nextPlayableIndex(start: Int): Int? {
    val lines = _uiState.value.lines
    for (i in start until lines.size) if (lines[i].id !in failedLineIds) return i
    return null
  }

  private fun previousPlayableIndex(start: Int): Int? {
    val lines = _uiState.value.lines
    for (i in start downTo 0) if (lines[i].id !in failedLineIds) return i
    return null
  }

  private fun stopPlayback(resetIndex: Boolean) {
    player.stop()
    _uiState.update {
      it.copy(
        currentIndex = if (resetIndex) -1 else it.currentIndex,
        playbackStatus = AudioPlaybackStatus.IDLE,
        positionMs = 0,
        durationMs = 0,
      )
    }
  }

  private fun stateForCatalog(
    catalog: ChampionAudioCatalog,
    skinNum: Int,
    skinName: String,
  ): ChampionAudioUiState {
    failedLineIds.clear()
    val group = catalog.groupFor(skinNum)
    val form = group?.forms?.firstOrNull()
    return ChampionAudioUiState(
      isLoading = false,
      loadError = false,
      catalog = catalog,
      selectedSkinNum = skinNum,
      selectedSkinName = skinName,
      selectedFormId = form?.id,
      selectedSection = null,
      currentIndex = -1,
      playbackStatus = AudioPlaybackStatus.IDLE,
    )
  }

  override fun onCleared() {
    player.setListener(null)
    player.stop()
    super.onCleared()
  }
}
