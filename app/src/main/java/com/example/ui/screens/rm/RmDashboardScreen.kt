package com.example.ui.screens.rm

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerFileEntity
import com.example.data.model.RmTargetEntity
import com.example.data.model.UserEntity
import com.example.ui.common.ActiveStatusBadge
import com.example.ui.common.ApplicationStatusBadge
import com.example.ui.common.EblPullRefreshBox
import com.example.ui.common.FloatableRmNavButton
import com.example.ui.common.KpiGridSection
import com.example.ui.common.PulsingLiveDot
import com.example.ui.common.StatusDistributionChart
import com.example.ui.common.TimeFilterBar
import com.example.ui.common.interactiveBounce
import com.example.ui.theme.EblGold
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.viewmodel.KpiStats
import com.example.util.DateUtils
import kotlinx.coroutines.launch

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun RmDashboardScreen(
  user: UserEntity,
  stats: KpiStats,
  selectedTimeFilter: DateUtils.TimeFilter,
  recentFiles: List<CustomerFileEntity>,
  onTimeFilterChange: (DateUtils.TimeFilter) -> Unit,
  onAddNewFile: () -> Unit,
  onViewAllFiles: () -> Unit,
  onViewPendingDocs: () -> Unit = {},
  onFileClick: (CustomerFileEntity) -> Unit,
  onDownloadReport: () -> Unit,
  target: RmTargetEntity? = null,
  targetCriteria: List<String> = listOf("Credit Card", "Corporate Card", "B2B"),
  assignedCriteria: List<String> = emptyList(),
  customTargets: Map<String, Int> = emptyMap(),
  allQuarterCustomTargets: Map<String, Map<String, Int>> = emptyMap(),
  unreadSmsCount: Int = 0,
  onOpenSmsInbox: () -> Unit = {},
  onOpenDbrChecklist: () -> Unit = {},
  onOpenCommunication: () -> Unit = {},
  onOpenImportantDocuments: () -> Unit = {},
  onOpenCompanyProfile: () -> Unit = {},
  onOpenRoutePlanner: () -> Unit = {},
  onOpenTargetVsAchievement: () -> Unit = {},
  bannerBackgroundUrl: String = "",
  onUploadBannerBackground: (() -> Unit)? = null,
  onUpdateLocation: (Double, Double, String) -> Unit = { _, _, _ -> },
  isBiometricEnabled: Boolean = false,
  onToggleBiometric: ((Boolean) -> Unit)? = null,
  onRefresh: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var isRefreshing by remember { mutableStateOf(false) }
  val isDark = com.example.ui.theme.LocalAppTheme.current.isDark

  // Auto-detect and sync RM live location in background
  LaunchedEffect(Unit) {
    if (com.example.util.LocationHelper.hasLocationPermission(context)) {
      try {
        val loc = com.example.util.LocationHelper.getCurrentLocation(context)
        onUpdateLocation(loc.latitude, loc.longitude, loc.address)
      } catch (_: Exception) {}
    }
  }


  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    EblPullRefreshBox(
      isRefreshing = isRefreshing,
      onRefresh = {
        isRefreshing = true
        onRefresh?.invoke()
        coroutineScope.launch {
          kotlinx.coroutines.delay(1000)
          isRefreshing = false
        }
      }
    ) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .testTag("rm_dashboard_screen")
      ) {
    // Welcome Theme Photo Banner
    item {
      com.example.ui.common.ThemeVisualBanner(
        title = "Welcome, ${user.name}",
        subtitle = "RM Code: ${user.rmCode} • ${user.officeAddress}",
        badgeText = "Relationship Manager",
        customBackgroundUrl = bannerBackgroundUrl,
        onUploadBackground = if (user.role == "MENTOR" || user.role == "ADMIN") onUploadBannerBackground else null,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
      )
    }

    // 1-Tap Quick Operations Strip for RM
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        RmQuickActionChip(
          label = "New File",
          icon = Icons.Default.Add,
          accentColor = Color(0xFF0F766E),
          isDark = isDark,
          onClick = onAddNewFile,
          testTag = "rm_quick_new_file"
        )
        RmQuickActionChip(
          label = "Quota & Target",
          icon = Icons.Default.TrendingUp,
          accentColor = Color(0xFF2563EB),
          isDark = isDark,
          onClick = onOpenTargetVsAchievement,
          testTag = "rm_quick_target"
        )
        RmQuickActionChip(
          label = "Route Plan",
          icon = Icons.Default.Navigation,
          accentColor = Color(0xFF16A34A),
          isDark = isDark,
          onClick = onOpenRoutePlanner,
          testTag = "rm_quick_route"
        )
        RmQuickActionChip(
          label = "DBR & Calc",
          icon = Icons.Default.Calculate,
          accentColor = Color(0xFFD97706),
          isDark = isDark,
          onClick = onOpenDbrChecklist,
          testTag = "rm_quick_dbr"
        )
        RmQuickActionChip(
          label = "Companies",
          icon = Icons.Default.Business,
          accentColor = Color(0xFF7C3AED),
          isDark = isDark,
          onClick = onOpenCompanyProfile,
          testTag = "rm_quick_companies"
        )
        RmQuickActionChip(
          label = "Team Hub",
          icon = Icons.Default.Forum,
          accentColor = Color(0xFFE11D48),
          isDark = isDark,
          onClick = onOpenCommunication,
          testTag = "rm_quick_team_hub"
        )
      }
    }

    // Live Quarterly Quota Snapshot Card
    item {
      val currentQ = remember { DateUtils.getCurrentQuarter() }
      val qCc = when (currentQ) {
        "Q1" -> target?.q1CreditCardTarget ?: 0
        "Q2" -> target?.q2CreditCardTarget ?: 0
        "Q3" -> target?.q3CreditCardTarget ?: 0
        "Q4" -> target?.q4CreditCardTarget ?: 0
        else -> target?.creditCardTarget ?: 0
      }
      val qCorp = when (currentQ) {
        "Q1" -> target?.q1CorporateCardTarget ?: 0
        "Q2" -> target?.q2CorporateCardTarget ?: 0
        "Q3" -> target?.q3CorporateCardTarget ?: 0
        "Q4" -> target?.q4CorporateCardTarget ?: 0
        else -> target?.corporateCardTarget ?: 0
      }
      val qB2b = when (currentQ) {
        "Q1" -> target?.q1B2bTarget ?: 0
        "Q2" -> target?.q2B2bTarget ?: 0
        "Q3" -> target?.q3B2bTarget ?: 0
        "Q4" -> target?.q4B2bTarget ?: 0
        else -> target?.b2bTarget ?: 0
      }
      val customQTotal = (allQuarterCustomTargets[currentQ] ?: customTargets).values.sum()
      val totalQuarterGoal = (qCc + qCorp + qB2b + customQTotal).coerceAtLeast(1)
      val rawProgress = (stats.stc.toFloat() / totalQuarterGoal).coerceIn(0f, 1f)
      val animatedQuotaProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "rm_quota_progress"
      )

      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFF0F766E).copy(alpha = 0.25f)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp)
          .interactiveBounce(pressedScale = 0.975f, onClick = onOpenTargetVsAchievement)
          .testTag("rm_quota_snapshot_card")
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0F766E).copy(alpha = 0.14f),
                modifier = Modifier.size(32.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Default.Stars, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(18.dp))
                }
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "$currentQ Quota Progress",
                  fontSize = 13.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isDark) Color(0xFFF1F5F9) else EblNavyDark
                )
                Text(
                  text = "CC ${stats.stcCreditCardCount}/$qCc • Corp ${stats.stcCorporateCardCount}/$qCorp • B2B ${stats.stcB2bCount}/$qB2b",
                  fontSize = 10.5.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFF0F766E).copy(alpha = 0.14f)
            ) {
              Text(
                text = "${stats.stc} / $totalQuarterGoal (${(rawProgress * 100).toInt()}%)",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color(0xFF5EEAD4) else Color(0xFF0F766E),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          LinearProgressIndicator(
            progress = { animatedQuotaProgress },
            modifier = Modifier
              .fillMaxWidth()
              .height(7.dp)
              .clip(RoundedCornerShape(4.dp)),
            color = Color(0xFF0F766E),
            trackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
          )
        }
      }
    }

    // 3D Banking Card Portfolio Deck (VISA / Mastercard / B2B & Payroll)
    item {
      com.example.ui.common.CardProductPortfolioStrip(
        files = recentFiles,
        onCardClick = { onViewAllFiles() }
      )
    }

    // Unread SMS Alert Card from Admin/Mentor
    if (unreadSmsCount > 0) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF450A0A).copy(alpha = 0.65f) else Color(0xFFFEF2F2)),
          border = BorderStroke(1.25.dp, Color(0xFFF87171)),
          elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .interactiveBounce(pressedScale = 0.97f, onClick = onOpenSmsInbox)
            .testTag("rm_unread_sms_alert_card")
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(Color(0xFFDC2626)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.Sms, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
              }
              Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  PulsingLiveDot(color = Color(0xFFEF4444), size = 6.dp)
                  Text(
                    text = "SMS Alert ($unreadSmsCount Unread)",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.5.sp,
                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                  )
                }
                Text(
                  text = "Your customer file or profile was updated/deleted. Tap to view details.",
                  fontSize = 11.sp,
                  color = if (isDark) Color(0xFFFECACA) else Color(0xFF7F1D1D)
                )
              }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFDC2626))
          }
        }
      }
    }

    // Time filter pills
    item {
      TimeFilterBar(
        selectedFilter = selectedTimeFilter,
        onFilterSelected = onTimeFilterChange
      )
    }



    // KPI Metrics Section
    item {
      KpiGridSection(
        stats = stats,
        onKpiClick = { onViewAllFiles() }
      )
    }

    // Status Distribution Chart
    item {
      StatusDistributionChart(stats = stats)
    }

    // Pending Documents Backlog Alert Card (If any files have missing docs)
    if (stats.pendingDocumentsCount > 0) {
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .interactiveBounce(pressedScale = 0.97f, onClick = onViewPendingDocs)
            .testTag("rm_pending_docs_backlog_card"),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF450A0A).copy(alpha = 0.55f) else Color(0xFFFEF2F2)),
          border = BorderStroke(1.25.dp, Color(0xFFFCA5A5)),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .background(Color(0xFFDC2626), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Warning,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Pending Documents Backlog",
                  fontSize = 13.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                )
                Text(
                  text = "${stats.pendingDocumentsCount} file(s) require missing documents",
                  fontSize = 11.sp,
                  color = if (isDark) Color(0xFFFECACA) else Color(0xFFB91C1C)
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFFDC2626)
            ) {
              Text(
                text = "View Backlog →",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }
          }
        }
      }
    }

    // Recent Files Header
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "My Recent Customer Files",
          fontSize = 15.5.sp,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.onBackground
        )
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = EblNavyPrimary.copy(alpha = if (isDark) 0.25f else 0.10f),
          modifier = Modifier.interactiveBounce(pressedScale = 0.94f, onClick = onViewAllFiles)
        ) {
          Text(
            text = "View All (${recentFiles.size}) →",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color(0xFF93C5FD) else EblNavyPrimary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
          )
        }
      }
    }

    // List of recent customer files
    if (recentFiles.isEmpty()) {
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text("No customer files found for this time period.", fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
              onClick = onAddNewFile,
              colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
              shape = RoundedCornerShape(10.dp)
            ) {
              Text("Create First Customer File")
            }
          }
        }
      }
    } else {
      items(recentFiles.take(5)) { file ->
        val statusBarColor = when (file.applicationStatus) {
          "STC" -> Color(0xFF0F766E)
          "Approved" -> Color(0xFF15803D)
          "Submitted", "Analyst Receive" -> Color(0xFF2563EB)
          "Declined" -> Color(0xFFDC2626)
          "Query", "Return to Source", "Condition" -> Color(0xFFD97706)
          else -> Color(0xFF64748B)
        }
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .interactiveBounce(pressedScale = 0.975f, onClick = { onFileClick(file) })
            .testTag("recent_file_${file.fileId}"),
          shape = RoundedCornerShape(15.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .width(4.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(statusBarColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = file.customerName,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                ApplicationStatusBadge(file.applicationStatus)
                Spacer(modifier = Modifier.width(4.dp))
                ActiveStatusBadge(file.activeStatus)
              }
              Spacer(modifier = Modifier.height(5.dp))
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                com.example.ui.common.ProductCardMiniBadge(
                  productType = file.productType,
                  ccNumber = file.ccNumber
                )
                Text(
                  text = "${file.companyName}${if (file.ccNumber.isNotBlank()) " • ${file.ccNumber}" else ""}",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1
                )
              }
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                text = "Mobile: ${file.mobile} • Updated: ${DateUtils.formatDateTime(file.updatedAt)}",
                fontSize = 10.5.sp,
                color = if (isDark) Color(0xFF93C5FD) else EblNavyPrimary,
                fontWeight = FontWeight.Medium
              )
            }
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
              modifier = Modifier.size(28.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.ChevronRight,
                  contentDescription = "View Details",
                  tint = statusBarColor,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(36.dp))
    }
  }
}

  // Floatable & Draggable Quick Nav Button for RM
  FloatableRmNavButton(
    onNavigateNewFile = onAddNewFile,
    onNavigateMyFiles = onViewAllFiles,
    onNavigatePendingDocs = onViewPendingDocs,
    onNavigateDashboard = { /* Already on dashboard */ },
    onNavigateCommunication = onOpenCommunication,
    onNavigateDbrChecklist = onOpenDbrChecklist,
    onNavigateImportantDocuments = onOpenImportantDocuments,
    onNavigateRoutePlanner = onOpenRoutePlanner
  )
}
}

@Composable
private fun RmQuickActionChip(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  accentColor: Color,
  isDark: Boolean,
  onClick: () -> Unit,
  testTag: String
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = if (isDark) accentColor.copy(alpha = 0.2f) else accentColor.copy(alpha = 0.1f),
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
    modifier = Modifier
      .interactiveBounce(pressedScale = 0.94f, onClick = onClick)
      .testTag(testTag)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = accentColor,
        modifier = Modifier.size(15.dp)
      )
      Text(
        text = label,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        color = if (isDark) Color(0xFFF1F5F9) else accentColor,
        maxLines = 1,
        softWrap = false
      )
    }
  }
}
