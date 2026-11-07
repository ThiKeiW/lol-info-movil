package com.thikeiw.lolinfo.ui.championdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thikeiw.lolinfo.data.CatalogRepository
import com.thikeiw.lolinfo.domain.model.Champion
import com.thikeiw.lolinfo.domain.model.Skin
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChampionDetailUiState(
  val champion: Champion? = null,
  val skins: List<Skin> = emptyList(),
  /** Splash de la skin base (no se lista): se usa como banner del campeón. */
  val baseSplashUrl: String? = null,
  val isLoading: Boolean = true,
  val isNotFound: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChampionDetailViewModel
@Inject
constructor(
  private val repository: CatalogRepository,
) : ViewModel() {

  private val championId = MutableStateFlow<String?>(null)

  val uiState: StateFlow<ChampionDetailUiState> =
    championId
      .filterNotNull()
      .flatMapLatest { id ->
        repository
          .observeChampion(id)
          .onEach { champion -> if (champion != null && champion.lore.isBlank()) fetchLore(champion.id) }
          .combine(repository.observeSkins(id)) { champion, allSkins ->
            ChampionDetailUiState(
              champion = champion,
              // La skin base (num 0) no se lista; su splash sirve de banner.
              skins = allSkins.filterNot { it.isBase },
              baseSplashUrl = allSkins.firstOrNull { it.isBase }?.splashUrl,
              isLoading = false,
              isNotFound = champion == null,
            )
          }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChampionDetailUiState(),
      )

  private val loreFetches = mutableSetOf<String>()

  private fun fetchLore(id: String) {
    if (!loreFetches.add(id)) return
    viewModelScope.launch { repository.fetchChampionLore(id) }
  }

  /** Nombre en inglés del skin para la búsqueda de video (canal SkinSpotlights en inglés). */
  suspend fun englishSkinNameForVideo(championId: String, skinNum: Int): String? =
    runCatching { repository.englishSkinName(championId, skinNum) }.getOrNull()

  /** Resuelve el video del skin: embed de YouTube si hay resultado, o la búsqueda del canal. */

  fun load(id: String) {
    if (championId.value != id) championId.value = id
  }
}
