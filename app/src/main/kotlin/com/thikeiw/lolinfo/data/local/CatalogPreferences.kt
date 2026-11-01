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

@Singleton
class CatalogPreferences @Inject constructor(@ApplicationContext private val context: Context) {
  private val versionKey = stringPreferencesKey("catalog_version")
  private val syncedAtKey = stringPreferencesKey("catalog_synced_at")

  val catalogVersion: Flow<String?> = context.catalogStore.data.map { it[versionKey] }

  val lastSyncedAt: Flow<Long?> =
    context.catalogStore.data.map { it[syncedAtKey]?.toLongOrNull() }

  suspend fun setCatalogVersion(version: String, syncedAt: Long) {
    context.catalogStore.edit { preferences ->
      preferences[versionKey] = version
      preferences[syncedAtKey] = syncedAt.toString()
    }
  }
}
