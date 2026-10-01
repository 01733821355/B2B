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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerFileEntity
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
  modifier: Modifier = Modifier
) {
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
