package com.thikeiw.lolinfo.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.catalogStore: DataStore<Preferences> by preferencesDataStore(name = "catalog")

/** Locales soportados por el catálogo (Data Dragon / Community Dragon). */
object CatalogLocales {
  const val ES_MX = "es_mx"
  const val EN_US = "en_us"

  /** Community Dragon guarda el inglés por defecto en la carpeta "default", no en "en_us". */
  const val COMMUNITY_DRAGON_DEFAULT = "default"
}

@Singleton
class CatalogPreferences @Inject constructor(@ApplicationContext private val context: Context) {
  private val versionKey = stringPreferencesKey("catalog_version")
  private val syncedAtKey = stringPreferencesKey("catalog_synced_at")
  private val schemaKey = stringPreferencesKey("catalog_schema")
  private val localeKey = stringPreferencesKey("catalog_locale")
  private val syncedLocaleKey = stringPreferencesKey("catalog_synced_locale")

  val catalogVersion: Flow<String?> = context.catalogStore.data.map { it[versionKey] }

  val catalogSchema: Flow<Int?> =
    context.catalogStore.data.map { it[schemaKey]?.toIntOrNull() }

  val lastSyncedAt: Flow<Long?> =
    context.catalogStore.data.map { it[syncedAtKey]?.toLongOrNull() }

  /** Idioma elegido por el usuario (por defecto Español MX). */
  val catalogLocale: Flow<String> =
    context.catalogStore.data.map { it[localeKey] ?: CatalogLocales.ES_MX }

  /** Idioma con el que se sincronizó el catálogo guardado en Room. */
  val catalogSyncedLocale: Flow<String?> =
    context.catalogStore.data.map { it[syncedLocaleKey] }

  suspend fun setCatalogVersion(version: String, syncedAt: Long, locale: String) {
    context.catalogStore.edit { preferences ->
      preferences[versionKey] = version
      preferences[syncedAtKey] = syncedAt.toString()
      preferences[schemaKey] = CATALOG_SCHEMA.toString()
      preferences[syncedLocaleKey] = locale
    }
  }

  suspend fun setLocale(locale: String) {
    context.catalogStore.edit { preferences -> preferences[localeKey] = locale }
  }

  companion object {
    const val CATALOG_SCHEMA = 4
  }
}
