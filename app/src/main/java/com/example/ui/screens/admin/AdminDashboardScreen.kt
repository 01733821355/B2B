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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
  var targetSettingRow by remember { mutableStateOf<com.example.ui.viewmodel.RmPerformanceRow?>(null) }

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
              contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp),
              modifier = Modifier.weight(1f).testTag("admin_nav_rm_mapping")
            ) {
              Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("RM Mapping", fontSize = 11.sp, maxLines = 1, softWrap = false)
            }

            OutlinedButton(
              onClick = { onNavigate(Screen.GlobalDatabase) },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
              shape = RoundedCornerShape(8.dp),
              contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp),
              modifier = Modifier.weight(1f).testTag("admin_nav_database")
            ) {
              Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Database", fontSize = 11.sp, maxLines = 1, softWrap = false)
            }

            OutlinedButton(
              onClick = { onNavigate(Screen.RmMapping) },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFDE047)),
              shape = RoundedCornerShape(8.dp),
              contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp),
              modifier = Modifier.weight(1f).testTag("admin_nav_add_rm")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("+ Add RM", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = { onNavigate(Screen.DbrChecklist) },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = EblGold),
              shape = RoundedCornerShape(8.dp),
              contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp),
              modifier = Modifier.weight(1f).testTag("admin_nav_dbr_tool")
            ) {
              Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(14.dp), tint = EblGold)
              Spacer(modifier = Modifier.width(4.dp))
              Text("DBR & Checklist Tool", color = EblGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
            OutlinedButton(
              onClick = { onNavigate(Screen.Reports) },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
              shape = RoundedCornerShape(8.dp),
              contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp),
              modifier = Modifier.weight(0.7f).testTag("admin_nav_reports")
            ) {
              Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Reports", fontSize = 11.sp, maxLines = 1, softWrap = false)
            }

            OutlinedButton(
              onClick = { onNavigate(Screen.ImportantDocuments) },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF67E8F9)),
              shape = RoundedCornerShape(8.dp),
              contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp),
              modifier = Modifier.weight(1f).testTag("admin_nav_important_docs")
            ) {
              Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF67E8F9))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Important Docs", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
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
            Text("RM Officer", modifier = Modifier.weight(1.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EblNavyDark, maxLines = 1, softWrap = false)
            Text("STC", modifier = Modifier.weight(0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E), maxLines = 1, softWrap = false)
            Text("Subm", modifier = Modifier.weight(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EblNavyDark, maxLines = 1, softWrap = false)
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
                    Text(row.rmName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EblNavyDark, maxLines = 1, softWrap = false)
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

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF0FDF4), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))) {
                      Text("CC: ${row.stats.stcCreditCardCount}/$ccTarget", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF166534), modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), maxLines = 1, softWrap = false)
                    }
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFEFF6FF), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))) {
                      Text("Corp: ${row.stats.stcCorporateCardCount}/$corpTarget", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E40AF), modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), maxLines = 1, softWrap = false)
                    }
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFAF5FF), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D5FF))) {
                      Text("B2B: ${row.stats.stcB2bCount}/$b2bTarget", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6B21A8), modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), maxLines = 1, softWrap = false)
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

    item {
      Spacer(modifier = Modifier.height(28.dp))
    }
  }

  // Set RM Targets Dialog for Admin
  if (targetSettingRow != null) {
    val row = targetSettingRow!!
    var ccInput by remember { mutableStateOf((row.target?.creditCardTarget ?: 20).toString()) }
    var corpInput by remember { mutableStateOf((row.target?.corporateCardTarget ?: 10).toString()) }
    var b2bInput by remember { mutableStateOf((row.target?.b2bTarget ?: 15).toString()) }

    AlertDialog(
      onDismissRequest = { targetSettingRow = null },
      title = { Text("Set Targets: ${row.rmName}", fontWeight = FontWeight.Bold) },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text("Assign monthly targets for RM Code: ${row.rmCode}", fontSize = 12.sp, color = Color.Gray)
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = ccInput,
            onValueChange = { ccInput = it.filter { ch -> ch.isDigit() } },
            label = { Text("Credit Card Monthly Target") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = corpInput,
            onValueChange = { corpInput = it.filter { ch -> ch.isDigit() } },
            label = { Text("Corporate Card Monthly Target") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = b2bInput,
            onValueChange = { b2bInput = it.filter { ch -> ch.isDigit() } },
            label = { Text("B2B Monthly Target") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val cc = ccInput.toIntOrNull() ?: 20
            val corp = corpInput.toIntOrNull() ?: 10
            val b2b = b2bInput.toIntOrNull() ?: 15
            viewModel.setRmTargets(row.rmCode, cc, corp, b2b) { success, _ ->
              if (success) targetSettingRow = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary)
        ) {
          Text("Save Targets")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { targetSettingRow = null }) {
          Text("Cancel")
        }
      }
    )
  }
}
