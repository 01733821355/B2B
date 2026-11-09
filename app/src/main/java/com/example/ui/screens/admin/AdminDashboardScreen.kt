package com.example.ui.screens.admin

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.ui.common.EblPullRefreshBox
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
  var targetSettingRow by remember { mutableStateOf<com.example.ui.viewmodel.RmPerformanceRow?>(null) }
  val isRefreshing by viewModel.isRefreshing.collectAsState()
  val isDark = com.example.ui.theme.LocalAppTheme.current.isDark

  EblPullRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = { viewModel.refreshData() },
    modifier = modifier.fillMaxSize()
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .testTag("admin_dashboard_screen")
    ) {
      // Executive Theme Photo Banner
      item {
        com.example.ui.common.ThemeVisualBanner(
          title = "Central Administration Console",
          subtitle = "Signed in: ${currentUser.name} (${currentUser.role})",
          badgeText = "System Administrator • Live Operations",
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
      }

      // Primary Admin Action: Add New Customer File
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onNavigate(Screen.CustomerForm(null)) }
            .testTag("admin_btn_add_customer"),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E3A8A) else Color(0xFFEFF6FF)),
          border = BorderStroke(1.5.dp, if (isDark) Color(0xFF3B82F6) else Color(0xFF93C5FD))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = CircleShape,
                color = EblNavyPrimary,
                modifier = Modifier.size(38.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "+ Add New Customer File",
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = if (isDark) Color.White else EblNavyDark
                )
                Text(
                  text = "Create new customer file & assign directly to RM officer",
                  fontSize = 11.sp,
                  color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                )
              }
            }
            Icon(
              Icons.Default.ArrowForwardIos,
              contentDescription = null,
              tint = if (isDark) Color(0xFF93C5FD) else EblNavyPrimary,
              modifier = Modifier.size(14.dp)
            )
          }
        }
      }

      // Admin Quick Action Shortcuts
      item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (isDark) Color(0xFF0F766E).copy(alpha = 0.25f) else Color(0xFFCCFBF1),
          border = BorderStroke(1.dp, if (isDark) Color(0xFF0D9488) else Color(0xFF99F6E4)),
          modifier = Modifier
            .weight(1f)
            .clickable { onNavigate(Screen.AppSettings) }
            .testTag("admin_quick_btn_settings")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = if (isDark) Color(0xFF2DD4BF) else Color(0xFF0F766E), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Settings", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF5EEAD4) else Color(0xFF0F766E), maxLines = 1, softWrap = false)
          }
        }

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.25f) else Color(0xFFEFF6FF),
          border = BorderStroke(1.dp, if (isDark) Color(0xFF2563EB) else Color(0xFFBFDBFE)),
          modifier = Modifier
            .weight(1f)
            .clickable { onNavigate(Screen.RmMapping) }
            .testTag("admin_quick_btn_rm_mapping")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(Icons.Default.Group, contentDescription = "RM Mapping", tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("RM Mapping", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8), maxLines = 1, softWrap = false)
          }
        }

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (isDark) Color(0xFF4C1D95).copy(alpha = 0.25f) else Color(0xFFF3E8FF),
          border = BorderStroke(1.dp, if (isDark) Color(0xFF7C3AED) else Color(0xFFDDD6FE)),
          modifier = Modifier
            .weight(1f)
            .clickable { onNavigate(Screen.Reports) }
            .testTag("admin_quick_btn_reports")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(Icons.Default.Assessment, contentDescription = "Reports", tint = if (isDark) Color(0xFFC084FC) else Color(0xFF7E22CE), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reports", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFD8B4FE) else Color(0xFF7E22CE), maxLines = 1, softWrap = false)
          }
        }
      }
    }
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
              .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE8EEF5))
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("RM Officer", modifier = Modifier.weight(1.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFF1F5F9) else EblNavyDark, maxLines = 1, softWrap = false)
            Text("STC", modifier = Modifier.weight(0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E), maxLines = 1, softWrap = false)
            Text("Subm", modifier = Modifier.weight(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFE2E8F0) else EblNavyDark, maxLines = 1, softWrap = false)
            Text("Apprv", modifier = Modifier.weight(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D), maxLines = 1, softWrap = false)
            Text("Query", modifier = Modifier.weight(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706), maxLines = 1, softWrap = false)
            Text("RTS", modifier = Modifier.weight(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE11D48), maxLines = 1, softWrap = false)
            Text("Decl", modifier = Modifier.weight(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626), maxLines = 1, softWrap = false)
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
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    viewModel.selectedRmCodeFilter.value = row.rmCode
                    onRmRowClicked(row.rmCode)
                  }
                  .padding(horizontal = 12.dp, vertical = 10.dp)
                  .testTag("rm_row_${row.rmCode}")
              ) {
                // Main Status Metrics Row
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1.7f)) {
                    Text(row.rmName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFF1F5F9) else EblNavyDark, maxLines = 1, softWrap = false)
                    Text("Code: ${row.rmCode}", fontSize = 10.sp, color = Color.Gray, maxLines = 1, softWrap = false)
                  }
                  Text("${row.stats.stc}", modifier = Modifier.weight(0.7f), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F766E))
                  Text("${row.stats.submitted}", modifier = Modifier.weight(0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                  Text("${row.stats.approved}", modifier = Modifier.weight(0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF15803D))
                  Text("${row.stats.query}", modifier = Modifier.weight(0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFFD97706))
                  Text("${row.stats.returnToSource}", modifier = Modifier.weight(0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFFE11D48))
                  Text("${row.stats.declined}", modifier = Modifier.weight(0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFFDC2626))
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Product Category Breakdown & Target vs Achievement Row
                val ccTarget = row.target?.creditCardTarget ?: 20
                val corpTarget = row.target?.corporateCardTarget ?: 10
                val b2bTarget = row.target?.b2bTarget ?: 15
                val annualCc = row.target?.annualCreditCardTarget ?: (ccTarget * 4)
                val annualCorp = row.target?.annualCorporateCardTarget ?: (corpTarget * 4)
                val annualB2b = row.target?.annualB2bTarget ?: (b2bTarget * 4)

                val curQ = DateUtils.getCurrentQuarter()
                val customQuarter = row.allQuarterCustomTargets[curQ] ?: emptyMap()
                val customAnnual = row.allQuarterCustomTargets["ALL"] ?: emptyMap()
                val standardSet = setOf("Credit Card", "Corporate Card", "B2B")
                val customCriteriaEntries = (customQuarter.keys + customAnnual.keys + row.assignedCriteria)
                  .map { com.example.data.repository.EblRepository.normalizeCriteria(it) }
                  .distinctBy { it.lowercase() }
                  .filter { k -> standardSet.none { it.equals(k, ignoreCase = true) } }
                  .mapNotNull { critName ->
                    val qVal = customQuarter.entries.firstOrNull { it.key.trim().equals(critName.trim(), ignoreCase = true) }?.value ?: (customQuarter[critName] ?: 0)
                    val sumQ = (row.allQuarterCustomTargets["Q1"]?.entries?.firstOrNull { it.key.trim().equals(critName.trim(), ignoreCase = true) }?.value ?: 0) +
                      (row.allQuarterCustomTargets["Q2"]?.entries?.firstOrNull { it.key.trim().equals(critName.trim(), ignoreCase = true) }?.value ?: 0) +
                      (row.allQuarterCustomTargets["Q3"]?.entries?.firstOrNull { it.key.trim().equals(critName.trim(), ignoreCase = true) }?.value ?: 0) +
                      (row.allQuarterCustomTargets["Q4"]?.entries?.firstOrNull { it.key.trim().equals(critName.trim(), ignoreCase = true) }?.value ?: 0)
                    val yrVal = if (sumQ > 0) sumQ else (customAnnual.entries.firstOrNull { it.key.trim().equals(critName.trim(), ignoreCase = true) }?.value ?: (customAnnual[critName] ?: 0))
                    if (qVal > 0 || yrVal > 0 || row.assignedCriteria.any { it.trim().equals(critName.trim(), ignoreCase = true) }) {
                      critName to (qVal to yrVal)
                    } else null
                  }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    modifier = Modifier.weight(1f, fill = false).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF0FDF4), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))) {
                      Text("CC: ${row.stats.stcCreditCardCount}/$ccTarget (Yr:$annualCc)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF166534), modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), maxLines = 1, softWrap = false)
                    }
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFEFF6FF), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))) {
                      Text("Corp: ${row.stats.stcCorporateCardCount}/$corpTarget (Yr:$annualCorp)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E40AF), modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), maxLines = 1, softWrap = false)
                    }
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFAF5FF), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D5FF))) {
                      Text("B2B: ${row.stats.stcB2bCount}/$b2bTarget (Yr:$annualB2b)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6B21A8), modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), maxLines = 1, softWrap = false)
                    }
                    customCriteriaEntries.forEach { (critName, pair) ->
                      Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFFFBEB), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))) {
                        Text("$critName: 0/${pair.first} (Yr:${pair.second})", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFFB45309), modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), maxLines = 1, softWrap = false)
                      }
                    }
                  }

                  Text(
                    text = "⚙ Set Target",
                    fontSize = 11.sp,
                    color = Color(0xFF2563EB),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { targetSettingRow = row }
                  )
                }
              }
              HorizontalDivider(color = Color(0xFFF1F5F9))
            }
          }
        }
      }
    }

    FloatingActionButton(
      onClick = { onNavigate(Screen.CustomerForm(null)) },
      containerColor = EblNavyPrimary,
      contentColor = Color.White,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(16.dp)
        .testTag("admin_fab_add_customer")
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add Customer")
        Spacer(modifier = Modifier.width(4.dp))
        Text("New File", fontWeight = FontWeight.Bold, fontSize = 12.sp)
      }
    }
  }

  // Set RM Targets Dialog for Admin
  if (targetSettingRow != null) {
    val row = targetSettingRow!!
    val targetCriteria by viewModel.targetCriteriaOptions.collectAsState()
    val customTargets by viewModel.getRmCustomTargetsFlow(row.rmCode).collectAsState(initial = row.allQuarterCustomTargets["ALL"] ?: emptyMap())
    val allQuarterCustomTargets by viewModel.getRmAllQuarterCustomTargetsFlow(row.rmCode).collectAsState(initial = row.allQuarterCustomTargets)
    val assignedCriteria by viewModel.getRmAssignedCriteriaFlow(row.rmCode).collectAsState(initial = row.assignedCriteria)

    com.example.ui.common.QuarterTargetSettingDialog(
      rmCode = row.rmCode,
      rmName = row.rmName,
      currentTarget = row.target,
      allCriteria = targetCriteria,
      assignedCriteria = if (assignedCriteria.isNotEmpty()) assignedCriteria else row.assignedCriteria,
      onAddCriteria = { newCrit -> viewModel.addTargetCriterion(newCrit) },
      onDeleteCriteria = { crit -> viewModel.deleteTargetCriterion(crit) },
      initialCustomTargets = if (customTargets.isNotEmpty()) customTargets else (row.allQuarterCustomTargets["ALL"] ?: emptyMap()),
      allQuarterCustomTargets = if (allQuarterCustomTargets.isNotEmpty()) allQuarterCustomTargets else row.allQuarterCustomTargets,
      onDismiss = { targetSettingRow = null },
      onSave = { quarter, year, cc, corp, b2b ->
        viewModel.setRmQuarterTargets(row.rmCode, quarter, year, cc, corp, b2b) { success, _ ->
          if (success) targetSettingRow = null
        }
      },
      onSaveAllQuarters = { year, q1Cc, q1Corp, q1B2b, q2Cc, q2Corp, q2B2b, q3Cc, q3Corp, q3B2b, q4Cc, q4Corp, q4B2b ->
        viewModel.setRmAllQuarterTargets(
          rmCode = row.rmCode,
          year = year,
          q1Cc = q1Cc, q1Corp = q1Corp, q1B2b = q1B2b,
          q2Cc = q2Cc, q2Corp = q2Corp, q2B2b = q2B2b,
          q3Cc = q3Cc, q3Corp = q3Corp, q3B2b = q3B2b,
          q4Cc = q4Cc, q4Corp = q4Corp, q4B2b = q4B2b
        ) { success, _ ->
          if (success) targetSettingRow = null
        }
      },
      onSaveCustomTargets = { quarter, customMap ->
        viewModel.saveRmCustomTargets(row.rmCode, customMap, quarter)
      },
      onSaveAllQuarterCustomTargets = { quarterlyMap ->
        viewModel.saveRmCustomTargetsAllQuarters(row.rmCode, quarterlyMap)
      },
      onSaveAssignedCriteria = { assignedList ->
        viewModel.saveRmAssignedCriteria(row.rmCode, assignedList)
      },
      onSaveAllTargetsUnified = { year, quarterlyMap, assignedList ->
        viewModel.saveAllQuarterTargetsUnified(row.rmCode, year, quarterlyMap, assignedList) { success ->
          if (success) targetSettingRow = null
        }
      }
    )
  }
}
