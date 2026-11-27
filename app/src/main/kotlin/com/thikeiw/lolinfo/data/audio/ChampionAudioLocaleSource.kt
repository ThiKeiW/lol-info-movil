package com.thikeiw.lolinfo.data.audio

import com.thikeiw.lolinfo.data.local.CatalogLocales
import com.thikeiw.lolinfo.data.local.CatalogPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

interface ChampionAudioLocaleSource {
  suspend fun locale(): String
}

@Singleton
class PreferencesChampionAudioLocaleSource @Inject constructor(
  private val preferences: CatalogPreferences,
) : ChampionAudioLocaleSource {
  override suspend fun locale(): String = preferences.catalogLocale.first().ifBlank { CatalogLocales.ES_MX }
}
