package com.example.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EblGold
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.viewmodel.KpiStats
import com.example.util.DateUtils

@Composable
fun TimeFilterBar(
  selectedFilter: DateUtils.TimeFilter,
  onFilterSelected: (DateUtils.TimeFilter) -> Unit,
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()
  Row(
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(scrollState)
      .padding(horizontal = 16.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    DateUtils.TimeFilter.values().forEach { filter ->
      val isSelected = filter == selectedFilter
      FilterChip(
        selected = isSelected,
        onClick = { onFilterSelected(filter) },
        label = {
          Text(
            text = filter.label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            softWrap = false
          )
        },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = EblNavyPrimary,
          selectedLabelColor = Color.White
        ),
        modifier = Modifier.testTag("time_filter_${filter.name}")
      )
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
  Card(
    modifier = modifier
      .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
      .testTag("kpi_card_${title.lowercase().replace(" ", "_")}"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, bgTint.copy(alpha = 0.8f))
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .background(bgTint, RoundedCornerShape(10.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
          )
        }
        Text(
          text = value,
          fontSize = 22.sp,
          fontWeight = FontWeight.ExtraBold,
          color = iconTint
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = title,
        fontSize = 12.sp,
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

@Composable
fun KpiGridSection(
  stats: KpiStats,
  onKpiClick: ((String) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
  ) {
    // 1. PREMIER HERO CARD: STC (Official Achievement / Production Done)
    // As explicitly specified: "stc sobar age hobe", "stc mane production hoya jaoa mane done hoa so stc hoyle sheta acivment hishabe count hobe"
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clickable { onKpiClick?.invoke("STC") }
        .testTag("kpi_card_stc_achievement"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0F766E)),
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(
            Brush.horizontalGradient(
              listOf(Color(0xFF0F766E), Color(0xFF115E59), Color(0xFF042F2E))
            )
          )
          .padding(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(46.dp)
                .background(Color(0xFFCCFBF1), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Stars,
                contentDescription = "STC Achievement",
                tint = Color(0xFF0F766E),
                modifier = Modifier.size(28.dp)
              )
            }
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "STC (Achievement / Done)",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = EblGold
                ) {
                  Text(
                    text = "TARGET KPI",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EblNavyDark,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Production Done • Official Goal Metric",
                fontSize = 11.sp,
                color = Color(0xFFCCFBF1)
              )
            }
          }

          Text(
            text = stats.stc.toString(),
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
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

    // 3. Row 2: Submitted & Analyst Receive ("colect3d submited er pore analyst receive nameo akta option rakho")
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

  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
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
        Text(
          text = "Total $total Files",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Spacer(modifier = Modifier.height(10.dp))

      // Custom horizontal segmented bar
      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .height(16.dp)
      ) {
        var currentX = 0f
        val canvasWidth = size.width
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

      // Multi-column clean legend (words won't wrap awkwardly!)
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(4).forEach { rowItems ->
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
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "${item.first}: ${item.second.first}",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
          }
        }
      }
    }
  }
}
