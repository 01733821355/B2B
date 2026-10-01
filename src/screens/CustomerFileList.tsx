import React, { useState, useMemo } from 'react';
import { CustomerFile, User, TimeFilter, ScreenId } from '../types';
import { storageService } from '../services/storageService';
import { ApplicationStatusBadge, ActiveStatusBadge, CpvStatusBadge } from '../components/StatusBadge';
import { CustomerDetailModal } from '../components/CustomerDetailModal';
import { TimeFilterBar } from '../components/TimeFilterBar';
import { formatDateOnly } from '../utils/dateUtils';
import {
  Search,
  Filter,
  Plus,
  RefreshCw,
  Edit,
  Trash2,
  Eye,
  X,
  FileText
} from 'lucide-react';

interface CustomerFileListProps {
  currentUser: User;
  onNavigate: (screen: ScreenId, editFileId?: string) => void;
  showDeletedOnly?: boolean;
}

export const CustomerFileList: React.FC<CustomerFileListProps> = ({
  currentUser,
  onNavigate,
  showDeletedOnly = false
}) => {
  const isPrivileged = currentUser.role === 'ADMIN' || currentUser.role === 'MENTOR';

  const [files, setFiles] = useState<CustomerFile[]>(() => storageService.getCustomerFiles());
  const [searchQuery, setSearchQuery] = useState('');
  const [timeFilter, setTimeFilter] = useState<TimeFilter>('ALL_TIME');
  const [productFilter, setProductFilter] = useState('All');
  const [statusFilter, setStatusFilter] = useState('All');
  const [activeFilter, setActiveFilter] = useState('All');
  const [cpvFilter, setCpvFilter] = useState('All');
  const [rmFilter, setRmFilter] = useState('All');

  const [selectedFile, setSelectedFile] = useState<CustomerFile | null>(null);
  const [fileToDelete, setFileToDelete] = useState<CustomerFile | null>(null);

  const refreshFiles = () => {
    setFiles(storageService.getCustomerFiles());
  };

  const handleSoftDelete = (file: CustomerFile) => {
    const updated = files.map(f => {
      if (f.fileId === file.fileId) {
        return { ...f, isDeleted: true, updatedBy: currentUser.rmCode, updatedAt: Date.now(), isSynced: false };
      }
      return f;
    });
    storageService.saveCustomerFiles(updated);
    storageService.addAuditLog('FILE_DELETE', `Soft deleted file ${file.fileId} (${file.customerName})`, file.fileId, file.assignedRmCode);
    setFiles(updated);
    setFileToDelete(null);
  };

  const handleRestore = (file: CustomerFile) => {
    const updated = files.map(f => {
      if (f.fileId === file.fileId) {
        return { ...f, isDeleted: false, updatedBy: currentUser.rmCode, updatedAt: Date.now(), isSynced: false };
      }
      return f;
    });
    storageService.saveCustomerFiles(updated);
    storageService.addAuditLog('FILE_RESTORE', `Restored file ${file.fileId}`, file.fileId, file.assignedRmCode);
    setFiles(updated);
  };

  const handlePermanentDelete = (file: CustomerFile) => {
    const updated = files.filter(f => f.fileId !== file.fileId);
    storageService.saveCustomerFiles(updated);
    storageService.addAuditLog('FILE_EXPUNGE', `Permanently deleted file ${file.fileId}`, file.fileId, file.assignedRmCode);
    setFiles(updated);
    setFileToDelete(null);
  };

  // Filtered files according to user role and search criteria
  const filteredFiles = useMemo(() => {
    return files.filter(file => {
      // 1. Ownership: RM can ONLY see their own records
      if (!isPrivileged && file.assignedRmCode !== currentUser.rmCode) {
        return false;
      }

      // 2. Soft-delete filter
      if (showDeletedOnly) {
        if (!file.isDeleted) return false;
      } else {
        if (file.isDeleted) return false;
      }

      // 3. Dropdown filters
      if (productFilter !== 'All' && file.productType !== productFilter) return false;
      if (statusFilter !== 'All' && file.applicationStatus !== statusFilter) return false;
      if (activeFilter !== 'All' && file.activeStatus !== activeFilter) return false;
      if (cpvFilter !== 'All' && file.cpvStatus !== cpvFilter) return false;
      if (isPrivileged && rmFilter !== 'All' && file.assignedRmCode !== rmFilter) return false;

      // 4. Search query
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase().trim();
        const matches =
          file.customerName.toLowerCase().includes(q) ||
          file.companyName.toLowerCase().includes(q) ||
          file.mobile.includes(q) ||
          (file.altMobile && file.altMobile.includes(q)) ||
          file.fileId.toLowerCase().includes(q) ||
          file.officeAddress.toLowerCase().includes(q) ||
          file.productType.toLowerCase().includes(q) ||
          file.applicationStatus.toLowerCase().includes(q) ||
          (isPrivileged && file.assignedRmCode.toLowerCase().includes(q));

        if (!matches) return false;
      }

      return true;
    });
  }, [files, searchQuery, productFilter, statusFilter, activeFilter, cpvFilter, rmFilter, isPrivileged, currentUser.rmCode, showDeletedOnly]);

  const rms = storageService.getUsers().filter(u => u.role === 'RM');

  return (
    <div className="space-y-4 text-xs">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
        <div>
          <h2 className="text-base font-bold text-ebl-navy-dark">
            {showDeletedOnly ? 'Trash Bin (Soft-Deleted Files)' : isPrivileged ? 'Global Customer Files Repository' : 'My Customer Files'}
          </h2>
          <p className="text-slate-500">
            {filteredFiles.length} record(s) matching current search and filter criteria
          </p>
        </div>

        <div className="flex items-center space-x-2">
          <button
            onClick={refreshFiles}
            className="p-2 border border-slate-300 rounded-lg hover:bg-slate-50 text-slate-600"
            title="Refresh database records"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
          {!showDeletedOnly && (
            <button
              onClick={() => onNavigate('customer_form')}
              className="flex items-center space-x-1.5 px-3 py-2 bg-ebl-navy-primary hover:bg-ebl-navy-secondary text-white font-bold rounded-lg transition"
            >
              <Plus className="w-4 h-4" />
              <span>New File</span>
            </button>
          )}
        </div>
      </div>

      {/* Search & Filter Bar */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm space-y-3">
        <div className="relative">
          <input
            type="text"
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            placeholder="Search by Customer Name, Mobile Number, Company, File ID, or RM Code..."
            className="w-full pl-9 pr-8 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none"
          />
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="absolute right-3 top-2.5 text-slate-400 hover:text-slate-600"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>

        {/* Filter Dropdowns Row */}
        <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-5 gap-2">
          <select
            value={productFilter}
            onChange={e => setProductFilter(e.target.value)}
            className="px-2.5 py-1.5 border border-slate-300 rounded-lg bg-white text-slate-700"
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
            className="px-2.5 py-1.5 border border-slate-300 rounded-lg bg-white text-slate-700"
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
            className="px-2.5 py-1.5 border border-slate-300 rounded-lg bg-white text-slate-700"
          >
            <option value="All">Active: All</option>
            <option value="Y">Active (Y)</option>
            <option value="N">Inactive (N)</option>
            <option value="C">Closed (C)</option>
          </select>

          <select
            value={cpvFilter}
            onChange={e => setCpvFilter(e.target.value)}
            className="px-2.5 py-1.5 border border-slate-300 rounded-lg bg-white text-slate-700"
          >
            <option value="All">CPV: All</option>
            <option value="Pending">Pending</option>
            <option value="Completed">Completed</option>
            <option value="Failed">Failed</option>
            <option value="Not Required">Not Required</option>
          </select>

          {isPrivileged && (
            <select
              value={rmFilter}
              onChange={e => setRmFilter(e.target.value)}
              className="px-2.5 py-1.5 border border-slate-300 rounded-lg bg-white text-slate-700 font-medium"
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
      </div>

      {/* Files Table / List */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        {filteredFiles.length === 0 ? (
          <div className="p-12 text-center text-slate-400">
            <FileText className="w-10 h-10 mx-auto mb-2 text-slate-300" />
            <p className="font-semibold">No customer files match your criteria</p>
            <button
              onClick={() => {
                setSearchQuery('');
                setProductFilter('All');
                setStatusFilter('All');
                setActiveFilter('All');
                setCpvFilter('All');
                setRmFilter('All');
              }}
              className="mt-2 text-ebl-navy-primary font-bold hover:underline"
            >
              Reset Filters
            </button>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200 text-slate-600 font-bold text-[11px] uppercase tracking-wider">
                  <th className="py-3 px-4">File ID</th>
                  <th className="py-3 px-4">Customer Name</th>
                  <th className="py-3 px-4">Company / Office</th>
                  <th className="py-3 px-4">Mobile</th>
                  <th className="py-3 px-4">Product</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4">Active</th>
                  {isPrivileged && <th className="py-3 px-4">RM Code</th>}
                  <th className="py-3 px-4">Updated</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredFiles.map(file => (
                  <tr key={file.fileId} className="hover:bg-slate-50/80 transition">
                    <td className="py-3 px-4 font-mono font-bold text-ebl-navy-primary whitespace-nowrap">
                      {file.fileId}
                    </td>
                    <td className="py-3 px-4 font-semibold text-slate-800 whitespace-nowrap">
                      {file.customerName}
                    </td>
                    <td className="py-3 px-4 text-slate-600 max-w-[180px] truncate" title={file.companyName}>
                      {file.companyName}
                    </td>
                    <td className="py-3 px-4 text-slate-600 whitespace-nowrap">
                      {file.mobile}
                    </td>
                    <td className="py-3 px-4 text-slate-700 whitespace-nowrap">
                      {file.productType}
                    </td>
                    <td className="py-3 px-4 whitespace-nowrap">
                      <ApplicationStatusBadge status={file.applicationStatus} />
                    </td>
                    <td className="py-3 px-4 whitespace-nowrap">
                      <ActiveStatusBadge status={file.activeStatus} />
                    </td>
                    {isPrivileged && (
                      <td className="py-3 px-4 font-semibold text-ebl-navy-secondary whitespace-nowrap">
                        {file.assignedRmCode}
                      </td>
                    )}
                    <td className="py-3 px-4 text-slate-400 whitespace-nowrap">
                      {formatDateOnly(file.updatedAt)}
                    </td>
                    <td className="py-3 px-4 text-right whitespace-nowrap">
                      <div className="flex items-center justify-end space-x-1">
                        <button
                          onClick={() => setSelectedFile(file)}
                          className="p-1.5 rounded hover:bg-slate-200 text-slate-600"
                          title="View Details"
                        >
                          <Eye className="w-4 h-4" />
                        </button>
                        {!showDeletedOnly ? (
                          <>
                            <button
                              onClick={() => onNavigate('customer_form', file.fileId)}
                              className="p-1.5 rounded hover:bg-slate-200 text-ebl-navy-primary"
                              title="Edit Record"
                            >
                              <Edit className="w-4 h-4" />
                            </button>
                            <button
                              onClick={() => setFileToDelete(file)}
                              className="p-1.5 rounded hover:bg-rose-100 text-rose-600"
                              title="Delete Record"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </>
                        ) : (
                          <>
                            <button
                              onClick={() => handleRestore(file)}
                              className="px-2 py-1 rounded bg-emerald-100 text-emerald-800 font-semibold"
                            >
                              Restore
                            </button>
                            {currentUser.role === 'MENTOR' && (
                              <button
                                onClick={() => handlePermanentDelete(file)}
                                className="px-2 py-1 rounded bg-rose-100 text-rose-800 font-semibold ml-1"
                              >
                                Expunge
                              </button>
                            )}
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Customer Detail Modal */}
      {selectedFile && (
        <CustomerDetailModal
          file={selectedFile}
          currentUser={currentUser}
          onClose={() => setSelectedFile(null)}
          onEdit={(id) => onNavigate('customer_form', id)}
        />
      )}

      {/* Delete Confirmation Modal */}
      {fileToDelete && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-fadeIn">
          <div className="bg-white rounded-2xl max-w-sm w-full p-6 shadow-2xl border border-slate-200">
            <h3 className="font-bold text-sm text-slate-800 mb-2">Confirm Delete</h3>
            <p className="text-xs text-slate-600 mb-4">
              Are you sure you want to delete file <strong>{fileToDelete.fileId}</strong> ({fileToDelete.customerName})?
              This action will be audited in system logs.
            </p>
            <div className="flex justify-end space-x-2">
              <button
                onClick={() => setFileToDelete(null)}
                className="px-4 py-2 border border-slate-300 rounded-lg hover:bg-slate-50 font-medium text-slate-700"
              >
                Cancel
              </button>
              <button
                onClick={() => handleSoftDelete(fileToDelete)}
                className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-lg font-bold"
              >
                Delete File
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
