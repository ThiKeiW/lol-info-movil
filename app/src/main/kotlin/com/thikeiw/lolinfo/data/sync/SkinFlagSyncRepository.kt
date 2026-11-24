package com.thikeiw.lolinfo.data.sync

import com.thikeiw.lolinfo.data.local.SkinFlagEntity
import kotlinx.coroutines.flow.StateFlow

sealed interface SyncState {
  data object Idle : SyncState

  data object Syncing : SyncState

  data class Success(val lastSyncedAt: Long) : SyncState

  data class Error(val message: String) : SyncState
}

/** Documento remoto tal como vive en Firestore (`users/{uid}/skinFlags/{skinId}`). */
data class RemoteSkinFlag(
  val skinId: Int,
  val isFavorite: Boolean,
  val isOwned: Boolean,
  val revision: Long,
  val updatedBy: String,
)

data class MergePlan(
  val toUpsert: List<SkinFlagEntity>,
  val toPush: List<SkinFlagEntity>,
)

interface SkinFlagSyncRepository {
  fun observeState(): StateFlow<SyncState>

  /** Baja lo remoto, fusiona con lo local por reloj lógico y sube lo que gane localmente. */
  suspend fun syncNow(): Result<Unit>

  /** Escritura puntual de una marca (fire-and-forget tras un toggle). */
  suspend fun pushFlag(flag: SkinFlagEntity): Result<Unit>
}

/**
 * Fusiona marcas locales y remotas con un reloj lógico (Lamport): gana la `revision` mayor y,
 * en empate, el `updatedBy` mayor. Nunca se comparan relojes de pared, así que no importa el
 * desfase entre dispositivos. Devuelve lo que hay que persistir en Room y lo que hay que subir.
 */
fun mergeSkinFlags(
  uid: String,
  local: List<SkinFlagEntity>,
  remote: List<RemoteSkinFlag>,
  deviceId: String,
): MergePlan {
  val localById = local.associateBy { it.skinId }
  val remoteById = remote.associateBy { it.skinId }
  val toUpsert = mutableListOf<SkinFlagEntity>()
  val toPush = mutableListOf<SkinFlagEntity>()

  for (skinId in localById.keys + remoteById.keys) {
    val localFlag = localById[skinId]
    val remoteFlag = remoteById[skinId]

    when {
      localFlag == null && remoteFlag != null -> toUpsert += remoteFlag.toEntity(uid)
      localFlag != null && remoteFlag == null -> {
        val push = localFlag.copy(uid = uid, pendingSync = true)
        toUpsert += push
        toPush += push
      }
      localFlag != null && remoteFlag != null -> {
        when {
          remoteFlag.isNewerThan(localFlag) -> toUpsert += remoteFlag.toEntity(uid)
          localFlag.matches(remoteFlag) -> toUpsert += localFlag.copy(uid = uid, pendingSync = false)
          else -> {
            // Gana local (o se rompe el empate exacto subiendo una revisión) y se re-sube.
            val bumpedRevision =
              if (localFlag.revision == remoteFlag.revision && localFlag.updatedBy == remoteFlag.updatedBy) {
                localFlag.revision + 1
              } else {
                localFlag.revision
              }
            val winner =
              localFlag.copy(
                uid = uid,
                revision = bumpedRevision,
                updatedBy = deviceId,
                pendingSync = true,
              )
            toUpsert += winner
            toPush += winner
          }
        }
      }
    }
  }

  return MergePlan(toUpsert = toUpsert, toPush = toPush)
}

private fun RemoteSkinFlag.isNewerThan(local: SkinFlagEntity): Boolean =
  revision > local.revision || (revision == local.revision && updatedBy > local.updatedBy)

private fun SkinFlagEntity.matches(remote: RemoteSkinFlag): Boolean =
  revision == remote.revision &&
    updatedBy == remote.updatedBy &&
    isFavorite == remote.isFavorite &&
    isOwned == remote.isOwned

private fun RemoteSkinFlag.toEntity(uid: String): SkinFlagEntity =
  SkinFlagEntity(
    uid = uid,
    skinId = skinId,
    isFavorite = isFavorite,
    isOwned = isOwned,
    revision = revision,
    updatedBy = updatedBy,
    pendingSync = false,
  )
