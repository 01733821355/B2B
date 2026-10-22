package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Universal dynamic typography generator responsive to Admin's font style
 * and text scaling preferences.
 */
fun getAppTypography(fontStyle: String = "DEFAULT_SANS", textScale: String = "STANDARD"): Typography {
  val family = when (fontStyle.uppercase().trim()) {
    "EXECUTIVE_SERIF", "SERIF" -> FontFamily.Serif
    "TECHNICAL_MONO", "MONO", "MONOSPACE" -> FontFamily.Monospace
    "CURSIVE" -> FontFamily.Cursive
    "CLEAN_SANS", "SANS", "DEFAULT_SANS" -> FontFamily.SansSerif
    else -> FontFamily.Default
  }

  val scaleMultiplier = when (textScale.uppercase().trim()) {
    "COMPACT" -> 0.92f
    "COMFORTABLE", "LARGE" -> 1.15f
    else -> 1.0f
  }

  return Typography(
    bodyLarge = TextStyle(
      fontFamily = family,
      fontWeight = FontWeight.Normal,
      fontSize = (16 * scaleMultiplier).sp,
      lineHeight = (24 * scaleMultiplier).sp,
      letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
      fontFamily = family,
      fontWeight = FontWeight.Normal,
      fontSize = (14 * scaleMultiplier).sp,
      lineHeight = (20 * scaleMultiplier).sp,
      letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
      fontFamily = family,
      fontWeight = FontWeight.Normal,
      fontSize = (12 * scaleMultiplier).sp,
      lineHeight = (16 * scaleMultiplier).sp,
    ),
    titleLarge = TextStyle(
      fontFamily = family,
      fontWeight = FontWeight.Bold,
      fontSize = (20 * scaleMultiplier).sp,
      lineHeight = (26 * scaleMultiplier).sp,
    ),
    titleMedium = TextStyle(
      fontFamily = family,
      fontWeight = FontWeight.SemiBold,
      fontSize = (16 * scaleMultiplier).sp,
      lineHeight = (22 * scaleMultiplier).sp,
    ),
    titleSmall = TextStyle(
      fontFamily = family,
      fontWeight = FontWeight.Medium,
      fontSize = (14 * scaleMultiplier).sp,
      lineHeight = (20 * scaleMultiplier).sp,
    ),
    labelLarge = TextStyle(
      fontFamily = family,
      fontWeight = FontWeight.Bold,
      fontSize = (13 * scaleMultiplier).sp,
    ),
    labelMedium = TextStyle(
      fontFamily = family,
      fontWeight = FontWeight.Medium,
      fontSize = (11 * scaleMultiplier).sp,
    ),
    labelSmall = TextStyle(
      fontFamily = family,
      fontWeight = FontWeight.Medium,
      fontSize = (10 * scaleMultiplier).sp,
    )
  )
}

// Fallback constant for backwards compatibility
val Typography = getAppTypography("DEFAULT_SANS", "STANDARD")
