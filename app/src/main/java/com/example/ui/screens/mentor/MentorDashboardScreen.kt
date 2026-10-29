package com.example.ui.screens.mentor

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Navigation
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.ui.common.EblPullRefreshBox
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
  val allFiles by viewModel.allFiles.collectAsState()
  val context = LocalContext.current

  var showDeletedRecordsDialog by remember { mutableStateOf(false) }
  var showEditAppNameDialog by remember { mutableStateOf(false) }
  var newAppNameInput by remember { mutableStateOf("") }
  var mentorTargetSettingRow by remember { mutableStateOf<com.example.ui.viewmodel.RmPerformanceRow?>(null) }

  val isRefreshing by viewModel.isRefreshing.collectAsState()

  LaunchedEffect(Unit) {
    while (true) {
      kotlinx.coroutines.delay(20_000)
      viewModel.refreshData()
    }
  }

  EblPullRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = { viewModel.refreshData() },
    modifier = modifier.fillMaxSize()
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .testTag("mentor_dashboard_screen")
    ) {
    // Mentor Command Theme Photo Banner
    item {
      com.example.ui.common.ThemeVisualBanner(
        title = "Operations Mentor Console",
        subtitle = "Executive Oversight • Authority Level (${currentUser.rmCode})",
        badgeText = "Mentor Governance • Operational Command",
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
      )
    }

    item {
      Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        val isDark = com.example.ui.theme.LocalAppTheme.current.isDark


        // Operational Quick Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          OutlinedButton(
            onClick = { onNavigate(Screen.GlobalDatabase) },
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = if (isDark) Color(0xFF581C87).copy(alpha = 0.35f) else Color(0xFFF5F3FF),
              contentColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFF6D28D9)
            ),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF8B5CF6) else Color(0xFFDDD6FE)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 4.dp),
            modifier = Modifier.weight(1f).testTag("mentor_btn_database")
          ) {
            Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Database", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
          }

          OutlinedButton(
            onClick = { onNavigate(Screen.RmMapping) },
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else Color(0xFFEFF6FF),
              contentColor = if (isDark) Color(0xFF93C5FD) else Color(0xFF1E40AF)
            ),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF3B82F6) else Color(0xFFBFDBFE)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 4.dp),
            modifier = Modifier.weight(1f).testTag("mentor_btn_rm_mapping")
          ) {
            Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("RMs", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
          }

          OutlinedButton(
            onClick = { onNavigate(Screen.MentorUserLocationTracking) },
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = if (isDark) Color(0xFF164E63).copy(alpha = 0.35f) else Color(0xFFECFEFF),
              contentColor = if (isDark) Color(0xFF67E8F9) else Color(0xFF0E7490)
            ),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF06B6D4) else Color(0xFFA5F3FC)),
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
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5),
              contentColor = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
            ),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF10B981) else Color(0xFFA7F3D0)),
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
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = if (isDark) Color(0xFF78350F).copy(alpha = 0.35f) else Color(0xFFFFFBEB),
              contentColor = if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309)
            ),
            border = BorderStroke(1.dp, if (isDark) Color(0xFFF59E0B) else Color(0xFFFDE68A)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 5.dp),
            modifier = Modifier.weight(1.2f).testTag("mentor_btn_dbr_checklist")
          ) {
            Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("DBR & Checklist", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
          }

          OutlinedButton(
            onClick = { onNavigate(Screen.ImportantDocuments) },
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = if (isDark) Color(0xFF164E63).copy(alpha = 0.35f) else Color(0xFFECFEFF),
              contentColor = if (isDark) Color(0xFF67E8F9) else Color(0xFF0E7490)
            ),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF06B6D4) else Color(0xFFA5F3FC)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 5.dp),
            modifier = Modifier.weight(1.1f).testTag("mentor_btn_important_docs")
          ) {
            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Important Docs", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          val deletedCount = allFiles.count { it.isDeleted }
          Button(
            onClick = { showDeletedRecordsDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 5.dp),
            modifier = Modifier.weight(1.1f).testTag("mentor_btn_quick_recover_deleted")
          ) {
            Icon(Icons.Default.RestoreFromTrash, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Recover Deleted ($deletedCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
          }

          OutlinedButton(
            onClick = { onNavigate(Screen.CommunicationHub) },
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = if (isDark) Color(0xFF881337).copy(alpha = 0.35f) else Color(0xFFFFF1F2),
              contentColor = if (isDark) Color(0xFFFDA4AF) else Color(0xFFBE123C)
            ),
            border = BorderStroke(1.dp, if (isDark) Color(0xFFF43F5E) else Color(0xFFFECDD3)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 5.dp),
            modifier = Modifier.weight(1f).testTag("mentor_btn_events_hub")
          ) {
            Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Events & Reports", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = { onNavigate(Screen.CompanyProfile) },
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else Color(0xFFEFF6FF),
              contentColor = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
            ),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF3B82F6) else Color(0xFF93C5FD)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 5.dp),
            modifier = Modifier.weight(1f).testTag("mentor_btn_company_profile")
          ) {
            Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Company Profile", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
          }

          OutlinedButton(
            onClick = { onNavigate(Screen.FieldRoutePlanner()) },
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5),
              contentColor = if (isDark) Color(0xFF6EE7B7) else Color(0xFF059669)
            ),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF10B981) else Color(0xFFA7F3D0)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 5.dp),
            modifier = Modifier.weight(1f).testTag("mentor_btn_route_planner")
          ) {
            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Route Planner", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
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

                val ccTarget = row.target?.getEffectiveCcTarget() ?: 20
                val corpTarget = row.target?.getEffectiveCorpTarget() ?: 10
                val b2bTarget = row.target?.getEffectiveB2bTarget() ?: 15
                val annualCc = row.target?.annualCreditCardTarget ?: (ccTarget * 4)
                val annualCorp = row.target?.annualCorporateCardTarget ?: (corpTarget * 4)
                val annualB2b = row.target?.annualB2bTarget ?: (b2bTarget * 4)
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF0FDF4), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))) {
                      Text("CC: ${row.stats.stcCreditCardCount}/$ccTarget (Yr:$annualCc)", fontSize = 10.sp, color = Color(0xFF166534), modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), maxLines = 1, softWrap = false)
                    }
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFEFF6FF), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))) {
                      Text("Corp: ${row.stats.stcCorporateCardCount}/$corpTarget (Yr:$annualCorp)", fontSize = 10.sp, color = Color(0xFF1E40AF), modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), maxLines = 1, softWrap = false)
                    }
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFAF5FF), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D5FF))) {
                      Text("B2B: ${row.stats.stcB2bCount}/$b2bTarget (Yr:$annualB2b)", fontSize = 10.sp, color = Color(0xFF6B21A8), modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), maxLines = 1, softWrap = false)
                    }
                  }

                  Text(
                    text = "⚙ Set Target",
                    fontSize = 11.sp,
                    color = Color(0xFF2563EB),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { mentorTargetSettingRow = row }
                  )
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
                showDeletedRecordsDialog = true
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

  // Mentor Deleted Records Recovery Dialog
  if (showDeletedRecordsDialog) {
    var searchQuery by remember { mutableStateOf("") }
    val deletedFiles = remember(allFiles) { allFiles.filter { it.isDeleted } }
    val filteredDeleted = remember(deletedFiles, searchQuery) {
      if (searchQuery.isBlank()) deletedFiles else {
        val q = searchQuery.trim().lowercase()
        deletedFiles.filter {
          it.customerName.lowercase().contains(q) ||
          it.fileId.lowercase().contains(q) ||
          it.mobile.lowercase().contains(q) ||
          it.assignedRmCode.lowercase().contains(q) ||
          it.companyName.lowercase().contains(q)
        }
      }
    }

    androidx.compose.ui.window.Dialog(
      onDismissRequest = { showDeletedRecordsDialog = false },
      properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier
          .fillMaxWidth(0.95f)
          .fillMaxHeight(0.88f)
          .testTag("mentor_deleted_records_dialog")
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
        ) {
          // Dialog Header
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .background(Color(0xFF7C3AED).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                  .padding(8.dp)
              ) {
                Icon(
                  Icons.Default.RestoreFromTrash,
                  contentDescription = null,
                  tint = Color(0xFF7C3AED),
                  modifier = Modifier.size(22.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Deleted Records Recovery",
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  color = EblNavyDark
                )
                Text(
                  text = "ডিলিট করা এন্ট্রি পুনরুদ্ধার ও শিটে রিলোড (${deletedFiles.size} records)",
                  fontSize = 11.sp,
                  color = Color.Gray
                )
              }
            }

            if (deletedFiles.isNotEmpty()) {
              OutlinedButton(
                onClick = {
                  deletedFiles.forEach { file ->
                    viewModel.restoreFile(file.fileId)
                  }
                  Toast.makeText(context, "✓ ${deletedFiles.size} টি এন্ট্রি সফলভাবে পুনরুদ্ধার হয়েছে!", Toast.LENGTH_SHORT).show()
                },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.testTag("btn_recover_all_entries")
              ) {
                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF059669))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Recover All", fontSize = 11.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Search Field
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by customer, phone, RM code...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_search_deleted_records")
          )

          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider(color = Color(0xFFE2E8F0))
          Spacer(modifier = Modifier.height(8.dp))

          // List of Deleted Entries
          if (filteredDeleted.isEmpty()) {
            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                  Icons.Default.CheckCircle,
                  contentDescription = null,
                  modifier = Modifier.size(48.dp),
                  tint = Color(0xFF10B981)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                  text = if (deletedFiles.isEmpty()) "কোনো ডিলিট করা এন্ট্রি নেই" else "কোনো মিল খুঁজে পাওয়া যায়নি",
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = EblNavyDark
                )
                Text(
                  text = if (deletedFiles.isEmpty()) "সব ফাইল বর্তমানে সক্রিয় রয়েছে।" else "ভিন্ন কোনো নাম বা ফোন নম্বর দিয়ে চেষ্টা করুন।",
                  fontSize = 11.sp,
                  color = Color.Gray
                )
              }
            }
          } else {
            LazyColumn(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              items(filteredDeleted, key = { it.fileId }) { file ->
                Card(
                  shape = RoundedCornerShape(10.dp),
                  colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                  border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("deleted_file_item_${file.fileId}")
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                          text = file.customerName,
                          fontWeight = FontWeight.Bold,
                          fontSize = 14.sp,
                          color = EblNavyDark
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                          color = Color(0xFFFEE2E2),
                          shape = RoundedCornerShape(4.dp)
                        ) {
                          Text(
                            text = "DELETED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                          )
                        }
                      }

                      Spacer(modifier = Modifier.height(2.dp))
                      Text(
                        text = "File ID: ${file.fileId} ${if (file.ccNumber.isNotBlank()) "• CC: ${file.ccNumber}" else ""} • ${file.productType}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                      )

                      if (file.companyName.isNotBlank()) {
                        Text(
                          text = "Company: ${file.companyName}",
                          fontSize = 11.sp,
                          color = Color.Gray
                        )
                      }

                      Text(
                        text = "Mobile: ${file.mobile} • Assigned RM: ${file.assignedRmCode}",
                        fontSize = 11.sp,
                        color = Color(0xFF334155)
                      )

                      Text(
                        text = "Deleted by: ${file.updatedBy} • ${DateUtils.formatDateTime(file.updatedAt)}",
                        fontSize = 10.sp,
                        color = Color.Gray
                      )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Individual Recover Button
                    Button(
                      onClick = {
                        viewModel.restoreFile(file.fileId)
                        Toast.makeText(
                          context,
                          "✓ '${file.customerName}' পুনরুদ্ধার হয়েছে ও শিটে লোড হয়েছে!",
                          Toast.LENGTH_SHORT
                        ).show()
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                      shape = RoundedCornerShape(8.dp),
                      contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                      modifier = Modifier.testTag("btn_recover_file_${file.fileId}")
                    ) {
                      Icon(
                        Icons.Default.Restore,
                        contentDescription = "Recover",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = "Recover",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                      )
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider(color = Color(0xFFE2E8F0))
          Spacer(modifier = Modifier.height(8.dp))

          // Bottom Close Action
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            Button(
              onClick = { showDeletedRecordsDialog = false },
              colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("btn_close_deleted_dialog")
            ) {
              Text("Close (বন্ধ করুন)")
            }
          }
        }
      }
    }
  }

  // Set RM Quarter Targets Dialog for Mentor
  if (mentorTargetSettingRow != null) {
    val row = mentorTargetSettingRow!!
    val targetCriteria by viewModel.targetCriteriaOptions.collectAsState()
    val customTargets by viewModel.getRmCustomTargetsFlow(row.rmCode).collectAsState(initial = emptyMap())
    val allQuarterCustomTargets by viewModel.getRmAllQuarterCustomTargetsFlow(row.rmCode).collectAsState(initial = emptyMap())
    val assignedCriteria by viewModel.getRmAssignedCriteriaFlow(row.rmCode).collectAsState(initial = emptyList())

    com.example.ui.common.QuarterTargetSettingDialog(
      rmCode = row.rmCode,
      rmName = row.rmName,
      currentTarget = row.target,
      allCriteria = targetCriteria,
      assignedCriteria = assignedCriteria,
      onAddCriteria = { newCrit -> viewModel.addTargetCriterion(newCrit) },
      onDeleteCriteria = { crit -> viewModel.deleteTargetCriterion(crit) },
      initialCustomTargets = customTargets,
      allQuarterCustomTargets = allQuarterCustomTargets,
      onDismiss = { mentorTargetSettingRow = null },
      onSave = { quarter, year, cc, corp, b2b ->
        viewModel.setRmQuarterTargets(row.rmCode, quarter, year, cc, corp, b2b) { success, _ ->
          if (success) mentorTargetSettingRow = null
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
      }
    )
  }
}
