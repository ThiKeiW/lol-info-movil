package com.thikeiw.lolinfo.ui.championdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.thikeiw.lolinfo.R
import com.thikeiw.lolinfo.domain.model.Champion
import com.thikeiw.lolinfo.domain.model.Skin
import com.thikeiw.lolinfo.ui.skinvideo.SkinVideoOverlay
import com.thikeiw.lolinfo.ui.theme.LoLSkinScopeTheme

@Composable
fun ChampionDetailScreen(
  championId: String,
  onBack: () -> Unit,
  viewModel: ChampionDetailViewModel = hiltViewModel(),
) {
  LaunchedEffect(championId) { viewModel.load(championId) }
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  ChampionDetailContent(uiState = uiState, onBack = onBack, viewModel = viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChampionDetailContent(
  uiState: ChampionDetailUiState,
  onBack: () -> Unit,
  viewModel: ChampionDetailViewModel? = null,
) {
  // Estado del modal de video sobrevive a rotaciones (recordando id y nombres, no el objeto).
  var activeVideoSkinId by rememberSaveable { mutableStateOf<Int?>(null) }
  var activeVideoSkinNum by rememberSaveable { mutableStateOf(0) }
  var activeVideoSkinName by rememberSaveable { mutableStateOf("") }
  var activeVideoChampionName by rememberSaveable { mutableStateOf("") }
  var activeVideoChampionId by rememberSaveable { mutableStateOf("") }

  Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(uiState.champion?.name ?: stringResource(R.string.champion_detail_title)) },
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
    when {
      uiState.isLoading ->
        Box(
          modifier = Modifier.padding(innerPadding).fillMaxSize(),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator()
        }
      uiState.champion == null ->
        Box(
          modifier = Modifier.padding(innerPadding).fillMaxSize(),
          contentAlignment = Alignment.Center,
        ) {
          Text(stringResource(R.string.champion_not_found))
        }
      else ->
        ChampionDetailList(
          champion = uiState.champion,
          skins = uiState.skins,
          baseSplashUrl = uiState.baseSplashUrl,
          onPlayVideo = { skin ->
            activeVideoSkinId = skin.id
            activeVideoSkinNum = skin.num
            activeVideoSkinName = skin.name
            activeVideoChampionName = uiState.champion?.name ?: ""
            activeVideoChampionId = skin.championId
          },
          modifier = Modifier.padding(innerPadding),
        )
    }
    }

    if (activeVideoSkinId != null) {
      SkinVideoOverlay(
        championName = activeVideoChampionName,
        skinName = activeVideoSkinName,
        championId = activeVideoChampionId,
        skinNum = activeVideoSkinNum,
        englishSkinNameResolver = {
          viewModel?.englishSkinNameForVideo(activeVideoChampionId, activeVideoSkinNum)
        },
        onDismiss = { activeVideoSkinId = null },
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChampionDetailList(
  champion: Champion,
  skins: List<Skin>,
  baseSplashUrl: String?,
  onPlayVideo: (Skin) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = 24.dp),
  ) {
    item {
      AsyncImage(
        model = baseSplashUrl,
        contentDescription = champion.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
      )
    }
    item {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(text = champion.name, style = MaterialTheme.typography.headlineSmall)
        Text(
          text = champion.title,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (champion.tags.isNotEmpty()) {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
          ) {
            champion.tags.forEach { tag -> SuggestionChip(onClick = {}, label = { Text(localizedTag(tag)) }) }
          }
        }
        val lore = champion.lore.ifBlank { champion.blurb }
        if (lore.isNotBlank()) {
          Text(
            text = lore,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp),
          )
        }
        Text(
          text = stringResource(R.string.champion_skins_count, skins.size),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
        )
      }
    }
    items(skins, key = { it.id }) { skin ->
      SkinCard(skin = skin, onPlay = { onPlayVideo(skin) })
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkinCard(skin: Skin, onPlay: () -> Unit) {
  val priceLabel =
    when {
      skin.priceMe != null -> stringResource(R.string.skin_price_me, skin.priceMe)
      skin.priceRp != null -> stringResource(R.string.skin_price_rp, skin.priceRp)
      else -> null
    }

  Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
    Column {
      AsyncImage(
        model = skin.splashUrl,
        contentDescription = skin.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
      )
      Column(modifier = Modifier.padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = skin.name,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
          )
          IconButton(onClick = onPlay) {
            Icon(
              imageVector = Icons.Filled.PlayArrow,
              contentDescription = stringResource(R.string.skin_watch_video),
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        }
        if (
          skin.rarity != null ||
            skin.isLegacy ||
            skin.skinLines.isNotEmpty() ||
            priceLabel != null
        ) {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 6.dp),
          ) {
            skin.rarity?.let { rarity ->
              SuggestionChip(onClick = {}, label = { Text(localizedRarity(rarity)) })
            }
            priceLabel?.let { price ->
              SuggestionChip(onClick = {}, label = { Text(price) })
            }
            if (skin.isLegacy) {
              SuggestionChip(onClick = {}, label = { Text(stringResource(R.string.skin_legacy)) })
            }
            skin.skinLines.forEach { line ->
              SuggestionChip(onClick = {}, label = { Text(line) })
            }
          }
        }
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun ChampionDetailContentPreview() {
  LoLSkinScopeTheme {
    ChampionDetailContent(
      uiState =
        ChampionDetailUiState(
          champion =
            Champion(
              id = "Ahri",
              key = 103,
              name = "Ahri",
              title = "la Zorra de Nueve Colas",
              blurb = "Ahri es una vastaya...",
              tags = listOf("Maga", "Asesina"),
              partype = "Maná",
              attack = 3,
              defense = 4,
              magic = 8,
              difficulty = 5,
              imageUrl = "",
            ),
          skins =
            listOf(
              Skin(
                id = 103000,
                championId = "Ahri",
                num = 0,
                name = "Ahri",
                rarity = null,
                isLegacy = false,
                skinLines = emptyList(),
                splashUrl = "",
                loadingUrl = "",
              ),
              Skin(
                id = 103001,
                championId = "Ahri",
                num = 1,
                name = "Ahri Dinastía",
                rarity = "kEpic",
                isLegacy = false,
                skinLines = listOf("Dinastía"),
                splashUrl = "",
                loadingUrl = "",
              ),
            ),
          isLoading = false,
        ),
      onBack = {},
    )
  }
}

/** Tags de Data Dragon (siempre en inglés) localizados con recursos de la app. */
private val TAG_LABELS: Map<String, Int> =
  mapOf(
    "Fighter" to R.string.tag_fighter,
    "Tank" to R.string.tag_tank,
    "Mage" to R.string.tag_mage,
    "Marksman" to R.string.tag_marksman,
    "Assassin" to R.string.tag_assassin,
    "Support" to R.string.tag_support,
  )

@Composable
private fun localizedTag(tag: String): String =
  TAG_LABELS[tag]?.let { stringResource(it) } ?: tag

/** Raridades de CommunityDragon (códigos k*) localizados con recursos de la app. */
@Composable
private fun localizedRarity(rarity: String): String =
  stringResource(
    when (rarity) {
      "kLegendary" -> R.string.skin_rarity_legendary
      "kMythic" -> R.string.skin_rarity_mythic
      "kEpic" -> R.string.skin_rarity_epic
      "kRare" -> R.string.skin_rarity_rare
      "kTranscendent" -> R.string.skin_rarity_transcendent
      "kUltimate" -> R.string.skin_rarity_ultimate
      "kExalted" -> R.string.skin_rarity_exalted
      else -> R.string.skin_rarity_other
    },
  )
