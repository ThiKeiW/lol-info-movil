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
import kotlinx.coroutines.flow.stateIn

data class ChampionDetailUiState(
  val champion: Champion? = null,
  val skins: List<Skin> = emptyList(),
  val isLoading: Boolean = true,
  val isNotFound: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChampionDetailViewModel @Inject constructor(private val repository: CatalogRepository) :
  ViewModel() {

  private val championId = MutableStateFlow<String?>(null)

  val uiState: StateFlow<ChampionDetailUiState> =
    championId
      .filterNotNull()
      .flatMapLatest { id ->
        combine(repository.observeChampion(id), repository.observeSkins(id)) {
          champion,
          skins ->
          ChampionDetailUiState(
            champion = champion,
            skins = skins,
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

  fun load(id: String) {
    if (championId.value != id) championId.value = id
  }
}
