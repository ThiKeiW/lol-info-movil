package com.thikeiw.lolinfo

import android.app.Application
import com.thikeiw.lolinfo.data.sync.SessionSyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LoLSkinScopeApp : Application() {
  @Inject lateinit var sessionSyncManager: SessionSyncManager

  override fun onCreate() {
    super.onCreate()
    sessionSyncManager.start()
  }
}
