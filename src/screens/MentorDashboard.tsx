import React, { useState } from 'react';
import { User, TimeFilter, ScreenId } from '../types';
import { storageService } from '../services/storageService';
import { TimeFilterBar } from '../components/TimeFilterBar';
import { KpiCards } from '../components/KpiCards';
import { Charts } from '../components/Charts';
import { formatDateTime } from '../utils/dateUtils';
import {
  ShieldAlert,
  Trash2,
  RefreshCw,
  Database,
  Users,
  History,
  CheckCircle,
  AlertOctagon
} from 'lucide-react';

interface MentorDashboardProps {
  currentUser: User;
  onNavigate: (screen: ScreenId, editFileId?: string) => void;
  onOpenTrashBin: () => void;
}

export const MentorDashboard: React.FC<MentorDashboardProps> = ({
  currentUser,
  onNavigate,
  onOpenTrashBin
}) => {
  const [timeFilter, setTimeFilter] = useState<TimeFilter>('ALL_TIME');

  const files = storageService.getCustomerFiles();
  const deletedFiles = files.filter(f => f.isDeleted);
  const activeFiles = files.filter(f => !f.isDeleted);
  const rms = storageService.getUsers().filter(u => u.role === 'RM');
  const auditLogs = storageService.getAuditLogs();
  const stats = storageService.calculateStats(activeFiles, timeFilter);

  return (
    <div className="space-y-6 text-xs">
      {/* Mentor Command Banner */}
      <div className="bg-gradient-to-r from-purple-950 via-indigo-950 to-ebl-navy-dark rounded-2xl p-6 text-white shadow-xl border border-purple-900 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2">
            <h1 className="text-xl sm:text-2xl font-black">Operations Mentor Command Console</h1>
            <span className="bg-purple-600 text-white font-bold px-2 py-0.5 rounded text-[11px]">
              SUPER-OPERATIONAL: {currentUser.rmCode}
            </span>
          </div>
          <p className="text-purple-200 mt-1">
            Enterprise Governance • Recover Soft-Deleted Files • System Health Oversight
          </p>
        </div>

        {/* Action buttons */}
        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={onOpenTrashBin}
            className="px-3.5 py-2 rounded-xl bg-purple-900/60 hover:bg-purple-800/80 border border-purple-700 text-purple-200 font-semibold transition flex items-center space-x-1.5"
          >
            <Trash2 className="w-3.5 h-3.5 text-amber-400" />
            <span>Trash Bin ({deletedFiles.length})</span>
          </button>
          <button
            onClick={() => onNavigate('global_database')}
            className="px-3.5 py-2 rounded-xl bg-purple-900/60 hover:bg-purple-800/80 border border-purple-700 text-white font-semibold transition flex items-center space-x-1.5"
          >
            <Database className="w-3.5 h-3.5 text-blue-300" />
            <span>All Files</span>
          </button>
          <button
            onClick={() => onNavigate('rm_mapping')}
            className="px-3.5 py-2 rounded-xl bg-purple-900/60 hover:bg-purple-800/80 border border-purple-700 text-white font-semibold transition flex items-center space-x-1.5"
          >
            <Users className="w-3.5 h-3.5 text-ebl-gold" />
            <span>RMs ({rms.length})</span>
          </button>
          <button
            onClick={() => onNavigate('audit_logs')}
            className="px-3.5 py-2 rounded-xl bg-ebl-gold text-ebl-navy-dark font-bold hover:bg-yellow-400 transition flex items-center space-x-1.5"
          >
            <History className="w-3.5 h-3.5" />
            <span>Audit Trail</span>
          </button>
        </div>
      </div>

      {/* Time Filter Bar */}
      <div className="bg-white p-2 rounded-xl border border-slate-200 shadow-sm">
        <TimeFilterBar selected={timeFilter} onChange={setTimeFilter} />
      </div>

      {/* Global KPIs */}
      <KpiCards stats={stats} onCardClick={() => onNavigate('global_database')} />

      {/* Distribution Charts */}
      <Charts stats={stats} files={activeFiles} />

      {/* Data Governance & Trash Bin Card */}
      <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h3 className="font-bold text-sm text-slate-800">Data Integrity & Soft-Delete Governance</h3>
            <p className="text-[11px] text-slate-500">
              Files soft-deleted by RMs or Admins remain recoverable exclusively by Mentor authority.
            </p>
          </div>
          <button
            onClick={onOpenTrashBin}
            className="px-3.5 py-1.5 bg-purple-100 hover:bg-purple-200 text-purple-900 font-bold rounded-lg transition"
          >
            Open Trash Bin ({deletedFiles.length} Deleted Files)
          </button>
        </div>

        {deletedFiles.length > 0 ? (
          <div className="p-3 bg-amber-50 border border-amber-200 rounded-lg text-amber-900 text-xs">
            There are <strong>{deletedFiles.length}</strong> deleted customer record(s) currently held in the recovery buffer. You can restore them or permanently expunge them.
          </div>
        ) : (
          <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-lg text-emerald-900 text-xs flex items-center space-x-2">
            <CheckCircle className="w-4 h-4 text-emerald-600 flex-shrink-0" />
            <span>Clean repository: zero soft-deleted files awaiting recovery or expunge.</span>
          </div>
        )}
      </div>

      {/* Real-time Audit Stream */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="px-5 py-4 border-b border-slate-100 flex items-center justify-between">
          <h3 className="font-bold text-sm text-slate-800">Recent Security Activity Stream</h3>
          <button
            onClick={() => onNavigate('audit_logs')}
            className="text-xs font-semibold text-ebl-navy-primary hover:underline"
          >
            View Complete Audit Log
          </button>
        </div>

        <div className="divide-y divide-slate-100">
          {auditLogs.slice(0, 6).map(log => (
            <div key={log.logId} className="px-5 py-3 hover:bg-slate-50 flex items-center justify-between transition">
              <div className="space-y-0.5">
                <div className="flex items-center space-x-2">
                  <span className="font-bold text-slate-700">{log.action}</span>
                  <span className="text-slate-400">• User: {log.userId} ({log.role})</span>
                  {log.fileId && (
                    <span className="font-mono text-ebl-navy-primary font-medium">• {log.fileId}</span>
                  )}
                </div>
                <p className="text-slate-600">{log.details}</p>
              </div>
              <span className="text-[11px] text-slate-400 whitespace-nowrap">
                {formatDateTime(log.timestamp)}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
