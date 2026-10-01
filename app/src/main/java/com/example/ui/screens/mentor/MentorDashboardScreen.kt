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
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = { onNavigate(Screen.GlobalDatabase) },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).testTag("mentor_btn_database")
            ) {
              Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Database", fontSize = 11.sp)
            }

            OutlinedButton(
              onClick = { onNavigate(Screen.RmMapping) },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).testTag("mentor_btn_rm_mapping")
            ) {
              Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("RM Mapping", fontSize = 11.sp)
            }

            OutlinedButton(
              onClick = { onNavigate(Screen.AuditLogs) },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).testTag("mentor_btn_audit_logs")
            ) {
              Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Audit", fontSize = 11.sp)
            }

            OutlinedButton(
              onClick = { onNavigate(Screen.MentorUserLocationTracking) },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).testTag("mentor_btn_user_locations")
            ) {
              Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Locations", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
