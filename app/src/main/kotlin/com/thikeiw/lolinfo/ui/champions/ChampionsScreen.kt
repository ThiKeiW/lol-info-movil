package com.thikeiw.lolinfo.ui.champions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.thikeiw.lolinfo.R
import com.thikeiw.lolinfo.domain.model.Champion
import com.thikeiw.lolinfo.ui.theme.LoLSkinScopeTheme

@Composable
fun ChampionsScreen(
  onChampionClick: (String) -> Unit,
  viewModel: ChampionsViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  ChampionsContent(
    uiState = uiState,
    onQueryChange = viewModel::onQueryChange,
    onRefresh = { viewModel.refresh(force = true) },
    onChampionClick = onChampionClick,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChampionsContent(
  uiState: ChampionsUiState,
  onQueryChange: (String) -> Unit,
  onRefresh: () -> Unit,
  onChampionClick: (String) -> Unit,
) {
  val subtitle =
    if (uiState.version != null) {
      stringResource(R.string.champions_subtitle, uiState.version, uiState.totalChampions)
    } else {
      stringResource(R.string.champions_not_synced)
    }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(stringResource(R.string.champions_title))
            Text(
              text = subtitle,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        actions = {
          if (uiState.isRefreshing) {
            CircularProgressIndicator(
              modifier = Modifier.padding(end = 16.dp).size(20.dp),
              strokeWidth = 2.dp,
            )
          } else {
            IconButton(onClick = onRefresh) {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = stringResource(R.string.action_refresh),
              )
            }
          }
        },
      )
    }
  ) { innerPadding ->
    Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
      OutlinedTextField(
        value = uiState.query,
        onValueChange = onQueryChange,
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(ChampionsSearchTestTag),
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
        placeholder = { Text(stringResource(R.string.champions_search_hint)) },
      )

      uiState.errorMessage?.let { message ->
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
          Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
          )
          TextButton(onClick = onRefresh) { Text(stringResource(R.string.action_retry)) }
        }
      }

      when {
        uiState.isLoading -> CenteredBox {
          CircularProgressIndicator()
          Text(
            text = stringResource(R.string.champions_loading),
            modifier = Modifier.padding(top = 72.dp),
            style = MaterialTheme.typography.bodyMedium,
          )
        }
        uiState.champions.isEmpty() -> CenteredBox {
          Text(
            text =
              if (uiState.query.isBlank()) stringResource(R.string.champions_empty)
              else stringResource(R.string.champions_no_results, uiState.query),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
          )
        }
        else ->
          LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 104.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
          ) {
            items(uiState.champions, key = { it.id }) { champion ->
              ChampionCard(champion = champion, onClick = { onChampionClick(champion.id) })
            }
          }
      }
    }
  }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun ChampionCard(champion: Champion, onClick: () -> Unit) {
  Card(onClick = onClick) {
    Column {
      AsyncImage(
        model = champion.imageUrl,
        contentDescription = champion.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
      )
      Text(
        text = champion.name,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().padding(8.dp),
      )
    }
  }
}

const val ChampionsSearchTestTag = "champions_search"

@Preview(showBackground = true)
@Composable
private fun ChampionsContentPreview() {
  LoLSkinScopeTheme {
    ChampionsContent(
      uiState =
        ChampionsUiState(
          champions =
            listOf(
              Champion(
                id = "Aatrox",
                key = 266,
                name = "Aatrox",
                title = "la Espada Darkin",
                blurb = "",
                tags = listOf("Fighter"),
                partype = "Sangre",
                attack = 8,
                defense = 4,
                magic = 3,
                difficulty = 4,
                imageUrl = "",
              ),
              Champion(
                id = "Ahri",
                key = 103,
                name = "Ahri",
                title = "la Zorra de Nueve Colas",
                blurb = "",
                tags = listOf("Mage", "Assassin"),
                partype = "Maná",
                attack = 3,
                defense = 4,
                magic = 8,
                difficulty = 5,
                imageUrl = "",
              ),
            ),
          totalChampions = 2,
          version = "16.21.1",
        ),
      onQueryChange = {},
      onRefresh = {},
      onChampionClick = {},
    )
  }
}
