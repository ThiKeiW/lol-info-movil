package com.thikeiw.lolinfo.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thikeiw.lolinfo.data.auth.AuthRepository
import com.thikeiw.lolinfo.data.auth.AuthState
import com.thikeiw.lolinfo.data.sync.SkinFlagSyncRepository
import com.thikeiw.lolinfo.data.sync.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AccountMessage {
  SignedInWithGoogle,
  LinkedWithGoogle,
  SignedOut,
  Error,
}

data class AccountUiState(
  val authState: AuthState = AuthState.SignedOut,
  val syncState: SyncState = SyncState.Idle,
  val isWorking: Boolean = false,
  val message: AccountMessage? = null,
)

@HiltViewModel
class AccountViewModel
@Inject
constructor(
  private val authRepository: AuthRepository,
  private val syncRepository: SkinFlagSyncRepository,
) : ViewModel() {

  private val isWorking = MutableStateFlow(false)
  private val message = MutableStateFlow<AccountMessage?>(null)

  val uiState: StateFlow<AccountUiState> =
    combine(authRepository.authState, syncRepository.observeState(), isWorking, message) {
        auth,
        sync,
        working,
        msg ->
        AccountUiState(
          authState = auth,
          syncState = sync,
          isWorking = working,
          message = msg,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AccountUiState(),
      )

  fun clearMessage() {
    message.value = null
  }

  fun continueAsGuest() {
    viewModelScope.launch { authRepository.ensureSignedIn() }
  }

  fun signInWithGoogle(idToken: String) {
    runTask(AccountMessage.SignedInWithGoogle) { authRepository.signInWithGoogle(idToken) }
  }

  fun linkWithGoogle(idToken: String) {
    runTask(AccountMessage.LinkedWithGoogle) { authRepository.linkWithGoogle(idToken) }
  }

  fun signOut() {
    viewModelScope.launch {
      authRepository.signOut()
      message.value = AccountMessage.SignedOut
    }
  }

  fun syncNow() {
    viewModelScope.launch {
      isWorking.value = true
      syncRepository.syncNow()
      isWorking.value = false
    }
  }

  private fun runTask(success: AccountMessage, block: suspend () -> Result<Unit>) {
    viewModelScope.launch {
      isWorking.value = true
      block()
        .onSuccess { message.value = success }
        .onFailure { message.value = AccountMessage.Error }
      isWorking.value = false
    }
  }
}
