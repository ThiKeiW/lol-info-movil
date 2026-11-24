package com.thikeiw.lolinfo.data.auth

import kotlinx.coroutines.flow.Flow

/** Estado de la sesión de Firebase: sin sesión, anónima o vinculada a Google. */
sealed interface AuthState {
  data object SignedOut : AuthState

  data class Anonymous(val uid: String) : AuthState

  data class Linked(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
  ) : AuthState
}

interface AuthRepository {
  /** uid actual (null si no hay sesión); cambia al iniciar/cerrar sesión o vincular Google. */
  val currentUid: Flow<String?>

  val authState: Flow<AuthState>

  /** Inicia sesión anónima si aún no hay usuario y devuelve el uid. */
  suspend fun ensureSignedIn(): Result<String>

  suspend fun signInAnonymously(): Result<String>

  /** Inicia sesión con Google usando el idToken de Credential Manager. */
  suspend fun signInWithGoogle(idToken: String): Result<Unit>

  /** Vincula la cuenta anónima actual con Google para conservar el mismo uid y sus datos. */
  suspend fun linkWithGoogle(idToken: String): Result<Unit>

  suspend fun signOut()
}
