package com.example.ui.screens.admin

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.ui.common.KpiGridSection
import com.example.ui.common.StatusDistributionChart
import com.example.ui.common.TimeFilterBar
import com.example.ui.theme.EblGold
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.Screen
import com.example.util.DateUtils

@Composable
fun AdminDashboardScreen(
  viewModel: AppViewModel,
  currentUser: UserEntity,
  onNavigate: (Screen) -> Unit,
  onRmRowClicked: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val stats by viewModel.kpiStats.collectAsState()
  val selectedTimeFilter by viewModel.selectedTimeFilter.collectAsState()
  val rmPerformanceList by viewModel.rmPerformanceList.collectAsState()
  val syncStatus by viewModel.syncStatus.collectAsState()

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("admin_dashboard_screen")
  ) {
    // Executive Banner
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
                text = "Central Administration Console",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Text(
                text = "Signed in: ${currentUser.name} (${currentUser.role})",
                fontSize = 12.sp,
                color = EblGold
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Operational Quick Links
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = { onNavigate(Screen.RmMapping) },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).testTag("admin_nav_rm_mapping")
            ) {
              Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("RM Mapping", fontSize = 11.sp)
            }

            OutlinedButton(
              onClick = { onNavigate(Screen.GlobalDatabase) },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).testTag("admin_nav_database")
            ) {
              Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Database", fontSize = 11.sp)
            }

            OutlinedButton(
              onClick = { onNavigate(Screen.GoogleSheetsSync) },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).testTag("admin_nav_sync")
            ) {
              Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Sheets Sync", fontSize = 11.sp)
            }
          }
        }
      }
    }

    // Time filter pills
    item {
      TimeFilterBar(
        selectedFilter = selectedTimeFilter,
        onFilterSelected = { viewModel.selectedTimeFilter.value = it }
      )
    }

    // Global KPI Metrics Section
    item {
      KpiGridSection(
        stats = stats,
        onKpiClick = { onNavigate(Screen.GlobalDatabase) }
      )
    }

    // Status Distribution Chart
    item {
      StatusDistributionChart(stats = stats)
    }

    // RM-Wise Performance Table Header
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "RM-Wise Performance Breakdown",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(
            text = "Tap on an RM row to inspect their files",
            fontSize = 11.sp,
            color = Color.Gray
          )
        }
        OutlinedButton(
          onClick = { onNavigate(Screen.Reports) }
        ) {
          Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Reports", fontSize = 11.sp)
        }
      }
    }

    // RM-Wise Performance Table Card
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          // Table Header
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFFE8EEF5))
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("RM Officer", modifier = Modifier.weight(1.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EblNavyDark)
            Text("Files", modifier = Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EblNavyDark)
            Text("Apprv", modifier = Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
            Text("Subm", modifier = Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
            Text("Decl", modifier = Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C))
            Text("Q/STC", modifier = Modifier.weight(0.9f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC2410C))
          }

          if (rmPerformanceList.isEmpty()) {
            Box(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              contentAlignment = Alignment.Center
            ) {
              Text("No RM officers configured.", color = Color.Gray, fontSize = 12.sp)
            }
          } else {
            rmPerformanceList.forEach { row ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    viewModel.selectedRmCodeFilter.value = row.rmCode
                    onRmRowClicked(row.rmCode)
                  }
                  .padding(horizontal = 12.dp, vertical = 10.dp)
                  .testTag("rm_row_${row.rmCode}"),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1.8f)) {
                  Text(row.rmName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EblNavyDark)
                  Text("Code: ${row.rmCode}", fontSize = 10.sp, color = Color.Gray)
                }
                Text("${row.stats.totalFiles}", modifier = Modifier.weight(0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("${row.stats.approved}", modifier = Modifier.weight(0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                Text("${row.stats.submitted}", modifier = Modifier.weight(0.8f), fontSize = 12.sp, color = Color(0xFF1D4ED8))
                Text("${row.stats.declined + row.stats.returnToSource}", modifier = Modifier.weight(0.8f), fontSize = 12.sp, color = Color(0xFFB91C1C))
                Text("${row.stats.query + row.stats.stc}", modifier = Modifier.weight(0.9f), fontSize = 12.sp, color = Color(0xFFC2410C))
              }
              HorizontalDivider(color = Color(0xFFF1F5F9))
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(28.dp))
    }
  }
}
