package com.example.ui.screens.rm

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.ui.common.FloatableRmNavButton
import com.example.ui.common.KpiGridSection
import com.example.ui.common.StatusDistributionChart
import com.example.ui.common.TimeFilterBar
import com.example.ui.theme.EblGold
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.viewmodel.KpiStats
import com.example.util.DateUtils

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
  onUpdateLocation: (Double, Double, String) -> Unit = { _, _, _ -> },
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  // Auto-detect and sync RM live location in background
  LaunchedEffect(Unit) {
    if (com.example.util.LocationHelper.hasLocationPermission(context)) {
      try {
        val loc = com.example.util.LocationHelper.getCurrentLocation(context)
        onUpdateLocation(loc.latitude, loc.longitude, loc.address)
      } catch (_: Exception) {}
    }
  }

  val ccTarget = target?.creditCardTarget ?: 20
  val corpTarget = target?.corporateCardTarget ?: 10
  val b2bTarget = target?.b2bTarget ?: 15
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .testTag("rm_dashboard_screen")
    ) {
    // Welcome Banner Card
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EblNavyDark)
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Welcome, ${user.name}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Text(
                text = "Assigned RM Code: ${user.rmCode} | ${user.officeAddress}",
                fontSize = 12.sp,
                color = EblGold
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Quick action buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = onAddNewFile,
              colors = ButtonDefaults.buttonColors(containerColor = EblGold),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).testTag("rm_dashboard_add_file_btn")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, tint = EblNavyDark, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("New File", color = EblNavyDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            OutlinedButton(
              onClick = onViewAllFiles,
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).testTag("rm_dashboard_view_all_btn")
            ) {
              Icon(Icons.Default.Folder, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("My Files", fontSize = 12.sp)
            }

            OutlinedButton(
              onClick = onDownloadReport,
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).testTag("rm_dashboard_reports_btn")
            ) {
              Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Report", fontSize = 12.sp)
            }
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

    // TARGET VS ACHIEVEMENT SECTION (Admin-Assigned Monthly Quotas)
    item {
      Card(
        modifier = Modifier
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
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EblNavyPrimary, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Target vs Achievement",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = EblNavyDark
              )
            }
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color(0xFFEFF6FF)
            ) {
              Text(
                text = "Admin Assigned Goals",
                fontSize = 10.sp,
                color = Color(0xFF1D4ED8),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // 3 Columns: Credit Card, Corporate Card, B2B
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Credit Card Goal
            val ccAchieved = stats.creditCardCount
            val ccProgress = (ccAchieved.toFloat() / ccTarget.coerceAtLeast(1)).coerceIn(0f, 1f)
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFFF0FDF4),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Credit Card", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                  Text("$ccAchieved", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF15803D))
                  Text(" / $ccTarget", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 2.dp))
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                  progress = { ccProgress },
                  modifier = Modifier.fillMaxWidth().height(6.dp),
                  color = Color(0xFF16A34A),
                  trackColor = Color(0xFFDCFCE7)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "${(ccProgress * 100).toInt()}% Done",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = Color(0xFF15803D)
                )
              }
            }

            // Corporate Card Goal
            val corpAchieved = stats.corporateCardCount
            val corpProgress = (corpAchieved.toFloat() / corpTarget.coerceAtLeast(1)).coerceIn(0f, 1f)
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFFEFF6FF),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Business, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Corp Card", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                  Text("$corpAchieved", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1D4ED8))
                  Text(" / $corpTarget", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 2.dp))
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                  progress = { corpProgress },
                  modifier = Modifier.fillMaxWidth().height(6.dp),
                  color = Color(0xFF2563EB),
                  trackColor = Color(0xFFDBEAFE)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "${(corpProgress * 100).toInt()}% Done",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = Color(0xFF1D4ED8)
                )
              }
            }

            // B2B Goal
            val b2bAchieved = stats.b2bCount
            val b2bProgress = (b2bAchieved.toFloat() / b2bTarget.coerceAtLeast(1)).coerceIn(0f, 1f)
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFFFAF5FF),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D5FF)),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Handshake, contentDescription = null, tint = Color(0xFF7E22CE), modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("B2B", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B21A8))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                  Text("$b2bAchieved", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF7E22CE))
                  Text(" / $b2bTarget", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 2.dp))
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                  progress = { b2bProgress },
                  modifier = Modifier.fillMaxWidth().height(6.dp),
                  color = Color(0xFF9333EA),
                  trackColor = Color(0xFFF3E8FF)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "${(b2bProgress * 100).toInt()}% Done",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = Color(0xFF7E22CE)
                )
              }
            }
          }
        }
      }
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
                text = "Mobile: ${file.mobile} • Updated: ${DateUtils.formatDateOnly(file.updatedAt)}",
                fontSize = 11.sp,
                color = Color.Gray
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

  // Floatable & Draggable Quick Nav Button for RM
  FloatableRmNavButton(
    onNavigateNewFile = onAddNewFile,
    onNavigateMyFiles = onViewAllFiles,
    onNavigatePendingDocs = onViewPendingDocs,
    onNavigateDashboard = { /* Already on dashboard */ }
  )
}
}
