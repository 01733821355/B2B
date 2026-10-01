package com.example.ui.screens.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SyncStatusEntity
import com.example.ui.theme.EblGold
import com.example.ui.theme.EblNavyDark
import com.example.ui.theme.EblNavyPrimary
import com.example.ui.viewmodel.AppViewModel
import com.example.util.DateUtils

@Composable
fun GoogleSheetsSyncScreen(
  viewModel: AppViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val syncStatus by viewModel.syncStatus.collectAsState()
  val status = syncStatus ?: SyncStatusEntity()
  val scrollState = rememberScrollState()

  var inputSheetUrl by remember(status.spreadsheetId) {
    mutableStateOf(
      if (status.spreadsheetId.isNotBlank()) "https://docs.google.com/spreadsheets/d/${status.spreadsheetId}/edit" else ""
    )
  }
  var isSavingUrl by remember { mutableStateOf(false) }
  var isSyncingNow by remember { mutableStateOf(false) }
  var feedbackMessage by remember { mutableStateOf<String?>(null) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(scrollState)
      .padding(14.dp)
      .testTag("google_sheets_sync_screen")
  ) {
    // Header Banner
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = EblNavyDark)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.CloudDone,
              contentDescription = null,
              tint = EblGold,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Google Sheets Auto-Sync",
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }

          Surface(
            color = Color(0xFF10B981).copy(alpha = 0.2f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = "REAL-TIME AUTO",
              color = Color(0xFF34D399),
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Every file creation, update, or deletion automatically syncs into your Google Sheet in the background. No manual sync or code needed!",
          fontSize = 12.sp,
          color = Color(0xFFE2E8F0),
          lineHeight = 16.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Real-Time Sync Status Badge Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Sync Engine Status",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = EblNavyDark
          )

          val (badgeText, badgeBg, badgeColor) = when (status.lastSyncStatus) {
            "SUCCESS" -> Triple("✓ Connected & Synced", Color(0xFFDCFCE7), Color(0xFF15803D))
            "IN_PROGRESS" -> Triple("Syncing...", Color(0xFFDBEAFE), Color(0xFF1D4ED8))
            "FAILED" -> Triple("Sync Issue", Color(0xFFFEE2E2), Color(0xFFB91C1C))
            else -> Triple("Ready", Color(0xFFFEF3C7), Color(0xFFB45309))
          }

          Surface(
            color = badgeBg,
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = badgeText,
              color = badgeColor,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text("Last Sync Time", fontSize = 10.sp, color = Color.Gray)
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = DateUtils.formatDateTime(status.lastSyncTimestamp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = EblNavyDark
              )
            }
          }

          Surface(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text("Connected Sheet ID", fontSize = 10.sp, color = Color.Gray)
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = status.spreadsheetId.take(16) + if (status.spreadsheetId.length > 16) "..." else "",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = EblNavyDark,
                fontFamily = FontFamily.Monospace
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Simple 1-Step Configuration (ONLY Google Sheet Link Required)
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Link, contentDescription = null, tint = EblNavyPrimary, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Google Spreadsheet Link",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = EblNavyDark
          )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Simply paste your Google Sheet URL. The app automatically configures all headers, tabs, and columns (including CC-number and GPS). No code or script required!",
          fontSize = 11.sp,
          color = Color.Gray,
          lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        com.example.ui.common.VoiceInputField(
          value = inputSheetUrl,
          onValueChange = {
            inputSheetUrl = it
            feedbackMessage = null
          },
          label = "Google Sheet Link (URL)",
          placeholder = "https://docs.google.com/spreadsheets/d/...",
          leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = EblNavyPrimary) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth(),
          testTag = "input_google_sheet_url"
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = {
              if (inputSheetUrl.isBlank()) {
                feedbackMessage = "Please enter or paste a valid Google Sheet URL."
                return@Button
              }
              isSavingUrl = true
              viewModel.setGoogleSheetUrl(inputSheetUrl) { success, msg ->
                isSavingUrl = false
                feedbackMessage = msg
              }
            },
            enabled = !isSavingUrl,
            colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f).testTag("btn_save_sheet_url")
          ) {
            if (isSavingUrl) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Connecting...", fontSize = 12.sp)
            } else {
              Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Connect Sheet", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }

          if (status.spreadsheetId.isNotBlank()) {
            OutlinedButton(
              onClick = {
                val fullUrl = if (inputSheetUrl.startsWith("http")) inputSheetUrl else "https://docs.google.com/spreadsheets/d/${status.spreadsheetId}/edit"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
                context.startActivity(intent)
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("btn_open_google_sheet")
            ) {
              Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Open Sheet", fontSize = 12.sp)
            }
          }
        }

        if (feedbackMessage != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = feedbackMessage!!,
            fontSize = 11.sp,
            color = if (feedbackMessage!!.contains("Success", ignoreCase = true) || feedbackMessage!!.contains("Linked", ignoreCase = true)) Color(0xFF059669) else Color(0xFFDC2626),
            fontWeight = FontWeight.Medium
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Real-Time Automatic Sync Information
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.AutoMode, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Auto-Synced Data Columns",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = EblNavyDark
          )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "The app synchronizes the following headers into your spreadsheet automatically:",
          fontSize = 11.sp,
          color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        val syncedColumns = listOf(
          "File ID", "Customer Name", "Company", "Office Address",
          "Mobile", "CC-number", "Product Type", "Status", "Active (Y/N/C)",
          "Assigned RM", "Pending Documents", "CPV Status", "Submission GPS Address"
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          syncedColumns.chunked(2).forEach { rowCols ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              rowCols.forEach { col ->
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = MaterialTheme.colorScheme.background,
                  border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                  modifier = Modifier.weight(1f)
                ) {
                  Text(
                    text = "• $col",
                    fontSize = 11.sp,
                    color = EblNavyDark,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                  )
                }
              }
              if (rowCols.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Manual 1-Tap Sync Now Button
        Button(
          onClick = {
            isSyncingNow = true
            viewModel.triggerGoogleSheetsSync { success, msg ->
              isSyncingNow = false
              feedbackMessage = msg
            }
          },
          enabled = !isSyncingNow,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth().testTag("btn_sync_now")
        ) {
          if (isSyncingNow) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Syncing Records...", fontSize = 12.sp)
          } else {
            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Sync All Data Now", fontSize = 12.sp)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}
