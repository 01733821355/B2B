package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val OutfitFontFamily = FontFamily(
  Font(R.font.outfit, FontWeight.Normal),
  Font(R.font.outfit, FontWeight.Medium),
  Font(R.font.outfit, FontWeight.SemiBold),
  Font(R.font.outfit, FontWeight.Bold),
  Font(R.font.outfit, FontWeight.ExtraBold)
)

val PlusJakartaFontFamily = FontFamily(
  Font(R.font.plus_jakarta_sans, FontWeight.Normal),
  Font(R.font.plus_jakarta_sans, FontWeight.Medium),
  Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
  Font(R.font.plus_jakarta_sans, FontWeight.Bold),
  Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold)
)

val JetBrainsMonoFontFamily = FontFamily(
  Font(R.font.jetbrains_mono, FontWeight.Normal),
  Font(R.font.jetbrains_mono, FontWeight.Medium),
  Font(R.font.jetbrains_mono, FontWeight.Bold)
)

/**
 * Universal dynamic typography generator responsive to Admin's font style
 * and text scaling preferences, backed by bundled Google Fonts.
 */
fun getAppTypography(fontStyle: String = "DEFAULT_SANS", textScale: String = "STANDARD"): Typography {
  val (displayFamily, bodyFamily) = when (fontStyle.uppercase().trim()) {
    "EXECUTIVE_SERIF", "SERIF" -> FontFamily.Serif to PlusJakartaFontFamily
    "TECHNICAL_MONO", "MONO", "MONOSPACE" -> JetBrainsMonoFontFamily to JetBrainsMonoFontFamily
    "CURSIVE" -> FontFamily.Cursive to PlusJakartaFontFamily
    "CLEAN_SANS", "SANS", "DEFAULT_SANS" -> OutfitFontFamily to PlusJakartaFontFamily
    else -> OutfitFontFamily to PlusJakartaFontFamily
  }

  val scaleMultiplier = when (textScale.uppercase().trim()) {
    "COMPACT" -> 0.92f
    "COMFORTABLE", "LARGE" -> 1.15f
    else -> 1.0f
  }

  return Typography(
    displayLarge = TextStyle(
      fontFamily = displayFamily,
      fontWeight = FontWeight.ExtraBold,
      fontSize = (36 * scaleMultiplier).sp,
      lineHeight = (42 * scaleMultiplier).sp,
      letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
      fontFamily = displayFamily,
      fontWeight = FontWeight.Bold,
      fontSize = (24 * scaleMultiplier).sp,
      lineHeight = (30 * scaleMultiplier).sp,
      letterSpacing = (-0.25).sp
    ),
    bodyLarge = TextStyle(
      fontFamily = bodyFamily,
      fontWeight = FontWeight.Normal,
      fontSize = (16 * scaleMultiplier).sp,
      lineHeight = (24 * scaleMultiplier).sp,
      letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
      fontFamily = bodyFamily,
      fontWeight = FontWeight.Normal,
      fontSize = (14 * scaleMultiplier).sp,
      lineHeight = (20 * scaleMultiplier).sp,
      letterSpacing = 0.15.sp
    ),
    bodySmall = TextStyle(
      fontFamily = bodyFamily,
      fontWeight = FontWeight.Normal,
      fontSize = (12 * scaleMultiplier).sp,
      lineHeight = (16 * scaleMultiplier).sp
    ),
    titleLarge = TextStyle(
      fontFamily = displayFamily,
      fontWeight = FontWeight.Bold,
      fontSize = (20 * scaleMultiplier).sp,
      lineHeight = (26 * scaleMultiplier).sp,
      letterSpacing = (-0.15).sp
    ),
    titleMedium = TextStyle(
      fontFamily = displayFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = (16 * scaleMultiplier).sp,
      lineHeight = (22 * scaleMultiplier).sp
    ),
    titleSmall = TextStyle(
      fontFamily = displayFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = (14 * scaleMultiplier).sp,
      lineHeight = (20 * scaleMultiplier).sp
    ),
    labelLarge = TextStyle(
      fontFamily = bodyFamily,
      fontWeight = FontWeight.Bold,
      fontSize = (13 * scaleMultiplier).sp,
      letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
      fontFamily = bodyFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = (11 * scaleMultiplier).sp,
      letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
      fontFamily = bodyFamily,
      fontWeight = FontWeight.Medium,
      fontSize = (10 * scaleMultiplier).sp,
      letterSpacing = 0.3.sp
    )
  )
}

// Fallback constant for backwards compatibility
val Typography = getAppTypography("DEFAULT_SANS", "STANDARD")
