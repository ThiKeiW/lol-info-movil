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

  @Query("SELECT * FROM skins")
  abstract fun observeAllSkins(): Flow<List<SkinEntity>>

  @Query("SELECT * FROM skin_flags WHERE uid = :uid")
  abstract fun observeSkinFlags(uid: String): Flow<List<SkinFlagEntity>>

  @Query("SELECT * FROM skin_flags WHERE uid = :uid")
  abstract suspend fun getSkinFlags(uid: String): List<SkinFlagEntity>

  @Query("SELECT * FROM skin_flags WHERE uid = :uid AND skinId = :skinId")
  abstract suspend fun getSkinFlag(uid: String, skinId: Int): SkinFlagEntity?

  @Query("SELECT * FROM skin_flags WHERE uid = :uid AND pendingSync = 1")
  abstract suspend fun getPendingSkinFlags(uid: String): List<SkinFlagEntity>

  @Upsert abstract suspend fun upsertSkinFlag(flag: SkinFlagEntity)

  @Upsert abstract suspend fun upsertSkinFlags(flags: List<SkinFlagEntity>)

  @Query("UPDATE skin_flags SET pendingSync = 0 WHERE uid = :uid AND skinId = :skinId AND revision = :revision")
  abstract suspend fun markSkinFlagSynced(uid: String, skinId: Int, revision: Long)

  @Query("DELETE FROM skin_flags WHERE uid = :uid AND skinId = :skinId")
  abstract suspend fun deleteSkinFlag(uid: String, skinId: Int)

  @Query(
    "INSERT OR IGNORE INTO skin_flags (uid, skinId, isFavorite, isOwned, revision, updatedBy, pendingSync) " +
      "SELECT :uid, skinId, isFavorite, isOwned, revision, updatedBy, 1 FROM skin_flags WHERE uid = 'legacy'"
  )
  abstract suspend fun claimLegacyInto(uid: String)

  @Query("DELETE FROM skin_flags WHERE uid = 'legacy'")
  abstract suspend fun deleteLegacyFlags()

  /** Reasigna las marcas locales (fase 2 / sin sesión) a la cuenta actual una sola vez. */
  @Transaction
  open suspend fun claimLegacyFlags(uid: String) {
    if (uid == LEGACY_UID) return
    claimLegacyInto(uid)
    deleteLegacyFlags()
  }

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
