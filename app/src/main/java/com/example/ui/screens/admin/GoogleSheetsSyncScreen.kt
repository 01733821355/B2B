package com.example.ui.screens.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
 * EBL Sales & RM Suite - Multi-Device Bi-Directional Auto-Sync Web App
 * Google Drive Owner Account: mdbappyhossain017@gmail.com
 * Cloud Storage: Permanent Google Drive storage in folder 'EBL RM Suite Drive Files'
 *
 * TABS: Customer_Files, RM_Details, Universal_Settings, Audit_Trail,
 *       Attachments, SMS_Notifications, RM_Location_Logs,
 *       Important_Documents, Chat_And_Calls, Team_Events, Event_Submissions
 */

function authorizeDrivePermissions() {
  // রান বাটন (Run) ক্লিক করে Google Drive পারমিশন Allow করুন
  var folderName = "EBL RM Suite Drive Files";
  var folders = DriveApp.getFoldersByName(folderName);
  var targetFolder = folders.hasNext() ? folders.next() : DriveApp.createFolder(folderName);
  targetFolder.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
  Logger.log("Google Drive Permissions Authorized Successfully! Folder: " + targetFolder.getName());
  return "Google Drive Authorized Successfully!";
}

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

function saveFileToDrive(base64Data, fileName, mimeType) {
  try {
    var folderName = "EBL RM Suite Drive Files";
    var folders = DriveApp.getFoldersByName(folderName);
    var targetFolder = folders.hasNext() ? folders.next() : DriveApp.createFolder(folderName);
    try { targetFolder.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW); } catch (fe) {}
    var cleanBase64 = String(base64Data || '');
    if (cleanBase64.indexOf(',') > -1) {
      cleanBase64 = cleanBase64.split(',')[1];
    }
    var decoded = Utilities.base64Decode(cleanBase64);
    var mType = mimeType || 'image/jpeg';
    var fName = fileName || ('file_' + new Date().getTime() + '.jpg');
    var blob = Utilities.newBlob(decoded, mType, fName);
    var file = targetFolder.createFile(blob);
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    var fileId = file.getId();
    var directUrl = "https://lh3.googleusercontent.com/d/" + fileId;
    var downloadUrl = "https://drive.google.com/thumbnail?id=" + fileId + "&sz=w1200";
    var viewUrl = file.getUrl();
    return { success: true, fileId: fileId, url: directUrl, directUrl: directUrl, downloadUrl: downloadUrl, driveUrl: viewUrl };
  } catch (err) {
    return { success: false, error: err.toString() };
  }
}

function doPost(e) {
  try {
    var contents = e.postData ? e.postData.contents : '{}';
    var data = JSON.parse(contents);
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    if (!ss && data.spreadsheetId) {
      try { ss = SpreadsheetApp.openById(data.spreadsheetId); } catch (e) {}
    }
    if (!ss) {
      try { ss = SpreadsheetApp.openById("1lb9Wou10ecl28EUgaXD2cA3YCNY7nNHp1BOFrrLezqI"); } catch (e) {}
    }

    // 1. Auto-create & format 'Customer_Files' Tab
    var fileSheet = getOrCreateSheet(ss, 'Customer_Files', [
      'SL', 'CC-Number', 'File ID', 'Customer Name', 'Company Name',
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
      'Attachment ID', 'File ID', 'File Name', 'Category', 'File Size (Bytes)', 'Uploaded By', 'Timestamp', 'Drive Link'
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

    // 8. Auto-create & format 'Important_Documents' Tab
    var docSheet = getOrCreateSheet(ss, 'Important_Documents', [
      'Doc ID', 'Title', 'Category', 'Description', 'File Name', 'File Type', 'File Size (Bytes)',
      'File URI', 'Storage Path', 'Uploaded By', 'Uploader Name', 'Uploader Role', 'Created At', 'Last Updated'
    ], '#047857');

    // 9. Auto-create & format 'Chat_And_Calls' Tab
    var chatSheet = getOrCreateSheet(ss, 'Chat_And_Calls', [
      'Message ID', 'Sender RM', 'Sender Name', 'Sender Role', 'Recipient RM', 'Message Text', 'Message Type', 'Event ID', 'Timestamp'
    ], '#1D4ED8');

    // 10. Auto-create & format 'Team_Events' Tab
    var eventSheet = getOrCreateSheet(ss, 'Team_Events', [
      'Event ID', 'Title', 'Description', 'Creator RM', 'Creator Name', 'Target Date', 'Allowed Fields', 'Status', 'Created At', 'Close Time'
    ], '#7E22CE');

    // 11. Auto-create & format 'Event_Submissions' Tab
    var respSheet = getOrCreateSheet(ss, 'Event_Submissions', [
      'Response ID', 'Event ID', 'RM Code', 'RM Name', 'Files Count', 'Requested Date', 'Location', 'Remarks', 'Customer Entries JSON', 'Submitted At'
    ], '#B45309');

    // Universal Demo Customer Information Purge (Ensure NO demo files exist on spreadsheet)
    var curCustFiles = fileSheet.getDataRange().getValues();
    var demoCustIds = ['cust-001', 'cust-002', 'cust-003', 'cust-004', 'cust-005'];
    for (var dcf = curCustFiles.length - 1; dcf >= 1; dcf--) {
      var dFid = String(curCustFiles[dcf][2] || '').trim().toLowerCase();
      var dCc = String(curCustFiles[dcf][1] || '').trim().toLowerCase();
      var dName = String(curCustFiles[dcf][3] || '').trim().toLowerCase();
      if (demoCustIds.indexOf(dFid) > -1 || demoCustIds.indexOf(dCc) > -1 || dName.indexOf('sample customer') > -1 || dName.indexOf('demo customer') > -1) {
        fileSheet.deleteRow(dcf + 1);
      }
    }

    // Direct Google Drive File Upload (Allows RM, Admin, Mentor photos to be saved to Google Drive)
    if (data.action === 'UPLOAD_FILE_TO_DRIVE' && data.fileBase64) {
      var driveRes = saveFileToDrive(data.fileBase64, data.fileName, data.fileType);
      if (driveRes && driveRes.success) {
        return ContentService.createTextOutput(JSON.stringify({
          status: "SUCCESS",
          fileUrl: driveRes.url,
          driveUrl: driveRes.driveUrl,
          fileId: driveRes.fileId
        })).setMimeType(ContentService.MimeType.JSON);
      } else {
        return ContentService.createTextOutput(JSON.stringify({
          status: "ERROR",
          message: driveRes ? driveRes.error : "Failed to upload to Google Drive"
        })).setMimeType(ContentService.MimeType.JSON);
      }
    }

    // Handle File Deletions (Immediately removes rows from Google Sheets across Customer_Files and other tabs)
    if (data.action === 'DELETE_FILE' || (data.deletedFileIds && data.deletedFileIds.length > 0)) {
      var toDelete = [];
      if (data.deletedFileIds && Array.isArray(data.deletedFileIds)) {
        for (var di = 0; di < data.deletedFileIds.length; di++) {
          var idStr = String(data.deletedFileIds[di] || '').trim();
          if (idStr) {
            toDelete.push(idStr.toLowerCase());
            toDelete.push(idStr.replace(/[-\s]/g, '').toLowerCase());
          }
        }
      }
      if (data.fileId) {
        var fIdStr = String(data.fileId).trim();
        toDelete.push(fIdStr.toLowerCase());
        toDelete.push(fIdStr.replace(/[-\s]/g, '').toLowerCase());
      }
      if (data.ccNumber) {
        var ccStr = String(data.ccNumber).trim();
        toDelete.push(ccStr.toLowerCase());
        toDelete.push(ccStr.replace(/[-\s]/g, '').toLowerCase());
      }

      var sheetsToCheck = [fileSheet];
      try {
        var allSheets = ss.getSheets();
        for (var si = 0; si < allSheets.length; si++) {
          var sName = allSheets[si].getName();
          if (sName !== 'RM_Details' && sName !== 'Universal_Settings' && sName !== 'Audit_Trail' &&
              sName !== 'Attachments' && sName !== 'SMS_Notifications' && sName !== 'RM_Location_Logs') {
            if (sheetsToCheck.indexOf(allSheets[si]) === -1) {
              sheetsToCheck.push(allSheets[si]);
            }
          }
        }
      } catch (e) {}

      for (var sIdx = 0; sIdx < sheetsToCheck.length; sIdx++) {
        var curTargetSheet = sheetsToCheck[sIdx];
        var curFiles = curTargetSheet.getDataRange().getValues();
        for (var d = curFiles.length - 1; d >= 1; d--) {
          var rowMatch = false;
          for (var col = 0; col < curFiles[d].length; col++) {
            var cellVal = String(curFiles[d][col] || '').trim().toLowerCase();
            var cleanCell = cellVal.replace(/[-\s]/g, '');
            if (!cellVal) continue;
            for (var k = 0; k < toDelete.length; k++) {
              if (cellVal === toDelete[k] || cleanCell === toDelete[k]) {
                rowMatch = true;
                break;
              }
            }
            if (rowMatch) break;
          }
          if (rowMatch) {
            curTargetSheet.deleteRow(d + 1);
          }
        }
      }
      // Persist deleted file IDs in Universal_Settings so any new mobile app install immediately knows they were deleted
      try {
        var setSheet = ss.getSheetByName('Universal_Settings') || ss.getSheetByName('Settings');
        if (setSheet) {
          var sVals = setSheet.getDataRange().getValues();
          var delRowIdx = -1;
          var curDelVal = '';
          for (var sr = 1; sr < sVals.length; sr++) {
            if (String(sVals[sr][0] || '').trim().toLowerCase() === 'deleted_file_ids') {
              delRowIdx = sr + 1;
              curDelVal = String(sVals[sr][1] || '').trim();
              break;
            }
          }
          var allDels = curDelVal ? curDelVal.split(',') : [];
          toDelete.forEach(function(td) { if (td && allDels.indexOf(td) === -1) allDels.push(td); });
          var newDelStr = allDels.join(',');
          if (delRowIdx > -1) {
            setSheet.getRange(delRowIdx, 2).setValue(newDelStr);
            setSheet.getRange(delRowIdx, 5).setValue(new Date().toISOString());
          } else {
            setSheet.appendRow(['deleted_file_ids', newDelStr, 'Universal Deleted File IDs', 'App_Sync', new Date().toISOString()]);
          }
        }
      } catch (errDel) {}

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

    // Handle Team Event Deletions (Immediately removes Event rows from Google Sheets)
    if (data.action === 'DELETE_EVENT' || (data.deletedEventIds && data.deletedEventIds.length > 0)) {
      var toDeleteEvts = data.deletedEventIds || [data.eventId].filter(Boolean);
      var curEvts = eventSheet.getDataRange().getValues();
      for (var de = curEvts.length - 1; de >= 1; de--) {
        var evIdVal = String(curEvts[de][0] || '').trim();
        for (var m = 0; m < toDeleteEvts.length; m++) {
          var targetEv = String(toDeleteEvts[m] || '').trim();
          if (targetEv && evIdVal.toLowerCase() === targetEv.toLowerCase()) {
            eventSheet.deleteRow(de + 1);
            break;
          }
        }
      }
      // Also purge from event responses sheet
      var curResps = respSheet.getDataRange().getValues();
      for (var dr = curResps.length - 1; dr >= 1; dr--) {
        var rEvId = String(curResps[dr][1] || '').trim();
        for (var rm = 0; rm < toDeleteEvts.length; rm++) {
          var targetEvR = String(toDeleteEvts[rm] || '').trim();
          if (targetEvR && rEvId.toLowerCase() === targetEvR.toLowerCase()) {
            respSheet.deleteRow(dr + 1);
            break;
          }
        }
      }
      // Also purge announcement messages from chat sheet (Column 7 is Event ID, column 5 is Text, column 0 is ID)
      var curChats = chatSheet.getDataRange().getValues();
      for (var dc = curChats.length - 1; dc >= 1; dc--) {
        var cMid = String(curChats[dc][0] || '').trim();
        var cTxt = String(curChats[dc][5] || '').trim();
        var cEvId = String(curChats[dc][7] || '').trim();
        for (var cm = 0; cm < toDeleteEvts.length; cm++) {
          var evTarget = String(toDeleteEvts[cm] || '').trim().toLowerCase();
          if (evTarget && (cEvId.toLowerCase() === evTarget || cTxt.toLowerCase().indexOf(evTarget) >= 0 || cMid.toLowerCase().indexOf(evTarget) >= 0 || evTarget === ('event_' + cEvId).toLowerCase())) {
            chatSheet.deleteRow(dc + 1);
            break;
          }
        }
      }
      // Persist deleted event IDs in Universal_Settings so any mobile app immediately knows they were deleted
      try {
        var setSheetEvt = ss.getSheetByName('Universal_Settings') || ss.getSheetByName('Settings');
        if (setSheetEvt) {
          var sValsEvt = setSheetEvt.getDataRange().getValues();
          var delEvtRowIdx = -1;
          var curDelEvtVal = '';
          for (var ser = 1; ser < sValsEvt.length; ser++) {
            if (String(sValsEvt[ser][0] || '').trim().toLowerCase() === 'deleted_event_ids') {
              delEvtRowIdx = ser + 1;
              curDelEvtVal = String(sValsEvt[ser][1] || '').trim();
              break;
            }
          }
          var allDelEvts = curDelEvtVal ? curDelEvtVal.split(',') : [];
          toDeleteEvts.forEach(function(tde) {
            var clTde = String(tde || '').trim();
            if (clTde && allDelEvts.indexOf(clTde) === -1) allDelEvts.push(clTde);
          });
          var newDelEvtStr = allDelEvts.join(',');
          if (delEvtRowIdx > -1) {
            setSheetEvt.getRange(delEvtRowIdx, 2).setValue(newDelEvtStr);
            setSheetEvt.getRange(delEvtRowIdx, 5).setValue(new Date().toISOString());
          } else {
            setSheetEvt.appendRow(['deleted_event_ids', newDelEvtStr, 'Universal Deleted Event IDs', 'App_Sync', new Date().toISOString()]);
          }
        }
      } catch (errEvtDel) {}

      if (data.action === 'DELETE_EVENT') {
        return ContentService.createTextOutput(JSON.stringify({ status: "SUCCESS", message: "Deleted Event from sheet." }))
          .setMimeType(ContentService.MimeType.JSON);
      }
    }

    // Handle Chat Message Deletions (Immediately removes Chat rows from Google Sheets)
    if (data.action === 'DELETE_CHAT' || (data.deletedChatIds && data.deletedChatIds.length > 0)) {
      var toDeleteChats = data.deletedChatIds || [data.chatId].filter(Boolean);
      var curChatsD = chatSheet.getDataRange().getValues();
      for (var dcd = curChatsD.length - 1; dcd >= 1; dcd--) {
        var chatIdVal = String(curChatsD[dcd][0] || '').trim();
        var chatEvId = String(curChatsD[dcd][7] || '').trim();
        for (var mc = 0; mc < toDeleteChats.length; mc++) {
          var targetChat = String(toDeleteChats[mc] || '').trim();
          if (targetChat && (chatIdVal.toLowerCase() === targetChat.toLowerCase() || (chatEvId && chatEvId.toLowerCase() === targetChat.toLowerCase()) || targetChat.toLowerCase() === ('event_' + chatEvId).toLowerCase())) {
            chatSheet.deleteRow(dcd + 1);
            break;
          }
        }
      }
      if (data.action === 'DELETE_CHAT') {
        return ContentService.createTextOutput(JSON.stringify({ status: "SUCCESS", message: "Deleted Chat message from sheet." }))
          .setMimeType(ContentService.MimeType.JSON);
      }
    }

    // Handle Important Document Deletions (Immediately removes Document rows from Google Sheets & Drive)
    if (data.action === 'DELETE_DOC' || (data.deletedDocIds && data.deletedDocIds.length > 0)) {
      var toDeleteDocs = data.deletedDocIds || [data.docId].filter(Boolean);
      var curDocsD = docSheet.getDataRange().getValues();
      for (var ddd = curDocsD.length - 1; ddd >= 1; ddd--) {
        var docIdVal = String(curDocsD[ddd][0] || '').trim();
        for (var md = 0; md < toDeleteDocs.length; md++) {
          var targetDoc = String(toDeleteDocs[md] || '').trim();
          if (targetDoc && docIdVal.toLowerCase() === targetDoc.toLowerCase()) {
            try {
              var docUri = String(curDocsD[ddd][7] || '');
              var dMatch = docUri.match(/\/d\/([a-zA-Z0-9_-]+)/) || docUri.match(/id=([a-zA-Z0-9_-]+)/);
              if (dMatch && dMatch[1]) {
                DriveApp.getFileById(dMatch[1]).setTrashed(true);
              }
            } catch (te) {}
            docSheet.deleteRow(ddd + 1);
            break;
          }
        }
      }
      try {
        var setSheetDoc = ss.getSheetByName('Universal_Settings') || ss.getSheetByName('Settings');
        if (setSheetDoc) {
          var sValsDoc = setSheetDoc.getDataRange().getValues();
          var delDocRowIdx = -1;
          var curDelDocVal = '';
          for (var sdr = 1; sdr < sValsDoc.length; sdr++) {
            if (String(sValsDoc[sdr][0] || '').trim().toLowerCase() === 'deleted_doc_ids') {
              delDocRowIdx = sdr + 1;
              curDelDocVal = String(sValsDoc[sdr][1] || '').trim();
              break;
            }
          }
          var allDelDocs = curDelDocVal ? curDelDocVal.split(',') : [];
          toDeleteDocs.forEach(function(tdd) {
            var clTdd = String(tdd || '').trim();
            if (clTdd && allDelDocs.indexOf(clTdd) === -1) allDelDocs.push(clTdd);
          });
          var newDelDocStr = allDelDocs.join(',');
          if (delDocRowIdx > -1) {
            setSheetDoc.getRange(delDocRowIdx, 2).setValue(newDelDocStr);
            setSheetDoc.getRange(delDocRowIdx, 5).setValue(new Date().toISOString());
          } else {
            setSheetDoc.appendRow(['deleted_doc_ids', newDelDocStr, 'Universal Deleted Important Document IDs', 'App_Sync', new Date().toISOString()]);
          }
        }
      } catch (errDocDel) {}

      if (data.action === 'DELETE_DOC') {
        return ContentService.createTextOutput(JSON.stringify({ status: "SUCCESS", message: "Deleted Document from sheet and Drive." }))
          .setMimeType(ContentService.MimeType.JSON);
      }
    }

    // Handle File Attachment Deletions (Immediately removes Attachment rows from Google Sheets & Drive)
    if (data.action === 'DELETE_ATTACHMENT' || (data.deletedAttachmentIds && data.deletedAttachmentIds.length > 0)) {
      var toDeleteAtts = data.deletedAttachmentIds || [data.attachmentId].filter(Boolean);
      var curAttsD = attachSheet.getDataRange().getValues();
      for (var da = curAttsD.length - 1; da >= 1; da--) {
        var attIdVal = String(curAttsD[da][0] || '').trim();
        for (var ma = 0; ma < toDeleteAtts.length; ma++) {
          var targetAtt = String(toDeleteAtts[ma] || '').trim();
          if (targetAtt && attIdVal.toLowerCase() === targetAtt.toLowerCase()) {
            try {
              var aUri = String(curAttsD[da][7] || '');
              var aMatch = aUri.match(/\/d\/([a-zA-Z0-9_-]+)/) || aUri.match(/id=([a-zA-Z0-9_-]+)/);
              if (aMatch && aMatch[1]) {
                DriveApp.getFileById(aMatch[1]).setTrashed(true);
              }
            } catch (ate) {}
            attachSheet.deleteRow(da + 1);
            break;
          }
        }
      }
      try {
        var setSheetAtt = ss.getSheetByName('Universal_Settings') || ss.getSheetByName('Settings');
        if (setSheetAtt) {
          var sValsAtt = setSheetAtt.getDataRange().getValues();
          var delAttRowIdx = -1;
          var curDelAttVal = '';
          for (var sar = 1; sar < sValsAtt.length; sar++) {
            if (String(sValsAtt[sar][0] || '').trim().toLowerCase() === 'deleted_attachment_ids') {
              delAttRowIdx = sar + 1;
              curDelAttVal = String(sValsAtt[sar][1] || '').trim();
              break;
            }
          }
          var allDelAtts = curDelAttVal ? curDelAttVal.split(',') : [];
          toDeleteAtts.forEach(function(tda) {
            var clTda = String(tda || '').trim();
            if (clTda && allDelAtts.indexOf(clTda) === -1) allDelAtts.push(clTda);
          });
          var newDelAttStr = allDelAtts.join(',');
          if (delAttRowIdx > -1) {
            setSheetAtt.getRange(delAttRowIdx, 2).setValue(newDelAttStr);
            setSheetAtt.getRange(delAttRowIdx, 5).setValue(new Date().toISOString());
          } else {
            setSheetAtt.appendRow(['deleted_attachment_ids', newDelAttStr, 'Universal Deleted Attachment IDs', 'App_Sync', new Date().toISOString()]);
          }
        }
      } catch (errAttDel) {}

      if (data.action === 'DELETE_ATTACHMENT') {
        return ContentService.createTextOutput(JSON.stringify({ status: "SUCCESS", message: "Deleted Attachment from sheet and Drive." }))
          .setMimeType(ContentService.MimeType.JSON);
      }
    }

    // Handle Direct Important Document Upsert (Immediately syncs new/updated document to Google Sheets & Drive)
    if (data.action === 'UPSERT_DOC' && data.document) {
      var docU = data.document;
      var didU = String(docU.docId || '').trim();
      if (didU) {
        if (docU.fileBase64) {
          var dUpload = saveFileToDrive(docU.fileBase64, docU.fileName, docU.fileType);
          if (dUpload && dUpload.success) {
            docU.fileUri = dUpload.url;
            docU.storagePath = dUpload.driveUrl;
          }
        }
        var curDocsU = docSheet.getDataRange().getValues();
        var foundDocRow = 0;
        for (var du = 1; du < curDocsU.length; du++) {
          if (String(curDocsU[du][0] || '').trim().toLowerCase() === didU.toLowerCase()) {
            foundDocRow = du + 1;
            break;
          }
        }
        var docRowU = [
          didU,
          docU.title || '',
          docU.category || 'Policies & Circulars',
          docU.description || '',
          docU.fileName || '',
          docU.fileType || 'application/pdf',
          docU.fileSizeBytes || 1024,
          docU.fileUri || '',
          docU.storagePath || '',
          docU.uploadedBy || '',
          docU.uploaderName || '',
          docU.uploaderRole || '',
          docU.createdAt || '',
          docU.updatedAt || ''
        ];
        if (foundDocRow > 0) {
          docSheet.getRange(foundDocRow, 1, 1, docRowU.length).setValues([docRowU]);
        } else {
          docSheet.appendRow(docRowU);
        }
        return ContentService.createTextOutput(JSON.stringify({
          status: "SUCCESS",
          message: "Upserted Document to sheet and Drive.",
          fileUri: docU.fileUri,
          storagePath: docU.storagePath
        })).setMimeType(ContentService.MimeType.JSON);
      }
    }

    // Handle Direct Attachment / Photo Upsert (Immediately syncs customer/status/CPV photos to Google Sheets & Drive)
    if (data.action === 'UPSERT_ATTACHMENT' && (data.attachment || (data.attachments && data.attachments.length > 0))) {
      var atU = data.attachment || data.attachments[0];
      var atIdU = String(atU.attachmentId || '').trim();
      if (atIdU) {
        var attDriveLink = atU.fileUri || atU.driveLink || '';
        if (atU.fileBase64 && (!attDriveLink || attDriveLink.indexOf('http') !== 0)) {
          var atRes = saveFileToDrive(atU.fileBase64, atU.fileName, atU.fileType);
          if (atRes && atRes.success) {
            attDriveLink = atRes.url;
          }
        }
        var curAttsU = attachSheet.getDataRange().getValues();
        var foundAttRow = 0;
        for (var au = 1; au < curAttsU.length; au++) {
          if (String(curAttsU[au][0] || '').trim().toLowerCase() === atIdU.toLowerCase()) {
            foundAttRow = au + 1;
            break;
          }
        }
        var attRowU = [
          atIdU,
          atU.fileId || '',
          atU.fileName || '',
          atU.category || 'Photo',
          atU.fileSize || atU.fileSizeBytes || 0,
          atU.uploadedBy || '',
          atU.uploadedAt || Utilities.formatDate(new Date(), "GMT+6", "yyyy-MM-dd HH:mm:ss"),
          attDriveLink
        ];
        if (foundAttRow > 0) {
          attachSheet.getRange(foundAttRow, 1, 1, attRowU.length).setValues([attRowU]);
        } else {
          attachSheet.appendRow(attRowU);
        }
        return ContentService.createTextOutput(JSON.stringify({
          status: "SUCCESS",
          message: "Upserted Attachment to sheet and Drive.",
          attachmentId: atIdU,
          fileUrl: attDriveLink,
          driveUrl: attDriveLink
        })).setMimeType(ContentService.MimeType.JSON);
      }
    }

    // Handle Direct Event Upsert (Immediately syncs team/delivery event to Team_Events tab)
    if (data.action === 'UPSERT_EVENT' && (data.event || (data.events && data.events.length > 0))) {
      var evU = data.event || data.events[0];
      var evIdU = String(evU.eventId || '').trim();
      if (evIdU) {
        var curEvsU = eventSheet.getDataRange().getValues();
        var foundEvRow = 0;
        for (var eu = 1; eu < curEvsU.length; eu++) {
          if (String(curEvsU[eu][0] || '').trim().toLowerCase() === evIdU.toLowerCase()) {
            foundEvRow = eu + 1;
            break;
          }
        }
        var evRowU = [
          evIdU,
          evU.title || '',
          evU.description || '',
          evU.creatorRmCode || '',
          evU.creatorName || '',
          evU.targetDate || '',
          evU.allowedFields || 'CUSTOMERS,COUNT,DATE,LOCATION,REMARKS',
          evU.status || 'ACTIVE',
          evU.createdAt || Utilities.formatDate(new Date(), "GMT+6", "yyyy-MM-dd HH:mm:ss"),
          evU.closeTime || ''
        ];
        if (foundEvRow > 0) {
          eventSheet.getRange(foundEvRow, 1, 1, evRowU.length).setValues([evRowU]);
        } else {
          eventSheet.appendRow(evRowU);
        }
        return ContentService.createTextOutput(JSON.stringify({
          status: "SUCCESS",
          message: "Upserted Event to sheet.",
          eventId: evIdU
        })).setMimeType(ContentService.MimeType.JSON);
      }
    }

    // Handle Direct Event Response Upsert (Immediately saves RM submissions for events)
    if (data.action === 'UPSERT_EVENT_RESPONSE' && (data.eventResponse || data.response || (data.eventResponses && data.eventResponses.length > 0))) {
      var respU = data.eventResponse || data.response || data.eventResponses[0];
      var respIdU = String(respU.responseId || '').trim();
      if (respIdU) {
        var curRespsU = respSheet.getDataRange().getValues();
        var foundRespRow = 0;
        for (var ru = 1; ru < curRespsU.length; ru++) {
          if (String(curRespsU[ru][0] || '').trim().toLowerCase() === respIdU.toLowerCase()) {
            foundRespRow = ru + 1;
            break;
          }
        }
        var respRowU = [
          respIdU,
          respU.eventId || '',
          respU.rmCode || '',
          respU.rmName || '',
          respU.filesCount || 1,
          respU.requestedDate || '',
          respU.location || '',
          respU.remarks || '',
          respU.customerEntriesJson || '',
          respU.submittedAt || Utilities.formatDate(new Date(), "GMT+6", "yyyy-MM-dd HH:mm:ss")
        ];
        if (foundRespRow > 0) {
          respSheet.getRange(foundRespRow, 1, 1, respRowU.length).setValues([respRowU]);
        } else {
          respSheet.appendRow(respRowU);
        }
        return ContentService.createTextOutput(JSON.stringify({
          status: "SUCCESS",
          message: "Upserted Event Response to sheet.",
          responseId: respIdU
        })).setMimeType(ContentService.MimeType.JSON);
      }
    }

    // Handle Direct Chat / RM-to-RM Message Upsert (Immediately appends message to Chat_And_Calls tab)
    if (data.action === 'UPSERT_CHAT' && (data.chatMessage || data.message || (data.chatMessages && data.chatMessages.length > 0))) {
      var cmU = data.chatMessage || data.message || data.chatMessages[0];
      var cmIdU = String(cmU.id || '').trim();
      if (cmIdU) {
        var curChatsU = chatSheet.getDataRange().getValues();
        var foundChatRow = 0;
        for (var cu = 1; cu < curChatsU.length; cu++) {
          if (String(curChatsU[cu][0] || '').trim().toLowerCase() === cmIdU.toLowerCase()) {
            foundChatRow = cu + 1;
            break;
          }
        }
        var chatRowU = [
          cmIdU,
          cmU.senderRmCode || '',
          cmU.senderName || '',
          cmU.senderRole || '',
          cmU.recipientRmCode || '',
          cmU.messageText || '',
          cmU.messageType || 'TEXT',
          cmU.eventId || '',
          cmU.timestamp || Utilities.formatDate(new Date(), "GMT+6", "yyyy-MM-dd HH:mm:ss")
        ];
        if (foundChatRow > 0) {
          chatSheet.getRange(foundChatRow, 1, 1, chatRowU.length).setValues([chatRowU]);
        } else {
          chatSheet.appendRow(chatRowU);
        }
        return ContentService.createTextOutput(JSON.stringify({
          status: "SUCCESS",
          message: "Upserted Chat Message to sheet.",
          id: cmIdU
        })).setMimeType(ContentService.MimeType.JSON);
      }
    }

    // Handle Direct SMS Notification Upsert (Immediately appends SMS to SMS_Notifications tab)
    if (data.action === 'UPSERT_SMS' && (data.sms || (data.smsList && data.smsList.length > 0))) {
      var sU = data.sms || data.smsList[0];
      var sIdU = String(sU.id || sU.sentTimestamp || '').trim();
      if (sIdU) {
        var curSmsU = smsSheet.getDataRange().getValues();
        var foundSmsRow = 0;
        for (var su = 1; su < curSmsU.length; su++) {
          if (String(curSmsU[su][0] || '').trim().toLowerCase() === sIdU.toLowerCase()) {
            foundSmsRow = su + 1;
            break;
          }
        }
        var smsRowU = [
          sIdU,
          sU.recipientRmCode || '',
          sU.recipientName || '',
          sU.recipientMobile || '',
          sU.triggeredByRole || '',
          sU.triggeredByCode || '',
          sU.actionType || '',
          sU.fileId || sU.targetType || '',
          sU.messageText || '',
          sU.status || 'DELIVERED',
          sU.sentTimestamp || Utilities.formatDate(new Date(), "GMT+6", "yyyy-MM-dd HH:mm:ss")
        ];
        if (foundSmsRow > 0) {
          smsSheet.getRange(foundSmsRow, 1, 1, smsRowU.length).setValues([smsRowU]);
        } else {
          smsSheet.appendRow(smsRowU);
        }
        cleanStaleSheetSms(smsSheet);
        return ContentService.createTextOutput(JSON.stringify({
          status: "SUCCESS",
          message: "Upserted SMS notification to sheet.",
          id: sIdU
        })).setMimeType(ContentService.MimeType.JSON);
      }
    }

    // Handle Direct Setting & Preset Upsert (Immediately syncs preset to Universal_Settings tab)
    if (data.action === 'UPSERT_SETTING' || data.action === 'UPSERT_PRESET') {
      var sKeyU = String(data.settingKey || data.key || '').trim();
      var sValU = String(data.settingValue != null ? data.settingValue : (data.value != null ? data.value : ''));
      if (sKeyU) {
        var curSettingsU = settingsSheet.getDataRange().getValues();
        var foundSetRow = 0;
        for (var stu = 1; stu < curSettingsU.length; stu++) {
          if (String(curSettingsU[stu][0] || '').trim().toLowerCase() === sKeyU.toLowerCase()) {
            foundSetRow = stu + 1;
            break;
          }
        }
        var nowStrU = Utilities.formatDate(new Date(), "GMT+6", "yyyy-MM-dd HH:mm:ss");
        var setRowU = [
          sKeyU,
          sValU,
          String(data.description || 'Universal Setting - Auto-synced across all mobile apps'),
          String(data.updatedBy || 'App'),
          nowStrU
        ];
        if (foundSetRow > 0) {
          settingsSheet.getRange(foundSetRow, 1, 1, setRowU.length).setValues([setRowU]);
        } else {
          settingsSheet.appendRow(setRowU);
        }
        return ContentService.createTextOutput(JSON.stringify({
          status: "SUCCESS",
          message: "Upserted setting to Universal_Settings tab.",
          key: sKeyU
        })).setMimeType(ContentService.MimeType.JSON);
      }
    }

    // Handle Direct RM Live Location Ping (Immediately saves GPS coords & address to RM_Details and Location_Logs)
    if (data.action === 'SYNC_LOCATION' || data.action === 'UPDATE_LOCATION') {
      var targetRm = String(data.rmCode || '').trim().toUpperCase();
      var latVal = parseFloat(data.latitude || data.lat || 0);
      var lngVal = parseFloat(data.longitude || data.lng || 0);
      var addrVal = String(data.address || '').trim();
      var timeVal = String(data.timestamp || new Date().toISOString());

      if (targetRm && latVal && lngVal) {
        var curRmsL = rmSheet.getDataRange().getValues();
        for (var rl = 1; rl < curRmsL.length; rl++) {
          if (String(curRmsL[rl][0] || '').trim().toUpperCase() === targetRm) {
            rmSheet.getRange(rl + 1, 15).setValue(latVal);
            rmSheet.getRange(rl + 1, 16).setValue(lngVal);
            rmSheet.getRange(rl + 1, 17).setValue(addrVal);
            rmSheet.getRange(rl + 1, 18).setValue(timeVal);
            rmSheet.getRange(rl + 1, 19).setValue(true);
            break;
          }
        }
        if (locSheet) {
          locSheet.appendRow([
            targetRm,
            String(data.userName || targetRm),
            latVal,
            lngVal,
            addrVal,
            timeVal,
            String(data.sourceAction || 'GPS_LIVE')
          ]);
        }
      }
      return ContentService.createTextOutput(JSON.stringify({ status: "SUCCESS", message: "Location updated in sheet." }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    // Handle SMS Notifications Deletion (Immediately removes SMS rows permanently & updates Universal_Settings)
    if (data.action === 'DELETE_SMS' || (data.deletedSmsIds && data.deletedSmsIds.length > 0)) {
      var toDeleteSms = data.deletedSmsIds || [data.smsId].filter(Boolean);
      var curSms = smsSheet.getDataRange().getValues();
      var targetMsgTxt = data.messageText ? String(data.messageText).trim().toLowerCase() : '';
      for (var ds = curSms.length - 1; ds >= 1; ds--) {
        var smsIdVal = String(curSms[ds][0] || '').trim().toLowerCase();
        var smsTimeVal = String(curSms[ds][10] || '').trim().toLowerCase();
        var smsTxtVal = String(curSms[ds][8] || '').trim().toLowerCase();
        var matchFound = false;
        for (var n = 0; n < toDeleteSms.length; n++) {
          var targetSms = String(toDeleteSms[n] || '').trim().toLowerCase();
          if (targetSms && (smsIdVal === targetSms || smsTimeVal === targetSms || (targetMsgTxt && smsTxtVal === targetMsgTxt))) {
            matchFound = true;
            break;
          }
        }
        if (matchFound) {
          smsSheet.deleteRow(ds + 1);
        }
      }
      // Persist deleted SMS IDs in Universal_Settings so all other client devices purge them
      try {
        var setSheetSms = ss.getSheetByName('Universal_Settings') || ss.getSheetByName('Settings');
        if (setSheetSms) {
          var sValsSms = setSheetSms.getDataRange().getValues();
          var delSmsRowIdx = -1;
          var curDelSmsVal = '';
          for (var ssr = 1; ssr < sValsSms.length; ssr++) {
            if (String(sValsSms[ssr][0] || '').trim().toLowerCase() === 'deleted_sms_ids') {
              delSmsRowIdx = ssr + 1;
              curDelSmsVal = String(sValsSms[ssr][1] || '').trim();
              break;
            }
          }
          var allDelSms = curDelSmsVal ? curDelSmsVal.split(',') : [];
          toDeleteSms.forEach(function(tds) {
            var clTds = String(tds || '').trim();
            if (clTds && allDelSms.indexOf(clTds) === -1) allDelSms.push(clTds);
          });
          var newDelSmsStr = allDelSms.join(',');
          if (delSmsRowIdx > -1) {
            setSheetSms.getRange(delSmsRowIdx, 2).setValue(newDelSmsStr);
            setSheetSms.getRange(delSmsRowIdx, 5).setValue(new Date().toISOString());
          } else {
            setSheetSms.appendRow(['deleted_sms_ids', newDelSmsStr, 'Universal Deleted SMS IDs', 'App_Sync', new Date().toISOString()]);
          }
        }
      } catch (errSmsDel) {}

      if (data.action === 'DELETE_SMS') {
        return ContentService.createTextOutput(JSON.stringify({ status: "SUCCESS", message: "Permanently deleted SMS row from sheet." }))
          .setMimeType(ContentService.MimeType.JSON);
      }
    }

    // Handle Clearing SMS Notifications for an RM or ALL
    if (data.action === 'CLEAR_SMS_FOR_RM') {
      var targetRm = String(data.rmCode || '').trim().toUpperCase();
      var curSmsClr = smsSheet.getDataRange().getValues();
      var clearedIds = [];
      for (var dsClr = curSmsClr.length - 1; dsClr >= 1; dsClr--) {
        var rowRecipient = String(curSmsClr[dsClr][1] || '').trim().toUpperCase();
        var rowId = String(curSmsClr[dsClr][0] || '').trim();
        if (targetRm === 'ALL' || rowRecipient === targetRm) {
          if (rowId) clearedIds.push(rowId);
          smsSheet.deleteRow(dsClr + 1);
        }
      }
      if (clearedIds.length > 0) {
        try {
          var setSheetClr = ss.getSheetByName('Universal_Settings') || ss.getSheetByName('Settings');
          if (setSheetClr) {
            var sValsClr = setSheetClr.getDataRange().getValues();
            var delClrRowIdx = -1;
            var curDelClrVal = '';
            for (var ckr = 1; ckr < sValsClr.length; ckr++) {
              if (String(sValsClr[ckr][0] || '').trim().toLowerCase() === 'deleted_sms_ids') {
                delClrRowIdx = ckr + 1;
                curDelClrVal = String(sValsClr[ckr][1] || '').trim();
                break;
              }
            }
            var allDelClr = curDelClrVal ? curDelClrVal.split(',') : [];
            clearedIds.forEach(function(cid) {
              if (cid && allDelClr.indexOf(cid) === -1) allDelClr.push(cid);
            });
            var newDelClrStr = allDelClr.join(',');
            if (delClrRowIdx > -1) {
              setSheetClr.getRange(delClrRowIdx, 2).setValue(newDelClrStr);
              setSheetClr.getRange(delClrRowIdx, 5).setValue(new Date().toISOString());
            } else {
              setSheetClr.appendRow(['deleted_sms_ids', newDelClrStr, 'Universal Deleted SMS IDs', 'App_Sync', new Date().toISOString()]);
            }
          }
        } catch (errClrDel) {}
      }
      return ContentService.createTextOutput(JSON.stringify({ status: "SUCCESS", message: "Permanently cleared SMS for " + targetRm }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    // Handle Universal Setting / Preset Deletion
    if (data.action === 'DELETE_SETTING' || data.action === 'DELETE_PRESET') {
      var setSheetDel = ss.getSheetByName('Universal_Settings') || ss.getSheetByName('Settings');
      var targetKey = String(data.settingKey || data.presetName || '').trim();
      var altKey = String(data.presetName || '').trim();
      if (setSheetDel && (targetKey || altKey)) {
        var sValsDel = setSheetDel.getDataRange().getValues();
        for (var sRow = sValsDel.length - 1; sRow >= 1; sRow--) {
          var rowKey = String(sValsDel[sRow][0] || '').trim();
          if (rowKey && (rowKey.toLowerCase() === targetKey.toLowerCase() || (altKey && rowKey.toLowerCase() === altKey.toLowerCase()))) {
            setSheetDel.deleteRow(sRow + 1);
          }
        }
        // Record in deleted_preset_keys so apps know it was permanently deleted
        try {
          var sValsDelKeys = setSheetDel.getDataRange().getValues();
          var delPresetRowIdx = -1;
          var curDelPresetVal = '';
          for (var pkr = 1; pkr < sValsDelKeys.length; pkr++) {
            if (String(sValsDelKeys[pkr][0] || '').trim().toLowerCase() === 'deleted_preset_keys') {
              delPresetRowIdx = pkr + 1;
              curDelPresetVal = String(sValsDelKeys[pkr][1] || '').trim();
              break;
            }
          }
          var allDelPresets = curDelPresetVal ? curDelPresetVal.split(',') : [];
          [targetKey, altKey].forEach(function(tk) {
            var clTk = String(tk || '').trim();
            if (clTk && allDelPresets.indexOf(clTk) === -1) allDelPresets.push(clTk);
          });
          var newDelPresetStr = allDelPresets.join(',');
          if (delPresetRowIdx > -1) {
            setSheetDel.getRange(delPresetRowIdx, 2).setValue(newDelPresetStr);
            setSheetDel.getRange(delPresetRowIdx, 5).setValue(new Date().toISOString());
          } else {
            setSheetDel.appendRow(['deleted_preset_keys', newDelPresetStr, 'Universal Deleted Setting & Preset Keys', 'App_Sync', new Date().toISOString()]);
          }
        } catch (errPDel) {}
      }
      return ContentService.createTextOutput(JSON.stringify({ status: "SUCCESS", message: "Deleted setting row from sheet." }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    // Handle Direct RM Password Update (or insert new RM row if not found in sheet)
    if (data.action === 'UPDATE_RM_PASSWORD') {
      var targetRmCode = String(data.rmCode || '').trim().toUpperCase();
      var curRmsP = rmSheet.getDataRange().getValues();
      var foundRm = false;
      for (var pr = 1; pr < curRmsP.length; pr++) {
        if (String(curRmsP[pr][0] || '').trim().toUpperCase() === targetRmCode) {
          rmSheet.getRange(pr + 1, 11).setValue(data.passwordHash || '');
          rmSheet.getRange(pr + 1, 12).setValue(data.salt || '');
          rmSheet.getRange(pr + 1, 14).setValue(data.updatedAt || '');
          foundRm = true;
          break;
        }
      }
      if (!foundRm && targetRmCode) {
        rmSheet.appendRow([
          targetRmCode,
          data.name || 'Relationship Manager',
          data.mobile || '',
          data.email || '',
          data.officeAddress || '',
          'RM',
          'ACTIVE',
          15, 5, 2,
          data.passwordHash || '',
          data.salt || '',
          data.updatedAt || new Date().toISOString(),
          data.updatedAt || new Date().toISOString()
        ]);
      }
      return ContentService.createTextOutput(JSON.stringify({ status: "SUCCESS", message: "RM Password & Profile synchronized in sheet." }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    if (data.action === 'FETCH_SHEET_DATA') {
      return handleFetchAllData();
    }

    // Upsert Customer Files (ONLY files sent by app - NEVER overwrite manual sheet edits)
    var filesToProcess = data.files || (data.file ? [data.file] : []);
    if (filesToProcess && filesToProcess.length > 0) {
      var fileExistingData = fileSheet.getDataRange().getValues();
      var headers = fileExistingData.length > 0 ? fileExistingData[0].map(function(h) { return String(h || '').trim().toLowerCase(); }) : [];
      var hasSlCol = headers.length > 0 && (headers[0] === 'sl' || headers[0] === 'sl no' || headers[0] === 'sl.' || headers[0] === 'serial');
      var fileIdRowMap = {};
      for (var r = 1; r < fileExistingData.length; r++) {
        var rowVals = fileExistingData[r];
        for (var c = 0; c < Math.min(rowVals.length, 6); c++) {
          var val = String(rowVals[c] || '').trim();
          if (val) {
            fileIdRowMap[val] = r + 1;
            fileIdRowMap[val.toLowerCase()] = r + 1;
            fileIdRowMap[val.replace(/[-\s]/g, '')] = r + 1;
            fileIdRowMap[val.replace(/[-\s]/g, '').toLowerCase()] = r + 1;
          }
        }
      }

      filesToProcess.forEach(function(f, index) {
        var fId = String(f.fileId || '').trim();
        var cc = String(f.ccNumber || '').trim();
        var sl = String(f.serialNumber || f.sl || (index + 1)).trim();
        if (!fId && !cc) return;

        var fClean = fId.replace(/[-\s]/g, '');
        var ccClean = cc.replace(/[-\s]/g, '');
        var targetRowIndex = (fId && fileIdRowMap[fId]) || (fId && fileIdRowMap[fId.toLowerCase()]) ||
                             (fClean && fileIdRowMap[fClean]) || (fClean && fileIdRowMap[fClean.toLowerCase()]) ||
                             (cc && fileIdRowMap[cc]) || (cc && fileIdRowMap[cc.toLowerCase()]) ||
                             (ccClean && fileIdRowMap[ccClean]) || (ccClean && fileIdRowMap[ccClean.toLowerCase()]);

        var baseRow = [
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
          f.cpvRemarks || f.remarks || '',
          f.cpvStatus || 'Pending',
          f.submissionAddress || '',
          f.submissionLat || 0.0,
          f.submissionLng || 0.0,
          f.updatedAt || '',
          f.updatedBy || 'App'
        ];

        var row = hasSlCol ? [sl || (index + 1)].concat(baseRow) : baseRow;

        if (targetRowIndex) {
          fileSheet.getRange(targetRowIndex, 1, 1, row.length).setValues([row]);
        } else {
          fileSheet.appendRow(row);
          var newRowIdx = fileSheet.getLastRow();
          if (fId) {
            fileIdRowMap[fId] = newRowIdx;
            fileIdRowMap[fId.toLowerCase()] = newRowIdx;
          }
          if (cc) {
            fileIdRowMap[cc] = newRowIdx;
            fileIdRowMap[cc.toLowerCase()] = newRowIdx;
          }
        }
      });

      if (data.action === 'UPSERT_CUSTOMER_FILE') {
        return ContentService.createTextOutput(JSON.stringify({
          status: "SUCCESS",
          message: "Customer file upserted to sheet."
        })).setMimeType(ContentService.MimeType.JSON);
      }
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

    // Upsert Universal Settings (1 row per preset/setting, updates in place without duplicates)
    if (data.settings && data.settings.length > 0) {
      var setExistingData = settingsSheet.getDataRange().getValues();
      var setMap = {};
      for (var s = 1; s < setExistingData.length; s++) {
        var sKey = String(setExistingData[s][0] || '').trim().toLowerCase();
        if (sKey) setMap[sKey] = s + 1;
      }
      data.settings.forEach(function(st) {
        var key = String(st.settingKey || '').trim();
        if (!key) return;
        var lookupKey = key.toLowerCase();
        var row = [key, st.settingValue || '', st.description || '', st.updatedBy || 'App', st.updatedAt || ''];
        if (setMap[lookupKey]) {
          var setRowIndex = setMap[lookupKey];
          settingsSheet.getRange(setRowIndex, 1, 1, row.length).setValues([row]);
        } else {
          settingsSheet.appendRow(row);
          setMap[lookupKey] = settingsSheet.getLastRow();
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

    // Record File Attachments Metadata & Drive Links
    if (data.attachments && data.attachments.length > 0) {
      var attData = attachSheet.getDataRange().getValues();
      var attMap = {};
      for (var a = 1; a < attData.length; a++) {
        var aId = String(attData[a][0] || '').trim();
        if (aId) attMap[aId] = a + 1;
      }
      data.attachments.forEach(function(at) {
        var atId = String(at.attachmentId || '').trim();
        if (!atId) return;
        var attDriveLink = at.fileUri || at.driveLink || '';
        if (at.fileBase64 && (!attDriveLink || attDriveLink.indexOf('http') !== 0)) {
          var atRes = saveFileToDrive(at.fileBase64, at.fileName, at.fileType);
          if (atRes && atRes.success) {
            attDriveLink = atRes.url;
          }
        }
        var attRow = [
          atId,
          at.fileId || '',
          at.fileName || '',
          at.category || '',
          at.fileSize || at.fileSizeBytes || 0,
          at.uploadedBy || '',
          at.uploadedAt || '',
          attDriveLink
        ];
        if (attMap[atId]) {
          attachSheet.getRange(attMap[atId], 1, 1, attRow.length).setValues([attRow]);
        } else {
          attachSheet.appendRow(attRow);
          attMap[atId] = attachSheet.getLastRow();
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
      cleanStaleSheetSms(smsSheet);
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

    // Upsert Important Documents (Policies, Circulars, Guidelines & Drive Links)
    if (data.importantDocuments && data.importantDocuments.length > 0) {
      var docExisting = docSheet.getDataRange().getValues();
      var docMap = {};
      for (var di = 1; di < docExisting.length; di++) {
        var did = String(docExisting[di][0] || '').trim();
        if (did) docMap[did] = di + 1;
      }
      data.importantDocuments.forEach(function(doc) {
        var did = String(doc.docId || '').trim();
        if (!did) return;
        var docDriveLink = doc.fileUri || '';
        var docStorage = doc.storagePath || '';
        if (doc.fileBase64 && (!docDriveLink || docDriveLink.indexOf('http') !== 0)) {
          var dRes = saveFileToDrive(doc.fileBase64, doc.fileName, doc.fileType);
          if (dRes && dRes.success) {
            docDriveLink = dRes.url;
            docStorage = dRes.driveUrl;
          }
        }
        var row = [
          did,
          doc.title || '',
          doc.category || 'Policies & Circulars',
          doc.description || '',
          doc.fileName || '',
          doc.fileType || 'application/pdf',
          doc.fileSizeBytes || 1024,
          docDriveLink,
          docStorage,
          doc.uploadedBy || '',
          doc.uploaderName || '',
          doc.uploaderRole || '',
          doc.createdAt || '',
          doc.updatedAt || ''
        ];
        if (docMap[did]) {
          docSheet.getRange(docMap[did], 1, 1, row.length).setValues([row]);
        } else {
          docSheet.appendRow(row);
          docMap[did] = docSheet.getLastRow();
        }
      });
    }

    // Upsert Chat and Calls
    if (data.chatMessages && data.chatMessages.length > 0) {
      var chatExisting = chatSheet.getDataRange().getValues();
      var chatMap = {};
      for (var ci = 1; ci < chatExisting.length; ci++) {
        var cid = String(chatExisting[ci][0] || '').trim();
        if (cid) chatMap[cid] = true;
      }
      data.chatMessages.forEach(function(m) {
        var mid = String(m.id || '').trim();
        if (!mid) return;
        if (!chatMap[mid]) {
          chatSheet.appendRow([
            mid,
            m.senderRmCode || '',
            m.senderName || '',
            m.senderRole || '',
            m.recipientRmCode || '',
            m.messageText || '',
            m.messageType || 'TEXT',
            m.eventId || '',
            m.timestamp || ''
          ]);
          chatMap[mid] = true;
        }
      });
    }

    // Upsert Team Events
    if (data.events && data.events.length > 0) {
      var evExisting = eventSheet.getDataRange().getValues();
      var evMap = {};
      for (var ei = 1; ei < evExisting.length; ei++) {
        var evid = String(evExisting[ei][0] || '').trim();
        if (evid) evMap[evid] = ei + 1;
      }
      data.events.forEach(function(ev) {
        var evid = String(ev.eventId || '').trim();
        if (!evid) return;
        var row = [
          evid,
          ev.title || '',
          ev.description || '',
          ev.creatorRmCode || '',
          ev.creatorName || '',
          ev.targetDate || '',
          ev.allowedFields || 'CUSTOMERS,COUNT,DATE,LOCATION,REMARKS',
          ev.status || 'ACTIVE',
          ev.createdAt || '',
          ev.closeTime || ''
        ];
        if (evMap[evid]) {
          eventSheet.getRange(evMap[evid], 1, 1, row.length).setValues([row]);
        } else {
          eventSheet.appendRow(row);
          evMap[evid] = eventSheet.getLastRow();
        }
      });
    }

    // Upsert Event Submissions
    if (data.eventResponses && data.eventResponses.length > 0) {
      var respExisting = respSheet.getDataRange().getValues();
      var respMap = {};
      for (var ri = 1; ri < respExisting.length; ri++) {
        var rid = String(respExisting[ri][0] || '').trim();
        if (rid) respMap[rid] = ri + 1;
      }
      data.eventResponses.forEach(function(r) {
        var rid = String(r.responseId || '').trim();
        if (!rid) return;
        var row = [
          rid,
          r.eventId || '',
          r.rmCode || '',
          r.rmName || '',
          r.filesCount || 1,
          r.requestedDate || '',
          r.location || '',
          r.remarks || '',
          r.customerEntriesJson || '',
          r.submittedAt || ''
        ];
        if (respMap[rid]) {
          respSheet.getRange(respMap[rid], 1, 1, row.length).setValues([row]);
        } else {
          respSheet.appendRow(row);
          respMap[rid] = respSheet.getLastRow();
        }
      });
    }

    return ContentService.createTextOutput(JSON.stringify({
      status: 'success',
      files: extractAllSheetFiles(fileSheet),
      rms: extractAllSheetRms(rmSheet),
      settings: extractAllSheetSettings(settingsSheet),
      importantDocuments: extractAllSheetDocuments(docSheet),
      chatMessages: extractAllSheetChat(chatSheet),
      events: extractAllSheetEvents(eventSheet),
      eventResponses: extractAllSheetResponses(respSheet),
      attachments: extractAllSheetAttachments(attachSheet),
      smsNotifications: extractAllSheetSms(smsSheet)
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
    var docSheet = ss.getSheetByName('Important_Documents');
    var chatSheet = ss.getSheetByName('Chat_And_Calls');
    var eventSheet = ss.getSheetByName('Team_Events');
    var respSheet = ss.getSheetByName('Event_Submissions');
    var attachSheet = ss.getSheetByName('Attachments');
    var smsSheet = ss.getSheetByName('SMS_Notifications');
    var locSheet = ss.getSheetByName('RM_Location_Logs') || ss.getSheetByName('Location_Logs');

    return ContentService.createTextOutput(JSON.stringify({
      status: 'success',
      files: fileSheet ? extractAllSheetFiles(fileSheet) : [],
      rms: rmSheet ? extractAllSheetRms(rmSheet) : [],
      settings: settingsSheet ? extractAllSheetSettings(settingsSheet) : [],
      importantDocuments: docSheet ? extractAllSheetDocuments(docSheet) : [],
      chatMessages: chatSheet ? extractAllSheetChat(chatSheet) : [],
      events: eventSheet ? extractAllSheetEvents(eventSheet) : [],
      eventResponses: respSheet ? extractAllSheetResponses(respSheet) : [],
      attachments: attachSheet ? extractAllSheetAttachments(attachSheet) : [],
      smsNotifications: smsSheet ? extractAllSheetSms(smsSheet) : [],
      locations: locSheet ? extractAllSheetLocations(locSheet) : []
    })).setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({
      status: 'error',
      message: err.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

function extractAllSheetAttachments(sheet) {
  if (!sheet) return [];
  var data = sheet.getDataRange().getValues();
  if (data.length <= 1) return [];
  var list = [];
  for (var r = 1; r < data.length; r++) {
    var row = data[r];
    var atId = String(row[0] || '').trim();
    if (!atId) continue;
    list.push({
      attachmentId: atId,
      fileId: String(row[1] || ''),
      fileName: String(row[2] || ''),
      category: String(row[3] || ''),
      fileSizeBytes: Number(row[4]) || 1024,
      fileSize: Number(row[4]) || 1024,
      uploadedBy: String(row[5] || ''),
      uploadedAt: String(row[6] || ''),
      fileUri: String(row[7] || ''),
      storagePath: String(row[7] || '')
    });
  }
  return list;
}

function extractAllSheetDocuments(sheet) {
  if (!sheet) return [];
  var data = sheet.getDataRange().getValues();
  if (data.length <= 1) return [];
  var list = [];
  for (var r = 1; r < data.length; r++) {
    var row = data[r];
    var did = String(row[0] || '').trim();
    if (!did) continue;
    list.push({
      docId: did,
      title: String(row[1] || ''),
      category: String(row[2] || ''),
      description: String(row[3] || ''),
      fileName: String(row[4] || ''),
      fileType: String(row[5] || ''),
      fileSizeBytes: Number(row[6]) || 1024,
      fileUri: String(row[7] || ''),
      storagePath: String(row[8] || ''),
      uploadedBy: String(row[9] || ''),
      uploaderName: String(row[10] || ''),
      uploaderRole: String(row[11] || ''),
      createdAt: String(row[12] || ''),
      updatedAt: String(row[13] || '')
    });
  }
  return list;
}

function cleanStaleSheetSms(smsSheet) {
  if (!smsSheet) return;
  try {
    var data = smsSheet.getDataRange().getValues();
    if (data.length <= 1) return;
    var now = new Date().getTime();
    var toDelete = [];
    for (var r = 1; r < data.length; r++) {
      var row = data[r];
      var nid = String(row[0] || '').trim();
      var timeStr = String(row[10] || '').trim();
      if (!nid) {
        toDelete.push(r + 1);
        continue;
      }
      var rowTime = 0;
      if (timeStr) {
        try {
          var parsed = Date.parse(timeStr.replace(/-/g, '/'));
          if (!isNaN(parsed)) rowTime = parsed;
        } catch (e) {}
      }
      var ageMs = rowTime > 0 ? (now - rowTime) : 999999;
      // Sheet keeps notification only for ~10 seconds transit buffer
      if (ageMs > 10000 || ageMs < -60000) {
        toDelete.push(r + 1);
      }
    }
    for (var d = toDelete.length - 1; d >= 0; d--) {
      smsSheet.deleteRow(toDelete[d]);
    }
  } catch (errClean) {}
}

function extractAllSheetSms(sheet) {
  if (!sheet) return [];
  var data = sheet.getDataRange().getValues();
  if (data.length <= 1) return [];
  var list = [];
  var now = new Date().getTime();
  var rowsToDelete = [];
  for (var r = 1; r < data.length; r++) {
    var row = data[r];
    var nid = String(row[0] || '').trim();
    if (!nid) continue;
    var timeStr = String(row[10] || '').trim();
    var rowTime = 0;
    if (timeStr) {
      try {
        var parsed = Date.parse(timeStr.replace(/-/g, '/'));
        if (!isNaN(parsed)) rowTime = parsed;
      } catch (e) {}
    }
    list.push({
      notificationId: nid,
      recipientRmCode: String(row[1] || '').trim(),
      recipientName: String(row[2] || '').trim(),
      recipientMobile: String(row[3] || '').trim(),
      triggeredByRole: String(row[4] || '').trim(),
      triggeredByCode: String(row[5] || '').trim(),
      actionType: String(row[6] || '').trim(),
      targetId: String(row[7] || '').trim(),
      messageText: String(row[8] || '').trim(),
      status: String(row[9] || 'DELIVERED').trim(),
      sentTimestamp: timeStr
    });

    // Sheet holds notification for 10-second transit window; then purges automatically
    var ageMs = rowTime > 0 ? (now - rowTime) : 999999;
    if (ageMs > 10000 || ageMs < -60000) {
      rowsToDelete.push(r + 1);
    }
  }

  // Prune expired notification rows from bottom to top so index order is preserved
  for (var d = rowsToDelete.length - 1; d >= 0; d--) {
    try {
      sheet.deleteRow(rowsToDelete[d]);
    } catch (delErr) {}
  }

  return list;
}

function extractAllSheetChat(sheet) {
  if (!sheet) return [];
  var data = sheet.getDataRange().getValues();
  if (data.length <= 1) return [];
  var list = [];
  for (var r = 1; r < data.length; r++) {
    var row = data[r];
    var mid = String(row[0] || '').trim();
    if (!mid) continue;
    list.push({
      id: mid,
      senderRmCode: String(row[1] || ''),
      senderName: String(row[2] || ''),
      senderRole: String(row[3] || ''),
      recipientRmCode: String(row[4] || ''),
      messageText: String(row[5] || ''),
      messageType: String(row[6] || 'TEXT'),
      eventId: String(row[7] || ''),
      timestamp: String(row[8] || '')
    });
  }
  return list;
}

function extractAllSheetEvents(sheet) {
  if (!sheet) return [];
  var data = sheet.getDataRange().getValues();
  if (data.length <= 1) return [];
  var list = [];
  for (var r = 1; r < data.length; r++) {
    var row = data[r];
    var eid = String(row[0] || '').trim();
    if (!eid) continue;
    list.push({
      eventId: eid,
      title: String(row[1] || ''),
      description: String(row[2] || ''),
      creatorRmCode: String(row[3] || ''),
      creatorName: String(row[4] || ''),
      targetDate: String(row[5] || ''),
      allowedFields: String(row[6] || 'CUSTOMERS,COUNT,DATE,LOCATION,REMARKS'),
      status: String(row[7] || 'ACTIVE'),
      createdAt: String(row[8] || ''),
      closeTime: String(row[9] || '')
    });
  }
  return list;
}

function extractAllSheetResponses(sheet) {
  if (!sheet) return [];
  var data = sheet.getDataRange().getValues();
  if (data.length <= 1) return [];
  var list = [];
  for (var r = 1; r < data.length; r++) {
    var row = data[r];
    var rid = String(row[0] || '').trim();
    if (!rid) continue;
    list.push({
      responseId: rid,
      eventId: String(row[1] || ''),
      rmCode: String(row[2] || ''),
      rmName: String(row[3] || ''),
      filesCount: Number(row[4]) || 1,
      requestedDate: String(row[5] || ''),
      location: String(row[6] || ''),
      remarks: String(row[7] || ''),
      customerEntriesJson: String(row[8] || ''),
      submittedAt: String(row[9] || '')
    });
  }
  return list;
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
  if (data.length <= 1) return [];
  var headers = data[0].map(function(h) { return String(h || '').trim().toLowerCase(); });
  var hasSl = headers.length > 0 && (headers[0] === 'sl' || headers[0] === 'sl no' || headers[0] === 'sl.' || headers[0] === 'serial' || headers[0] === 'serial no');
  var offset = hasSl ? 1 : 0;

  var list = [];
  for (var r = 1; r < data.length; r++) {
    var row = data[r];
    var slVal = hasSl ? String(row[0] || r).trim() : String(r);
    var ccVal = String(row[offset] || '');
    var fIdVal = String(row[offset + 1] || ccVal || '');
    if (!ccVal && !fIdVal) continue;
    list.push({
      serialNumber: slVal,
      sl: slVal,
      ccNumber: ccVal,
      fileId: fIdVal,
      customerName: String(row[offset + 2] || ''),
      companyName: String(row[offset + 3] || ''),
      officeAddress: String(row[offset + 4] || ''),
      mobile: String(row[offset + 5] || ''),
      email: String(row[offset + 6] || ''),
      productType: String(row[offset + 7] || ''),
      applicationStatus: String(row[offset + 8] || 'Submitted'),
      activeStatus: String(row[offset + 9] || 'N'),
      assignedRmCode: String(row[offset + 10] || ''),
      pendingDocuments: String(row[offset + 11] || ''),
      cpvRemarks: String(row[offset + 12] || ''),
      remarks: String(row[offset + 12] || ''),
      cpvStatus: String(row[offset + 13] || 'Pending'),
      submissionAddress: String(row[offset + 14] || ''),
      submissionLat: parseFloat(row[offset + 15]) || 0.0,
      submissionLng: parseFloat(row[offset + 16]) || 0.0,
      updatedAt: String(row[offset + 17] || ''),
      updatedBy: String(row[offset + 18] || 'Sheet')
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
      updatedAt: String(row[13] || ''),
      lastLatitude: parseFloat(row[14]) || 0.0,
      lastLongitude: parseFloat(row[15]) || 0.0,
      lastLocationAddress: String(row[16] || ''),
      lastLocationTime: String(row[17] || ''),
      isOnline: Boolean(row[18] === true || String(row[18]).toLowerCase() === 'true')
    });
  }
  return list;
}

function extractAllSheetLocations(sheet) {
  if (!sheet) return [];
  var data = sheet.getDataRange().getValues();
  if (data.length <= 1) return [];
  var list = [];
  var start = Math.max(1, data.length - 100);
  for (var r = data.length - 1; r >= start; r--) {
    var row = data[r];
    if (!row[0]) continue;
    list.push({
      rmCode: String(row[0] || '').trim().toUpperCase(),
      userName: String(row[1] || ''),
      latitude: parseFloat(row[2]) || 0.0,
      longitude: parseFloat(row[3]) || 0.0,
      address: String(row[4] || ''),
      timestamp: String(row[5] || ''),
      sourceAction: String(row[6] || 'GPS')
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

    Spacer(modifier = Modifier.height(10.dp))

    // Google Drive Cloud Storage Account Card
    Card(
      modifier = Modifier.fillMaxWidth().testTag("google_drive_account_card"),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
      border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
    ) {
      Row(
        modifier = Modifier.padding(12.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF2563EB)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
          Text("Google Drive Cloud Storage", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E3A8A))
          Text("Target Account: mdbappyhossain017@gmail.com", fontSize = 11.sp, color = Color(0xFF1D4ED8), fontWeight = FontWeight.SemiBold)
          Text("Folder: 'EBL RM Suite Drive Files' (All uploaded customer photos & documents persist permanently)", fontSize = 10.sp, color = Color(0xFF3B82F6))
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

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
                text = "Continuous Live Auto-Sync (0.05s)",
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
                "প্রতি ০.০৫ সেকেন্ডে (0.05 second / 50ms) অ্যাপ এবং গুগল শিট স্বয়ংক্রিয়ভাবে লাইভ সিঙ্ক হচ্ছে (ম্যানুয়ালি পুশ বা পুল চাপার প্রয়োজন নেই)।"
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
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier.weight(1f)
          ) {
            if (isSavingScriptUrl) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
              Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Save Connector", fontSize = 11.sp, maxLines = 1, softWrap = false)
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
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Copy Script Code", fontSize = 11.sp, maxLines = 1, softWrap = false)
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
            Text("কীভাবে গুগল শিট ও গুগল ড্রাইভ সেটআপ করবেন (Drive & Sheet Setup):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EblNavyDark)
            Spacer(modifier = Modifier.height(4.dp))
            Text("১. আপনার গুগল শিটে যান এবং Extensions > Apps Script-এ ক্লিক করুন।", fontSize = 11.sp, color = Color.DarkGray)
            Text("২. ওপরের 'Copy Script Code' বাটনে চাপ দিয়ে কোডটি কপি করে Apps Script-এ পেস্ট করুন।", fontSize = 11.sp, color = Color.DarkGray)
            Text("৩. [জরুরী] ওপরে ফাংশন ড্রপডাউন থেকে 'authorizeDrivePermissions' সিলেক্ট করে 'Run' (▶️) বাটনে চাপ দিন এবং Google Permissions 'Allow' করুন। এতে ড্রাইভে ফটো/ডকুমেন্ট সেভ করার পারমিশন চালু হবে।", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
            Text("৪. Deploy > Manage deployments > Edit > New version নির্বাচন করে 'Deploy' চাপুন ('Who has access' = 'Anyone')।", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF047857))
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
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier.weight(1f).testTag("btn_sync_now")
          ) {
            if (isSyncingNow) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Pushing...", fontSize = 11.sp, maxLines = 1, softWrap = false)
            } else {
              Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Push to Sheet", fontSize = 11.sp, maxLines = 1, softWrap = false)
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
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier.weight(1f).testTag("btn_pull_sheets")
          ) {
            if (isPullingNow) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Pulling...", fontSize = 11.sp, maxLines = 1, softWrap = false)
            } else {
              Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Pull from Sheet", fontSize = 11.sp, maxLines = 1, softWrap = false)
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}
