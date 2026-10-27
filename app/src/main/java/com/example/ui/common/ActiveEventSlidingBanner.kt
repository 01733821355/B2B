package com.example.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TeamEventEntity
import com.example.ui.theme.EblGold

@Composable
fun ActiveEventSlidingBanner(
  activeEvents: List<TeamEventEntity>,
  onEventClick: (TeamEventEntity) -> Unit,
  modifier: Modifier = Modifier
) {
  if (activeEvents.isEmpty()) return

  // Pulsing animation for the LIVE event indicator dot
  val infiniteTransition = rememberInfiniteTransition(label = "live_event_pulse")
  val alphaPulse by infiniteTransition.animateFloat(
    initialValue = 0.35f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "live_dot_alpha"
  )

  // Construct scrolling ticker text showing active event details
  val bannerText = activeEvents.joinToString(separator = "    ★★★    ") { event ->
    val deadlineText = if (event.closeTime.isNotBlank()) "⏳ শেষ সময়: ${event.closeTime}" else "📅 তারিখ: ${event.targetDate}"
    "📢 চলমান ইভেন্ট: '${event.title}' • $deadlineText • 🎯 লক্ষ্য: ${event.targetDate} • ${event.description.take(60)}${if (event.description.length > 60) "..." else ""} • তথ্য এন্ট্রি ও বিস্তারিত দেখতে ট্যাপ করুন ➔"
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("active_event_sliding_banner")
      .clickable {
        activeEvents.firstOrNull()?.let { onEventClick(it) }
      },
    color = Color(0xFF0F172A),
    shadowElevation = 4.dp
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.horizontalGradient(
            colors = listOf(
              Color(0xFF0F172A),
              Color(0xFF1E3A8A),
              Color(0xFF0F172A)
            )
          )
        )
        .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Pulsing Live Event Tag
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .background(
              color = Color(0xFFDC2626).copy(alpha = 0.25f),
              shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .alpha(alphaPulse)
              .background(Color(0xFFEF4444), CircleShape)
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = "LIVE EVENT",
            color = Color(0xFFFCA5A5),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Center Sliding Marquee Notification Label
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(4.dp))
        ) {
          Text(
            text = bannerText,
            color = Color(0xFFFEF08A), // Warm gold highlight
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.basicMarquee(
              iterations = Int.MAX_VALUE,
              velocity = 45.dp
            )
          )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Arrow Action Icon
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
          contentDescription = "Open Event Hub",
          tint = EblGold,
          modifier = Modifier.size(14.dp)
        )
      }
    }
  }
}
