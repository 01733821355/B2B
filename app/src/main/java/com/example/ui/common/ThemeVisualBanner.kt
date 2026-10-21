package com.example.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AppThemeId
import com.example.ui.theme.EblGold
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.LocalAppTheme
import com.example.util.DateUtils
import kotlinx.coroutines.delay

/**
 * Dynamic theme-aware, time-dependent visual photo banner card.
 * Changes photographic artwork, illustration, greeting, and ambient styling based on:
 * 1. Current Active Theme (Day, Night, Summer, Rainy, Winter, Puja, Eid, etc.)
 * 2. Time of day in Dhaka (Morning, Afternoon, Evening, Night)
 */
@Composable
fun ThemeVisualBanner(
  title: String,
  subtitle: String,
  badgeText: String? = null,
  showLiveClock: Boolean = true,
  actionContent: (@Composable () -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val currentTheme = LocalAppTheme.current
  val timePeriod = remember { DateUtils.getCurrentTimeOfDayPeriod() }

  var currentMillis by remember { mutableStateOf(DateUtils.currentDhakaMillis()) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(30_000) // update clock every 30s
      currentMillis = DateUtils.currentDhakaMillis()
    }
  }

  // Determine dynamic art drawable based on theme and time of day
  val artDrawableRes = when (currentTheme.id) {
    AppThemeId.NIGHT -> R.drawable.ic_theme_night_art
    AppThemeId.SUMMER -> R.drawable.ic_theme_summer_art
    AppThemeId.RAINY -> R.drawable.ic_theme_rainy_art
    AppThemeId.WINTER -> R.drawable.ic_theme_winter_art
    AppThemeId.PUJA -> R.drawable.ic_theme_puja_art
    AppThemeId.EID -> R.drawable.ic_theme_eid_art
    AppThemeId.LANGUAGE_DAY -> R.drawable.ic_theme_language_art
    AppThemeId.INDEPENDENCE_DAY -> R.drawable.ic_theme_independence_art
    AppThemeId.DAY, AppThemeId.AUTO -> {
      if (timePeriod == DateUtils.TimeOfDayPeriod.NIGHT) {
        R.drawable.ic_theme_night_art
      } else {
        R.drawable.ic_theme_day_art
      }
    }
  }

  val formattedTime = remember(currentMillis) {
    DateUtils.formatDateTime(currentMillis)
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("theme_visual_banner"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = currentTheme.primary),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(185.dp)
    ) {
      // Background Corporate Photo with subtle gradient blend
      Image(
        painter = painterResource(id = R.drawable.img_corporate_team),
        contentDescription = "Corporate Office",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
        alpha = 0.18f
      )

      // Theme Art Artwork on Right Edge
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .size(width = 190.dp, height = 120.dp)
          .padding(end = 4.dp, bottom = 4.dp)
          .clip(RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
      ) {
        Image(
          painter = painterResource(id = artDrawableRes),
          contentDescription = "Theme Illustration",
          contentScale = ContentScale.FillBounds,
          modifier = Modifier.fillMaxSize(),
          alpha = 0.88f
        )
      }

      // Layered Gradient Scrim to ensure crisp contrast for text
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.horizontalGradient(
              listOf(
                currentTheme.primary.copy(alpha = 0.96f),
                currentTheme.primary.copy(alpha = 0.88f),
                currentTheme.secondary.copy(alpha = 0.60f),
                Color.Transparent
              )
            )
          )
      )

      // Banner Content
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        // Top Row: Time Greeting & Live Time / Theme Chip
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Greeting & Time Badge
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = currentTheme.secondary.copy(alpha = 0.9f),
            border = androidx.compose.foundation.BorderStroke(1.dp, currentTheme.tertiary.copy(alpha = 0.6f))
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = timePeriod.emoji,
                fontSize = 11.sp
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = timePeriod.greeting,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                softWrap = false
              )
            }
          }

          // Active Theme Badge with Emoji
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.45f)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = currentTheme.id.emoji,
                fontSize = 11.sp
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = currentTheme.name,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = currentTheme.tertiary,
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }

        // Center Content: Title, Subtitle, Details
        Column(modifier = Modifier.fillMaxWidth(0.72f)) {
          Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = subtitle,
            fontSize = 11.sp,
            color = currentTheme.tertiary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false
          )
          if (badgeText != null) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
              text = badgeText,
              fontSize = 10.sp,
              color = Color.White.copy(alpha = 0.85f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              softWrap = false
            )
          }
        }

        // Bottom Row: Live Time and Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (showLiveClock) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = "Time",
                tint = currentTheme.tertiary,
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = formattedTime,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium
              )
            }
          }

          if (actionContent != null) {
            actionContent()
          }
        }
      }
    }
  }
}
