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
  var inputAppsScriptUrl by remember(status.appsScriptUrl) {
    mutableStateOf(
      if (status.appsScriptUrl.isNotBlank()) status.appsScriptUrl
      else "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec"
    )
  }
  var isSavingUrl by remember { mutableStateOf(false) }
  var isSavingScriptUrl by remember { mutableStateOf(false) }
  var isSyncingNow by remember { mutableStateOf(false) }
  var isPullingNow by remember { mutableStateOf(false) }
  val isRealtimeAutoSyncEnabled by viewModel.isRealtimeAutoSyncEnabled.collectAsState()
  val isSyncingInProgress by viewModel.isSyncingInProgress.collectAsState()
  var feedbackMessage by remember { mutableStateOf<String?>(null) }
  var showScriptCodeDialog by remember { mutableStateOf(false) }

  val appsScriptTemplate = """
/**
 * EBL Sales & RM Suite - 7-Tab Multi-Device Bi-Directional Auto-Sync Web App
 * Tabs: Customer_Files, RM_Details, Universal_Settings, Audit_Trail, Attachments, SMS_Notifications, RM_Location_Logs
 *
 * FEATURES:
 * 1. Supports Manual Editing directly in Google Sheets without data loss!
 * 2. onEdit trigger stamps 'Manual_Sheet_Edit' to prevent auto-sync overwriting manual edits.
 * 3. doGet returns full multi-tab JSON for any connected browser or mobile app.
 * 4. Full multi-device real-time sync for RMs, Targets, Customer Files, and Universal Settings.
 */

function doGet(e) {
  return handleFetchAllData();
}

function onEdit(e) {
  try {
    if (!e || !e.range) return;
    var range = e.range;
    var sheet = range.getSheet();
    var sheetName = sheet.getName();
    var row = range.getRow();
    if (row <= 1) return; // Header row ignored

    var nowStr = Utilities.formatDate(new Date(), "GMT+6", "yyyy-MM-dd HH:mm:ss");

    if (sheetName === 'Customer_Files' || sheetName === 'Files') {
      sheet.getRange(row, 18).setValue(nowStr); // Last Updated
      sheet.getRange(row, 19).setValue('Manual_Sheet_Edit'); // Updated By
    } else if (sheetName === 'RM_Details' || sheetName === 'RM_Directory') {
      sheet.getRange(row, 14).setValue(nowStr); // Last Updated
    } else if (sheetName === 'Universal_Settings' || sheetName === 'Settings') {
      sheet.getRange(row, 4).setValue('Manual_Sheet_Edit'); // Last Updated By
      sheet.getRange(row, 5).setValue(nowStr); // Last Updated At
    }
  } catch (err) {}
}

function doPost(e) {
  try {
    var contents = e.postData ? e.postData.contents : '{}';
    var data = JSON.parse(contents);
    var ss = SpreadsheetApp.getActiveSpreadsheet();

    // 1. Auto-create & format 'Customer_Files' Tab
    var fileSheet = getOrCreateSheet(ss, 'Customer_Files', [
      'CC-Number', 'File ID', 'Customer Name', 'Company Name',
      'Office Address', 'Mobile', 'Email', 'Product Type',
      'Application Status', 'Active Status', 'Assigned RM Code', 'Pending Documents',
      'CPV Remarks', 'CPV Status', 'GPS Submission Address', 'GPS Lat', 'GPS Lng',
      'Last Updated', 'Updated By'
    ], '#0A192F');

    // 2. Auto-create & format 'RM_Details' Tab
    var rmSheet = getOrCreateSheet(ss, 'RM_Details', [
      'RM Code', 'Full Name', 'Mobile', 'Email',
      'Office Address', 'Role', 'Account Status',
      'Credit Card Target', 'Corporate Card Target', 'B2B Target',
      'Password Hash', 'Salt', 'Created At', 'Last Updated'
    ], '#1E3A8A');

    // 3. Auto-create & format 'Universal_Settings' Tab
    var settingsSheet = getOrCreateSheet(ss, 'Universal_Settings', [
      'Setting Key', 'Setting Value', 'Description', 'Last Updated By', 'Last Updated At'
    ], '#065F46');

    // 4. Auto-create & format 'Audit_Trail' Tab
    var auditSheet = getOrCreateSheet(ss, 'Audit_Trail', [
      'Log ID', 'User / RM Code', 'Role', 'Action Type', 'Target ID / File ID', 'Details', 'Timestamp'
    ], '#7C2D12');

    // 5. Auto-create & format 'Attachments' Tab
    var attachSheet = getOrCreateSheet(ss, 'Attachments', [
      'Attachment ID', 'File ID', 'File Name', 'Category', 'File Size (Bytes)', 'Uploaded By', 'Timestamp'
    ], '#4C1D95');

    // 6. Auto-create & format 'SMS_Notifications' Tab
    var smsSheet = getOrCreateSheet(ss, 'SMS_Notifications', [
      'Notification ID', 'Recipient RM Code', 'Recipient Name', 'Recipient Mobile',
      'Triggered By Role', 'Triggered By Code', 'Action Type', 'Target ID',
      'Message Text', 'Delivery Status', 'Timestamp'
    ], '#991B1B');

    // 7. Auto-create & format 'RM_Location_Logs' Tab
    var locSheet = getOrCreateSheet(ss, 'RM_Location_Logs', [
      'RM Code', 'RM Name', 'Latitude', 'Longitude', 'Location Address', 'Timestamp', 'Source Action'
    ], '#0F766E');

    // Handle File Deletions (Immediately removes rows from Google Sheets when deleted in app)
    if (data.action === 'DELETE_FILE' || (data.deletedFileIds && data.deletedFileIds.length > 0)) {
      var toDelete = data.deletedFileIds || [data.fileId, data.ccNumber].filter(Boolean);
      var curFiles = fileSheet.getDataRange().getValues();
      for (var d = curFiles.length - 1; d >= 1; d--) {
        var rCc = String(curFiles[d][0] || '').trim().toLowerCase();
        var rFid = String(curFiles[d][1] || '').trim().toLowerCase();
        for (var k = 0; k < toDelete.length; k++) {
          var targetK = String(toDelete[k] || '').trim().toLowerCase();
          if (targetK && (rFid === targetK || rCc === targetK)) {
            fileSheet.deleteRow(d + 1);
            break;
          }
        }
      }
      if (data.action === 'DELETE_FILE') {
        return ContentService.createTextOutput(JSON.stringify({ status: "SUCCESS", message: "Deleted file row from sheet." }))
          .setMimeType(ContentService.MimeType.JSON);
      }
    }

    // Handle RM Profile Deletions (Immediately removes RM rows from Google Sheets when deleted in app)
    if (data.action === 'DELETE_RM' || (data.deletedRmCodes && data.deletedRmCodes.length > 0)) {
      var toDeleteRms = data.deletedRmCodes || [data.rmCode].filter(Boolean);
      var curRms = rmSheet.getDataRange().getValues();
      for (var dr = curRms.length - 1; dr >= 1; dr--) {
        var rmCodeVal = String(curRms[dr][0] || '').trim().toUpperCase();
        for (var m = 0; m < toDeleteRms.length; m++) {
          var targetRm = String(toDeleteRms[m] || '').trim().toUpperCase();
          if (targetRm && rmCodeVal === targetRm) {
            rmSheet.deleteRow(dr + 1);
            break;
          }
        }
      }
      if (data.action === 'DELETE_RM') {
        return ContentService.createTextOutput(JSON.stringify({ status: "SUCCESS", message: "Deleted RM row from sheet." }))
          .setMimeType(ContentService.MimeType.JSON);
      }
    }

    if (data.action === 'FETCH_SHEET_DATA') {
      return handleFetchAllData();
    }

    // Upsert Customer Files (ONLY files sent by app - NEVER overwrite manual sheet edits)
    if (data.files && data.files.length > 0) {
      var fileExistingData = fileSheet.getDataRange().getValues();
      var fileIdRowMap = {};
      for (var r = 1; r < fileExistingData.length; r++) {
        var key = String(fileExistingData[r][1] || fileExistingData[r][0] || '').trim();
        if (key) fileIdRowMap[key] = r + 1;
      }

      data.files.forEach(function(f) {
        var matchKey = String(f.fileId || f.ccNumber || '').trim();
        if (!matchKey) return;

        var row = [
          f.ccNumber || '',
          f.fileId || '',
          f.customerName || '',
          f.companyName || '',
          f.officeAddress || '',
          f.mobile || '',
          f.email || '',
          f.productType || '',
          f.applicationStatus || 'Submitted',
          f.activeStatus || 'N',
          f.assignedRmCode || '',
          f.pendingDocuments || '',
          f.cpvRemarks || '',
          f.cpvStatus || 'Pending',
          f.submissionAddress || '',
          f.submissionLat || 0.0,
          f.submissionLng || 0.0,
          f.updatedAt || '',
          f.updatedBy || 'App'
        ];

        if (fileIdRowMap[matchKey]) {
          var rowIndex = fileIdRowMap[matchKey];
          var existingRow = fileExistingData[rowIndex - 1];
          var existingUpdatedBy = String(existingRow[18] || '');
          var existingUpdatedAt = String(existingRow[17] || '');
          var appUpdatedAt = String(f.updatedAt || '');

          // If row was edited manually in Google Sheet, NEVER overwrite with older/equal app data!
          if (existingUpdatedBy === 'Manual_Sheet_Edit' && (!appUpdatedAt || existingUpdatedAt >= appUpdatedAt)) {
            // Keep manual sheet edits safe!
          } else {
            fileSheet.getRange(rowIndex, 1, 1, row.length).setValues([row]);
          }
        } else {
          fileSheet.appendRow(row);
          fileIdRowMap[matchKey] = fileSheet.getLastRow();
        }
      });
    }

    // Upsert RM Details (Sync RMs with passwords and targets across all mobile apps)
    if (data.rms && data.rms.length > 0) {
      var rmExistingData = rmSheet.getDataRange().getValues();
      var rmIdRowMap = {};
      for (var r2 = 1; r2 < rmExistingData.length; r2++) {
        var rmKey = String(rmExistingData[r2][0] || '').trim().toUpperCase();
        if (rmKey) rmIdRowMap[rmKey] = r2 + 1;
      }

      data.rms.forEach(function(rm) {
        var cleanCode = String(rm.rmCode || '').trim().toUpperCase();
        if (!cleanCode) return;
        var row = [
          cleanCode,
          rm.name || '',
          rm.mobile || '',
          rm.email || '',
          rm.officeAddress || '',
          rm.role || 'RM',
          rm.accountStatus || 'ACTIVE',
          rm.creditCardTarget || 15,
          rm.corporateCardTarget || 5,
          rm.b2bTarget || 2,
          rm.passwordHash || '',
          rm.salt || '',
          rm.createdAt || '',
          rm.updatedAt || ''
        ];
        if (rmIdRowMap[cleanCode]) {
          var rmRowIndex = rmIdRowMap[cleanCode];
          var existingRmRow = rmExistingData[rmRowIndex - 1];
          if (!row[10] && existingRmRow[10]) row[10] = existingRmRow[10];
          if (!row[11] && existingRmRow[11]) row[11] = existingRmRow[11];
          rmSheet.getRange(rmRowIndex, 1, 1, row.length).setValues([row]);
        } else {
          rmSheet.appendRow(row);
          rmIdRowMap[cleanCode] = rmSheet.getLastRow();
        }
      });
    }

    // Upsert Universal Settings
    if (data.settings && data.settings.length > 0) {
      var setExistingData = settingsSheet.getDataRange().getValues();
      var setMap = {};
      for (var s = 1; s < setExistingData.length; s++) {
        var sKey = String(setExistingData[s][0] || '').trim();
        if (sKey) setMap[sKey] = s + 1;
      }
      data.settings.forEach(function(st) {
        var key = String(st.settingKey || '').trim();
        if (!key) return;
        var row = [key, st.settingValue || '', st.description || '', st.updatedBy || 'App', st.updatedAt || ''];
        if (setMap[key]) {
          var setRowIndex = setMap[key];
          var existingSetRow = setExistingData[setRowIndex - 1];
          if (String(existingSetRow[3] || '') === 'Manual_Sheet_Edit') {
            // Keep manual sheet edit safe
          } else {
            settingsSheet.getRange(setRowIndex, 1, 1, row.length).setValues([row]);
          }
        } else {
          settingsSheet.appendRow(row);
          setMap[key] = settingsSheet.getLastRow();
        }
      });
    }

    // Record Audit Trails
    if (data.auditLogs && data.auditLogs.length > 0) {
      data.auditLogs.forEach(function(al) {
        auditSheet.appendRow([
          al.logId || '',
          al.userId || '',
          al.role || '',
          al.action || '',
          al.targetId || '',
          al.details || '',
          al.timestamp || ''
        ]);
      });
    }

    // Record File Attachments Metadata
    if (data.attachments && data.attachments.length > 0) {
      var attData = attachSheet.getDataRange().getValues();
      var attMap = {};
      for (var a = 1; a < attData.length; a++) {
        var aId = String(attData[a][0] || '').trim();
        if (aId) attMap[aId] = true;
      }
      data.attachments.forEach(function(at) {
        var atId = String(at.attachmentId || '').trim();
        if (!attMap[atId]) {
          attachSheet.appendRow([
            atId,
            at.fileId || '',
            at.fileName || '',
            at.category || '',
            at.fileSize || 0,
            at.uploadedBy || '',
            at.uploadedAt || ''
          ]);
          attMap[atId] = true;
        }
      });
    }

    // Record SMS Notifications
    if (data.sms && data.sms.length > 0) {
      var smsData = smsSheet.getDataRange().getValues();
      var smsMap = {};
      for (var sm = 1; sm < smsData.length; sm++) {
        var smsId = String(smsData[sm][0] || '').trim();
        if (smsId) smsMap[smsId] = true;
      }
      data.sms.forEach(function(s) {
        var sId = String(s.id || s.sentTimestamp || '').trim();
        if (!smsMap[sId]) {
          smsSheet.appendRow([
            sId,
            s.recipientRmCode || '',
            s.recipientName || '',
            s.recipientMobile || '',
            s.triggeredByRole || '',
            s.triggeredByCode || '',
            s.actionType || '',
            s.fileId || s.targetType || '',
            s.messageText || '',
            s.status || 'DELIVERED',
            s.sentTimestamp || ''
          ]);
          smsMap[sId] = true;
        }
      });
    }

    // Record Location Logs
    if (data.locations && data.locations.length > 0) {
      data.locations.forEach(function(l) {
        locSheet.appendRow([
          l.rmCode || '',
          l.userName || '',
          l.latitude || 0,
          l.longitude || 0,
          l.address || '',
          l.timestamp || '',
          l.sourceAction || ''
        ]);
      });
    }

    return ContentService.createTextOutput(JSON.stringify({
      status: 'success',
      files: extractAllSheetFiles(fileSheet),
      rms: extractAllSheetRms(rmSheet),
      settings: extractAllSheetSettings(settingsSheet)
    })).setMimeType(ContentService.MimeType.JSON);

  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({
      status: 'error',
      message: err.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

function handleFetchAllData() {
  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var fileSheet = ss.getSheetByName('Customer_Files') || ss.getSheetByName('Files');
    var rmSheet = ss.getSheetByName('RM_Details') || ss.getSheetByName('RM_Directory');
    var settingsSheet = ss.getSheetByName('Universal_Settings') || ss.getSheetByName('Settings');

    return ContentService.createTextOutput(JSON.stringify({
      status: 'success',
      files: fileSheet ? extractAllSheetFiles(fileSheet) : [],
      rms: rmSheet ? extractAllSheetRms(rmSheet) : [],
      settings: settingsSheet ? extractAllSheetSettings(settingsSheet) : []
    })).setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({
      status: 'error',
      message: err.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

function getOrCreateSheet(ss, sheetName, headers, headerColor) {
  var sheet = ss.getSheetByName(sheetName);
  if (!sheet) {
    sheet = ss.insertSheet(sheetName);
    sheet.appendRow(headers);
    var hr = sheet.getRange(1, 1, 1, headers.length);
    hr.setBackground(headerColor || '#0A192F')
      .setFontColor('#FFFFFF')
      .setFontWeight('bold');
    sheet.setFrozenRows(1);
  }
  return sheet;
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
      applicationStatus: String(row[8] || 'Submitted'),
      activeStatus: String(row[9] || 'N'),
      assignedRmCode: String(row[10] || ''),
      pendingDocuments: String(row[11] || ''),
      cpvRemarks: String(row[12] || ''),
      cpvStatus: String(row[13] || 'Pending'),
      submissionAddress: String(row[14] || ''),
      updatedAt: String(row[17] || ''),
      updatedBy: String(row[18] || 'Sheet')
    });
  }
  return list;
}

function extractAllSheetRms(sheet) {
  var data = sheet.getDataRange().getValues();
  var list = [];
  for (var r = 1; r < data.length; r++) {
    var row = data[r];
    if (!row[0]) continue;
    list.push({
      rmCode: String(row[0] || '').trim().toUpperCase(),
      name: String(row[1] || ''),
      mobile: String(row[2] || ''),
      email: String(row[3] || ''),
      officeAddress: String(row[4] || ''),
      role: String(row[5] || 'RM'),
      accountStatus: String(row[6] || 'ACTIVE'),
      creditCardTarget: parseInt(row[7]) || 15,
      corporateCardTarget: parseInt(row[8]) || 5,
      b2bTarget: parseInt(row[9]) || 2,
      passwordHash: String(row[10] || ''),
      salt: String(row[11] || ''),
      createdAt: String(row[12] || ''),
      updatedAt: String(row[13] || '')
    });
  }
  return list;
}

function extractAllSheetSettings(sheet) {
  var data = sheet.getDataRange().getValues();
  var list = [];
  for (var r = 1; r < data.length; r++) {
    var row = data[r];
    if (!row[0]) continue;
    list.push({
      settingKey: String(row[0] || '').trim(),
      settingValue: String(row[1] || ''),
      description: String(row[2] || '')
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
            Text("৫. আপনার Web App URL-টি স্বয়ংক্রিয়ভাবে ডিফল্ট কনফিগার করা আছে।", fontSize = 11.sp, color = Color.DarkGray)
            Text("৬. শিটে ম্যানুয়ালি এডিট করা যাবে (onEdit স্বয়ংক্রিয়ভাবে সেভ রাখবে, কোনো ডাটা মুছে যাবে না)।", fontSize = 11.sp, color = Color(0xFF0369A1))
            Text("৭. স্বয়ংক্রিয়ভাবে তৈরি হবে ৭টি ট্যাব: Customer_Files, RM_Details, Universal_Settings, Audit_Trail, Attachments, SMS_Notifications, RM_Location_Logs।", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF059669))
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
