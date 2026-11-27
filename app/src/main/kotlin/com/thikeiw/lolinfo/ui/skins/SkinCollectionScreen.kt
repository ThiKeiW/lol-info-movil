package com.thikeiw.lolinfo.ui.skins

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.thikeiw.lolinfo.R
import com.thikeiw.lolinfo.data.CatalogRepository
import com.thikeiw.lolinfo.domain.model.Skin
import com.thikeiw.lolinfo.ui.championaudio.ChampionAudioSheet
import com.thikeiw.lolinfo.ui.skinvideo.SkinVideoOverlay
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Filtros combinables para las vistas de colección: texto, rarezas y legado. */
fun applySkinFilters(
  skins: List<Skin>,
  query: String,
  selectedRarities: Set<String>,
  legacyOnly: Boolean,
): List<Skin> {
  val normalizedQuery = query.trim()
  return skins
    .filter { skin ->
      val matchesQuery =
        normalizedQuery.isBlank() ||
          skin.name.contains(normalizedQuery, ignoreCase = true) ||
          skin.championName.contains(normalizedQuery, ignoreCase = true)
      val matchesRarity = selectedRarities.isEmpty() || skin.rarity in selectedRarities
      val matchesLegacy = !legacyOnly || skin.isLegacy
      matchesQuery && matchesRarity && matchesLegacy
    }
    .sortedWith(compareBy({ it.championName }, { it.name }, { it.num }))
}

data class SkinCollectionUiState(
  val skins: List<Skin> = emptyList(),
  val totalSkins: Int = 0,
  val query: String = "",
  val selectedRarities: Set<String> = emptySet(),
  val legacyOnly: Boolean = false,
  val isLoading: Boolean = true,
)

/** Base compartida por las vistas de favoritos y de skins en posesión. */
abstract class SkinCollectionViewModelBase(
  private val repository: CatalogRepository,
  private val onlyFavorites: Boolean,
) : ViewModel() {

  private val query = MutableStateFlow("")
  private val selectedRarities = MutableStateFlow(emptySet<String>())
  private val legacyOnly = MutableStateFlow(false)

  val uiState: StateFlow<SkinCollectionUiState> =
    combine(repository.observeAllSkins(), query, selectedRarities, legacyOnly) {
      allSkins,
      currentQuery,
      rarities,
      legacy ->
      val marked =
        if (onlyFavorites) allSkins.filter { it.isFavorite } else allSkins.filter { it.isOwned }
      SkinCollectionUiState(
        skins = applySkinFilters(marked, currentQuery, rarities, legacy),
        totalSkins = marked.size,
        query = currentQuery,
        selectedRarities = rarities,
        legacyOnly = legacy,
        isLoading = false,
      )
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = SkinCollectionUiState(),
    )

  fun onQueryChange(value: String) {
    query.value = value
  }

  fun onRarityToggle(rarity: String) {
    selectedRarities.value =
      if (rarity in selectedRarities.value) selectedRarities.value - rarity
      else selectedRarities.value + rarity
  }

  fun onLegacyToggle() {
    legacyOnly.value = !legacyOnly.value
  }

  fun clearFilters() {
    selectedRarities.value = emptySet()
    legacyOnly.value = false
  }

  suspend fun toggleFavorite(skin: Skin) {
    repository.setSkinFavorite(skin.id, !skin.isFavorite)
  }

  suspend fun toggleOwned(skin: Skin) {
    repository.setSkinOwned(skin.id, !skin.isOwned)
  }

  /** Nombre en inglés del skin para la búsqueda de video en el canal SkinSpotlights. */
  suspend fun englishSkinNameForVideo(championId: String, skinNum: Int): String? =
    runCatching { repository.englishSkinName(championId, skinNum) }.getOrNull()
}

@HiltViewModel
class FavoriteSkinsViewModel
@Inject
constructor(repository: CatalogRepository) : SkinCollectionViewModelBase(repository, true)

@HiltViewModel
class OwnedSkinsViewModel
@Inject
constructor(repository: CatalogRepository) : SkinCollectionViewModelBase(repository, false)

@Composable
fun FavoriteSkinsScreen(
  onBack: () -> Unit,
  viewModel: FavoriteSkinsViewModel = hiltViewModel(),
) {
  val scope = rememberCoroutineScope()
  SkinCollectionScreen(
    uiState = viewModel.uiState.collectAsStateWithLifecycle().value,
    title = stringResource(R.string.favorites_title),
    emptyMessage = stringResource(R.string.favorites_empty),
    onBack = onBack,
    onQueryChange = viewModel::onQueryChange,
    onRarityToggle = viewModel::onRarityToggle,
    onLegacyToggle = viewModel::onLegacyToggle,
    onClearFilters = viewModel::clearFilters,
    onToggleFavorite = { skin -> scope.launch { viewModel.toggleFavorite(skin) } },
    onToggleOwned = { skin -> scope.launch { viewModel.toggleOwned(skin) } },
    englishSkinNameResolver = viewModel::englishSkinNameForVideo,
  )
}

@Composable
fun OwnedSkinsScreen(
  onBack: () -> Unit,
  viewModel: OwnedSkinsViewModel = hiltViewModel(),
) {
  val scope = rememberCoroutineScope()
  SkinCollectionScreen(
    uiState = viewModel.uiState.collectAsStateWithLifecycle().value,
    title = stringResource(R.string.owned_title),
    emptyMessage = stringResource(R.string.owned_empty),
    onBack = onBack,
    onQueryChange = viewModel::onQueryChange,
    onRarityToggle = viewModel::onRarityToggle,
    onLegacyToggle = viewModel::onLegacyToggle,
    onClearFilters = viewModel::clearFilters,
    onToggleFavorite = { skin -> scope.launch { viewModel.toggleFavorite(skin) } },
    onToggleOwned = { skin -> scope.launch { viewModel.toggleOwned(skin) } },
    englishSkinNameResolver = viewModel::englishSkinNameForVideo,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkinCollectionScreen(
  uiState: SkinCollectionUiState,
  title: String,
  emptyMessage: String,
  onBack: () -> Unit,
  onQueryChange: (String) -> Unit,
  onRarityToggle: (String) -> Unit,
  onLegacyToggle: () -> Unit,
  onClearFilters: () -> Unit,
  onToggleFavorite: (Skin) -> Unit,
  onToggleOwned: (Skin) -> Unit,
  englishSkinNameResolver: suspend (championId: String, skinNum: Int) -> String?,
) {
  val scope = rememberCoroutineScope()
  var activeVideoSkinId by rememberSaveable { mutableStateOf<Int?>(null) }
  var activeVideoSkinNum by rememberSaveable { mutableStateOf(0) }
  var activeVideoSkinName by rememberSaveable { mutableStateOf("") }
  var activeVideoChampionName by rememberSaveable { mutableStateOf("") }
  var activeVideoChampionId by rememberSaveable { mutableStateOf("") }

  var activeAudioSkinNum by rememberSaveable { mutableStateOf<Int?>(null) }
  var activeAudioSkinName by rememberSaveable { mutableStateOf("") }
  var activeAudioChampionId by rememberSaveable { mutableStateOf("") }
  var activeAudioChampionName by rememberSaveable { mutableStateOf("") }

  Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Column {
              Text(title)
              Text(
                text = stringResource(R.string.skins_collection_count, uiState.skins.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          },
          navigationIcon = {
            IconButton(onClick = onBack) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
              )
            }
          },
        )
      }
    ) { innerPadding ->
      Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
        OutlinedTextField(
          value = uiState.query,
          onValueChange = onQueryChange,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
          singleLine = true,
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          trailingIcon = {
            if (uiState.query.isNotEmpty()) {
              IconButton(onClick = { onQueryChange("") }) {
                Icon(
                  imageVector = Icons.Default.Clear,
                  contentDescription = stringResource(R.string.action_clear_search),
                )
              }
            }
          },
          placeholder = { Text(stringResource(R.string.skins_search_hint)) },
        )

        SkinFilterRow(
          selectedRarities = uiState.selectedRarities,
          legacyOnly = uiState.legacyOnly,
          onRarityToggle = onRarityToggle,
          onLegacyToggle = onLegacyToggle,
          onClearFilters = onClearFilters,
        )

        when {
          uiState.isLoading ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator()
            }
          uiState.skins.isEmpty() ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Text(
                text =
                  if (uiState.totalSkins == 0) emptyMessage
                  else stringResource(R.string.skins_no_results, uiState.query),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
              )
            }
          else ->
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(bottom = 24.dp),
            ) {
              items(uiState.skins, key = { it.id }) { skin ->
                SkinCard(
                  skin = skin,
                  onPlay = {
                    activeVideoSkinId = skin.id
                    activeVideoSkinNum = skin.num
                    activeVideoSkinName = skin.name
                    activeVideoChampionName = skin.championName
                    activeVideoChampionId = skin.championId
                  },
                  onAudio = {
                    activeAudioSkinNum = skin.num
                    activeAudioSkinName = skin.name
                    activeAudioChampionId = skin.championId
                    activeAudioChampionName = skin.championName
                  },
                  onToggleFavorite = { onToggleFavorite(skin) },
                  onToggleOwned = { onToggleOwned(skin) },
                )
              }
            }
        }
      }
    }

    if (activeVideoSkinId != null) {
      SkinVideoOverlay(
        championName = activeVideoChampionName,
        skinName = activeVideoSkinName,
        championId = activeVideoChampionId,
        skinNum = activeVideoSkinNum,
        englishSkinNameResolver = {
          englishSkinNameResolver(activeVideoChampionId, activeVideoSkinNum)
        },
        onDismiss = { activeVideoSkinId = null },
      )
    }

    if (activeAudioSkinNum != null) {
      ChampionAudioSheet(
        championId = activeAudioChampionId,
        skinNum = activeAudioSkinNum ?: 0,
        championName = activeAudioChampionName,
        skinName = activeAudioSkinName,
        onDismiss = { activeAudioSkinNum = null },
      )
    }
  }
}

/** Fila de chips de filtro: rarezas, legado y limpiar (solo cuando hay filtros activos). */
@Composable
private fun SkinFilterRow(
  selectedRarities: Set<String>,
  legacyOnly: Boolean,
  onRarityToggle: (String) -> Unit,
  onLegacyToggle: () -> Unit,
  onClearFilters: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    FILTERABLE_RARITIES.forEach { rarity ->
      FilterChip(
        selected = rarity in selectedRarities,
        onClick = { onRarityToggle(rarity) },
        label = { Text(LocalizedRarity(rarity)) },
      )
    }
    FilterChip(
      selected = legacyOnly,
      onClick = onLegacyToggle,
      label = { Text(stringResource(R.string.filter_legacy)) },
    )
    if (selectedRarities.isNotEmpty() || legacyOnly) {
      FilterChip(
        selected = false,
        onClick = onClearFilters,
        label = { Text(stringResource(R.string.filter_clear)) },
      )
    }
  }
}
