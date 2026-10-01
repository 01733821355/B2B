import React, { useState, useMemo } from 'react';
import { User, TimeFilter } from '../types';
import { storageService } from '../services/storageService';
import { TimeFilterBar } from '../components/TimeFilterBar';
import { ApplicationStatusBadge, ActiveStatusBadge, CpvStatusBadge } from '../components/StatusBadge';
import { formatDateOnly, formatDateTime, matchesTimeFilter } from '../utils/dateUtils';
import { Download, Printer, Filter, FileSpreadsheet, X } from 'lucide-react';

interface ReportsProps {
  currentUser: User;
}

export const Reports: React.FC<ReportsProps> = ({ currentUser }) => {
  const isPrivileged = currentUser.role === 'ADMIN' || currentUser.role === 'MENTOR';

  const [timeFilter, setTimeFilter] = useState<TimeFilter>('ALL_TIME');
  const [productFilter, setProductFilter] = useState('All');
  const [statusFilter, setStatusFilter] = useState('All');
  const [activeFilter, setActiveFilter] = useState('All');
  const [rmFilter, setRmFilter] = useState('All');

  const [showExportModal, setShowExportModal] = useState(false);
  const [csvContent, setCsvContent] = useState('');

  const allFiles = storageService.getCustomerFiles();
  const rms = storageService.getUsers().filter(u => u.role === 'RM');

  const filteredFiles = useMemo(() => {
    return allFiles.filter(file => {
      if (file.isDeleted) return false;

      // Ownership enforcement
      if (!isPrivileged && file.assignedRmCode !== currentUser.rmCode) {
        return false;
      }

      // Time Filter
      if (!matchesTimeFilter(file.updatedAt, timeFilter)) return false;

      // Dropdown filters
      if (productFilter !== 'All' && file.productType !== productFilter) return false;
      if (statusFilter !== 'All' && file.applicationStatus !== statusFilter) return false;
      if (activeFilter !== 'All' && file.activeStatus !== activeFilter) return false;
      if (isPrivileged && rmFilter !== 'All' && file.assignedRmCode !== rmFilter) return false;

      return true;
    });
  }, [allFiles, timeFilter, productFilter, statusFilter, activeFilter, rmFilter, isPrivileged, currentUser.rmCode]);

  const handleGenerateCsv = () => {
    const headers = isPrivileged
      ? ['File ID', 'Customer Name', 'Company', 'Mobile', 'Email', 'Product Type', 'Status', 'Active Status', 'RM Code', 'CPV Status', 'Pending Docs', 'Remarks', 'Created Date', 'Last Updated']
      : ['File ID', 'Customer Name', 'Company', 'Mobile', 'Email', 'Product Type', 'Status', 'Active Status', 'CPV Status', 'Pending Docs', 'Remarks', 'Created Date', 'Last Updated'];

    const rows = filteredFiles.map(f => {
      const escape = (val: string) => `"${(val || '').replace(/"/g, '""')}"`;
      if (isPrivileged) {
        return [
          escape(f.fileId),
          escape(f.customerName),
          escape(f.companyName),
          escape(f.mobile),
          escape(f.email || ''),
          escape(f.productType),
          escape(f.applicationStatus),
          escape(f.activeStatus),
          escape(f.assignedRmCode),
          escape(f.cpvStatus),
          escape(f.pendingDocuments ? f.pendingDocuments.join('; ') : ''),
          escape(f.remarks || ''),
          escape(formatDateTime(f.createdAt)),
          escape(formatDateTime(f.updatedAt))
        ];
      } else {
        return [
          escape(f.fileId),
          escape(f.customerName),
          escape(f.companyName),
          escape(f.mobile),
          escape(f.email || ''),
          escape(f.productType),
          escape(f.applicationStatus),
          escape(f.activeStatus),
          escape(f.cpvStatus),
          escape(f.pendingDocuments ? f.pendingDocuments.join('; ') : ''),
          escape(f.remarks || ''),
          escape(formatDateTime(f.createdAt)),
          escape(formatDateTime(f.updatedAt))
        ];
      }
    });

    const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
    setCsvContent(csv);
    setShowExportModal(true);
    storageService.addAuditLog('REPORT_EXPORT', `Generated report with ${filteredFiles.length} records.`);
  };

  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="space-y-6 text-xs">
      {/* Top Banner */}
      <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2">
            <FileSpreadsheet className="w-5 h-5 text-ebl-navy-primary" />
            <h1 className="text-base font-bold text-ebl-navy-dark">
              {isPrivileged ? 'Enterprise Performance & File Reports' : 'My RM Performance Report'}
            </h1>
          </div>
          <p className="text-slate-500 mt-0.5">
            {filteredFiles.length} record(s) matching selected parameters
          </p>
        </div>

        <div className="flex items-center space-x-2 no-print">
          <button
            onClick={handlePrint}
            className="flex items-center space-x-1.5 px-3 py-2 border border-slate-300 rounded-lg hover:bg-slate-50 font-semibold text-slate-700 transition"
          >
            <Printer className="w-4 h-4" />
            <span>Print View</span>
          </button>
          <button
            onClick={handleGenerateCsv}
            className="flex items-center space-x-1.5 px-4 py-2 bg-ebl-navy-primary text-white rounded-lg font-bold hover:bg-ebl-navy-secondary transition shadow-sm"
          >
            <Download className="w-4 h-4" />
            <span>Export to CSV</span>
          </button>
        </div>
      </div>

      {/* Time Filter Bar */}
      <div className="bg-white p-2 rounded-xl border border-slate-200 shadow-sm no-print">
        <TimeFilterBar selected={timeFilter} onChange={setTimeFilter} />
      </div>

      {/* Filter Selectors */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex flex-wrap gap-2 no-print">
        <select
          value={productFilter}
          onChange={e => setProductFilter(e.target.value)}
          className="px-3 py-1.5 border border-slate-300 rounded-lg bg-white text-slate-700"
        >
          <option value="All">Product: All</option>
          <option value="Credit Card">Credit Card</option>
          <option value="B2B">B2B</option>
          <option value="Corporate Card">Corporate Card</option>
          <option value="Split">Split</option>
          <option value="Limit Enhancement">Limit Enhancement</option>
        </select>

        <select
          value={statusFilter}
          onChange={e => setStatusFilter(e.target.value)}
          className="px-3 py-1.5 border border-slate-300 rounded-lg bg-white text-slate-700"
        >
          <option value="All">Status: All</option>
          <option value="Collected">Collected</option>
          <option value="Submitted">Submitted</option>
          <option value="Approved">Approved</option>
          <option value="Declined">Declined</option>
          <option value="Query">Query</option>
          <option value="Return to Source">Return to Source</option>
          <option value="Condition">Condition</option>
          <option value="STC">STC</option>
        </select>

        <select
          value={activeFilter}
          onChange={e => setActiveFilter(e.target.value)}
          className="px-3 py-1.5 border border-slate-300 rounded-lg bg-white text-slate-700"
        >
          <option value="All">Active: All</option>
          <option value="Y">Active (Y)</option>
          <option value="N">Inactive (N)</option>
          <option value="C">Closed (C)</option>
        </select>

        {isPrivileged && (
          <select
            value={rmFilter}
            onChange={e => setRmFilter(e.target.value)}
            className="px-3 py-1.5 border border-slate-300 rounded-lg bg-white text-slate-700 font-medium"
          >
            <option value="All">RM: All Officers</option>
            {rms.map(r => (
              <option key={r.rmCode} value={r.rmCode}>
                {r.rmCode} - {r.name}
              </option>
            ))}
          </select>
        )}
      </div>

      {/* Report Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="p-4 border-b border-slate-100 flex justify-between items-center bg-slate-50">
          <span className="font-bold text-slate-800 text-xs">Report Records Preview</span>
          <span className="text-[11px] text-slate-500">Period: {timeFilter}</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-100 border-b border-slate-200 text-slate-700 font-bold text-[11px] uppercase tracking-wider">
                <th className="py-2.5 px-3">File ID</th>
                <th className="py-2.5 px-3">Customer Name</th>
                <th className="py-2.5 px-3">Company</th>
                <th className="py-2.5 px-3">Mobile</th>
                <th className="py-2.5 px-3">Product</th>
                <th className="py-2.5 px-3">Status</th>
                <th className="py-2.5 px-3">Active</th>
                {isPrivileged && <th className="py-2.5 px-3">RM</th>}
                <th className="py-2.5 px-3">CPV</th>
                <th className="py-2.5 px-3">Created</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-xs">
              {filteredFiles.map(f => (
                <tr key={f.fileId} className="hover:bg-slate-50/70">
                  <td className="py-2.5 px-3 font-mono font-bold text-ebl-navy-primary">{f.fileId}</td>
                  <td className="py-2.5 px-3 font-semibold text-slate-800">{f.customerName}</td>
                  <td className="py-2.5 px-3 text-slate-600">{f.companyName}</td>
                  <td className="py-2.5 px-3 text-slate-600">{f.mobile}</td>
                  <td className="py-2.5 px-3 text-slate-700">{f.productType}</td>
                  <td className="py-2.5 px-3"><ApplicationStatusBadge status={f.applicationStatus} /></td>
                  <td className="py-2.5 px-3"><ActiveStatusBadge status={f.activeStatus} /></td>
                  {isPrivileged && <td className="py-2.5 px-3 font-semibold text-ebl-navy-secondary">{f.assignedRmCode}</td>}
                  <td className="py-2.5 px-3"><CpvStatusBadge status={f.cpvStatus} /></td>
                  <td className="py-2.5 px-3 text-slate-400">{formatDateOnly(f.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* CSV Export Modal */}
      {showExportModal && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-2xl w-full p-6 shadow-2xl border border-slate-200">
            <div className="flex justify-between items-center mb-3">
              <h3 className="font-bold text-sm text-slate-800">Exported Report Data (CSV)</h3>
              <button onClick={() => setShowExportModal(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>
            <p className="text-slate-500 text-xs mb-3">
              Copy this standard CSV content or import it into Microsoft Excel / Google Sheets:
            </p>
            <textarea
              readOnly
              rows={12}
              value={csvContent}
              className="w-full p-3 font-mono text-[10px] bg-slate-50 border border-slate-300 rounded-lg text-slate-800"
            />
            <div className="mt-4 flex justify-end">
              <button
                onClick={() => setShowExportModal(false)}
                className="px-4 py-2 bg-ebl-navy-primary text-white rounded-lg font-bold"
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
