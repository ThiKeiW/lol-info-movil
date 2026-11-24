package com.thikeiw.lolinfo.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Identificador estable de esta instalación, usado para desempatar revisiones (Lamport). */
@Singleton
class DeviceIdProvider @Inject constructor(@ApplicationContext context: Context) {
  private val preferences = context.getSharedPreferences("device", Context.MODE_PRIVATE)

  val deviceId: String by lazy {
    preferences.getString(KEY_DEVICE_ID, null)
      ?: UUID.randomUUID().toString().also { generated ->
        preferences.edit().putString(KEY_DEVICE_ID, generated).apply()
      }
  }

  private companion object {
    const val KEY_DEVICE_ID = "device_id"
  }
}
