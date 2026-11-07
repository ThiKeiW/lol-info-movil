package com.thikeiw.lolinfo

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.thikeiw.lolinfo.ui.theme.LoLSkinScopeTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun attachBaseContext(newBase: Context) {
    // El idioma de la interfaz se fija al crear la Activity (sincrono, SP del app_locale).
    val preferences = newBase.getSharedPreferences(APP_LOCALE_STORE, Context.MODE_PRIVATE)
    val tag = preferences.getString(APP_LOCALE_KEY, "es-MX") ?: "es-MX"
    val configuration = Configuration(newBase.resources.configuration)
    configuration.setLocale(Locale.forLanguageTag(tag))
    super.attachBaseContext(newBase.createConfigurationContext(configuration))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      LoLSkinScopeTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation()
        }
      }
    }
  }

  companion object {
    const val APP_LOCALE_STORE = "app_locale"
    const val APP_LOCALE_KEY = "locale"
  }
}
