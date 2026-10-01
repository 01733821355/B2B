import React, { useState } from 'react';
import { storageService } from '../services/storageService';
import { formatDateTime } from '../utils/dateUtils';
import { Shield, Search, Filter, History } from 'lucide-react';

export const AuditLogs: React.FC = () => {
  const [logs] = useState(() => storageService.getAuditLogs());
  const [search, setSearch] = useState('');

  const filteredLogs = logs.filter(l => {
    if (!search.trim()) return true;
    const q = search.toLowerCase().trim();
    return (
      l.action.toLowerCase().includes(q) ||
      l.userId.toLowerCase().includes(q) ||
      l.details.toLowerCase().includes(q) ||
      (l.fileId && l.fileId.toLowerCase().includes(q)) ||
      (l.rmCode && l.rmCode.toLowerCase().includes(q))
    );
  });

  return (
    <div className="space-y-4 text-xs">
      <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm flex items-center justify-between">
        <div>
          <div className="flex items-center space-x-2">
            <History className="w-5 h-5 text-ebl-navy-primary" />
            <h1 className="text-base font-bold text-ebl-navy-dark">Security Audit Trail</h1>
          </div>
          <p className="text-slate-500 mt-0.5">
            Immutable chronological logging of all privileged operations, logins, and file mutations
          </p>
        </div>
      </div>

      <div className="relative">
        <input
          type="text"
          value={search}
          onChange={e => setSearch(e.target.value)}
          placeholder="Search audit trail by user, action, file ID, or details..."
          className="w-full pl-9 pr-4 py-2 bg-white border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none"
        />
        <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
      </div>

      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        {filteredLogs.length === 0 ? (
          <div className="p-8 text-center text-slate-400">No audit log records found</div>
        ) : (
          <div className="divide-y divide-slate-100">
            {filteredLogs.map(log => (
              <div key={log.logId} className="p-3.5 hover:bg-slate-50 transition flex items-start justify-between gap-4">
                <div className="space-y-1">
                  <div className="flex items-center space-x-2">
                    <span className="px-2 py-0.5 rounded bg-slate-100 font-bold text-ebl-navy-dark text-[11px]">
                      {log.action}
                    </span>
                    <span className="font-semibold text-slate-700">
                      User: {log.userId} ({log.role})
                    </span>
                    {log.rmCode && (
                      <span className="text-slate-500 font-medium">• RM: {log.rmCode}</span>
                    )}
                    {log.fileId && (
                      <span className="font-mono text-ebl-navy-primary font-bold">• File: {log.fileId}</span>
                    )}
                  </div>
                  <p className="text-slate-600 text-xs">{log.details}</p>
                </div>
                <span className="text-slate-400 whitespace-nowrap text-[11px]">
                  {formatDateTime(log.timestamp)}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
