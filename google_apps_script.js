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
        return ContentService.createTextOutput(JSON.stringify({ status: "SUCCESS", message: "Deleted Event and related records from sheet." }))
          .setMimeType(ContentService.MimeType.JSON);
      }
    }

    // Handle Important Documents Deletion (Immediately removes doc from Important_Documents tab & Drive)
    if (data.action === 'DELETE_DOCUMENT' || (data.deletedDocIds && data.deletedDocIds.length > 0)) {
      var toDeleteDocs = data.deletedDocIds || [data.docId].filter(Boolean);
      var curDocs = docSheet.getDataRange().getValues();
      for (var dd = curDocs.length - 1; dd >= 1; dd--) {
        var docIdVal = String(curDocs[dd][0] || '').trim();
        for (var k = 0; k < toDeleteDocs.length; k++) {
          var targetDoc = String(toDeleteDocs[k] || '').trim();
          if (targetDoc && docIdVal.toLowerCase() === targetDoc.toLowerCase()) {
            docSheet.deleteRow(dd + 1);
            break;
          }
        }
      }
      // Persist deleted document IDs in Universal_Settings
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
            setSheetDoc.appendRow(['deleted_doc_ids', newDelDocStr, 'Universal Deleted Document IDs', 'App_Sync', new Date().toISOString()]);
          }
        }
      } catch (errDocDel) {}

      if (data.action === 'DELETE_DOCUMENT') {
        return ContentService.createTextOutput(JSON.stringify({ status: "SUCCESS", message: "Deleted document row from sheet." }))
          .setMimeType(ContentService.MimeType.JSON);
      }
    }

    // Handle Direct Chat / Call Message Deletion (Immediately removes message row from Chat_And_Calls tab)
    if (data.action === 'DELETE_MESSAGE' || (data.deletedMessageIds && data.deletedMessageIds.length > 0)) {
      var toDeleteMsgs = data.deletedMessageIds || [data.messageId].filter(Boolean);
      var curMsgs = chatSheet.getDataRange().getValues();
      for (var dm = curMsgs.length - 1; dm >= 1; dm--) {
        var msgIdVal = String(curMsgs[dm][0] || '').trim();
        for (var j = 0; j < toDeleteMsgs.length; j++) {
          var targetMsg = String(toDeleteMsgs[j] || '').trim();
          if (targetMsg && msgIdVal.toLowerCase() === targetMsg.toLowerCase()) {
            chatSheet.deleteRow(dm + 1);
            break;
          }
        }
      }
      // Persist deleted message IDs in Universal_Settings so other client devices delete them too
      try {
        var setSheetMsg = ss.getSheetByName('Universal_Settings') || ss.getSheetByName('Settings');
        if (setSheetMsg) {
          var sValsMsg = setSheetMsg.getDataRange().getValues();
          var delMsgRowIdx = -1;
          var curDelMsgVal = '';
          for (var smr = 1; smr < sValsMsg.length; smr++) {
            if (String(sValsMsg[smr][0] || '').trim().toLowerCase() === 'deleted_chat_ids') {
              delMsgRowIdx = smr + 1;
              curDelMsgVal = String(sValsMsg[smr][1] || '').trim();
              break;
            }
          }
          var allDelMsgs = curDelMsgVal ? curDelMsgVal.split(',') : [];
          toDeleteMsgs.forEach(function(tdm) {
            var clTdm = String(tdm || '').trim();
            if (clTdm && allDelMsgs.indexOf(clTdm) === -1) allDelMsgs.push(clTdm);
          });
          var newDelMsgStr = allDelMsgs.join(',');
          if (delMsgRowIdx > -1) {
            setSheetMsg.getRange(delMsgRowIdx, 2).setValue(newDelMsgStr);
            setSheetMsg.getRange(delMsgRowIdx, 5).setValue(new Date().toISOString());
          } else {
            setSheetMsg.appendRow(['deleted_chat_ids', newDelMsgStr, 'Universal Deleted Chat Message IDs', 'App_Sync', new Date().toISOString()]);
          }
        }
      } catch (errMsgDel) {}

      if (data.action === 'DELETE_MESSAGE') {
        return ContentService.createTextOutput(JSON.stringify({ status: "SUCCESS", message: "Deleted chat message row from sheet." }))
          .setMimeType(ContentService.MimeType.JSON);
      }
    }

    // Handle Direct Document Upsert (Immediately syncs doc metadata & drive link to Important_Documents tab)
    if (data.action === 'UPSERT_DOCUMENT' && (data.document || (data.importantDocuments && data.importantDocuments.length > 0))) {
      var docU = data.document || data.importantDocuments[0];
      var docIdU = String(docU.docId || '').trim();
      if (docIdU) {
        var curDocsU = docSheet.getDataRange().getValues();
        var foundDocRow = 0;
        for (var du = 1; du < curDocsU.length; du++) {
          if (String(curDocsU[du][0] || '').trim().toLowerCase() === docIdU.toLowerCase()) {
            foundDocRow = du + 1;
            break;
          }
        }
        var docDriveLinkU = docU.fileUri || '';
        var docStorageU = docU.storagePath || '';
        if (docU.fileBase64 && (!docDriveLinkU || docDriveLinkU.indexOf('http') !== 0)) {
          var dResU = saveFileToDrive(docU.fileBase64, docU.fileName, docU.fileType);
          if (dResU && dResU.success) {
            docDriveLinkU = dResU.url;
            docStorageU = dResU.driveUrl;
          }
        }
        var docRowU = [
          docIdU,
          docU.title || '',
          docU.category || 'Policies & Circulars',
          docU.description || '',
          docU.fileName || '',
          docU.fileType || 'application/pdf',
          docU.fileSizeBytes || 1024,
          docDriveLinkU,
          docStorageU,
          docU.uploadedBy || '',
          docU.uploaderName || '',
          docU.uploaderRole || '',
          docU.createdAt || Utilities.formatDate(new Date(), "GMT+6", "yyyy-MM-dd HH:mm:ss"),
          docU.updatedAt || Utilities.formatDate(new Date(), "GMT+6", "yyyy-MM-dd HH:mm:ss")
        ];
        if (foundDocRow > 0) {
          docSheet.getRange(foundDocRow, 1, 1, docRowU.length).setValues([docRowU]);
        } else {
          docSheet.appendRow(docRowU);
        }
        return ContentService.createTextOutput(JSON.stringify({
          status: "SUCCESS",
          message: "Upserted Document to sheet.",
          docId: docIdU,
          fileUri: docDriveLinkU
        })).setMimeType(ContentService.MimeType.JSON);
      }
    }

    // Handle Direct Chat / Communication Upsert (Immediately syncs chat message to Chat_And_Calls tab)
    if (data.action === 'UPSERT_MESSAGE' && (data.message || (data.chatMessages && data.chatMessages.length > 0))) {
      var msgU = data.message || data.chatMessages[0];
      var msgIdU = String(msgU.id || '').trim();
      if (msgIdU) {
        var curMsgsU = chatSheet.getDataRange().getValues();
        var foundMsgRow = 0;
        for (var mu = 1; mu < curMsgsU.length; mu++) {
          if (String(curMsgsU[mu][0] || '').trim().toLowerCase() === msgIdU.toLowerCase()) {
            foundMsgRow = mu + 1;
            break;
          }
        }
        var msgRowU = [
          msgIdU,
          msgU.senderRmCode || '',
          msgU.senderName || '',
          msgU.senderRole || '',
          msgU.recipientRmCode || '',
          msgU.messageText || '',
          msgU.messageType || 'TEXT',
          msgU.eventId || '',
          msgU.timestamp || Utilities.formatDate(new Date(), "GMT+6", "yyyy-MM-dd HH:mm:ss")
        ];
        if (foundMsgRow > 0) {
          chatSheet.getRange(foundMsgRow, 1, 1, msgRowU.length).setValues([msgRowU]);
        } else {
          chatSheet.appendRow(msgRowU);
        }
        return ContentService.createTextOutput(JSON.stringify({
          status: "SUCCESS",
          message: "Upserted Chat message to sheet.",
          id: msgIdU
        })).setMimeType(ContentService.MimeType.JSON);
      }
    }

    // Handle Direct SMS Notification Upsert (Immediately saves sent SMS to SMS_Notifications tab)
    if (data.action === 'UPSERT_SMS' && (data.sms || (data.smsNotifications && data.smsNotifications.length > 0))) {
      var smsU = data.sms || data.smsNotifications[0];
      var sIdU = String(smsU.id || smsU.sentTimestamp || '').trim();
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
          smsU.recipientRmCode || '',
          smsU.recipientName || '',
          smsU.recipientMobile || '',
          smsU.triggeredByRole || '',
          smsU.triggeredByCode || '',
          smsU.actionType || '',
          smsU.fileId || smsU.targetType || '',
          smsU.messageText || '',
          smsU.status || 'DELIVERED',
          smsU.sentTimestamp || Utilities.formatDate(new Date(), "GMT+6", "yyyy-MM-dd HH:mm:ss")
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
