package com.thikeiw.lolinfo.util

import java.io.UnsupportedEncodingException
import java.net.URLEncoder

object SpotlightUrls {
  const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
      "Chrome/126.0.0.0 Safari/537.36"

  private const val CHANNEL_SEARCH_PREFIX = "https://www.youtube.com/@SkinSpotlights/search?query="

  /** Construye la consulta de búsqueda para un skin, omitiendo el nombre del campeón si es redundante. */
  fun queryOf(championName: String, skinName: String): String =
    if (skinName.contains(championName, ignoreCase = true)) skinName
    else "$championName $skinName"

  /** URL de búsqueda dentro del canal de SkinSpotlights (solo se renderiza bien en escritorio). */
  fun searchUrl(query: String): String =
    CHANNEL_SEARCH_PREFIX + try {
      URLEncoder.encode(query, "UTF-8")
    } catch (e: UnsupportedEncodingException) {
      query
    }
}

/**
 * SkinSpotlights no publica un índice de sus vídeos, así que enlazamos a la búsqueda dentro de su
 * canal (canal principal de LoL, no SkinSpotlights Wild Rift).
 */
fun skinSpotlightUrl(championName: String, skinName: String): String =
  SpotlightUrls.searchUrl(SpotlightUrls.queryOf(championName, skinName))

fun spotlightQuery(championName: String, skinName: String): String =
  SpotlightUrls.queryOf(championName, skinName)

fun embedVideoUrl(videoId: String): String =
  "https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1&rel=0"
