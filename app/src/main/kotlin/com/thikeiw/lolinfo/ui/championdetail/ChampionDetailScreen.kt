package com.thikeiw.lolinfo.ui.championdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.thikeiw.lolinfo.ui.theme.LoLSkinScopeTheme

@Composable
fun ChampionDetailScreen(
  championId: String,
  onBack: () -> Unit,
  viewModel: ChampionDetailViewModel = hiltViewModel(),
) {
  LaunchedEffect(championId) { viewModel.load(championId) }
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  ChampionDetailContent(uiState = uiState, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChampionDetailContent(
  uiState: ChampionDetailUiState,
  onBack: () -> Unit,
) {
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
          modifier = Modifier.padding(innerPadding),
        )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChampionDetailList(champion: Champion, skins: List<Skin>, modifier: Modifier = Modifier) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = 24.dp),
  ) {
    item {
      AsyncImage(
        model = skins.firstOrNull { it.isBase }?.splashUrl,
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
            champion.tags.forEach { tag -> SuggestionChip(onClick = {}, label = { Text(tag) }) }
          }
        }
        if (champion.blurb.isNotBlank()) {
          Text(
            text = champion.blurb,
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
    items(skins, key = { it.id }) { skin -> SkinCard(skin = skin) }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkinCard(skin: Skin) {
  Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
    Column {
      AsyncImage(
        model = skin.splashUrl,
        contentDescription = skin.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
      )
      Column(modifier = Modifier.padding(12.dp)) {
        Text(text = skin.name, style = MaterialTheme.typography.titleSmall)
        if (skin.rarity != null || skin.isLegacy || skin.skinLines.isNotEmpty()) {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 6.dp),
          ) {
            skin.rarity?.let { rarity ->
              SuggestionChip(onClick = {}, label = { Text(rarity) })
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
                rarity = "Épica",
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
