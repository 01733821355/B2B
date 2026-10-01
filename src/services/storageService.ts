import {
  User,
  CustomerFile,
  FileAttachment,
  AuditLog,
  AppSetting,
  SyncStatus,
  KpiStats,
  TimeFilter,
  RmLiveLocation
} from '../types';
import { generateSalt, hashPassword, generateId } from '../utils/securityUtils';
import { matchesTimeFilter } from '../utils/dateUtils';

const STORAGE_KEY_USERS = 'ebl_users_db_v1';
const STORAGE_KEY_FILES = 'ebl_customer_files_v1';
const STORAGE_KEY_ATTACHMENTS = 'ebl_file_attachments_v1';
const STORAGE_KEY_LOGS = 'ebl_audit_logs_v1';
const STORAGE_KEY_SETTINGS = 'ebl_app_settings_v1';
const STORAGE_KEY_SYNC = 'ebl_sync_status_v1';
const STORAGE_KEY_SESSION = 'ebl_auth_session_v1';
const STORAGE_KEY_LOCATIONS = 'ebl_rm_locations_v1';

class StorageService {
  private initialized = false;

  async init(): Promise<void> {
    if (this.initialized) return;

    if (!localStorage.getItem(STORAGE_KEY_USERS)) {
      await this.seedInitialData();
    }
    this.initialized = true;
  }

  private async seedInitialData(): Promise<void> {
    const now = Date.now();
    const tenDaysAgo = now - 10 * 86400000;
    const threeDaysAgo = now - 3 * 86400000;

    // 1. Users
    // Admin0 (Initial: #123456A)
    const adminSalt = generateSalt();
    const adminHash = await hashPassword('#123456A', adminSalt);
    const adminUser: User = {
      rmCode: 'Admin0',
      name: 'Central Administrator',
      role: 'ADMIN',
      passwordHash: adminHash,
      salt: adminSalt,
      mobile: '+8801700000001',
      email: 'admin0@ebl.com.bd',
      officeAddress: 'EBL Head Office, Gulshan Avenue, Dhaka',
      accountStatus: 'ACTIVE',
      mustChangePassword: true,
      createdAt: now,
      authUid: 'AUTH_ADMIN_0'
    };

    // Mentor 12345 (Initial: 12345)
    const mentorSalt = generateSalt();
    const mentorHash = await hashPassword('12345', mentorSalt);
    const mentorUser: User = {
      rmCode: '12345',
      name: 'Operations Mentor',
      role: 'MENTOR',
      passwordHash: mentorHash,
      salt: mentorSalt,
      mobile: '+8801700000002',
      email: 'mentor12345@ebl.com.bd',
      officeAddress: 'EBL Operations Center, Motijheel, Dhaka',
      accountStatus: 'ACTIVE',
      mustChangePassword: true,
      createdAt: now,
      authUid: 'AUTH_MENTOR_12345'
    };

    // RM 104393
    const rm1Salt = generateSalt();
    const rm1Hash = await hashPassword('password123', rm1Salt);
    const rm1User: User = {
      rmCode: '104393',
      name: 'Tanvir Ahmed',
      role: 'RM',
      passwordHash: rm1Hash,
      salt: rm1Salt,
      mobile: '01711223344',
      email: 'tanvir.104393@ebl.com.bd',
      officeAddress: 'EBL Gulshan Branch, Dhaka',
      accountStatus: 'ACTIVE',
      mustChangePassword: false,
      createdAt: tenDaysAgo,
      authUid: 'AUTH_RM_104393'
    };

    // RM 104394
    const rm2Salt = generateSalt();
    const rm2Hash = await hashPassword('password123', rm2Salt);
    const rm2User: User = {
      rmCode: '104394',
      name: 'Nusrat Jahan',
      role: 'RM',
      passwordHash: rm2Hash,
      salt: rm2Salt,
      mobile: '01819556677',
      email: 'nusrat.104394@ebl.com.bd',
      officeAddress: 'EBL Principal Branch, Motijheel',
      accountStatus: 'ACTIVE',
      mustChangePassword: false,
      createdAt: tenDaysAgo,
      authUid: 'AUTH_RM_104394'
    };

    // RM 104395
    const rm3Salt = generateSalt();
    const rm3Hash = await hashPassword('password123', rm3Salt);
    const rm3User: User = {
      rmCode: '104395',
      name: 'Rafiqul Islam',
      role: 'RM',
      passwordHash: rm3Hash,
      salt: rm3Salt,
      mobile: '01912334455',
      email: 'rafiqul.104395@ebl.com.bd',
      officeAddress: 'EBL Dhanmondi Branch, Road 27',
      accountStatus: 'ACTIVE',
      mustChangePassword: false,
      createdAt: tenDaysAgo,
      authUid: 'AUTH_RM_104395'
    };

    localStorage.setItem(STORAGE_KEY_USERS, JSON.stringify([adminUser, mentorUser, rm1User, rm2User, rm3User]));

    // 2. Customer Files
    const sampleFiles: CustomerFile[] = [
      {
        fileId: 'EBL-2026-4393-101',
        customerName: 'Kazi Mahbubur Rahman',
        companyName: 'Square Pharmaceuticals Ltd',
        officeAddress: 'Square Centre, 48 Mohakhali C/A, Dhaka',
        mobile: '01713001122',
        altMobile: '01819001122',
        email: 'mahbub.kazi@squarepharma.com',
        productType: 'Credit Card',
        applicationStatus: 'Approved',
        activeStatus: 'Y',
        assignedRmCode: '104393',
        pendingDocuments: [],
        remarks: 'Pre-approved corporate executive card limit 350,000 BDT. VIP client.',
        cpvStatus: 'Completed',
        cpvDate: '2026-09-23',
        cpvAddress: 'Square Centre, 48 Mohakhali C/A',
        cpvRemarks: 'CPV verified in person. Company HR confirmed employment.',
        cpvLastUpdatedBy: '104393',
        createdAt: tenDaysAgo,
        updatedAt: now,
        submittedAt: tenDaysAgo + 86400000,
        approvedAt: threeDaysAgo,
        createdBy: '104393',
        updatedBy: 'Admin0',
        isDeleted: false,
        isSynced: true
      },
      {
        fileId: 'EBL-2026-4393-102',
        customerName: 'Farhana Yasmin',
        companyName: 'Grameenphone Ltd',
        officeAddress: 'GPHouse, Bashundhara, Baridhara, Dhaka',
        mobile: '01711889900',
        email: 'farhana.y@grameenphone.com',
        productType: 'Corporate Card',
        applicationStatus: 'Submitted',
        activeStatus: 'N',
        assignedRmCode: '104393',
        pendingDocuments: ['Salary Certificate', 'BS (Bank Statement)'],
        remarks: 'Forwarded to Head Office credit risk team. Awaiting 6-month statement.',
        cpvStatus: 'Completed',
        cpvDate: '2026-09-25',
        cpvAddress: 'GPHouse, Bashundhara',
        cpvRemarks: 'Office verification complete with GP HR department.',
        cpvLastUpdatedBy: '104393',
        createdAt: threeDaysAgo,
        updatedAt: now,
        submittedAt: now,
        createdBy: '104393',
        updatedBy: '104393',
        isDeleted: false,
        isSynced: true
      },
      {
        fileId: 'EBL-2026-4393-103',
        customerName: 'Shahidul Alam Chowdhury',
        companyName: 'Bengal Group of Industries',
        officeAddress: 'Bengal House, 75 Gulshan Ave, Dhaka',
        mobile: '01911445566',
        email: 'shahidul@bengalgroup.com',
        productType: 'Limit Enhancement',
        applicationStatus: 'Query',
        activeStatus: 'N',
        assignedRmCode: '104393',
        pendingDocuments: ['TIN', 'Trade License 2025-26'],
        remarks: 'Query raised by Credit Division: updated Trade License required.',
        cpvStatus: 'Pending',
        cpvDate: '',
        cpvAddress: '75 Gulshan Ave, Dhaka',
        cpvRemarks: 'Scheduled for next working day.',
        cpvLastUpdatedBy: '104393',
        createdAt: threeDaysAgo,
        updatedAt: now,
        submittedAt: threeDaysAgo,
        createdBy: '104393',
        updatedBy: '104393',
        isDeleted: false,
        isSynced: false
      },
      {
        fileId: 'EBL-2026-4393-104',
        customerName: 'Syed Ariful Haque',
        companyName: 'Apex Footwear Ltd',
        officeAddress: 'House 6, Road 137, Gulshan 1, Dhaka',
        mobile: '01819223344',
        productType: 'Credit Card',
        applicationStatus: 'Collected',
        activeStatus: 'N',
        assignedRmCode: '104393',
        pendingDocuments: ['NID', 'Office ID', 'Card Copy'],
        remarks: 'Application signed. Customer requested credit card copy of existing SCB card.',
        cpvStatus: 'Not Required',
        cpvDate: '',
        cpvAddress: '',
        cpvRemarks: '',
        cpvLastUpdatedBy: '104393',
        createdAt: now,
        updatedAt: now,
        createdBy: '104393',
        updatedBy: '104393',
        isDeleted: false,
        isSynced: false
      },
      {
        fileId: 'EBL-2026-4393-105',
        customerName: 'Tariqul Islam Babul',
        companyName: 'Beximco Communications',
        officeAddress: 'SAMU Tower, Gulshan-1, Dhaka',
        mobile: '01611002233',
        productType: 'Split',
        applicationStatus: 'Return to Source',
        activeStatus: 'N',
        assignedRmCode: '104393',
        pendingDocuments: ['BS (Bank Statement)'],
        remarks: 'CIB report shows delayed installment. Returned to RM for clarification.',
        cpvStatus: 'Failed',
        cpvDate: '2026-09-22',
        cpvAddress: 'SAMU Tower, Gulshan-1',
        cpvRemarks: 'Customer relocated to new department.',
        cpvLastUpdatedBy: 'Admin0',
        createdAt: tenDaysAgo,
        updatedAt: threeDaysAgo,
        submittedAt: tenDaysAgo + 86400000,
        createdBy: '104393',
        updatedBy: 'Admin0',
        isDeleted: false,
        isSynced: true
      },
      {
        fileId: 'EBL-2026-4394-201',
        customerName: 'Anisur Rahman',
        companyName: 'Unilever Bangladesh Ltd',
        officeAddress: 'ZN Tower, Plot 2, Road 8, Gulshan-1',
        mobile: '01715667788',
        productType: 'B2B',
        applicationStatus: 'Approved',
        activeStatus: 'Y',
        assignedRmCode: '104394',
        pendingDocuments: [],
        remarks: 'Corporate vendor supplier facility 1.2M BDT approved.',
        cpvStatus: 'Completed',
        cpvDate: '2026-09-23',
        cpvAddress: 'ZN Tower, Gulshan-1',
        cpvRemarks: 'Physical verification successful.',
        cpvLastUpdatedBy: '104394',
        createdAt: tenDaysAgo,
        updatedAt: threeDaysAgo,
        submittedAt: tenDaysAgo + 86400000,
        approvedAt: threeDaysAgo,
        createdBy: '104394',
        updatedBy: '104394',
        isDeleted: false,
        isSynced: true
      },
      {
        fileId: 'EBL-2026-4394-202',
        customerName: 'Mehzabin Akhter',
        companyName: 'British American Tobacco BD',
        officeAddress: 'Mohakhali New DOHS Road, Dhaka',
        mobile: '01817554433',
        productType: 'Credit Card',
        applicationStatus: 'Condition',
        activeStatus: 'N',
        assignedRmCode: '104394',
        pendingDocuments: ['Salary Certificate'],
        remarks: 'Approved subject to submission of original pay slip with HR seal.',
        cpvStatus: 'Completed',
        cpvDate: '2026-09-24',
        cpvAddress: 'BAT Mohakhali Office',
        cpvRemarks: 'Employment verified with HR.',
        cpvLastUpdatedBy: '104394',
        createdAt: threeDaysAgo,
        updatedAt: now,
        submittedAt: threeDaysAgo,
        createdBy: '104394',
        updatedBy: '104394',
        isDeleted: false,
        isSynced: true
      },
      {
        fileId: 'EBL-2026-4395-301',
        customerName: 'Zubair Al Mahmud',
        companyName: 'Navana Group',
        officeAddress: 'Islam Chamber, 125/A Motijheel C/A',
        mobile: '01712998877',
        productType: 'Credit Card',
        applicationStatus: 'STC',
        activeStatus: 'N',
        assignedRmCode: '104395',
        pendingDocuments: ['BS (Bank Statement)', 'Loan Certificate'],
        remarks: 'Subject To Clearance (STC) - branch manager sign-off required.',
        cpvStatus: 'Pending',
        cpvDate: '',
        cpvAddress: 'Motijheel C/A',
        cpvRemarks: 'Scheduled.',
        cpvLastUpdatedBy: '104395',
        createdAt: threeDaysAgo,
        updatedAt: now,
        submittedAt: threeDaysAgo,
        createdBy: '104395',
        updatedBy: '104395',
        isDeleted: false,
        isSynced: true
      },
      {
        fileId: 'EBL-2026-4395-302',
        customerName: 'Naimul Hasan',
        companyName: 'Standard Chartered BackOffice',
        officeAddress: '67 Gulshan Avenue, Dhaka',
        mobile: '01918332211',
        productType: 'Credit Card',
        applicationStatus: 'Declined',
        activeStatus: 'C',
        assignedRmCode: '104395',
        pendingDocuments: [],
        remarks: 'Declined due to internal bank policy limit threshold.',
        cpvStatus: 'Completed',
        cpvDate: '2026-09-18',
        cpvAddress: '67 Gulshan Avenue',
        cpvRemarks: 'Office verified.',
        cpvLastUpdatedBy: 'Admin0',
        createdAt: tenDaysAgo,
        updatedAt: threeDaysAgo,
        submittedAt: tenDaysAgo,
        createdBy: '104395',
        updatedBy: 'Admin0',
        isDeleted: false,
        isSynced: true
      }
    ];
    localStorage.setItem(STORAGE_KEY_FILES, JSON.stringify(sampleFiles));

    // 3. Attachments
    const sampleAttachments: FileAttachment[] = [
      {
        attachmentId: 'ATT-101-1',
        fileId: 'EBL-2026-4393-101',
        category: 'NID',
        fileName: 'NID_Kazi_Mahbubur_Rahman.pdf',
        fileType: 'application/pdf',
        storagePath: 'files/104393/EBL-2026-4393-101/nid.pdf',
        fileUri: '',
        fileSizeBytes: 524288,
        uploadedBy: '104393',
        uploadedAt: tenDaysAgo
      },
      {
        attachmentId: 'ATT-101-2',
        fileId: 'EBL-2026-4393-101',
        category: 'Salary Certificate',
        fileName: 'Salary_Certificate_Square.pdf',
        fileType: 'application/pdf',
        storagePath: 'files/104393/EBL-2026-4393-101/salary.pdf',
        fileUri: '',
        fileSizeBytes: 245760,
        uploadedBy: '104393',
        uploadedAt: tenDaysAgo
      },
      {
        attachmentId: 'ATT-101-3',
        fileId: 'EBL-2026-4393-101',
        category: 'CPV',
        fileName: 'CPV_Photo_Square_Center.jpg',
        fileType: 'image/jpeg',
        storagePath: 'files/104393/EBL-2026-4393-101/cpv.jpg',
        fileUri: '',
        fileSizeBytes: 1048576,
        uploadedBy: '104393',
        uploadedAt: threeDaysAgo
      }
    ];
    localStorage.setItem(STORAGE_KEY_ATTACHMENTS, JSON.stringify(sampleAttachments));

    // 4. Audit Logs
    const sampleLogs: AuditLog[] = [
      {
        logId: 'LOG-001',
        userId: 'SYSTEM',
        role: 'SYSTEM',
        action: 'BOOTSTRAP',
        timestamp: now - 86400000,
        details: 'Initial database bootstrap completed with secure role definitions.'
      },
      {
        logId: 'LOG-002',
        userId: '104393',
        role: 'RM',
        action: 'FILE_CREATE',
        fileId: 'EBL-2026-4393-101',
        rmCode: '104393',
        timestamp: tenDaysAgo,
        details: 'Created customer file for Kazi Mahbubur Rahman (Square Pharma).'
      },
      {
        logId: 'LOG-003',
        userId: 'Admin0',
        role: 'ADMIN',
        action: 'FILE_UPDATE',
        fileId: 'EBL-2026-4393-101',
        rmCode: '104393',
        timestamp: threeDaysAgo,
        details: 'Approved customer file. Active card status marked as Y.'
      }
    ];
    localStorage.setItem(STORAGE_KEY_LOGS, JSON.stringify(sampleLogs));

    // 5. Settings
    const sampleSettings: AppSetting[] = [
      {
        settingKey: 'PRODUCT_TYPES',
        settingValue: 'Credit Card,B2B,Corporate Card,Split,Limit Enhancement',
        updatedBy: 'SYSTEM',
        updatedAt: now
      },
      {
        settingKey: 'PENDING_DOCS_LIST',
        settingValue: 'NID,TIN,Office ID,Salary Certificate,BS (Bank Statement),BIN,Trade License 2024-25,Trade License 2025-26,Trade License 2026-27,Loan Certificate,Card Statement (Month),Card Copy',
        updatedBy: 'SYSTEM',
        updatedAt: now
      },
      {
        settingKey: 'WEEK_START_DAY',
        settingValue: 'SATURDAY',
        updatedBy: 'SYSTEM',
        updatedAt: now
      }
    ];
    localStorage.setItem(STORAGE_KEY_SETTINGS, JSON.stringify(sampleSettings));

    // 6. Sync Status
    const syncStatus: SyncStatus = {
      spreadsheetId: '1lb9Wou10ecl28EUgaXD2cA3YCNY7nNHp1BOFrrLezqI',
      lastSyncTimestamp: threeDaysAgo,
      lastSyncStatus: 'SUCCESS',
      lastSyncMessage: 'Synchronized 7 customer files and 5 RM mapping entries to Google Sheets.',
      pendingRecordsCount: 2,
      appsScriptUrl: 'https://script.google.com/macros/s/AKfycbz_EBL_SYNC_DEMO/exec',
      syncSecretKey: 'EBL_RM_SECURE_TOKEN_2026'
    };
    localStorage.setItem(STORAGE_KEY_SYNC, JSON.stringify(syncStatus));
  }

  // Auth & Session
  getCurrentUser(): User | null {
    const raw = localStorage.getItem(STORAGE_KEY_SESSION);
    return raw ? JSON.parse(raw) : null;
  }

  setCurrentUser(user: User | null): void {
    if (user) {
      localStorage.setItem(STORAGE_KEY_SESSION, JSON.stringify(user));
    } else {
      localStorage.removeItem(STORAGE_KEY_SESSION);
    }
  }

  getUsers(): User[] {
    const raw = localStorage.getItem(STORAGE_KEY_USERS);
    return raw ? JSON.parse(raw) : [];
  }

  saveUsers(users: User[]): void {
    localStorage.setItem(STORAGE_KEY_USERS, JSON.stringify(users));
  }

  // Customer Files
  getCustomerFiles(): CustomerFile[] {
    const raw = localStorage.getItem(STORAGE_KEY_FILES);
    return raw ? JSON.parse(raw) : [];
  }

  saveCustomerFiles(files: CustomerFile[]): void {
    localStorage.setItem(STORAGE_KEY_FILES, JSON.stringify(files));
    this.updatePendingCount();
  }

  // Attachments
  getAttachments(): FileAttachment[] {
    const raw = localStorage.getItem(STORAGE_KEY_ATTACHMENTS);
    return raw ? JSON.parse(raw) : [];
  }

  saveAttachments(attachments: FileAttachment[]): void {
    localStorage.setItem(STORAGE_KEY_ATTACHMENTS, JSON.stringify(attachments));
  }

  // Audit Logs
  getAuditLogs(): AuditLog[] {
    const raw = localStorage.getItem(STORAGE_KEY_LOGS);
    return raw ? JSON.parse(raw) : [];
  }

  addAuditLog(action: string, details: string, fileId?: string, rmCode?: string): void {
    const user = this.getCurrentUser();
    const logs = this.getAuditLogs();
    const newLog: AuditLog = {
      logId: 'LOG-' + generateId(),
      userId: user ? user.rmCode : 'SYSTEM',
      role: user ? user.role : 'SYSTEM',
      action,
      fileId,
      rmCode: rmCode || (user && user.role === 'RM' ? user.rmCode : undefined),
      timestamp: Date.now(),
      details
    };
    logs.unshift(newLog);
    localStorage.setItem(STORAGE_KEY_LOGS, JSON.stringify(logs.slice(0, 1000)));
  }

  // Settings
  getSettings(): AppSetting[] {
    const raw = localStorage.getItem(STORAGE_KEY_SETTINGS);
    return raw ? JSON.parse(raw) : [];
  }

  updateSetting(key: string, value: string): void {
    const settings = this.getSettings();
    const idx = settings.findIndex(s => s.settingKey === key);
    const user = this.getCurrentUser();
    const entry: AppSetting = {
      settingKey: key,
      settingValue: value,
      updatedBy: user ? user.rmCode : 'ADMIN',
      updatedAt: Date.now()
    };
    if (idx >= 0) {
      settings[idx] = entry;
    } else {
      settings.push(entry);
    }
    localStorage.setItem(STORAGE_KEY_SETTINGS, JSON.stringify(settings));
    this.addAuditLog('SETTING_UPDATE', `Updated setting '${key}'`);
  }

  // Sync Status
  getSyncStatus(): SyncStatus {
    const raw = localStorage.getItem(STORAGE_KEY_SYNC);
    if (raw) return JSON.parse(raw);
    return {
      spreadsheetId: '1lb9Wou10ecl28EUgaXD2cA3YCNY7nNHp1BOFrrLezqI',
      lastSyncStatus: 'IDLE',
      lastSyncMessage: 'Ready to synchronize with Google Sheets.',
      pendingRecordsCount: 0,
      appsScriptUrl: '',
      syncSecretKey: ''
    };
  }

  saveSyncStatus(status: SyncStatus): void {
    localStorage.setItem(STORAGE_KEY_SYNC, JSON.stringify(status));
  }

  private updatePendingCount(): void {
    const files = this.getCustomerFiles();
    const unsynced = files.filter(f => !f.isSynced).length;
    const current = this.getSyncStatus();
    current.pendingRecordsCount = unsynced;
    this.saveSyncStatus(current);
  }

  // KPI Calculator
  calculateStats(files: CustomerFile[], timeFilter: TimeFilter): KpiStats {
    const filtered = files.filter(f => !f.isDeleted && matchesTimeFilter(f.updatedAt, timeFilter));
    let collected = 0;
    let submitted = 0;
    let approved = 0;
    let declined = 0;
    let query = 0;
    let returnToSource = 0;
    let condition = 0;
    let stc = 0;
    let pendingDocumentsCount = 0;
    let activeY = 0;
    let activeN = 0;
    let activeC = 0;

    for (const f of filtered) {
      const s = f.applicationStatus.toLowerCase();
      if (s === 'collected') collected++;
      else if (s === 'submitted') submitted++;
      else if (s === 'approved') approved++;
      else if (s === 'declined') declined++;
      else if (s === 'query') query++;
      else if (s === 'return to source') returnToSource++;
      else if (s === 'condition') condition++;
      else if (s === 'stc') stc++;

      const a = f.activeStatus.toUpperCase();
      if (a === 'Y') activeY++;
      else if (a === 'N') activeN++;
      else if (a === 'C') activeC++;

      if (f.pendingDocuments && f.pendingDocuments.length > 0) {
        pendingDocumentsCount += f.pendingDocuments.length;
      }
    }

    return {
      totalFiles: filtered.length,
      collected,
      submitted,
      approved,
      declined,
      query,
      returnToSource,
      condition,
      stc,
      pendingDocumentsCount,
      activeY,
      activeN,
      activeC
    };
  }

  // RM Live Locations for Mentor Monitoring Only
  getRmLiveLocations(): RmLiveLocation[] {
    const raw = localStorage.getItem(STORAGE_KEY_LOCATIONS);
    if (raw) return JSON.parse(raw);

    // Initial default positions across Dhaka for RMs
    const now = Date.now();
    const defaults: RmLiveLocation[] = [
      {
        rmCode: '104393',
        name: 'Tanvir Ahmed',
        mobile: '01711223344',
        branch: 'Gulshan Branch',
        latitude: 23.7925,
        longitude: 90.4078,
        accuracy: 12,
        address: 'Road 11, Banani / Gulshan-2, Dhaka',
        timestamp: now - 8 * 60000,
        status: 'ONLINE',
        lastAction: 'Field Verification: Square Pharma Office'
      },
      {
        rmCode: '104394',
        name: 'Nusrat Jahan',
        mobile: '01819556677',
        branch: 'Principal Branch',
        latitude: 23.7313,
        longitude: 90.4187,
        accuracy: 18,
        address: 'Dilkusha C/A, Motijheel, Dhaka',
        timestamp: now - 15 * 60000,
        status: 'FIELD_VISIT',
        lastAction: 'Client Document Collection: Unilever BD'
      },
      {
        rmCode: '104395',
        name: 'Rafiqul Islam',
        mobile: '01912334455',
        branch: 'Dhanmondi Branch',
        latitude: 23.7461,
        longitude: 90.3742,
        accuracy: 25,
        address: 'Road 27, Dhanmondi, Dhaka',
        timestamp: now - 35 * 60000,
        status: 'ONLINE',
        lastAction: 'At Dhanmondi Branch Desk'
      }
    ];

    localStorage.setItem(STORAGE_KEY_LOCATIONS, JSON.stringify(defaults));
    return defaults;
  }

  updateRmLocation(
    rmCode: string,
    latitude: number,
    longitude: number,
    address: string,
    action = 'Customer File Activity'
  ): void {
    const locations = this.getRmLiveLocations();
    const user = this.getUsers().find(u => u.rmCode === rmCode);
    const existingIdx = locations.findIndex(l => l.rmCode === rmCode);

    const updatedEntry: RmLiveLocation = {
      rmCode,
      name: user ? user.name : `RM ${rmCode}`,
      mobile: user ? user.mobile : '',
      branch: user ? user.officeAddress : 'EBL Field',
      latitude,
      longitude,
      accuracy: 10,
      address,
      timestamp: Date.now(),
      status: 'ONLINE',
      lastAction: action
    };

    if (existingIdx >= 0) {
      locations[existingIdx] = updatedEntry;
    } else {
      locations.push(updatedEntry);
    }

    localStorage.setItem(STORAGE_KEY_LOCATIONS, JSON.stringify(locations));
    this.addAuditLog('LOCATION_PING', `Location updated for RM ${rmCode} at ${address.slice(0, 40)}`, undefined, rmCode);
  }
}

export const storageService = new StorageService();
