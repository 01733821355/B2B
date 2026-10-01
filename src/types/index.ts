export type UserRole = 'RM' | 'ADMIN' | 'MENTOR';
export type AccountStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';

export interface User {
  rmCode: string;
  name: string;
  role: UserRole;
  passwordHash: string;
  salt: string;
  mobile: string;
  email: string;
  officeAddress: string;
  accountStatus: AccountStatus;
  mustChangePassword: boolean;
  createdAt: number;
  lastLogin?: number;
  authUid?: string;
  lastKnownLocation?: {
    latitude: number;
    longitude: number;
    address: string;
    timestamp: number;
  };
}

export type ApplicationStatus =
  | 'Collected'
  | 'Submitted'
  | 'Approved'
  | 'Declined'
  | 'Query'
  | 'Return to Source'
  | 'Condition'
  | 'STC';

export type ActiveStatus = 'Y' | 'N' | 'C';

export type CpvStatus = 'Pending' | 'Completed' | 'Failed' | 'Not Required';

export interface CustomerFile {
  fileId: string;
  customerName: string;
  companyName: string;
  officeAddress: string;
  mobile: string;
  altMobile?: string;
  email?: string;
  productType: string;
  applicationStatus: ApplicationStatus;
  activeStatus: ActiveStatus;
  assignedRmCode: string;
  pendingDocuments: string[];
  remarks: string;
  
  // CPV
  cpvStatus: CpvStatus;
  cpvDate: string;
  cpvAddress: string;
  cpvRemarks: string;
  cpvPhotoUri?: string;
  cpvSupportingDocUri?: string;
  cpvLastUpdatedBy: string;

  // GPS Location tags
  locationCoordinates?: {
    latitude: number;
    longitude: number;
    accuracy?: number;
    timestamp?: number;
  };

  // Metadata
  createdAt: number;
  updatedAt: number;
  submittedAt?: number;
  approvedAt?: number;
  createdBy: string;
  updatedBy: string;
  isDeleted: boolean;
  isSynced: boolean;
}

export interface FileAttachment {
  attachmentId: string;
  fileId: string;
  category: string;
  fileName: string;
  fileType: string;
  storagePath: string;
  fileUri: string;
  fileSizeBytes: number;
  uploadedBy: string;
  uploadedAt: number;
}

export interface AuditLog {
  logId: string;
  userId: string;
  role: string;
  action: string;
  fileId?: string;
  rmCode?: string;
  timestamp: number;
  details: string;
}

export interface AppSetting {
  settingKey: string;
  settingValue: string;
  updatedBy: string;
  updatedAt: number;
}

export interface SyncStatus {
  spreadsheetId: string;
  lastSyncTimestamp?: number;
  lastSyncStatus: 'IDLE' | 'IN_PROGRESS' | 'SUCCESS' | 'FAILED';
  lastSyncMessage: string;
  pendingRecordsCount: number;
  appsScriptUrl: string;
  syncSecretKey: string;
}

export interface KpiStats {
  totalFiles: number;
  collected: number;
  submitted: number;
  approved: number;
  declined: number;
  query: number;
  returnToSource: number;
  condition: number;
  stc: number;
  pendingDocumentsCount: number;
  activeY: number;
  activeN: number;
  activeC: number;
}

export interface RmLiveLocation {
  rmCode: string;
  name: string;
  mobile: string;
  branch: string;
  latitude: number;
  longitude: number;
  accuracy: number;
  address: string;
  timestamp: number;
  status: 'ONLINE' | 'FIELD_VISIT' | 'OFFLINE';
  lastAction: string;
}

export type TimeFilter = 'TODAY' | 'THIS_WEEK' | 'LAST_WEEK' | 'THIS_MONTH' | 'LAST_MONTH' | 'ALL_TIME';

export type ScreenId =
  | 'login'
  | 'rm_dashboard'
  | 'customer_form'
  | 'customer_list'
  | 'admin_dashboard'
  | 'global_database'
  | 'rm_mapping'
  | 'reports'
  | 'sheets_sync'
  | 'audit_logs'
  | 'app_settings'
  | 'mentor_dashboard'
  | 'mentor_location_tracker';
