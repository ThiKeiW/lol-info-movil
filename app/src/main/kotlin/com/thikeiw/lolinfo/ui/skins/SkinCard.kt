package com.thikeiw.lolinfo.ui.skins

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.thikeiw.lolinfo.R
import com.thikeiw.lolinfo.domain.model.Skin
import com.thikeiw.lolinfo.ui.theme.Gold

/** Card de skin reutilizada por el detalle de campeón y las vistas de colección (favoritos/poseídas). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkinCard(
  skin: Skin,
  onPlay: () -> Unit,
  onToggleFavorite: () -> Unit = {},
  onToggleOwned: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val priceLabel =
    when {
      skin.priceMe != null -> stringResource(R.string.skin_price_me, skin.priceMe)
      skin.priceRp != null -> stringResource(R.string.skin_price_rp, skin.priceRp)
      else -> null
    }

  Card(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
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
          // Icono de favorito + estado de posesión, junto al botón de reproducir video.
          IconButton(onClick = onToggleFavorite) {
            Icon(
              imageVector = if (skin.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
              contentDescription =
                stringResource(
                  if (skin.isFavorite) R.string.skin_unfavorite else R.string.skin_favorite,
                ),
              tint = if (skin.isFavorite) Gold else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          OwnedStatusText(isOwned = skin.isOwned, onToggle = onToggleOwned)
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
              SuggestionChip(
                onClick = {},
                icon = rarityGem(rarity)?.let { file -> { GemIcon(file) } },
                label = { Text(LocalizedRarity(rarity)) },
              )
            }
            priceLabel?.let { price ->
              val priceIcon: Int =
                if (skin.priceMe != null) R.drawable.ic_mythic_essence else R.drawable.ic_rp
              SuggestionChip(
                onClick = {},
                icon = { Image(painterResource(priceIcon), contentDescription = null) },
                label = { Text(price) },
              )
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

/** "En posesión" (verde) / "No obtenido" (gris); tocar alterna la marca. */
@Composable
private fun OwnedStatusText(isOwned: Boolean, onToggle: () -> Unit) {
  TextButton(onClick = onToggle) {
    Text(
      text =
        stringResource(if (isOwned) R.string.skin_owned else R.string.skin_not_owned),
      color = if (isOwned) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
    )
  }
}

/** Raridades de CommunityDragon (códigos k*) localizadas con recursos de la app. */
@Composable
fun LocalizedRarity(rarity: String): String =
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

/** Archivo de gema de rareza (CommunityDragon, assets locales); null = sin icono. */
fun rarityGem(rarity: String): String? =
  when (rarity) {
    "kEpic" -> "gem_epic.png"
    "kMythic" -> "gem_mythic.png"
    "kLegendary" -> "gem_legendary.png"
    "kTranscendent" -> "gem_transcendent.png"
    "kUltimate" -> "gem_ultimate.png"
    "kExalted" -> "gem_exalted.png"
    else -> null
  }

private const val ASSET_PREFIX = "file:///android_asset/"

@Composable
fun GemIcon(file: String) {
  AsyncImage(
    model = ASSET_PREFIX + file,
    contentDescription = null,
    contentScale = ContentScale.Fit,
    modifier = Modifier.size(18.dp),
  )
}

/** Rarezas filtrables en las vistas de colección, en orden de aparición estable. */
val FILTERABLE_RARITIES: List<String> =
  listOf("kEpic", "kLegendary", "kMythic", "kRare", "kTranscendent", "kUltimate", "kExalted")
