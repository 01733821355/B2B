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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
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
  var inputAppsScriptUrl by remember(status.appsScriptUrl) { mutableStateOf(status.appsScriptUrl) }
  var isSavingUrl by remember { mutableStateOf(false) }
  var isSavingScriptUrl by remember { mutableStateOf(false) }
  var isSyncingNow by remember { mutableStateOf(false) }
  var isPullingNow by remember { mutableStateOf(false) }
  val isRealtimeAutoSyncEnabled by viewModel.isRealtimeAutoSyncEnabled.collectAsState()
  val isSyncingInProgress by viewModel.isSyncingInProgress.collectAsState()
  var feedbackMessage by remember { mutableStateOf<String?>(null) }
  var showScriptCodeDialog by remember { mutableStateOf(false) }

  val appsScriptTemplate = """
function doGet(e) {
  return handleFetchFiles();
}

function doPost(e) {
  try {
    var data = JSON.parse(e.postData.contents);
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    
    // 1. Auto-create & format 'Customer_Files' Tab
    var fileSheet = ss.getSheetByName('Customer_Files');
    if (!fileSheet) {
      fileSheet = ss.insertSheet('Customer_Files');
      var headers = [
        'CC-Number', 'File ID', 'Customer Name', 'Company Name',
        'Office Address', 'Mobile', 'Email', 'Product Type',
        'Status', 'Active', 'Assigned RM', 'Pending Documents',
        'CPV Remarks', 'GPS Location Address', 'GPS Lat', 'GPS Lng', 'Last Updated'
      ];
      fileSheet.appendRow(headers);
      var hr = fileSheet.getRange(1, 1, 1, headers.length);
      hr.setBackground('#0A192F').setFontColor('#FFFFFF').setFontWeight('bold');
      fileSheet.setFrozenRows(1);
    }

    if (data.action === 'FETCH_SHEET_DATA') {
      return handleFetchFiles();
    }
    
    // 2. Auto-create & format 'RM_Location_Logs' Tab
    var locSheet = ss.getSheetByName('RM_Location_Logs');
    if (!locSheet) {
      locSheet = ss.insertSheet('RM_Location_Logs');
      var locHeaders = ['RM Code', 'RM Name', 'Latitude', 'Longitude', 'Location Address', 'Timestamp', 'Trigger Action'];
      locSheet.appendRow(locHeaders);
      var lr = locSheet.getRange(1, 1, 1, locHeaders.length);
      lr.setBackground('#1E3A8A').setFontColor('#FFFFFF').setFontWeight('bold');
      locSheet.setFrozenRows(1);
    }
    
    // 3. Upsert Files Data by CC-Number / File ID
    if (data.files && data.files.length > 0) {
      var existingData = fileSheet.getDataRange().getValues();
      var idRowMap = {};
      for (var r = 1; r < existingData.length; r++) {
        var key = existingData[r][0] || existingData[r][1];
        if (key) idRowMap[key] = r + 1;
      }
      
      data.files.forEach(function(f) {
        var row = [
          f.ccNumber, f.fileId, f.customerName, f.companyName,
          f.officeAddress, f.mobile, f.email, f.productType,
          f.applicationStatus, f.activeStatus, f.assignedRmCode,
          f.pendingDocuments, f.cpvRemarks, f.submissionAddress,
          f.submissionLat, f.submissionLng, f.updatedAt
        ];
        var match = f.ccNumber || f.fileId;
        if (idRowMap[match]) {
          fileSheet.getRange(idRowMap[match], 1, 1, row.length).setValues([row]);
        } else {
          fileSheet.appendRow(row);
          idRowMap[match] = fileSheet.getLastRow();
        }
      });
    }
    
    // 4. Record Location Logs
    if (data.locations && data.locations.length > 0) {
      data.locations.forEach(function(l) {
        locSheet.appendRow([l.rmCode, l.userName, l.latitude, l.longitude, l.address, l.timestamp, l.sourceAction]);
      });
    }
    
    return ContentService.createTextOutput(JSON.stringify({
      status: 'success',
      syncedCount: (data.files ? data.files.length : 0),
      latestFiles: extractAllSheetFiles(fileSheet)
    })).setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({
      status: 'error',
      message: err.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

function handleFetchFiles() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var fileSheet = ss.getSheetByName('Customer_Files');
  var files = fileSheet ? extractAllSheetFiles(fileSheet) : [];
  return ContentService.createTextOutput(JSON.stringify({
    status: 'success',
    filesCount: files.length,
    files: files
  })).setMimeType(ContentService.MimeType.JSON);
}

function extractAllSheetFiles(sheet) {
  var data = sheet.getDataRange().getValues();
  var list = [];
  for (var r = 1; r < data.length; r++) {
    var row = data[r];
    if (!row[0] && !row[1]) continue;
    list.push({
      ccNumber: String(row[0] || ''),
      fileId: String(row[1] || row[0] || ''),
      customerName: String(row[2] || ''),
      companyName: String(row[3] || ''),
      officeAddress: String(row[4] || ''),
      mobile: String(row[5] || ''),
      email: String(row[6] || ''),
      productType: String(row[7] || ''),
      applicationStatus: String(row[8] || ''),
      activeStatus: String(row[9] || ''),
      assignedRmCode: String(row[10] || ''),
      pendingDocuments: String(row[11] || ''),
      cpvRemarks: String(row[12] || ''),
      submissionAddress: String(row[13] || ''),
      updatedAt: String(row[16] || '')
    });
  }
  return list;
}
""".trimIndent()

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

    // Real-Time Continuous 2-Way Auto-Sync Card (Hands-free automatic polling every 4s)
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(
        containerColor = if (isRealtimeAutoSyncEnabled) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface
      ),
      border = androidx.compose.foundation.BorderStroke(
        1.dp,
        if (isRealtimeAutoSyncEnabled) Color(0xFF86EFAC) else Color(0xFFE2E8F0)
      )
    ) {
      Row(
        modifier = Modifier.padding(14.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(if (isRealtimeAutoSyncEnabled) Color(0xFF16A34A) else Color.Gray),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (isSyncingInProgress) Icons.Default.Refresh else Icons.Default.Sync,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }
          Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "Continuous Live Auto-Sync",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isRealtimeAutoSyncEnabled) Color(0xFF14532D) else MaterialTheme.colorScheme.onSurface
              )
              if (isSyncingInProgress) {
                Text(
                  text = "• Syncing now...",
                  fontSize = 10.sp,
                  color = Color(0xFF16A34A),
                  fontWeight = FontWeight.Bold
                )
              }
            }
            Text(
              text = if (isRealtimeAutoSyncEnabled)
                "প্রতি ৪ সেকেন্ডে অ্যাপ এবং গুগল শিট স্বয়ংক্রিয়ভাবে সিঙ্ক হচ্ছে (ম্যানুয়ালি পুশ বা পুল চাপার প্রয়োজন নেই)।"
              else
                "অটো-সিঙ্ক বন্ধ রয়েছে। স্বয়ংক্রিয় সিঙ্ক চালু করতে টগল করুন।",
              fontSize = 11.sp,
              color = Color.DarkGray
            )
          }
        }

        Switch(
          checked = isRealtimeAutoSyncEnabled,
          onCheckedChange = { viewModel.toggleRealtimeAutoSync(it) }
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

    // Automated Apps Script Web App Connector (Auto-creates tabs & headers)
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Code, contentDescription = null, tint = EblNavyPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Apps Script Web App Connector",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = EblNavyDark
            )
          }
          Surface(
            color = Color(0xFFEFF6FF),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = "AUTO TABS & HEADERS",
              color = Color(0xFF1D4ED8),
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Paste the Google Apps Script Web App URL below to automatically create 'Customer_Files' and 'RM_Location_Logs' sheets with formatted headers upon sync.",
          fontSize = 11.sp,
          color = Color.Gray,
          lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        com.example.ui.common.VoiceInputField(
          value = inputAppsScriptUrl,
          onValueChange = { inputAppsScriptUrl = it },
          label = "Apps Script Web App URL",
          placeholder = "https://script.google.com/macros/s/.../exec",
          leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, tint = EblNavyPrimary) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          testTag = "input_apps_script_url"
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = {
              if (inputAppsScriptUrl.isBlank()) {
                feedbackMessage = "Please paste your Google Apps Script Web App URL."
                return@Button
              }
              isSavingScriptUrl = true
              viewModel.updateAppsScriptConfig(inputAppsScriptUrl.trim(), "EBL_SYNC_KEY") { success, msg ->
                isSavingScriptUrl = false
                feedbackMessage = if (success) "Apps Script Web App connected successfully!" else msg
              }
            },
            enabled = !isSavingScriptUrl,
            colors = ButtonDefaults.buttonColors(containerColor = EblNavyPrimary),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            if (isSavingScriptUrl) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
              Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Save Connector", fontSize = 11.sp)
            }
          }

          OutlinedButton(
            onClick = {
              val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
              val clip = android.content.ClipData.newPlainText("Google Apps Script", appsScriptTemplate)
              clipboard.setPrimaryClip(clip)
              feedbackMessage = "Google Apps Script code copied to clipboard! Paste it into Extensions > Apps Script."
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Copy Script Code", fontSize = 11.sp)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bengali & English Setup Instructions
        Surface(
          color = Color(0xFFF8FAFC),
          shape = RoundedCornerShape(8.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text("কীভাবে গুগল শিট সেটআপ করবেন (How to Setup):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EblNavyDark)
            Spacer(modifier = Modifier.height(4.dp))
            Text("১. আপনার গুগল শিটে যান এবং Extensions > Apps Script-এ ক্লিক করুন।", fontSize = 11.sp, color = Color.DarkGray)
            Text("২. ওপরের 'Copy Script Code' বাটনে চাপ দিয়ে কোডটি কপি করে Apps Script-এ পেস্ট করুন।", fontSize = 11.sp, color = Color.DarkGray)
            Text("৩. Deploy > New deployment সিলেক্ট করুন, Type দিন 'Web app'।", fontSize = 11.sp, color = Color.DarkGray)
            Text("৪. 'Who has access' অপশনে 'Anyone' নির্বাচন করে Deploy চাপুন।", fontSize = 11.sp, color = Color.DarkGray)
            Text("৫. পাওয়া Web App URL-টি কপি করে এখানে পেস্ট করে 'Save Connector' চাপুন।", fontSize = 11.sp, color = Color.DarkGray)
            Text("৬. ব্যস! অ্যাপ স্বয়ংক্রিয়ভাবে 'Customer_Files' ও 'RM_Location_Logs' ট্যাব ও হেডার তৈরি করবে।", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF059669))
          }
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

        // Two-Way Sync Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = {
              isSyncingNow = true
              viewModel.triggerGoogleSheetsSync { success, msg ->
                isSyncingNow = false
                feedbackMessage = msg
              }
            },
            enabled = !isSyncingNow && !isPullingNow,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f).testTag("btn_sync_now")
          ) {
            if (isSyncingNow) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Pushing...", fontSize = 11.sp)
            } else {
              Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Push to Sheet", fontSize = 11.sp)
            }
          }

          Button(
            onClick = {
              isPullingNow = true
              viewModel.pullDataFromGoogleSheets { success, msg ->
                isPullingNow = false
                feedbackMessage = msg
              }
            },
            enabled = !isSyncingNow && !isPullingNow,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f).testTag("btn_pull_sheets")
          ) {
            if (isPullingNow) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Pulling...", fontSize = 11.sp)
            } else {
              Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Pull from Sheet", fontSize = 11.sp)
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}
