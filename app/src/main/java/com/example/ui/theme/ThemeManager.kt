package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import org.json.JSONObject
import java.util.Calendar

enum class AppThemeId(
  val code: String,
  val displayName: String,
  val banglaName: String,
  val emoji: String,
  val motif: String,
  val accentColor: Color
) {
  AUTO("AUTO", "Auto (Seasonal & Day/Night)", "Auto Mode", "🌟", "Smart Seasonal & Day/Night sync", Color(0xFFF59E0B)),
  DAY("DAY", "Day / Light Mode", "Day / Light Mode", "☀️", "Corporate Navy Blue & Gold", Color(0xFF0F2C59)),
  NIGHT("NIGHT", "Night / Dark Mode", "Night / Dark Mode", "🌙", "Deep Navy & Cyber Gold", Color(0xFF1E3A8A)),
  SUMMER("SUMMER", "Summer (Boishakh & Jaishtha)", "Summer Season", "🌾", "Warm Amber & Golden Sunshine", Color(0xFFEA580C)),
  RAINY("RAINY", "Rainy (Asharh & Shrabon)", "Monsoon Season", "🌧️", "Fresh Rain & Aqua Cyan", Color(0xFF0891B2)),
  WINTER("WINTER", "Winter (Mist & Dew)", "Winter Season", "❄️", "Mist, Dew & Frost Blue", Color(0xFF38BDF8)),
  PUJA("PUJA", "Sharadiya Durga Puja", "Durga Puja Festival", "🪔", "Autumn Festivities & Vermilion", Color(0xFFB91C1C)),
  EID("EID", "Eid Celebration", "Eid Celebration", "✨", "Emerald Green & Festive Gold", Color(0xFF047857)),
  LANGUAGE_DAY("LANGUAGE_DAY", "International Mother Language Day", "Language Day", "🏛️", "Memorial Crimson & Black", Color(0xFFDC2626)),
  INDEPENDENCE_DAY("INDEPENDENCE_DAY", "Independence & National Day", "Independence Day", "🇧🇩", "National Forest Green & Red", Color(0xFF006A4E))
}

data class AppThemeConfig(
  val id: AppThemeId,
  val name: String,
  val banglaName: String,
  val primary: Color,
  val secondary: Color,
  val tertiary: Color,
  val background: Color,
  val surface: Color,
  val isDark: Boolean = false,
  val onPrimary: Color = Color.White,
  val onBackground: Color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A),
  val onSurface: Color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A)
) {
  fun toM3ColorScheme(): ColorScheme {
    return if (isDark) {
      darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = secondary,
        onPrimaryContainer = Color(0xFFD2E3FC),
        secondary = secondary,
        onSecondary = Color.White,
        secondaryContainer = secondary.copy(alpha = 0.5f),
        onSecondaryContainer = Color(0xFFE8F0FE),
        tertiary = tertiary,
        onTertiary = Color.Black,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surface.copy(alpha = 0.85f),
        onSurfaceVariant = Color(0xFFCBD5E1),
        outline = secondary.copy(alpha = 0.6f)
      )
    } else {
      lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primary.copy(alpha = 0.12f),
        onPrimaryContainer = primary,
        secondary = secondary,
        onSecondary = Color.White,
        secondaryContainer = secondary.copy(alpha = 0.12f),
        onSecondaryContainer = secondary,
        tertiary = tertiary,
        onTertiary = Color.White,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = Color(0xFFF1F5F9),
        onSurfaceVariant = Color(0xFF334155),
        outline = Color(0xFFCBD5E1)
      )
    }
  }

  fun toJson(): String {
    val obj = JSONObject()
    obj.put("primary", colorToHex(primary))
    obj.put("secondary", colorToHex(secondary))
    obj.put("tertiary", colorToHex(tertiary))
    obj.put("background", colorToHex(background))
    obj.put("surface", colorToHex(surface))
    return obj.toString()
  }

  companion object {
    fun colorToHex(c: Color): String {
      val a = (c.alpha * 255).toInt()
      val r = (c.red * 255).toInt()
      val g = (c.green * 255).toInt()
      val b = (c.blue * 255).toInt()
      return "#%02X%02X%02X%02X".format(a, r, g, b)
    }

    fun parseHexColor(hex: String, default: Color): Color {
      return try {
        val clean = hex.trim().removePrefix("#")
        when (clean.length) {
          6 -> {
            val num = clean.toLong(16)
            Color(0xFF000000 or num)
          }
          8 -> {
            val num = clean.toLong(16)
            Color(num)
          }
          else -> default
        }
      } catch (_: Exception) {
        default
      }
    }
  }
}

val LocalAppTheme = compositionLocalOf { ThemeManager.DefaultDayTheme }

object ThemeManager {

  // 1. Day / Light Mode
  val DefaultDayTheme = AppThemeConfig(
    id = AppThemeId.DAY,
    name = "Day / Light Mode",
    banglaName = "Day / Light Mode",
    primary = Color(0xFF0A192F),
    secondary = Color(0xFF0F2C59),
    tertiary = Color(0xFFC99700),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    isDark = false
  )

  // 2. Night / Dark Mode
  val DefaultNightTheme = AppThemeConfig(
    id = AppThemeId.NIGHT,
    name = "Night / Dark Mode",
    banglaName = "Night / Dark Mode",
    primary = Color(0xFF8AB4F8),
    secondary = Color(0xFF1E3A8A),
    tertiary = Color(0xFFFDE047),
    background = Color(0xFF0B1422),
    surface = Color(0xFF101E33),
    onPrimary = Color(0xFF041E42),
    isDark = true
  )

  // 3. Summer Theme
  val DefaultSummerTheme = AppThemeConfig(
    id = AppThemeId.SUMMER,
    name = "Summer / Boishakh",
    banglaName = "Summer Season",
    primary = Color(0xFFC2410C), // Warm Sunset Amber
    secondary = Color(0xFFEA580C),
    tertiary = Color(0xFFB45309), // High-contrast warm amber
    background = Color(0xFFFFFDF7), // Soft sunlit cream
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1C1917),
    onSurface = Color(0xFF1C1917),
    isDark = false
  )

  // 4. Rainy Theme
  val DefaultRainyTheme = AppThemeConfig(
    id = AppThemeId.RAINY,
    name = "Rainy / Monsoon",
    banglaName = "Monsoon Season",
    primary = Color(0xFF0E7490), // Monsoon Deep Aqua Cyan
    secondary = Color(0xFF155E75),
    tertiary = Color(0xFF0369A1), // Sky rain blue
    background = Color(0xFFF0F9FF), // Fresh raindrop tint
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0C4A6E),
    onSurface = Color(0xFF0C4A6E),
    isDark = false
  )

  // 5. Winter Theme
  val DefaultWinterTheme = AppThemeConfig(
    id = AppThemeId.WINTER,
    name = "Winter / Fog & Frost",
    banglaName = "Winter Season",
    primary = Color(0xFF1E293B), // Slate mist deep
    secondary = Color(0xFF334155),
    tertiary = Color(0xFF0284C7), // Crisp frost blue
    background = Color(0xFFF8FAFC), // Chilly pearl
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    isDark = false
  )

  // 6. Sharadiya Durga Puja Theme
  val DefaultPujaTheme = AppThemeConfig(
    id = AppThemeId.PUJA,
    name = "Sharadiya Durga Puja",
    banglaName = "Durga Puja Festival",
    primary = Color(0xFF991B1B), // Festive Vermilion Red
    secondary = Color(0xFF7F1D1D),
    tertiary = Color(0xFFB45309), // Marigold Gold
    background = Color(0xFFFFFBEB), // Festive warm cream
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF450A0A),
    onSurface = Color(0xFF450A0A),
    isDark = false
  )

  // 7. Eid Celebration Theme
  val DefaultEidTheme = AppThemeConfig(
    id = AppThemeId.EID,
    name = "Eid Celebration",
    banglaName = "Eid Celebration",
    primary = Color(0xFF065F46), // Crescent Emerald Green
    secondary = Color(0xFF064E3B),
    tertiary = Color(0xFFB45309), // Royal festive gold
    background = Color(0xFFF0FDF4), // Fresh Eid emerald tint
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF022C22),
    onSurface = Color(0xFF022C22),
    isDark = false
  )

  // 8. International Mother Language Day Theme
  val DefaultLanguageDayTheme = AppThemeConfig(
    id = AppThemeId.LANGUAGE_DAY,
    name = "Language Day (21st Feb)",
    banglaName = "Language Day",
    primary = Color(0xFF18181B), // Memorial deep charcoal black
    secondary = Color(0xFF27272A),
    tertiary = Color(0xFFB91C1C), // Shahid Minar Crimson Red
    background = Color(0xFFFAFAFA), // Solemn marble white
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF09090B),
    onSurface = Color(0xFF09090B),
    isDark = false
  )

  // 9. Independence & National Day Theme
  val DefaultIndependenceDayTheme = AppThemeConfig(
    id = AppThemeId.INDEPENDENCE_DAY,
    name = "Independence Day (26th Mar)",
    banglaName = "Independence Day",
    primary = Color(0xFF006A4E), // Bangladesh Flag National Forest Green
    secondary = Color(0xFF04523B),
    tertiary = Color(0xFFB91C1C), // High-contrast deep flag red
    background = Color(0xFFF2FBF6), // Subtle mint tint
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF022C22),
    onSurface = Color(0xFF022C22),
    isDark = false
  )

  val allDefaultThemes = listOf(
    DefaultDayTheme,
    DefaultNightTheme,
    DefaultSummerTheme,
    DefaultRainyTheme,
    DefaultWinterTheme,
    DefaultPujaTheme,
    DefaultEidTheme,
    DefaultLanguageDayTheme,
    DefaultIndependenceDayTheme
  )

  /**
   * Automatic seasonal & holiday switching logic for Bangladesh calendar & Gregorian months
   */
  fun determineSeasonalTheme(cal: Calendar = Calendar.getInstance()): AppThemeConfig {
    val month = cal.get(Calendar.MONTH) // 0-indexed: Jan=0, Feb=1, Mar=2, ..., Dec=11
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val hour = cal.get(Calendar.HOUR_OF_DAY) // 0..23

    // 1. Language Day window: Feb 15 - Feb 28
    if (month == Calendar.FEBRUARY && day in 15..28) {
      return DefaultLanguageDayTheme
    }

    // 2. Independence Day (March 20-31) & Victory Day (Dec 14-20)
    if ((month == Calendar.MARCH && day in 20..31) || (month == Calendar.DECEMBER && day in 14..20)) {
      return DefaultIndependenceDayTheme
    }

    // 3. Autumn / Durga Puja season: September 20 - October 31
    if ((month == Calendar.SEPTEMBER && day >= 20) || (month == Calendar.OCTOBER)) {
      return DefaultPujaTheme
    }

    // 4. Summer season (গ্রীষ্মকাল): April 1 to May 31
    if (month == Calendar.APRIL || month == Calendar.MAY) {
      return DefaultSummerTheme
    }

    // 5. Rainy season (বর্ষাকাল): June 1 to August 15
    if (month == Calendar.JUNE || month == Calendar.JULY || (month == Calendar.AUGUST && day <= 15)) {
      return DefaultRainyTheme
    }

    // 6. Winter season (শীতকাল): November 15 to February 14
    if ((month == Calendar.NOVEMBER && day >= 15) || month == Calendar.DECEMBER || month == Calendar.JANUARY || (month == Calendar.FEBRUARY && day < 15)) {
      return DefaultWinterTheme
    }

    // 7. Day / Night auto-switch by time of day: 6:30 PM (18:30) to 6:00 AM -> Night
    return if (hour >= 19 || hour < 6) {
      DefaultNightTheme
    } else {
      DefaultDayTheme
    }
  }

  fun getThemeById(idStr: String, customJson: String? = null, universalFontColorHex: String? = null): AppThemeConfig {
    val themeId = try {
      AppThemeId.valueOf(idStr.trim().uppercase())
    } catch (_: Exception) {
      if (idStr.equals("AUTO", true)) AppThemeId.AUTO else AppThemeId.DAY
    }

    val baseTheme = when (themeId) {
      AppThemeId.AUTO -> determineSeasonalTheme()
      AppThemeId.DAY -> DefaultDayTheme
      AppThemeId.NIGHT -> DefaultNightTheme
      AppThemeId.SUMMER -> DefaultSummerTheme
      AppThemeId.RAINY -> DefaultRainyTheme
      AppThemeId.WINTER -> DefaultWinterTheme
      AppThemeId.PUJA -> DefaultPujaTheme
      AppThemeId.EID -> DefaultEidTheme
      AppThemeId.LANGUAGE_DAY -> DefaultLanguageDayTheme
      AppThemeId.INDEPENDENCE_DAY -> DefaultIndependenceDayTheme
    }

    val customized = if (!customJson.isNullOrBlank()) {
      try {
        val obj = JSONObject(customJson)
        val pri = if (obj.has("primary")) AppThemeConfig.parseHexColor(obj.getString("primary"), baseTheme.primary) else baseTheme.primary
        val sec = if (obj.has("secondary")) AppThemeConfig.parseHexColor(obj.getString("secondary"), baseTheme.secondary) else baseTheme.secondary
        val ter = if (obj.has("tertiary")) AppThemeConfig.parseHexColor(obj.getString("tertiary"), baseTheme.tertiary) else baseTheme.tertiary
        val bg = if (obj.has("background")) AppThemeConfig.parseHexColor(obj.getString("background"), baseTheme.background) else baseTheme.background
        val srf = if (obj.has("surface")) AppThemeConfig.parseHexColor(obj.getString("surface"), baseTheme.surface) else baseTheme.surface
        baseTheme.copy(
          primary = pri,
          secondary = sec,
          tertiary = ter,
          background = bg,
          surface = srf
        )
      } catch (_: Exception) {
        baseTheme
      }
    } else {
      baseTheme
    }

    return if (!universalFontColorHex.isNullOrBlank()) {
      val parsedFontColor = AppThemeConfig.parseHexColor(universalFontColorHex, customized.onSurface)
      val lum = (parsedFontColor.red * 0.299f + parsedFontColor.green * 0.587f + parsedFontColor.blue * 0.114f)
      if (customized.isDark) {
        if (lum < 0.45f) {
          customized.copy(
            onBackground = Color(0xFFF8FAFC),
            onSurface = Color(0xFFF8FAFC)
          )
        } else {
          customized.copy(
            onBackground = parsedFontColor,
            onSurface = parsedFontColor
          )
        }
      } else {
        if (lum > 0.55f) {
          customized.copy(
            onBackground = Color(0xFF0F172A),
            onSurface = Color(0xFF0F172A)
          )
        } else {
          customized.copy(
            onBackground = parsedFontColor,
            onSurface = parsedFontColor
          )
        }
      }
    } else {
      customized
    }
  }
}
