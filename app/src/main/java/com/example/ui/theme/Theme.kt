package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
  primary = EblNavyPrimary,
  onPrimary = Color.White,
  primaryContainer = EblNavyContainer,
  onPrimaryContainer = EblNavyDark,
  secondary = EblNavySecondary,
  onSecondary = Color.White,
  secondaryContainer = EblNavyLight,
  onSecondaryContainer = EblNavyPrimary,
  tertiary = EblGold,
  onTertiary = Color.White,
  tertiaryContainer = EblGoldLight,
  onTertiaryContainer = EblGoldDark,
  background = EblBackgroundLight,
  onBackground = EblTextPrimary,
  surface = EblSurfaceLight,
  onSurface = EblTextPrimary,
  surfaceVariant = EblSurfaceVariant,
  onSurfaceVariant = EblTextSecondary,
  outline = EblOutline
)

private val DarkColorScheme = darkColorScheme(
  primary = Color(0xFF8AB4F8),
  onPrimary = Color(0xFF041E42),
  primaryContainer = Color(0xFF082C5E),
  onPrimaryContainer = Color(0xFFD2E3FC),
  secondary = Color(0xFFAECBFA),
  onSecondary = Color(0xFF041E42),
  secondaryContainer = Color(0xFF133E75),
  onSecondaryContainer = Color(0xFFE8F0FE),
  tertiary = EblGold,
  onTertiary = Color.Black,
  background = Color(0xFF0B1422),
  onBackground = Color(0xFFE2E8F0),
  surface = Color(0xFF101E33),
  onSurface = Color(0xFFE2E8F0),
  surfaceVariant = Color(0xFF192A44),
  onSurfaceVariant = Color(0xFF94A3B8),
  outline = Color(0xFF2C3E5B)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        window.statusBarColor = colorScheme.primary.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
      }
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
