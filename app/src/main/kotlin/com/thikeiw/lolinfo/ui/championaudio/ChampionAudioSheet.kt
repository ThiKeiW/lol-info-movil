package com.thikeiw.lolinfo.ui.championaudio

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thikeiw.lolinfo.R
import com.thikeiw.lolinfo.domain.model.AudioLine
import com.thikeiw.lolinfo.domain.model.AudioSection
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChampionAudioSheet(
  championId: String,
  skinNum: Int,
  championName: String,
  skinName: String,
  onDismiss: () -> Unit,
  viewModel: ChampionAudioViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(championId, skinNum, skinName) {
    viewModel.load(championId, skinNum, skinName)
  }

  DisposableEffect(Unit) {
    onDispose { viewModel.stop() }
  }

  ModalBottomSheet(
    onDismissRequest = {
      viewModel.stop()
      onDismiss()
    },
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp).padding(horizontal = 16.dp),
    ) {
      Text(
        text = stringResource(R.string.audio_sheet_title, skinName),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = championName,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
      )

      when {
        state.isLoading -> LoadingContent()
        state.loadError ->
          ErrorContent(onRetry = { viewModel.retry(championId, skinNum, skinName) })
        state.catalog?.hasAnyAudio == false -> EmptyContent()
        state.catalog != null -> {
          CatalogContent(state = state, viewModel = viewModel)
        }
      }

      Spacer(Modifier.height(8.dp))
      Text(
        text = stringResource(R.string.audio_attribution),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 16.dp),
      )
    }
  }
}

@Composable
private fun LoadingContent() {
  Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
    CircularProgressIndicator()
  }
}

@Composable
private fun ErrorContent(onRetry: () -> Unit) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(text = stringResource(R.string.audio_error), style = MaterialTheme.typography.bodyMedium)
    TextButton(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
      Text(stringResource(R.string.action_retry))
    }
  }
}

@Composable
private fun EmptyContent() {
  Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
    Text(text = stringResource(R.string.audio_empty), style = MaterialTheme.typography.bodyMedium)
  }
}

@Composable
private fun ColumnScope.CatalogContent(state: ChampionAudioUiState, viewModel: ChampionAudioViewModel) {
  val catalog = state.catalog ?: return
  if (state.isFallback) {
    NoteText(stringResource(R.string.audio_skin_fallback_note))
  }

  val group = state.selectedGroup
  if (group != null && group.forms.size > 1) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
    ) {
      group.forms.forEach { form ->
        FilterChip(
          selected = form.id == state.selectedFormId || (state.selectedFormId == null && form == group.forms.firstOrNull()),
          onClick = { viewModel.selectForm(form.id) },
          label = { Text(localizedAudioForm(form.label)) },
        )
      }
    }
  }

  val sections = state.sections
  if (sections.size > 1) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
    ) {
      FilterChip(
        selected = state.selectedSection == null,
        onClick = { viewModel.selectSection(null) },
        label = { Text(stringResource(R.string.audio_section_all)) },
      )
      sections.forEach { section ->
        FilterChip(
          selected = state.selectedSection == section.title,
          onClick = { viewModel.selectSection(section.title) },
          label = { Text(localizedAudioSection(section.title)) },
        )
      }
    }
  }

  CompositionLocalProvider(LocalOverscrollFactory provides null) {
    AudioLineList(
      sections = if (state.selectedSection == null) sections else sections.filter { it.title == state.selectedSection },
      currentLineId = state.currentLine?.id,
      onPlay = viewModel::playLine,
      modifier = Modifier.weight(1f).fillMaxWidth(),
    )
  }

  MiniPlayer(state = state, viewModel = viewModel)
}

@Composable
private fun NoteText(text: String) {
  Surface(
    color = MaterialTheme.colorScheme.secondaryContainer,
    shape = MaterialTheme.shapes.small,
    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSecondaryContainer,
      modifier = Modifier.padding(12.dp),
    )
  }
}

@Composable
private fun AudioLineList(
  sections: List<AudioSection>,
  currentLineId: String?,
  onPlay: (AudioLine) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(modifier = modifier) {
    sections.forEach { section ->
      item(key = "section-${section.title}") {
        Text(
          text = localizedAudioSection(section.title),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )
      }
      items(section.lines, key = { it.id }) { line ->
        AudioLineRow(line = line, isCurrent = line.id == currentLineId, onPlay = { onPlay(line) })
      }
    }
  }
}

@Composable
private fun AudioLineRow(line: AudioLine, isCurrent: Boolean, onPlay: () -> Unit) {
  val background =
    if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    else MaterialTheme.colorScheme.surface
  Surface(color = background, shape = MaterialTheme.shapes.small) {
    Row(
      modifier = Modifier.fillMaxWidth().clickable(onClick = onPlay).padding(horizontal = 8.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = if (isCurrent) Icons.Filled.PlayArrow else Icons.Filled.PlayArrow,
        contentDescription = null,
        tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
      )
      Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
        Text(
          text = line.transcript?.takeIf { it.isNotBlank() } ?: stringResource(R.string.audio_no_transcript),
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
        )
        val subtitle =
          if (line.isLocalized) stringResource(R.string.audio_localized_badge)
          else line.formLabel ?: ""
        if (subtitle.isNotBlank()) {
          Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun MiniPlayer(state: ChampionAudioUiState, viewModel: ChampionAudioViewModel) {
  val line = state.currentLine ?: return
  HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
  Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)) {
    Text(
      text = line.transcript?.takeIf { it.isNotBlank() } ?: localizedAudioSection(line.section),
      style = MaterialTheme.typography.bodySmall,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    val duration = state.durationMs.coerceAtLeast(0)
    Slider(
      value = state.positionMs.coerceAtLeast(0).toFloat(),
      onValueChange = { viewModel.seekTo(it.toLong()) },
      valueRange = 0f..(duration.toFloat().coerceAtLeast(1f)),
      modifier = Modifier.fillMaxWidth(),
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(formatTime(state.positionMs), style = MaterialTheme.typography.labelSmall)
      Text(formatTime(duration), style = MaterialTheme.typography.labelSmall)
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = viewModel::previous) {
        Icon(Icons.Filled.SkipPrevious, contentDescription = stringResource(R.string.audio_previous))
      }
      IconButton(onClick = viewModel::togglePlayPause) {
        if (state.playbackStatus == AudioPlaybackStatus.PREPARING) {
          CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
          Icon(
            imageVector =
              if (state.playbackStatus == AudioPlaybackStatus.PLAYING) Icons.Filled.Pause
              else Icons.Filled.PlayArrow,
            contentDescription =
              stringResource(
                if (state.playbackStatus == AudioPlaybackStatus.PLAYING) R.string.audio_pause
                else R.string.audio_play,
              ),
          )
        }
      }
      IconButton(onClick = viewModel::next) {
        Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.audio_next))
      }
    }
    if (state.playbackStatus == AudioPlaybackStatus.ERROR) {
      TextButton(onClick = viewModel::togglePlayPause, modifier = Modifier.align(Alignment.CenterHorizontally)) {
        Text(stringResource(R.string.action_retry))
      }
    }
  }
}

@Composable
private fun localizedAudioForm(label: String?): String =
  when (label?.trim()?.lowercase(Locale.ROOT)) {
    null, "", "original", "base" -> stringResource(R.string.audio_form_original)
    "old" -> stringResource(R.string.audio_form_old)
    "chosen" -> stringResource(R.string.audio_form_chosen)
    "risen legend" -> stringResource(R.string.audio_form_risen_legend)
    "indie" -> stringResource(R.string.audio_form_indie)
    "rising star" -> stringResource(R.string.audio_form_rising_star)
    "superstar" -> stringResource(R.string.audio_form_superstar)
    else -> label
  }

@Composable
private fun localizedAudioSection(title: String): String =
  when (title.trim().lowercase(Locale.ROOT)) {
    "champion select" -> stringResource(R.string.audio_section_champion_select)
    "ban" -> stringResource(R.string.audio_section_ban)
    "movement" -> stringResource(R.string.audio_section_movement)
    "first move" -> stringResource(R.string.audio_section_first_move)
    "moving" -> stringResource(R.string.audio_section_moving)
    "long move" -> stringResource(R.string.audio_section_long_move)
    "idle" -> stringResource(R.string.audio_section_idle)
    "first encounter" -> stringResource(R.string.audio_section_first_encounter)
    "taunt" -> stringResource(R.string.audio_section_taunt)
    "joke" -> stringResource(R.string.audio_section_joke)
    "laugh" -> stringResource(R.string.audio_section_laugh)
    "dance" -> stringResource(R.string.audio_section_dance)
    "attack" -> stringResource(R.string.audio_section_attack)
    "basic attacking" -> stringResource(R.string.audio_section_basic_attacking)
    "attacking" -> stringResource(R.string.audio_section_attacking)
    "ability casting" -> stringResource(R.string.audio_section_ability_casting)
    "kills and objectives" -> stringResource(R.string.audio_section_kills_objectives)
    "other gameplay" -> stringResource(R.string.audio_section_other_gameplay)
    "death" -> stringResource(R.string.audio_section_death)
    "game start" -> stringResource(R.string.audio_section_game_start)
    "game end" -> stringResource(R.string.audio_section_game_end)
    "recall" -> stringResource(R.string.audio_section_recall)
    "low health" -> stringResource(R.string.audio_section_low_health)
    "shopping" -> stringResource(R.string.audio_section_shopping)
    "pinging" -> stringResource(R.string.audio_section_pinging)
    "general" -> stringResource(R.string.audio_section_general)
    else -> title
  }

private fun formatTime(ms: Long): String {
  val totalSeconds = (ms / 1000).coerceAtLeast(0)
  val minutes = totalSeconds / 60
  val seconds = totalSeconds % 60
  return "%d:%02d".format(minutes, seconds)
}
