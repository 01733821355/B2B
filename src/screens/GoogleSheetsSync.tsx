import React, { useState } from 'react';
import { storageService } from '../services/storageService';
import { sheetsSyncService } from '../services/sheetsSyncService';
import { formatDateTime } from '../utils/dateUtils';
import {
  CloudSync,
  CloudCheck,
  AlertTriangle,
  RefreshCw,
  Code,
  Settings,
  Table,
  CheckCircle2,
  XCircle,
  Copy,
  Check
} from 'lucide-react';

export const GoogleSheetsSync: React.FC = () => {
  const [syncStatus, setSyncStatus] = useState(() => storageService.getSyncStatus());
  const [isSyncing, setIsSyncing] = useState(false);
  const [syncFeedback, setSyncFeedback] = useState<string | null>(null);

  const [showConfigModal, setShowConfigModal] = useState(false);
  const [showScriptModal, setShowScriptModal] = useState(false);
  const [urlInput, setUrlInput] = useState(syncStatus.appsScriptUrl);
  const [keyInput, setKeyInput] = useState(syncStatus.syncSecretKey);

  const [copiedScript, setCopiedScript] = useState(false);

  const handleSyncNow = async () => {
    setIsSyncing(true);
    setSyncFeedback(null);
    const res = await sheetsSyncService.syncNow();
    setIsSyncing(false);
    setSyncFeedback(res.message);
    setSyncStatus(storageService.getSyncStatus());
  };

  const handleSaveConfig = (e: React.FormEvent) => {
    e.preventDefault();
    const updated = {
      ...syncStatus,
      appsScriptUrl: urlInput.trim(),
      syncSecretKey: keyInput.trim()
    };
    storageService.saveSyncStatus(updated);
    setSyncStatus(updated);
    setShowConfigModal(false);
  };

  const scriptCode = sheetsSyncService.generateAppsScriptCode(syncStatus.spreadsheetId, syncStatus.syncSecretKey);

  const handleCopyScript = () => {
    navigator.clipboard.writeText(scriptCode);
    setCopiedScript(true);
    setTimeout(() => setCopiedScript(false), 2000);
  };

  return (
    <div className="space-y-6 text-xs">
      {/* Top Banner */}
      <div className="bg-gradient-to-r from-ebl-navy-dark to-ebl-navy-primary text-white p-6 rounded-2xl shadow-lg border border-ebl-navy-primary flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold">Google Sheets Database Synchronization</h1>
          <p className="text-slate-300 mt-1">
            Spreadsheet ID:{' '}
            <span className="font-mono bg-slate-800/80 px-2 py-0.5 rounded text-ebl-gold font-bold">
              {syncStatus.spreadsheetId}
            </span>
          </p>
        </div>

        <div className="flex items-center space-x-2">
          <button
            onClick={() => setShowScriptModal(true)}
            className="flex items-center space-x-1.5 px-3 py-2 bg-slate-800 hover:bg-slate-700 text-white rounded-lg font-semibold border border-slate-600 transition"
          >
            <Code className="w-4 h-4 text-ebl-gold" />
            <span>Apps Script Code</span>
          </button>
          <button
            onClick={() => setShowConfigModal(true)}
            className="flex items-center space-x-1.5 px-3 py-2 bg-slate-800 hover:bg-slate-700 text-white rounded-lg font-semibold border border-slate-600 transition"
          >
            <Settings className="w-4 h-4" />
            <span>Endpoint Config</span>
          </button>
        </div>
      </div>

      {/* Sync Status Banner */}
      <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm space-y-4">
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <div className="flex items-center space-x-2">
            <h3 className="font-bold text-sm text-slate-800">Operational Sync Engine Status</h3>
            {syncStatus.lastSyncStatus === 'SUCCESS' && (
              <span className="flex items-center space-x-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800">
                <CheckCircle2 className="w-3.5 h-3.5" />
                <span>Connected & Healthy</span>
              </span>
            )}
            {syncStatus.lastSyncStatus === 'IN_PROGRESS' && (
              <span className="flex items-center space-x-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-blue-100 text-blue-800 animate-pulse">
                <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                <span>Syncing in Progress...</span>
              </span>
            )}
            {syncStatus.lastSyncStatus === 'FAILED' && (
              <span className="flex items-center space-x-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-rose-100 text-rose-800">
                <XCircle className="w-3.5 h-3.5" />
                <span>Sync Error</span>
              </span>
            )}
          </div>

          <button
            onClick={handleSyncNow}
            disabled={isSyncing}
            className="flex items-center space-x-2 px-5 py-2 bg-ebl-navy-primary hover:bg-ebl-navy-secondary text-white font-bold rounded-lg transition shadow disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 ${isSyncing ? 'animate-spin' : ''}`} />
            <span>{isSyncing ? 'Syncing...' : 'Sync Now'}</span>
          </button>
        </div>

        {syncFeedback && (
          <div className={`p-3 rounded-lg font-medium text-xs ${syncFeedback.includes('Failed') ? 'bg-rose-50 text-rose-800 border border-rose-200' : 'bg-emerald-50 text-emerald-800 border border-emerald-200'}`}>
            {syncFeedback}
          </div>
        )}

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="p-3.5 bg-slate-50 border border-slate-200 rounded-xl">
            <span className="text-slate-500 font-medium">Last Sync Timestamp:</span>
            <p className="font-bold text-slate-800 text-sm mt-0.5">
              {formatDateTime(syncStatus.lastSyncTimestamp)}
            </p>
          </div>

          <div className="p-3.5 bg-slate-50 border border-slate-200 rounded-xl">
            <span className="text-slate-500 font-medium">Pending Un-Synced Queue:</span>
            <p className="font-bold text-slate-800 text-sm mt-0.5">
              <span className={syncStatus.pendingRecordsCount > 0 ? 'text-amber-600' : 'text-emerald-700'}>
                {syncStatus.pendingRecordsCount} Record(s)
              </span>
            </p>
          </div>

          <div className="p-3.5 bg-slate-50 border border-slate-200 rounded-xl">
            <span className="text-slate-500 font-medium">Connector Mode:</span>
            <p className="font-bold text-slate-800 text-sm mt-0.5">
              {syncStatus.appsScriptUrl ? 'REST Web App Webhook' : 'Internal Master Ledger'}
            </p>
          </div>
        </div>

        <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg text-slate-600">
          <span className="font-semibold text-slate-700">Latest Sync Diagnostic:</span>{' '}
          {syncStatus.lastSyncMessage}
        </div>
      </div>

      {/* Sheet Architecture Tabs Schema */}
      <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm space-y-4">
        <h3 className="font-bold text-sm text-ebl-navy-dark flex items-center space-x-2">
          <Table className="w-4 h-4 text-ebl-navy-primary" />
          <span>Synchronized Sheets Schema (5 Tabs Structure)</span>
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          <div className="p-3.5 bg-slate-50 border border-slate-200 rounded-lg">
            <h4 className="font-bold text-slate-800">Sheet 1: RM_Mapping</h4>
            <p className="text-[11px] text-slate-500 mt-1">
              Columns: RM_CODE, RM_NAME, MOBILE, EMAIL, OFFICE_ADDRESS, ACCOUNT_STATUS, CREATED_AT, LAST_LOGIN, AUTH_UID
            </p>
          </div>

          <div className="p-3.5 bg-slate-50 border border-slate-200 rounded-lg">
            <h4 className="font-bold text-slate-800">Sheet 2: Customer_Files</h4>
            <p className="text-[11px] text-slate-500 mt-1">
              Columns: FILE_ID, CUSTOMER_NAME, COMPANY_NAME, OFFICE_ADDRESS, MOBILE, ALT_MOBILE, EMAIL, PRODUCT_TYPE, APPLICATION_STATUS, ACTIVE_STATUS, RM_CODE, PENDING_DOCUMENTS, REMARKS, CPV_STATUS, CPV_DATE, CPV_ADDRESS, CPV_REMARKS, CREATED_AT, UPDATED_AT, CREATED_BY, UPDATED_BY, SUBMITTED_AT, APPROVED_AT, DELETED
            </p>
          </div>

          <div className="p-3.5 bg-slate-50 border border-slate-200 rounded-lg">
            <h4 className="font-bold text-slate-800">Sheet 3: File_Attachments</h4>
            <p className="text-[11px] text-slate-500 mt-1">
              Columns: ATTACHMENT_ID, FILE_ID, CATEGORY, FILE_NAME, FILE_TYPE, STORAGE_PATH, UPLOADED_BY, UPLOADED_AT
            </p>
          </div>

          <div className="p-3.5 bg-slate-50 border border-slate-200 rounded-lg">
            <h4 className="font-bold text-slate-800">Sheet 4: Audit_Logs</h4>
            <p className="text-[11px] text-slate-500 mt-1">
              Columns: LOG_ID, USER_ID, ROLE, ACTION, FILE_ID, RM_CODE, TIMESTAMP, DETAILS
            </p>
          </div>

          <div className="p-3.5 bg-slate-50 border border-slate-200 rounded-lg md:col-span-2">
            <h4 className="font-bold text-slate-800">Sheet 5: App_Settings</h4>
            <p className="text-[11px] text-slate-500 mt-1">
              Columns: SETTING_KEY, SETTING_VALUE, UPDATED_BY, UPDATED_AT
            </p>
          </div>
        </div>
      </div>

      {/* Config Modal */}
      {showConfigModal && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-200">
            <h3 className="font-bold text-sm text-slate-800 mb-3">Google Apps Script Endpoint Setup</h3>
            <form onSubmit={handleSaveConfig} className="space-y-4">
              <div>
                <label className="block font-semibold text-slate-700 mb-1">Web App Deployment URL</label>
                <input
                  type="url"
                  value={urlInput}
                  onChange={e => setUrlInput(e.target.value)}
                  placeholder="https://script.google.com/macros/s/.../exec"
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none"
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Shared Secret Authentication Key</label>
                <input
                  type="text"
                  value={keyInput}
                  onChange={e => setKeyInput(e.target.value)}
                  placeholder="EBL_RM_SECURE_TOKEN_2026"
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none font-mono"
                />
              </div>

              <div className="flex justify-end space-x-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowConfigModal(false)}
                  className="px-4 py-2 border border-slate-300 rounded-lg text-slate-700 font-medium"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 bg-ebl-navy-primary text-white rounded-lg font-bold hover:bg-ebl-navy-secondary"
                >
                  Save Config
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Script Source Modal */}
      {showScriptModal && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-2xl w-full p-6 shadow-2xl border border-slate-200 flex flex-col max-h-[85vh]">
            <div className="flex justify-between items-center mb-3">
              <div>
                <h3 className="font-bold text-sm text-slate-800">Google Apps Script Connector Code (Code.gs)</h3>
                <p className="text-[11px] text-slate-500">Deploy as Web App in your Google Sheets Extensions</p>
              </div>
              <button
                onClick={handleCopyScript}
                className="flex items-center space-x-1.5 px-3 py-1.5 bg-slate-100 hover:bg-slate-200 rounded-lg font-bold text-slate-700 text-xs"
              >
                {copiedScript ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
                <span>{copiedScript ? 'Copied!' : 'Copy Code'}</span>
              </button>
            </div>

            <textarea
              readOnly
              value={scriptCode}
              className="w-full flex-1 p-3 font-mono text-[10px] bg-slate-900 text-slate-200 rounded-lg border border-slate-700 leading-relaxed overflow-y-auto"
              rows={14}
            />

            <div className="mt-4 flex justify-end">
              <button
                onClick={() => setShowScriptModal(false)}
                className="px-5 py-2 bg-ebl-navy-primary text-white rounded-lg font-bold"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
