package com.thikeiw.lolinfo.data.sync

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.thikeiw.lolinfo.data.auth.AuthRepository
import com.thikeiw.lolinfo.data.local.CatalogDao
import com.thikeiw.lolinfo.data.local.DeviceIdProvider
import com.thikeiw.lolinfo.data.local.SkinFlagEntity
import com.thikeiw.lolinfo.data.remote.firebase.awaitResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class FirestoreSkinFlagSyncRepository
@Inject
constructor(
  private val firestore: FirebaseFirestore,
  private val dao: CatalogDao,
  private val authRepository: AuthRepository,
  private val deviceIdProvider: DeviceIdProvider,
) : SkinFlagSyncRepository {

  private val state = MutableStateFlow<SyncState>(SyncState.Idle)
  private val mutex = Mutex()
  private val pushMutex = Mutex()

  override fun observeState(): StateFlow<SyncState> = state.asStateFlow()

  override suspend fun syncNow(): Result<Unit> =
    mutex.withLock {
      val uid =
        authRepository.ensureSignedIn().getOrElse { error ->
          state.value = SyncState.Error(error.message ?: "No se pudo iniciar sesión")
          return@withLock Result.failure(error)
        }

      state.value = SyncState.Syncing
      runCatching {
          dao.claimLegacyFlags(uid)
          val remote =
            flagsCollection(uid)
              .get(Source.SERVER)
              .awaitResult()
              .documents
              .mapNotNull { it.toRemoteSkinFlag() }
          val local = dao.getSkinFlags(uid)
          val plan = mergeSkinFlags(uid, local, remote, deviceIdProvider.deviceId)

          if (plan.toUpsert.isNotEmpty()) dao.upsertSkinFlags(plan.toUpsert)
          pushMutex.withLock {
            plan.toPush.forEach { pushFlagInternal(it) }
          }

          state.value = SyncState.Success(System.currentTimeMillis())
        }
        .onFailure { error ->
          if (error is CancellationException) throw error
          state.value = SyncState.Error(error.message ?: "No se pudo sincronizar")
        }
    }

  override suspend fun pushFlag(flag: SkinFlagEntity): Result<Unit> =
    runCatching {
        pushMutex.withLock {
          // Relee el estado actual: si hubo toggles rápidos, subimos solo la última revisión.
          pushFlagInternal(dao.getSkinFlag(flag.uid, flag.skinId) ?: flag)
        }
      }
      .onFailure { error ->
        if (error is CancellationException) throw error
      }

  private suspend fun pushFlagInternal(flag: SkinFlagEntity) {
    flagsCollection(flag.uid)
      .document(flag.skinId.toString())
      .set(flag.toRemoteMap(), SetOptions.merge())
      .awaitResult()
    dao.markSkinFlagSynced(flag.uid, flag.skinId, flag.revision)
  }

  private fun flagsCollection(uid: String) =
    firestore.collection("users").document(uid).collection("skinFlags")
}

private fun SkinFlagEntity.toRemoteMap(): Map<String, Any> =
  mapOf(
    "isFavorite" to isFavorite,
    "isOwned" to isOwned,
    "revision" to revision,
    "updatedBy" to updatedBy,
    "serverUpdatedAt" to FieldValue.serverTimestamp(),
  )

private fun DocumentSnapshot.toRemoteSkinFlag(): RemoteSkinFlag? {
  val skinId = id.toIntOrNull() ?: return null
  return RemoteSkinFlag(
    skinId = skinId,
    isFavorite = getBoolean("isFavorite") ?: false,
    isOwned = getBoolean("isOwned") ?: false,
    revision = getLong("revision") ?: 0L,
    updatedBy = getString("updatedBy").orEmpty(),
  )
}
