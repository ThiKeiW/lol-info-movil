package com.thikeiw.lolinfo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = Gold,
    onPrimary = Ink,
    primaryContainer = GoldDark,
    onPrimaryContainer = GoldBright,
    secondary = HextechBlue,
    onSecondary = Ink,
    secondaryContainer = HextechBlueDeep,
    onSecondaryContainer = GoldBright,
    tertiary = MagicTeal,
    onTertiary = Ink,
    background = Ink,
    onBackground = GoldBright,
    surface = Navy,
    onSurface = GoldBright,
    surfaceVariant = Slate,
    onSurfaceVariant = Color(0xFFB9C2CC),
    surfaceContainerLowest = Ink,
    surfaceContainerLow = Color(0xFF0C1524),
    surfaceContainer = NavyRaised,
    surfaceContainerHigh = Color(0xFF16202F),
    surfaceContainerHighest = Color(0xFF1C2738),
    outline = Color(0xFF5B6673),
    outlineVariant = Color(0xFF2C3644),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = GoldDark,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = GoldBright,
    onPrimaryContainer = Color(0xFF241A05),
    secondary = HextechBlueDeep,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDE9F2),
    onSecondaryContainer = Color(0xFF00313F),
    tertiary = Color(0xFF00786E),
    onTertiary = Color(0xFFFFFFFF),
    background = Parchment,
    onBackground = Color(0xFF1B1B17),
    surface = ParchmentRaised,
    onSurface = Color(0xFF1B1B17),
    surfaceVariant = Color(0xFFE6E0D2),
    onSurfaceVariant = Color(0xFF4B463B),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9F5EC),
    surfaceContainer = Color(0xFFF2EDE1),
    surfaceContainerHigh = Color(0xFFECE6D8),
    surfaceContainerHighest = Color(0xFFE6DFD0),
    outline = Color(0xFF7C7669),
    outlineVariant = Color(0xFFCFC8B8),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
  )

@Composable
fun LoLSkinScopeTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+ but would dilute the League identity.
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
