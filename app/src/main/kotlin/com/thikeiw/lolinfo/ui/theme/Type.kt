package com.thikeiw.lolinfo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// League's brand fonts (Beaufort / Spiegel) are not redistributable, so we use the
// platform serif for display styles to keep the "epic" feel and sans for body copy.
private val Display = FontFamily.Serif
private val Body = FontFamily.SansSerif

val Typography =
  Typography(
    displayLarge =
      TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 52.sp,
        lineHeight = 60.sp,
        letterSpacing = 0.sp,
      ),
    displayMedium =
      TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        lineHeight = 50.sp,
        letterSpacing = 0.sp,
      ),
    headlineMedium =
      TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.2.sp,
      ),
    headlineSmall =
      TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.3.sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
      ),
    labelLarge =
      TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.4.sp,
      ),
    labelMedium =
      TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
      ),
  )
