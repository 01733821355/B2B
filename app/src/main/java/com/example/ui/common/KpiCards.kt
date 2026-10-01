package com.example.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
      .padding(horizontal = 16.dp, vertical = 8.dp),
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
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
    shape = RoundedCornerShape(12.dp),
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
          text = title,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.Medium
        )
        Box(
          modifier = Modifier
            .size(32.dp)
            .background(bgTint, RoundedCornerShape(8.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
          )
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = value,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      if (subtitle != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = subtitle,
          fontSize = 10.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KpiGridSection(
  stats: KpiStats,
  onKpiClick: ((String) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
    // Primary summary row
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
        title = "Approved",
        value = stats.approved.toString(),
        icon = Icons.Default.CheckCircle,
        iconTint = Color(0xFF15803D),
        bgTint = Color(0xFFDCFCE7),
        subtitle = "Success rate",
        onClick = { onKpiClick?.invoke("Approved") },
        modifier = Modifier.weight(1f)
      )
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
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Secondary row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      MetricKpiCard(
        title = "Collected",
        value = stats.collected.toString(),
        icon = Icons.Default.Assignment,
        iconTint = Color(0xFF0D9488),
        bgTint = Color(0xFFCCFBF1),
        onClick = { onKpiClick?.invoke("Collected") },
        modifier = Modifier.weight(1f)
      )
      MetricKpiCard(
        title = "Query / STC",
        value = (stats.query + stats.stc + stats.condition).toString(),
        icon = Icons.Default.Error,
        iconTint = Color(0xFFD97706),
        bgTint = Color(0xFFFEF3C7),
        subtitle = "Q:${stats.query} | STC:${stats.stc} | Cond:${stats.condition}",
        onClick = { onKpiClick?.invoke("Query") },
        modifier = Modifier.weight(1f)
      )
      MetricKpiCard(
        title = "Declined / RTS",
        value = (stats.declined + stats.returnToSource).toString(),
        icon = Icons.Default.Error,
        iconTint = Color(0xFFDC2626),
        bgTint = Color(0xFFFEE2E2),
        subtitle = "Dec:${stats.declined} | RTS:${stats.returnToSource}",
        onClick = { onKpiClick?.invoke("Declined") },
        modifier = Modifier.weight(1f)
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Third row: Pending Docs & Card Status (Y / N / C)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      MetricKpiCard(
        title = "Pending Docs",
        value = stats.pendingDocumentsCount.toString(),
        icon = Icons.Default.PendingActions,
        iconTint = Color(0xFF9333EA),
        bgTint = Color(0xFFF3E8FF),
        subtitle = "Missing checklists",
        modifier = Modifier.weight(1f)
      )
      MetricKpiCard(
        title = "Active Cards",
        value = stats.activeY.toString(),
        icon = Icons.Default.CreditCard,
        iconTint = Color(0xFF16A34A),
        bgTint = Color(0xFFDCFCE7),
        subtitle = "Inactive: ${stats.activeN} | Closed: ${stats.activeC}",
        modifier = Modifier.weight(1f)
      )
    }
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
    Pair("Approved", Pair(stats.approved, Color(0xFF16A34A))),
    Pair("Submitted", Pair(stats.submitted, Color(0xFF2563EB))),
    Pair("Collected", Pair(stats.collected, Color(0xFF0D9488))),
    Pair("Query", Pair(stats.query, Color(0xFFEA580C))),
    Pair("STC / Cond", Pair(stats.stc + stats.condition, Color(0xFF7C3AED))),
    Pair("Declined", Pair(stats.declined + stats.returnToSource, Color(0xFFDC2626)))
  ).filter { it.second.first > 0 }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = "File Status Distribution",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(12.dp))

      // Custom horizontal segmented bar
      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .height(18.dp)
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

      Spacer(modifier = Modifier.height(14.dp))

      // Legend
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        items.take(3).forEach { item ->
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .background(item.second.second, CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${item.first}: ${item.second.first}",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
      if (items.size > 3) {
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          items.drop(3).forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .background(item.second.second, CircleShape)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${item.first}: ${item.second.first}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}
