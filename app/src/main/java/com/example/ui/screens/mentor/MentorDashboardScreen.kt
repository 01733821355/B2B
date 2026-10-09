package com.example.ui.screens.mentor

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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerFileEntity
import com.example.data.model.UserEntity
import com.example.ui.common.KpiGridSection
import com.example.ui.common.TimeFilterBar
import com.example.ui.common.VoiceInputField
import com.example.ui.theme.EblGold
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.Screen
import com.example.util.DateUtils

@Composable
fun MentorDashboardScreen(
  viewModel: AppViewModel,
  currentUser: UserEntity,
  onNavigate: (Screen) -> Unit,
  modifier: Modifier = Modifier
) {
  val stats by viewModel.kpiStats.collectAsState()
  val selectedTimeFilter by viewModel.selectedTimeFilter.collectAsState()
  val auditLogs by viewModel.auditLogs.collectAsState()
  val allRms by viewModel.allRms.collectAsState()
  val appCustomName by viewModel.appCustomName.collectAsState()
  val rmPerformanceList by viewModel.rmPerformanceList.collectAsState()

  var showDeletedRecordsDialog by remember { mutableStateOf(false) }
  var showEditAppNameDialog by remember { mutableStateOf(false) }
  var newAppNameInput by remember { mutableStateOf("") }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("mentor_dashboard_screen")
  ) {
    // Mentor Command Banner
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B))
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Operations Mentor Console",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Text(
                text = "Executive Oversight • Highest Authority Level (${currentUser.rmCode})",
                fontSize = 12.sp,
                color = EblGold
              )
            }
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = null,
              tint = EblGold,
              modifier = Modifier.size(28.dp)
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Operational Quick Action Buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            OutlinedButton(
              onClick = { onNavigate(Screen.GlobalDatabase) },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
              shape = RoundedCornerShape(8.dp),
              contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 4.dp),
              modifier = Modifier.weight(1f).testTag("mentor_btn_database")
            ) {
              Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(12.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text("Database", fontSize = 10.sp, maxLines = 1, softWrap = false)
            }

            OutlinedButton(
              onClick = { onNavigate(Screen.RmMapping) },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
              shape = RoundedCornerShape(8.dp),
              contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 4.dp),
              modifier = Modifier.weight(1f).testTag("mentor_btn_rm_mapping")
            ) {
              Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(12.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text("RMs", fontSize = 10.sp, maxLines = 1, softWrap = false)
            }

            OutlinedButton(
              onClick = { onNavigate(Screen.MentorUserLocationTracking) },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
              shape = RoundedCornerShape(8.dp),
              contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 4.dp),
              modifier = Modifier.weight(1f).testTag("mentor_btn_user_locations")
            ) {
              Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text("Radar", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }

            OutlinedButton(
              onClick = { onNavigate(Screen.GoogleSheetsSync) },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF34D399)),
              shape = RoundedCornerShape(8.dp),
              contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 4.dp),
              modifier = Modifier.weight(1f).testTag("mentor_btn_sheets_sync")
            ) {
              Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(12.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text("Sheets", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
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
              contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 5.dp),
              modifier = Modifier.weight(1.2f).testTag("mentor_btn_dbr_checklist")
            ) {
              Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(13.dp), tint = EblGold)
              Spacer(modifier = Modifier.width(4.dp))
              Text("DBR & Checklist", color = EblGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }

            OutlinedButton(
              onClick = { onNavigate(Screen.ImportantDocuments) },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF67E8F9)),
              shape = RoundedCornerShape(8.dp),
              contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 5.dp),
              modifier = Modifier.weight(1.1f).testTag("mentor_btn_important_docs")
            ) {
              Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFF67E8F9))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Important Docs", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          val isBioMentor = viewModel.isBiometricEnabled(currentUser.rmCode)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF0F2642))
              .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = "Fingerprint Login",
                tint = if (isBioMentor) Color(0xFF34D399) else Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "Fingerprint Login (ফিঙ্গারপ্রিন্ট লগইন)",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
                Text(
                  text = if (isBioMentor) "Enabled (লগইনে ফিঙ্গারপ্রিন্ট সক্রিয়)" else "Disabled (পাসওয়ার্ড লগইন প্রয়োজন)",
                  fontSize = 9.sp,
                  color = if (isBioMentor) Color(0xFF34D399) else Color.LightGray
                )
              }
            }
            Switch(
              checked = isBioMentor,
              onCheckedChange = { viewModel.setBiometricEnabled(currentUser.rmCode, it) },
              modifier = Modifier.testTag("switch_mentor_biometric")
            )
          }
        }
      }
    }

    // Pending RM Approvals Alert Banner (Only for Mentor)
    val pendingRms = allRms.filter { it.accountStatus == "PENDING_APPROVAL" }
    if (pendingRms.isNotEmpty()) {
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFFB45309))
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "${pendingRms.size} RM Assigned - Pending Approval",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF92400E)
                )
                Text(
                  text = "Assigned by Admin • Requires Mentor authorization",
                  fontSize = 10.sp,
                  color = Color(0xFFB45309)
                )
              }
            }
            Button(
              onClick = { onNavigate(Screen.RmMapping) },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309)),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text("Approve", fontSize = 11.sp)
            }
          }
        }
      }
    }

    // Google Sheets Auto-Sync Card (EXCLUSIVE TO MENTOR)
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp)
          .clickable { onNavigate(Screen.GoogleSheetsSync) }
          .testTag("mentor_card_sheets_sync"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                .size(42.dp)
                .background(Color(0xFF064E3B), RoundedCornerShape(10.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = null,
                tint = Color(0xFF34D399),
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "Google Sheets Auto-Sync Console",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = EblNavyDark
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  color = Color(0xFF10B981).copy(alpha = 0.15f),
                  shape = RoundedCornerShape(4.dp)
                ) {
                  Text(
                    text = "MENTOR EXCLUSIVE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF059669),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Auto-create tabs, formatted headers, and real-time data sync.",
                fontSize = 11.sp,
                color = Color.Gray
              )
            }
          }
        }
      }
    }

    // Mentor Live Location Tracking Feature Card (EXCLUSIVE TO MENTOR)
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp)
          .clickable { onNavigate(Screen.MentorUserLocationTracking) }
          .testTag("mentor_card_user_location_radar"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                .size(42.dp)
                .background(Color(0xFF0F325E), RoundedCornerShape(10.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = EblGold,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "User & RM Live Location Monitor",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = EblNavyDark
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  color = Color(0xFF10B981).copy(alpha = 0.15f),
                  shape = RoundedCornerShape(4.dp)
                ) {
                  Text(
                    text = "MENTOR ONLY",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF059669),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Real-time GPS tracking & field monitoring of RM locations in Dhaka.",
                fontSize = 11.sp,
                color = Color.Gray
              )
            }
          }
        }
      }
    }

    // Universal App Name Configuration (EXCLUSIVE TO MENTOR)
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                .size(42.dp)
                .background(Color(0xFF312E81), RoundedCornerShape(10.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = EblGold,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Universal App Name",
                fontSize = 11.sp,
                color = Color.Gray
              )
              Text(
                text = appCustomName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = EblNavyDark
              )
              Text(
                text = "Universal across whole app • Configurable by Mentor",
                fontSize = 11.sp,
                color = Color(0xFF059669)
              )
            }
          }

          Button(
            onClick = {
              newAppNameInput = appCustomName
              showEditAppNameDialog = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("btn_mentor_edit_app_name")
          ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Change", fontSize = 12.sp)
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

    // Executive Metrics
    item {
      KpiGridSection(
        stats = stats,
        onKpiClick = { onNavigate(Screen.GlobalDatabase) }
      )
    }

    // RM-Wise Performance Breakdown for Mentor
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "RM-Wise Performance & Target Achievement",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = EblNavyDark
              )
              Text(
                text = "STC, Approvals, Declines, Query/RTS and category quotas",
                fontSize = 10.sp,
                color = Color.Gray
              )
            }
          }

          // Table Header
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFFE8EEF5))
              .padding(horizontal = 12.dp, vertical = 8.dp),
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
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
              Text("No RM officers found.", color = Color.Gray, fontSize = 12.sp)
            }
          } else {
            rmPerformanceList.forEach { row ->
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    viewModel.selectedRmCodeFilter.value = row.rmCode
                    onNavigate(Screen.GlobalDatabase)
                  }
                  .padding(horizontal = 12.dp, vertical = 8.dp)
              ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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

                Spacer(modifier = Modifier.height(4.dp))

                val ccTarget = row.target?.creditCardTarget ?: 20
                val corpTarget = row.target?.corporateCardTarget ?: 10
                val b2bTarget = row.target?.b2bTarget ?: 15
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF0FDF4), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))) {
                    Text("CC: ${row.stats.stcCreditCardCount}/$ccTarget", fontSize = 10.sp, color = Color(0xFF166534), modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), maxLines = 1, softWrap = false)
                  }
                  Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFEFF6FF), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))) {
                    Text("Corp: ${row.stats.stcCorporateCardCount}/$corpTarget", fontSize = 10.sp, color = Color(0xFF1E40AF), modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), maxLines = 1, softWrap = false)
                  }
                  Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFAF5FF), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D5FF))) {
                    Text("B2B: ${row.stats.stcB2bCount}/$b2bTarget", fontSize = 10.sp, color = Color(0xFF6B21A8), modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), maxLines = 1, softWrap = false)
                  }
                }
              }
              HorizontalDivider(color = Color(0xFFF1F5F9))
            }
          }
        }
      }
    }

    // Mentor Governance Panel (Soft Delete Recovery / Permanent Purge)
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Mentor Data Integrity & Recovery Controls",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = EblNavyDark
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Inspect soft-deleted records across all RMs, restore accidentally deleted customer files, or permanently expunge audit-cleared files.",
            fontSize = 11.sp,
            color = Color.Gray
          )

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = {
                viewModel.showDeletedFilesOnly.value = true
                onNavigate(Screen.GlobalDatabase)
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).testTag("mentor_btn_trash_bin")
            ) {
              Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Recover Deleted Files", fontSize = 11.sp)
            }

            Button(
              onClick = { onNavigate(Screen.GoogleSheetsSync) },
              colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).testTag("mentor_btn_sync_now")
            ) {
              Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Sync Sheets", fontSize = 11.sp)
            }
          }
        }
      }
    }

    // Recent Audit Logs Live Stream
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Real-Time Security Audit Stream",
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
        Text(
          text = "View All",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = EblNavyPrimary,
          modifier = Modifier.clickable { onNavigate(Screen.AuditLogs) }
        )
      }
    }

    items(auditLogs.take(6)) { log ->
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = log.action,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = EblNavyDark
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("• ${log.userId} (${log.role})", fontSize = 11.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(log.details, fontSize = 11.sp, color = Color.DarkGray)
            Text(
              text = DateUtils.formatDateTime(log.timestamp),
              fontSize = 10.sp,
              color = Color.Gray
            )
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(28.dp))
    }
  }

  // Universal App Name Edit Dialog
  if (showEditAppNameDialog) {
    AlertDialog(
      onDismissRequest = { showEditAppNameDialog = false },
      title = { Text("Universal App Name", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text(
            "Change the name of the app manually. This name will immediately reflect across all screens, headers, and portals universally for all users.",
            fontSize = 12.sp,
            color = Color.Gray
          )
          Spacer(modifier = Modifier.height(12.dp))
          VoiceInputField(
            value = newAppNameInput,
            onValueChange = { newAppNameInput = it },
            label = "Universal App Name *",
            placeholder = "Enter custom app name...",
            testTag = "input_custom_app_name"
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newAppNameInput.isNotBlank()) {
              viewModel.setAppCustomName(newAppNameInput.trim()) { _, _ -> }
              showEditAppNameDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary)
        ) {
          Text("Save Universal Name")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showEditAppNameDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}
