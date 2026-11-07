package com.thikeiw.lolinfo.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CatalogDao {
  @Query("SELECT * FROM champions ORDER BY name COLLATE NOCASE")
  abstract fun observeChampions(): Flow<List<ChampionEntity>>

  @Query("SELECT * FROM champions WHERE id = :id")
  abstract fun observeChampion(id: String): Flow<ChampionEntity?>

  @Query("SELECT * FROM champions WHERE id = :id")
  abstract suspend fun getChampion(id: String): ChampionEntity?

  @Query("UPDATE champions SET lore = :lore WHERE id = :id")
  abstract suspend fun updateLore(id: String, lore: String)

  @Query("SELECT * FROM skins WHERE championId = :id ORDER BY num")
  abstract fun observeSkins(id: String): Flow<List<SkinEntity>>

  @Query("SELECT COUNT(*) FROM champions") abstract suspend fun countChampions(): Int

  @Upsert abstract suspend fun upsertChampions(items: List<ChampionEntity>)

  @Upsert abstract suspend fun upsertSkins(items: List<SkinEntity>)

  @Query("DELETE FROM champions") abstract suspend fun deleteChampions()

  @Query("DELETE FROM skins") abstract suspend fun deleteSkins()

  @Transaction
  open suspend fun replaceCatalog(champions: List<ChampionEntity>, skins: List<SkinEntity>) {
    deleteSkins()
    deleteChampions()
    upsertChampions(champions)
    upsertSkins(skins)
  }
}
