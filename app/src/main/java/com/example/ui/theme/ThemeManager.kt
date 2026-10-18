package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import org.json.JSONObject
import java.util.Calendar

enum class AppThemeId(val code: String, val displayName: String, val banglaName: String) {
  AUTO("AUTO", "Auto (Seasonal & Day/Night)", "স্বয়ংক্রিয় (ঋতু ও দিন/রাত)"),
  DAY("DAY", "Day / Light Mode", "দিন / হালকা মোড"),
  NIGHT("NIGHT", "Night / Dark Mode", "রাত / ডার্ক মোড"),
  SUMMER("SUMMER", "Summer (Boishakh & Jaishtha)", "গ্রীষ্মকাল (বৈশাখ ও জ্যৈষ্ঠ)"),
  RAINY("RAINY", "Rainy (Asharh & Shrabon)", "বর্ষাকাল (আষাঢ় ও শ্রাবণ)"),
  WINTER("WINTER", "Winter (Mist & Dew)", "শীতকাল (কুয়াশা ও শিশির)"),
  PUJA("PUJA", "Sharadiya Durga Puja", "শারদীয় দুর্গোৎসব"),
  EID("EID", "Eid Celebration", "পবিত্র ঈদ আনন্দ"),
  LANGUAGE_DAY("LANGUAGE_DAY", "International Mother Language Day", "আন্তর্জাতিক মাতৃভাষা দিবস (২১শে ফেব্রুয়ারি)"),
  INDEPENDENCE_DAY("INDEPENDENCE_DAY", "Independence & National Day", "মহান স্বাধীনতা ও জাতীয় দিবস (২৬শে মার্চ)")
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
        onSurfaceVariant = Color(0xFF94A3B8),
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
        onSurfaceVariant = Color(0xFF64748B),
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
    banglaName = "দিন / হালকা মোড",
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
    banglaName = "রাত / ডার্ক মোড",
    primary = Color(0xFF8AB4F8),
    secondary = Color(0xFF1E3A8A),
    tertiary = Color(0xFFFDE047),
    background = Color(0xFF0B1422),
    surface = Color(0xFF101E33),
    onPrimary = Color(0xFF041E42),
    isDark = true
  )

  // 3. Summer Theme (গ্রীষ্মকালীন রোদ্দুর ও সোনালী বৈশাখী আভা)
  val DefaultSummerTheme = AppThemeConfig(
    id = AppThemeId.SUMMER,
    name = "Summer / Boishakh",
    banglaName = "গ্রীষ্মকাল (বৈশাখ ও জ্যৈষ্ঠ)",
    primary = Color(0xFFC2410C), // Warm Sunset Amber
    secondary = Color(0xFFEA580C),
    tertiary = Color(0xFFF59E0B), // Warm Marigold Sun
    background = Color(0xFFFFFDF7), // Soft sunlit cream
    surface = Color(0xFFFFFFFF),
    isDark = false
  )

  // 4. Rainy Theme (বর্ষাকালীন সতেজ মেঘ ও নীলচে জল)
  val DefaultRainyTheme = AppThemeConfig(
    id = AppThemeId.RAINY,
    name = "Rainy / Monsoon",
    banglaName = "বর্ষাকাল (আষাঢ় ও শ্রাবণ)",
    primary = Color(0xFF0891B2), // Monsoon Deep Aqua Cyan
    secondary = Color(0xFF0E7490),
    tertiary = Color(0xFF0284C7), // Sky rain blue
    background = Color(0xFFF0F9FF), // Fresh raindrop tint
    surface = Color(0xFFFFFFFF),
    isDark = false
  )

  // 5. Winter Theme (শীতকালীন কুয়াশা ও শিশির ভেজা বরফ নীল)
  val DefaultWinterTheme = AppThemeConfig(
    id = AppThemeId.WINTER,
    name = "Winter / Fog & Frost",
    banglaName = "শীতকাল (কুয়াশা ও শিশির)",
    primary = Color(0xFF334155), // Slate mist
    secondary = Color(0xFF475569),
    tertiary = Color(0xFF38BDF8), // Crisp frost blue
    background = Color(0xFFF8FAFC), // Chilly pearl
    surface = Color(0xFFFFFFFF),
    isDark = false
  )

  // 6. Sharadiya Durga Puja Theme (কাশফুল, শিউলি ও সিঁদুর রাঙা উৎসব)
  val DefaultPujaTheme = AppThemeConfig(
    id = AppThemeId.PUJA,
    name = "Sharadiya Durga Puja",
    banglaName = "শারদীয় দুর্গোৎসব",
    primary = Color(0xFFB91C1C), // Festive Vermilion Red
    secondary = Color(0xFF991B1B),
    tertiary = Color(0xFFD97706), // Marigold Gold
    background = Color(0xFFFFFBEB), // Festive warm cream
    surface = Color(0xFFFFFFFF),
    isDark = false
  )

  // 7. Eid Celebration Theme (স্নিগ্ধ চাঁদের সবুজ ও উৎসবের সোনালী)
  val DefaultEidTheme = AppThemeConfig(
    id = AppThemeId.EID,
    name = "Eid Celebration",
    banglaName = "পবিত্র ঈদ আনন্দ",
    primary = Color(0xFF047857), // Crescent Emerald Green
    secondary = Color(0xFF065F46),
    tertiary = Color(0xFFD97706), // Royal festive gold
    background = Color(0xFFF0FDF4), // Fresh Eid emerald tint
    surface = Color(0xFFFFFFFF),
    isDark = false
  )

  // 8. International Mother Language Day Theme (২১শে ফেব্রুয়ারি রক্তস্নাত শহিদ মিনার স্মারক)
  val DefaultLanguageDayTheme = AppThemeConfig(
    id = AppThemeId.LANGUAGE_DAY,
    name = "Language Day (21st Feb)",
    banglaName = "আন্তর্জাতিক মাতৃভাষা দিবস",
    primary = Color(0xFF18181B), // Memorial deep charcoal black
    secondary = Color(0xFF3F3F46),
    tertiary = Color(0xFFDC2626), // Shahid Minar Crimson Red
    background = Color(0xFFFAFAFA), // Solemn marble white
    surface = Color(0xFFFFFFFF),
    isDark = false
  )

  // 9. Independence & National Day Theme (মহান স্বাধীনতা ও জাতীয় দিবস - লাল সবুজের বাংলাদেশ)
  val DefaultIndependenceDayTheme = AppThemeConfig(
    id = AppThemeId.INDEPENDENCE_DAY,
    name = "Independence Day (26th Mar)",
    banglaName = "মহান স্বাধীনতা ও জাতীয় দিবস",
    primary = Color(0xFF006A4E), // Bangladesh Flag National Forest Green
    secondary = Color(0xFF04523B),
    tertiary = Color(0xFFF42A41), // Bangladesh Flag Sun Red
    background = Color(0xFFF2FBF6), // Subtle mint tint
    surface = Color(0xFFFFFFFF),
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

  fun getThemeById(idStr: String, customJson: String? = null): AppThemeConfig {
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

    if (customJson.isNullOrBlank()) return baseTheme

    return try {
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
  }
}
