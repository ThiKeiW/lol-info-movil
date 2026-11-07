package com.thikeiw.lolinfo.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class SkinPrice(val rp: Int? = null, val me: Int? = null)

@Singleton
class SkinPriceCatalog @Inject constructor(@ApplicationContext private val context: Context) {
  private val json = Json { ignoreUnknownKeys = true }

  fun prices(): Map<Int, SkinPrice> =
    runCatching {
        context.assets.open(ASSET_NAME).bufferedReader().use { reader ->
          json
            .decodeFromString<Map<String, SkinPrice>>(reader.readText())
            .mapNotNull { (key, price) -> key.toIntOrNull()?.let { it to price } }
            .toMap()
        }
      }
      .getOrDefault(emptyMap())

  private companion object {
    const val ASSET_NAME = "skin_prices.json"
  }
}
