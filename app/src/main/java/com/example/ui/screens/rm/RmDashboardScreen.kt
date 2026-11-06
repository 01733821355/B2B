package com.example.ui.screens.rm

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.ui.common.StatusDistributionChart
import com.example.ui.common.TimeFilterBar
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



    // Unread SMS Alert Card from Admin/Mentor
    if (unreadSmsCount > 0) {
      item {
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF87171)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onOpenSmsInbox() }
            .testTag("rm_unread_sms_alert_card")
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
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
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(Color(0xFFDC2626)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.Sms, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
              }
              Column {
                Text(
                  text = "SMS Alert from Admin/Mentor ($unreadSmsCount Unread)",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = Color(0xFF991B1B)
                )
                Text(
                  text = "Your customer file or profile was updated/deleted. Tap to view details.",
                  fontSize = 11.sp,
                  color = Color(0xFF7F1D1D)
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
            .clickable { onViewPendingDocs() }
            .testTag("rm_pending_docs_backlog_card"),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
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
                  .size(38.dp)
                  .background(Color(0xFFDC2626), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Warning,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Pending Documents Backlog",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF991B1B)
                )
                Text(
                  text = "${stats.pendingDocumentsCount} file(s) require missing documents",
                  fontSize = 11.sp,
                  color = Color(0xFFB91C1C)
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFFDC2626)
            ) {
              Text(
                text = "View Backlog →",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
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
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
        Text(
          text = "View All (${recentFiles.size})",
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = EblNavyPrimary,
          modifier = Modifier.clickable { onViewAllFiles() }
        )
      }
    }

    // List of recent customer files
    if (recentFiles.isEmpty()) {
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
              colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary)
            ) {
              Text("Create First Customer File")
            }
          }
        }
      }
    } else {
      items(recentFiles.take(5)) { file ->
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onFileClick(file) }
            .testTag("recent_file_${file.fileId}"),
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = file.customerName,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                ApplicationStatusBadge(file.applicationStatus)
                Spacer(modifier = Modifier.width(4.dp))
                ActiveStatusBadge(file.activeStatus)
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "${file.fileId} • ${file.companyName} • ${file.productType}${if (file.ccNumber.isNotBlank()) " • CC: ${file.ccNumber}" else ""}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "Created: ${DateUtils.formatDateTime(file.createdAt)}",
                fontSize = 10.sp,
                color = Color.Gray
              )
              Text(
                text = "Mobile: ${file.mobile} • Updated: ${DateUtils.formatDateTime(file.updatedAt)}",
                fontSize = 11.sp,
                color = if (isDark) Color(0xFF93C5FD) else EblNavyPrimary,
                fontWeight = FontWeight.Medium
              )
            }
            Icon(
              imageVector = Icons.Default.ChevronRight,
              contentDescription = "View Details",
              tint = Color.Gray
            )
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
