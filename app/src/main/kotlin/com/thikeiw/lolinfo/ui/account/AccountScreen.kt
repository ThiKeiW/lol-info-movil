package com.thikeiw.lolinfo.ui.account

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.thikeiw.lolinfo.BuildConfig
import com.thikeiw.lolinfo.R
import com.thikeiw.lolinfo.data.auth.AuthState
import com.thikeiw.lolinfo.data.sync.SyncState
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
  onBack: () -> Unit,
  viewModel: AccountViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val googleConfigured = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

  val onGoogle: () -> Unit = {
    when {
      !googleConfigured ->
        scope.launch {
          snackbarHostState.showSnackbar(context.getString(R.string.account_google_not_configured))
        }
      else ->
        scope.launch {
          runCatching { requestGoogleIdToken(context) }
            .onSuccess { token ->
              if (uiState.authState is AuthState.Anonymous) viewModel.linkWithGoogle(token)
              else viewModel.signInWithGoogle(token)
            }
            .onFailure { error ->
              snackbarHostState.showSnackbar(
                error.message ?: context.getString(R.string.account_error)
              )
            }
        }
    }
  }

  LaunchedEffect(uiState.message) {
    val message = uiState.message ?: return@LaunchedEffect
    snackbarHostState.showSnackbar(context.getString(message.toRes()))
    viewModel.clearMessage()
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.account_title)) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.action_back),
            )
          }
        },
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.padding(innerPadding)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      SessionCard(uiState.authState)
      SyncStatusCard(uiState.syncState)

      if (!googleConfigured) {
        Text(
          text = stringResource(R.string.account_google_not_configured),
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
        )
      }

      when (uiState.authState) {
        is AuthState.Linked -> {
          Button(
            onClick = viewModel::syncNow,
            enabled = !uiState.isWorking,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(stringResource(R.string.account_sync_now))
          }
          OutlinedButton(
            onClick = viewModel::signOut,
            enabled = !uiState.isWorking,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(stringResource(R.string.account_sign_out))
          }
        }

        is AuthState.Anonymous -> {
          Button(
            onClick = onGoogle,
            enabled = !uiState.isWorking,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(stringResource(R.string.account_link_google))
          }
          OutlinedButton(
            onClick = viewModel::syncNow,
            enabled = !uiState.isWorking,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(stringResource(R.string.account_sync_now))
          }
          TextButton(
            onClick = viewModel::signOut,
            enabled = !uiState.isWorking,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(stringResource(R.string.account_sign_out))
          }
        }

        AuthState.SignedOut -> {
          Button(
            onClick = onGoogle,
            enabled = !uiState.isWorking,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(stringResource(R.string.account_sign_in_google))
          }
          OutlinedButton(
            onClick = viewModel::continueAsGuest,
            enabled = !uiState.isWorking,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(stringResource(R.string.account_continue_guest))
          }
        }
      }
    }
  }
}

@Composable
private fun SessionCard(state: AuthState) {
  Card(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      when (state) {
        is AuthState.Linked -> {
          if (!state.photoUrl.isNullOrBlank()) {
            AsyncImage(
              model = state.photoUrl,
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier.size(48.dp).clip(CircleShape),
            )
          } else {
            Icon(
              imageVector = Icons.Default.AccountCircle,
              contentDescription = null,
              modifier = Modifier.size(48.dp),
            )
          }
          Column {
            Text(
              text = state.displayName ?: stringResource(R.string.account_linked_title),
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text = state.email ?: state.uid,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        is AuthState.Anonymous -> {
          Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
          )
          Column {
            Text(
              text = stringResource(R.string.account_anonymous_title),
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text = stringResource(R.string.account_anonymous_body),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        AuthState.SignedOut -> {
          Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
          )
          Column {
            Text(
              text = stringResource(R.string.account_signed_out_title),
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text = stringResource(R.string.account_signed_out_body),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SyncStatusCard(state: SyncState) {
  val text =
    when (state) {
      SyncState.Idle -> stringResource(R.string.account_sync_idle)
      SyncState.Syncing -> stringResource(R.string.account_sync_syncing)
      is SyncState.Success ->
        stringResource(R.string.account_sync_success, formatTimestamp(state.lastSyncedAt))
      is SyncState.Error -> stringResource(R.string.account_sync_error, state.message)
    }
  val color =
    if (state is SyncState.Error) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.onSurfaceVariant

  Card(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = color)
      Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
  }
}

@StringRes
private fun AccountMessage.toRes(): Int =
  when (this) {
    AccountMessage.SignedInWithGoogle -> R.string.account_signed_in_google
    AccountMessage.LinkedWithGoogle -> R.string.account_linked_google
    AccountMessage.SignedOut -> R.string.account_signed_out
    AccountMessage.Error -> R.string.account_error
  }

private fun formatTimestamp(millis: Long): String =
  DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(millis))

private suspend fun requestGoogleIdToken(context: Context): String {
  val credentialManager = CredentialManager.create(context)
  val googleIdOption =
    GetGoogleIdOption.Builder()
      .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
      .setFilterByAuthorizedAccounts(false)
      .setAutoSelectEnabled(false)
      .build()
  val request =
    GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
  val response = credentialManager.getCredential(context, request)
  val credential = response.credential
  if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
    error("Credencial de Google inesperada: ${credential.type}")
  }
  return GoogleIdTokenCredential.createFrom(credential.data).idToken
}
