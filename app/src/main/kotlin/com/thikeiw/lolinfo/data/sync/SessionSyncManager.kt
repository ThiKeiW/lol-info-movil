package com.thikeiw.lolinfo.data.sync

import com.thikeiw.lolinfo.data.auth.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Arranca la sesión (anónima si hace falta) y mantiene la sincronización: cada vez que cambia el
 * uid (login, vínculo con Google) se hace un `syncNow`. Es idempotente y vive en el proceso.
 */
@Singleton
class SessionSyncManager
@Inject
constructor(
  private val authRepository: AuthRepository,
  private val syncRepository: SkinFlagSyncRepository,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var started = false

  @Synchronized
  fun start() {
    if (started) return
    started = true
    scope.launch {
      authRepository.ensureSignedIn()
      authRepository.currentUid.collectLatest { uid ->
        if (uid != null) syncRepository.syncNow()
      }
    }
  }
}
