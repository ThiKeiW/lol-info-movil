package com.thikeiw.lolinfo.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.thikeiw.lolinfo.data.remote.firebase.awaitResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
class FirebaseAuthRepository @Inject constructor(private val auth: FirebaseAuth) : AuthRepository {

  override val authState: Flow<AuthState> =
    callbackFlow {
        val listener =
          FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser.toAuthState())
          }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
      }
      .distinctUntilChanged()

  override val currentUid: Flow<String?> =
    authState
      .map { state ->
        when (state) {
          is AuthState.SignedOut -> null
          is AuthState.Anonymous -> state.uid
          is AuthState.Linked -> state.uid
        }
      }
      .distinctUntilChanged()

  override suspend fun ensureSignedIn(): Result<String> {
    val current = auth.currentUser
    return if (current != null) Result.success(current.uid) else signInAnonymously()
  }

  override suspend fun signInAnonymously(): Result<String> = runCatching {
    val user =
      auth.signInAnonymously().awaitResult().user
        ?: error("Firebase no devolvió un usuario anónimo")
    user.uid
  }

  override suspend fun signInWithGoogle(idToken: String): Result<Unit> = runCatching {
    val credential = GoogleAuthProvider.getCredential(idToken, null)
    auth.signInWithCredential(credential).awaitResult()
    Unit
  }

  override suspend fun linkWithGoogle(idToken: String): Result<Unit> = runCatching {
    val user =
      auth.currentUser
        ?: auth.signInAnonymously().awaitResult().user
        ?: error("No hay una sesión para vincular")
    val credential = GoogleAuthProvider.getCredential(idToken, null)
    try {
      user.linkWithCredential(credential).awaitResult()
    } catch (collision: FirebaseAuthUserCollisionException) {
      // La cuenta de Google ya pertenece a otro usuario (p. ej. vinculada en otro equipo):
      // en vez de fallar, iniciamos sesión con esa cuenta para recuperar su colección.
      auth.signInWithCredential(credential).awaitResult()
    }
    Unit
  }

  override suspend fun signOut() {
    auth.signOut()
  }
}

private fun FirebaseUser?.toAuthState(): AuthState =
  when {
    this == null -> AuthState.SignedOut
    isAnonymous -> AuthState.Anonymous(uid)
    else -> AuthState.Linked(uid, email, displayName, photoUrl?.toString())
  }
