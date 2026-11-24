package com.thikeiw.lolinfo.data.remote.firebase

import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** Espera un `Task` de Google Play Services como corrutina, sin depender de play-services-kotlin. */
internal suspend fun <T> Task<T>.awaitResult(): T =
  suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
      if (task.isSuccessful) {
        continuation.resume(task.result)
      } else {
        continuation.resumeWithException(
          task.exception ?: IllegalStateException("La tarea de Firebase falló")
        )
      }
    }
  }
