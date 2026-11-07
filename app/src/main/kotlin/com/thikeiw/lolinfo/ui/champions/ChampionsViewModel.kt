package com.thikeiw.lolinfo.ui.champions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thikeiw.lolinfo.data.CatalogRepository
import com.thikeiw.lolinfo.data.local.CatalogLocales
import com.thikeiw.lolinfo.domain.model.Champion
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChampionsUiState(
  val champions: List<Champion> = emptyList(),
  val totalChampions: Int = 0,
  val query: String = "",
  val version: String? = null,
  val isRefreshing: Boolean = false,
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
)

@HiltViewModel
class ChampionsViewModel @Inject constructor(private val repository: CatalogRepository) :
  ViewModel() {

  private val query = MutableStateFlow("")
  private val isRefreshing = MutableStateFlow(false)
  private val errorMessage = MutableStateFlow<String?>(null)

  val uiState: StateFlow<ChampionsUiState> =
    combine(
        repository.observeChampions(),
        query,
        isRefreshing,
        errorMessage,
        repository.observeCatalogVersion(),
      ) { champions, currentQuery, refreshing, error, version ->
        val filtered =
          if (currentQuery.isBlank()) champions
          else
            champions.filter {
              it.name.contains(currentQuery, ignoreCase = true) ||
                it.id.contains(currentQuery, ignoreCase = true)
            }

        ChampionsUiState(
          champions = filtered,
          totalChampions = champions.size,
          query = currentQuery,
          version = version,
          isRefreshing = refreshing,
          isLoading = champions.isEmpty() && refreshing,
          errorMessage = error,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChampionsUiState(),
      )

  init {
    refresh(force = false)
  }

  /** Idioma actual del catálogo para el selector de la top bar. */
  val locale: StateFlow<String> =
    repository
      .observeLocale()
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CatalogLocales.ES_MX,
      )

  /** Cambia el idioma del catálogo (la UI se aplica en ChampionsScreen con recreación). */
  suspend fun tryChangeLocale(newLocale: String): Boolean {
    val current = repository.observeLocale().first()
    if (current == newLocale) return false
    repository.setLocale(newLocale)
    repository.refresh(force = true)
    return true
  }

  fun onQueryChange(value: String) {
    query.value = value
  }

  fun refresh(force: Boolean = true) {
    if (isRefreshing.value) return

    viewModelScope.launch {
      isRefreshing.value = true
      errorMessage.value = null
      try {
        repository.refresh(force).onFailure { errorMessage.value = it.toUserMessage() }
      } catch (cancellation: CancellationException) {
        throw cancellation
      } finally {
        isRefreshing.value = false
      }
    }
  }
}

internal fun Throwable.toUserMessage(): String =
  message?.takeIf { it.isNotBlank() } ?: "No se pudo actualizar el catálogo"
