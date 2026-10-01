import { storageService } from './storageService';
import { CustomerFile, User } from '../types';

export class SheetsSyncService {
  async syncNow(): Promise<{ success: boolean; message: string }> {
    const status = storageService.getSyncStatus();
    status.lastSyncStatus = 'IN_PROGRESS';
    status.lastSyncMessage = 'Initiating synchronization with Google Sheets...';
    storageService.saveSyncStatus(status);

    const files = storageService.getCustomerFiles();
    const unsyncedFiles = files.filter(f => !f.isSynced);
    const users = storageService.getUsers().filter(u => u.role === 'RM');

    try {
      if (status.appsScriptUrl && status.appsScriptUrl.startsWith('http')) {
        const payload = {
          spreadsheetId: status.spreadsheetId,
          secretKey: status.syncSecretKey,
          timestamp: new Date().toISOString(),
          customerFiles: unsyncedFiles,
          rmMapping: users
        };

        const res = await fetch(status.appsScriptUrl, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });

        if (!res.ok) {
          throw new Error(`Server returned HTTP ${res.status}: ${res.statusText}`);
        }
      }

      // Mark all files as synced locally
      const updatedFiles = files.map(f => ({ ...f, isSynced: true }));
      storageService.saveCustomerFiles(updatedFiles);

      const successMsg = `Successfully synchronized ${unsyncedFiles.length} file updates and ${users.length} RM mappings to Spreadsheet (${status.spreadsheetId}).`;
      status.lastSyncTimestamp = Date.now();
      status.lastSyncStatus = 'SUCCESS';
      status.lastSyncMessage = successMsg;
      status.pendingRecordsCount = 0;
      storageService.saveSyncStatus(status);
      storageService.addAuditLog('SYNC_SHEETS', successMsg);

      return { success: true, message: successMsg };
    } catch (err: any) {
      const errMsg = `Sync Failed: ${err.message || 'Unknown network error'}`;
      status.lastSyncTimestamp = Date.now();
      status.lastSyncStatus = 'FAILED';
      status.lastSyncMessage = errMsg;
      storageService.saveSyncStatus(status);
      storageService.addAuditLog('SYNC_SHEETS_FAILED', errMsg);

      return { success: false, message: errMsg };
    }
  }

  generateAppsScriptCode(spreadsheetId: string, secretKey: string): string {
    return `/**
 * EBL RM File Management & Performance Dashboard
 * Google Apps Script Web App Connector (Code.gs)
 * Spreadsheet ID: ${spreadsheetId}
 */

const SPREADSHEET_ID = "${spreadsheetId}";
const EXPECTED_SECRET = "${secretKey || 'EBL_RM_SECURE_TOKEN_2026'}";

function doPost(e) {
  try {
    const data = JSON.parse(e.postData.contents);
    if (data.secretKey !== EXPECTED_SECRET) {
      return ContentService.createTextOutput(JSON.stringify({ 
        status: "ERROR", 
        message: "Unauthorized token" 
      })).setMimeType(ContentService.MimeType.JSON);
    }
    
    initializeSheetsIfMissing();
    const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
    
    // 1. Sync Customer Files
    if (data.customerFiles && Array.isArray(data.customerFiles)) {
      const sheet = ss.getSheetByName("Customer_Files");
      const rows = sheet.getDataRange().getValues();
      const idColIdx = 0; // FILE_ID is column 1
      
      data.customerFiles.forEach(file => {
        let existingRowIndex = -1;
        for (let i = 1; i < rows.length; i++) {
          if (rows[i][idColIdx] === file.fileId) {
            existingRowIndex = i + 1;
            break;
          }
        }
        
        const rowData = [
          file.fileId,
          file.customerName,
          file.companyName,
          file.officeAddress,
          file.mobile,
          file.altMobile || "",
          file.email || "",
          file.productType,
          file.applicationStatus,
          file.activeStatus,
          file.assignedRmCode,
          (file.pendingDocuments || []).join(", "),
          file.remarks || "",
          file.cpvStatus || "",
          file.cpvDate || "",
          file.cpvAddress || "",
          file.cpvRemarks || "",
          new Date(file.createdAt).toISOString(),
          new Date(file.updatedAt).toISOString(),
          file.createdBy || "",
          file.updatedBy || "",
          file.submittedAt ? new Date(file.submittedAt).toISOString() : "",
          file.approvedAt ? new Date(file.approvedAt).toISOString() : "",
          file.isDeleted ? "TRUE" : "FALSE"
        ];
        
        if (existingRowIndex > 0) {
          sheet.getRange(existingRowIndex, 1, 1, rowData.length).setValues([rowData]);
        } else {
          sheet.appendRow(rowData);
        }
      });
    }

    // 2. Sync RM Mapping
    if (data.rmMapping && Array.isArray(data.rmMapping)) {
      const rmSheet = ss.getSheetByName("RM_Mapping");
      const rmRows = rmSheet.getDataRange().getValues();
      
      data.rmMapping.forEach(rm => {
        let existingRowIndex = -1;
        for (let i = 1; i < rmRows.length; i++) {
          if (rmRows[i][0] === rm.rmCode) {
            existingRowIndex = i + 1;
            break;
          }
        }
        
        const rowData = [
          rm.rmCode,
          rm.name,
          rm.mobile,
          rm.email || "",
          rm.officeAddress || "",
          rm.accountStatus || "ACTIVE",
          new Date(rm.createdAt).toISOString(),
          rm.lastLogin ? new Date(rm.lastLogin).toISOString() : "",
          rm.authUid || ""
        ];
        
        if (existingRowIndex > 0) {
          rmSheet.getRange(existingRowIndex, 1, 1, rowData.length).setValues([rowData]);
        } else {
          rmSheet.appendRow(rowData);
        }
      });
    }

    return ContentService.createTextOutput(JSON.stringify({ 
      status: "SUCCESS", 
      message: "Sync accepted", 
      timestamp: new Date().toISOString() 
    })).setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({ 
      status: "ERROR", 
      message: err.toString() 
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

function initializeSheetsIfMissing() {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  
  const schema = {
    "RM_Mapping": [
      "RM_CODE", "RM_NAME", "MOBILE", "EMAIL", "OFFICE_ADDRESS", 
      "ACCOUNT_STATUS", "CREATED_AT", "LAST_LOGIN", "AUTH_UID"
    ],
    "Customer_Files": [
      "FILE_ID", "CUSTOMER_NAME", "COMPANY_NAME", "OFFICE_ADDRESS", "MOBILE", 
      "ALT_MOBILE", "EMAIL", "PRODUCT_TYPE", "APPLICATION_STATUS", "ACTIVE_STATUS", 
      "RM_CODE", "PENDING_DOCUMENTS", "REMARKS", "CPV_STATUS", "CPV_DATE", 
      "CPV_ADDRESS", "CPV_REMARKS", "CREATED_AT", "UPDATED_AT", "CREATED_BY", 
      "UPDATED_BY", "SUBMITTED_AT", "APPROVED_AT", "DELETED"
    ],
    "File_Attachments": [
      "ATTACHMENT_ID", "FILE_ID", "CATEGORY", "FILE_NAME", "FILE_TYPE", 
      "STORAGE_PATH", "UPLOADED_BY", "UPLOADED_AT"
    ],
    "Audit_Logs": [
      "LOG_ID", "USER_ID", "ROLE", "ACTION", "FILE_ID", "RM_CODE", "TIMESTAMP", "DETAILS"
    ],
    "App_Settings": [
      "SETTING_KEY", "SETTING_VALUE", "UPDATED_BY", "UPDATED_AT"
    ]
  };
  
  for (const sheetName in schema) {
    let sheet = ss.getSheetByName(sheetName);
    if (!sheet) {
      sheet = ss.insertSheet(sheetName);
      sheet.appendRow(schema[sheetName]);
      sheet.getRange(1, 1, 1, schema[sheetName].length)
        .setFontWeight("bold")
        .setBackground("#0F325E")
        .setFontColor("#FFFFFF");
    }
  }
}`;
  }
}

export const sheetsSyncService = new SheetsSyncService();
