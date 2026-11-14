package com.example.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EblGold
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppTheme
import com.example.ui.viewmodel.KpiStats
import com.example.util.DateUtils

@Composable
fun TimeFilterBar(
  selectedFilter: DateUtils.TimeFilter,
  onFilterSelected: (DateUtils.TimeFilter) -> Unit,
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()
  val haptics = LocalHapticFeedback.current
  val currentTheme = LocalAppTheme.current

  Row(
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(scrollState)
      .padding(horizontal = 16.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    DateUtils.TimeFilter.values().forEach { filter ->
      val isSelected = filter == selectedFilter
      val interactionSource = remember { MutableInteractionSource() }
      val isPressed by interactionSource.collectIsPressedAsState()

      val scale by animateFloatAsState(
        targetValue = when {
          isPressed -> 0.93f
          isSelected -> 1.03f
          else -> 1f
        },
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessMedium
        ),
        label = "chip_scale"
      )

      val bgColor by animateColorAsState(
        targetValue = if (isSelected) currentTheme.primary else MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 200),
        label = "chip_bg"
      )

      val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "chip_fg"
      )

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        shadowElevation = if (isSelected) 4.dp else 1.dp,
        border = BorderStroke(
          width = if (isSelected) 1.2.dp else 0.8.dp,
          color = if (isSelected) currentTheme.tertiary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        ),
        modifier = Modifier
          .graphicsLayer {
            scaleX = scale
            scaleY = scale
          }
          .clip(RoundedCornerShape(14.dp))
          .clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.material3.ripple(),
            onClick = {
              haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onFilterSelected(filter)
            }
          )
          .testTag("time_filter_${filter.name}")
      ) {
        Text(
          text = filter.label,
          fontSize = 12.sp,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
          color = contentColor,
          maxLines = 1,
          softWrap = false,
          modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
        )
      }
    }
  }
}

@Composable
fun MetricKpiCard(
  title: String,
  value: String,
  icon: ImageVector,
  iconTint: Color,
  bgTint: Color,
  subtitle: String? = null,
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val isDark = LocalAppTheme.current.isDark
  val haptics = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val scale by animateFloatAsState(
    targetValue = if (isPressed && onClick != null) 0.955f else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium
    ),
    label = "kpi_card_scale"
  )

  val displayTint = remember(iconTint, isDark) {
    if (!isDark) iconTint
    else Color(
      red = (iconTint.red * 0.55f + 0.45f).coerceIn(0f, 1f),
      green = (iconTint.green * 0.55f + 0.45f).coerceIn(0f, 1f),
      blue = (iconTint.blue * 0.55f + 0.45f).coerceIn(0f, 1f),
      alpha = 1f
    )
  }

  val numericValue = remember(value) { value.toIntOrNull() }

  Card(
    modifier = modifier
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .clip(RoundedCornerShape(18.dp))
      .then(
        if (onClick != null) {
          Modifier.clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.material3.ripple(color = displayTint),
            onClick = {
              haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onClick()
            }
          )
        } else Modifier
      )
      .testTag("kpi_card_${title.lowercase().replace(" ", "_")}"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isPressed) 1.dp else 5.dp),
    border = BorderStroke(
      1.dp,
      if (isDark) displayTint.copy(alpha = 0.35f) else iconTint.copy(alpha = 0.16f)
    )
  ) {
    Column {
      // Subtle top hairline metallic accent
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(2.5.dp)
          .background(
            Brush.horizontalGradient(
              listOf(displayTint.copy(alpha = 0.85f), displayTint.copy(alpha = 0.25f), Color.Transparent)
            )
          )
      )
      Column(modifier = Modifier.padding(13.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // 3D Glossy Beveled Icon Medallion
          Box(
            modifier = Modifier
              .size(40.dp)
              .background(iconTint.copy(alpha = if (isDark) 0.55f else 0.35f), RoundedCornerShape(12.dp))
              .padding(bottom = 2.5.dp)
              .background(
                Brush.verticalGradient(
                  listOf(
                    iconTint.copy(alpha = if (isDark) 0.92f else 0.95f),
                    Color(
                      red = (iconTint.red * 0.72f),
                      green = (iconTint.green * 0.72f),
                      blue = (iconTint.blue * 0.72f),
                      alpha = 1f
                    )
                  )
                ),
                RoundedCornerShape(11.dp)
              ),
            contentAlignment = Alignment.Center
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .align(Alignment.TopCenter)
                .background(
                  Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)),
                  RoundedCornerShape(topStart = 11.dp, topEnd = 11.dp)
                )
            )
            Icon(
              imageVector = icon,
              contentDescription = title,
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }

          if (numericValue != null) {
            AnimatedCountText(
              targetCount = numericValue,
              color = displayTint,
              fontSize = 24.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = JetBrainsMonoFontFamily
            )
          } else {
            Text(
              text = value,
              fontSize = 22.sp,
              fontWeight = FontWeight.ExtraBold,
              color = displayTint
            )
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = title,
          fontSize = 12.5.sp,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          softWrap = false
        )
        if (subtitle != null) {
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = subtitle,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false
          )
        }
      }
    }
  }
}

@Composable
fun KpiGridSection(
  stats: KpiStats,
  onKpiClick: ((String) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val haptics = LocalHapticFeedback.current
  val heroInteraction = remember { MutableInteractionSource() }
  val isHeroPressed by heroInteraction.collectIsPressedAsState()
  val heroScale by animateFloatAsState(
    targetValue = if (isHeroPressed) 0.97f else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium
    ),
    label = "hero_stc_scale"
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
  ) {
    // 1. PREMIER HERO CARD: STC (Official Achievement / Production Done)
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .graphicsLayer {
          scaleX = heroScale
          scaleY = heroScale
        }
        .clip(RoundedCornerShape(18.dp))
        .clickable(
          interactionSource = heroInteraction,
          indication = androidx.compose.material3.ripple(color = Color.White),
          onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onKpiClick?.invoke("STC")
          }
        )
        .testTag("kpi_card_stc_achievement"),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0F766E)),
      elevation = CardDefaults.cardElevation(defaultElevation = if (isHeroPressed) 2.dp else 7.dp),
      border = BorderStroke(1.dp, EblGold.copy(alpha = 0.55f))
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(
            Brush.horizontalGradient(
              listOf(Color(0xFF0F766E), Color(0xFF115E59), Color(0xFF042F2E))
            )
          )
      ) {
        // Subtle live shimmer sheen
        Box(
          modifier = Modifier
            .matchParentSize()
            .background(rememberShimmerBrush(Color.White.copy(alpha = 0.1f)))
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            modifier = Modifier
              .weight(1f)
              .padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(50.dp)
                .background(Color(0xFF92400E).copy(alpha = 0.65f), CircleShape)
                .padding(bottom = 2.5.dp)
                .background(
                  Brush.verticalGradient(listOf(Color(0xFFFEF08A), EblGold, Color(0xFFD97706))),
                  CircleShape
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Stars,
                contentDescription = "STC Achievement",
                tint = EblNavyDark,
                modifier = Modifier.size(27.dp)
              )
            }
            Column {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                  text = "STC (Achievement / Done)",
                  fontSize = 14.5.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = Color.White,
                  maxLines = 1,
                  softWrap = false
                )
                Surface(
                  shape = RoundedCornerShape(5.dp),
                  color = EblGold
                ) {
                  Text(
                    text = "TARGET KPI",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EblNavyDark,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                    maxLines = 1,
                    softWrap = false
                  )
                }
              }
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                text = "Production Done • Official Goal Metric",
                fontSize = 11.sp,
                color = Color(0xFFCCFBF1),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
              )
            }
          }

          AnimatedCountText(
            targetCount = stats.stc,
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = JetBrainsMonoFontFamily
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 2. Primary 2-Column Row: Total Files & Collected
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      MetricKpiCard(
        title = "Total Files",
        value = stats.totalFiles.toString(),
        icon = Icons.Default.Assignment,
        iconTint = Color(0xFF1E3A8A),
        bgTint = Color(0xFFDBEAFE),
        subtitle = "Active files",
        onClick = { onKpiClick?.invoke("Total") },
        modifier = Modifier.weight(1f)
      )
      MetricKpiCard(
        title = "Collected",
        value = stats.collected.toString(),
        icon = Icons.Default.Assignment,
        iconTint = Color(0xFF0284C7),
        bgTint = Color(0xFFE0F2FE),
        subtitle = "Initial collection",
        onClick = { onKpiClick?.invoke("Collected") },
        modifier = Modifier.weight(1f)
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 3. Row 2: Submitted & Analyst Receive
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      MetricKpiCard(
        title = "Submitted",
        value = stats.submitted.toString(),
        icon = Icons.Default.Send,
        iconTint = Color(0xFF2563EB),
        bgTint = Color(0xFFEFF6FF),
        subtitle = "In processing",
        onClick = { onKpiClick?.invoke("Submitted") },
        modifier = Modifier.weight(1f)
      )
      MetricKpiCard(
        title = "Analyst Receive",
        value = stats.analystReceive.toString(),
        icon = Icons.Default.VerifiedUser,
        iconTint = Color(0xFF4338CA),
        bgTint = Color(0xFFEEF2FF),
        subtitle = "Under review",
        onClick = { onKpiClick?.invoke("Analyst Receive") },
        modifier = Modifier.weight(1f)
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 4. Row 3: Approved & Query
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      MetricKpiCard(
        title = "Approved",
        value = stats.approved.toString(),
        icon = Icons.Default.CheckCircle,
        iconTint = Color(0xFF15803D),
        bgTint = Color(0xFFDCFCE7),
        subtitle = "Awaiting STC card",
        onClick = { onKpiClick?.invoke("Approved") },
        modifier = Modifier.weight(1f)
      )
      MetricKpiCard(
        title = "Query",
        value = stats.query.toString(),
        icon = Icons.Default.Help,
        iconTint = Color(0xFFD97706),
        bgTint = Color(0xFFFEF3C7),
        subtitle = "RM action required",
        onClick = { onKpiClick?.invoke("Query") },
        modifier = Modifier.weight(1f)
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 5. Row 4: Return To Source (RTS) & Declined - SEPARATE!
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      MetricKpiCard(
        title = "Return To Source",
        value = stats.returnToSource.toString(),
        icon = Icons.Default.KeyboardReturn,
        iconTint = Color(0xFFE11D48),
        bgTint = Color(0xFFFFE4E6),
        subtitle = "RTS file returned",
        onClick = { onKpiClick?.invoke("Return To Source") },
        modifier = Modifier.weight(1f)
      )
      MetricKpiCard(
        title = "Declined",
        value = stats.declined.toString(),
        icon = Icons.Default.Cancel,
        iconTint = Color(0xFFDC2626),
        bgTint = Color(0xFFFEE2E2),
        subtitle = "Rejected files",
        onClick = { onKpiClick?.invoke("Declined") },
        modifier = Modifier.weight(1f)
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 6. Row 5: Condition & Pending Docs
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      MetricKpiCard(
        title = "Condition",
        value = stats.condition.toString(),
        icon = Icons.Default.Rule,
        iconTint = Color(0xFF9333EA),
        bgTint = Color(0xFFF3E8FF),
        subtitle = "Conditional files",
        onClick = { onKpiClick?.invoke("Condition") },
        modifier = Modifier.weight(1f)
      )
      MetricKpiCard(
        title = "Pending Docs",
        value = stats.pendingDocumentsCount.toString(),
        icon = Icons.Default.PendingActions,
        iconTint = Color(0xFF7C3AED),
        bgTint = Color(0xFFEDE9FE),
        subtitle = "Missing items",
        onClick = { onKpiClick?.invoke("PendingDocs") },
        modifier = Modifier.weight(1f)
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 7. Row 6: Active Cards
    MetricKpiCard(
      title = "Active Cards",
      value = stats.activeY.toString(),
      icon = Icons.Default.CreditCard,
      iconTint = Color(0xFF16A34A),
      bgTint = Color(0xFFDCFCE7),
      subtitle = "Active: ${stats.activeY} | Inactive: ${stats.activeN} | Closed: ${stats.activeC}",
      onClick = { onKpiClick?.invoke("ActiveCards") },
      modifier = Modifier.fillMaxWidth()
    )
  }
}

@Composable
fun StatusDistributionChart(
  stats: KpiStats,
  modifier: Modifier = Modifier
) {
  val total = stats.totalFiles
  if (total == 0) return

  val items = listOf(
    Pair("STC", Pair(stats.stc, Color(0xFF0F766E))),
    Pair("Submitted", Pair(stats.submitted, Color(0xFF2563EB))),
    Pair("Analyst Recv", Pair(stats.analystReceive, Color(0xFF4338CA))),
    Pair("Approved", Pair(stats.approved, Color(0xFF16A34A))),
    Pair("Collected", Pair(stats.collected, Color(0xFF0284C7))),
    Pair("Query", Pair(stats.query, Color(0xFFD97706))),
    Pair("RTS", Pair(stats.returnToSource, Color(0xFFE11D48))),
    Pair("Declined", Pair(stats.declined, Color(0xFFDC2626))),
    Pair("Condition", Pair(stats.condition, Color(0xFF9333EA)))
  ).filter { it.second.first > 0 }

  var playAnim by remember { mutableStateOf(false) }
  LaunchedEffect(total) { playAnim = true }
  val fillProgress by animateFloatAsState(
    targetValue = if (playAnim) 1f else 0.05f,
    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
    label = "chart_fill_progress"
  )

  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
  ) {
    Column(modifier = Modifier.padding(15.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "File Status Distribution",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
          Text(
            text = "Total $total Files",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
          )
        }
      }
      Spacer(modifier = Modifier.height(12.dp))

      // Animated rounded horizontal segmented bar
      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .height(16.dp)
          .clip(RoundedCornerShape(8.dp))
      ) {
        var currentX = 0f
        val canvasWidth = size.width * fillProgress
        drawRoundRect(
          color = Color.LightGray.copy(alpha = 0.2f),
          topLeft = Offset.Zero,
          size = size,
          cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        )
        for (item in items) {
          val fraction = item.second.first.toFloat() / total.toFloat()
          val barWidth = canvasWidth * fraction
          drawRect(
            color = item.second.second,
            topLeft = Offset(currentX, 0f),
            size = Size(barWidth, size.height)
          )
          currentX += barWidth
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Multi-column clean legend
      Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        items.chunked(3).forEach { rowItems ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            rowItems.forEach { item ->
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
              ) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .background(item.second.second, CircleShape)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                  text = "${item.first}: ${item.second.first}",
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
            repeat(3 - rowItems.size) {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }
    }
  }
}
