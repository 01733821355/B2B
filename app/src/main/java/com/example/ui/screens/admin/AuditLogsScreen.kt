package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.model.UserEntity
import com.example.ui.common.VoiceInputField
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.viewmodel.AppViewModel
import com.example.util.DateUtils

@Composable
fun AuditLogsScreen(
  viewModel: AppViewModel,
  currentUser: UserEntity,
  modifier: Modifier = Modifier
) {
  val auditLogs by viewModel.auditLogs.collectAsState()
  var searchQuery by remember { mutableStateOf("") }

  val filteredLogs = auditLogs.filter { log ->
    if (searchQuery.isBlank()) true
    else {
      val q = searchQuery.lowercase()
      log.action.lowercase().contains(q) ||
        log.userId.lowercase().contains(q) ||
        log.details.lowercase().contains(q) ||
        (log.fileId?.lowercase()?.contains(q) == true) ||
        (log.rmCode?.lowercase()?.contains(q) == true)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("audit_logs_screen")
  ) {
    // Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Security Audit Trail",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = EblNavyDark
        )
        Text(
          text = "Immutable chronological activity logging",
          fontSize = 11.sp,
          color = Color.Gray
        )
      }
      Icon(
        imageVector = Icons.Default.Security,
        contentDescription = null,
        tint = EblNavyPrimary,
        modifier = Modifier.padding(end = 4.dp)
      )
    }

    // Search bar with voice input
    VoiceInputField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      label = "Search Audit Logs",
      placeholder = "Filter logs by user, action, file ID...",
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      testTag = "search_audit_logs"
    )

    Spacer(modifier = Modifier.height(10.dp))

    if (filteredLogs.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Text("No audit log entries found.", color = Color.Gray, fontSize = 13.sp)
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp)
      ) {
        items(filteredLogs, key = { it.logId }) { log ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .background(Color(0xFFE8EEF5), RoundedCornerShape(4.dp))
                      .padding(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                    Text(log.action, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EblNavyDark)
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "User: ${log.userId} (${log.role})",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                  )
                }

                Text(
                  text = DateUtils.formatDateTime(log.timestamp),
                  fontSize = 10.sp,
                  color = Color.Gray
                )
              }

              Spacer(modifier = Modifier.height(6.dp))

              Text(log.details, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)

              if (log.fileId != null || log.rmCode != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  if (log.fileId != null) {
                    Text("File: ${log.fileId}", fontSize = 10.sp, color = EblNavyPrimary, fontWeight = FontWeight.SemiBold)
                  }
                  if (log.rmCode != null) {
                    Text("RM: ${log.rmCode}", fontSize = 10.sp, color = Color.Gray)
                  }
                }
              }
            }
          }
        }

        item {
          Spacer(modifier = Modifier.height(24.dp))
        }
      }
    }
  }
}
