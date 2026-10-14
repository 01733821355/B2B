import React, { useState } from 'react';
import { CustomerFile, User } from '../types';
import { storageService } from '../services/storageService';
import { ApplicationStatusBadge, ActiveStatusBadge, CpvStatusBadge } from './StatusBadge';
import { formatDateOnly, formatDateTime } from '../utils/dateUtils';
import { X, Edit, FileText, Download, Building, Phone, Mail, MapPin, Eye } from 'lucide-react';

interface CustomerDetailModalProps {
  file: CustomerFile;
  currentUser: User;
  onClose: () => void;
  onEdit: (fileId: string) => void;
  onFileUpdated?: () => void;
}

export const CustomerDetailModal: React.FC<CustomerDetailModalProps> = ({
  file: initialFile,
  currentUser,
  onClose,
  onEdit,
  onFileUpdated
}) => {
  const [currentRecord, setCurrentRecord] = useState<CustomerFile>(initialFile);
  const [isUpdatingStatus, setIsUpdatingStatus] = useState(false);
  const [statusSuccessMsg, setStatusSuccessMsg] = useState('');
  const statusOptions = storageService.getFileStatuses();
  const attachments = storageService.getAttachments().filter(a => a.fileId === currentRecord.fileId);
  const [previewDoc, setPreviewDoc] = useState<any | null>(null);

  const isPrivileged = currentUser.role === 'ADMIN' || currentUser.role === 'MENTOR';

  const handleStatusChange = (newStatus: string) => {
    if (!newStatus || newStatus === currentRecord.applicationStatus) return;
    setIsUpdatingStatus(true);
    const allFiles = storageService.getCustomerFiles();
    const idx = allFiles.findIndex(f => f.fileId === currentRecord.fileId);
    if (idx >= 0) {
      const updated: CustomerFile = {
        ...allFiles[idx],
        applicationStatus: newStatus as any,
        updatedAt: Date.now(),
        updatedBy: currentUser.rmCode
      };
      allFiles[idx] = updated;
      storageService.saveCustomerFiles(allFiles);
      storageService.addAuditLog(
        'FILE_STATUS_UPDATE',
        `Changed file ${currentRecord.fileId} status from '${currentRecord.applicationStatus}' to '${newStatus}'`,
        currentRecord.fileId,
        currentUser.rmCode
      );
      setCurrentRecord(updated);
      setStatusSuccessMsg(`Status updated to ${newStatus}`);
      setTimeout(() => setStatusSuccessMsg(''), 2500);
      if (onFileUpdated) {
        onFileUpdated();
      }
    }
    setIsUpdatingStatus(false);
  };

  return (
    <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-fadeIn">
      <div className="bg-white rounded-2xl max-w-2xl w-full max-h-[90vh] shadow-2xl flex flex-col overflow-hidden border border-slate-200 text-xs">
        {/* Modal Header */}
        <div className="bg-ebl-navy-dark text-white px-6 py-4 flex items-center justify-between border-b border-ebl-navy-primary">
          <div>
            <div className="flex items-center space-x-2">
              <h2 className="text-base font-bold">{currentRecord.customerName}</h2>
              <span className="text-xs text-ebl-gold font-mono">{currentRecord.fileId}</span>
            </div>
            <p className="text-[11px] text-slate-300">
              {currentRecord.companyName} • RM Code: <strong>{currentRecord.assignedRmCode}</strong>
            </p>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-white p-1">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-6 overflow-y-auto space-y-5">
          {/* Status Badges & Quick Status Change */}
          <div className="flex flex-wrap items-center justify-between gap-3 pb-3 border-b border-slate-100">
            <div className="flex flex-wrap items-center gap-2">
              <ApplicationStatusBadge status={currentRecord.applicationStatus} />
              <ActiveStatusBadge status={currentRecord.activeStatus} />
              <CpvStatusBadge status={currentRecord.cpvStatus} />
            </div>

            {/* Quick Status Updater for Admin / Mentor / RM */}
            <div className="flex items-center space-x-2">
              <span className="text-[11px] font-semibold text-slate-600">Update Status:</span>
              <select
                value={currentRecord.applicationStatus}
                onChange={e => handleStatusChange(e.target.value)}
                disabled={isUpdatingStatus}
                className="px-2.5 py-1 text-xs border border-slate-300 rounded-lg bg-white font-medium text-slate-700 shadow-sm focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none"
              >
                {statusOptions.map(st => (
                  <option key={st} value={st}>{st}</option>
                ))}
              </select>
            </div>
          </div>

          {statusSuccessMsg && (
            <div className="p-2.5 bg-emerald-50 border border-emerald-200 rounded-lg text-emerald-800 text-xs font-semibold flex items-center space-x-2">
              <span>✓</span>
              <span>{statusSuccessMsg}</span>
            </div>
          )}

          {/* Section A: Customer Details Grid */}
          <div className="bg-slate-50 rounded-xl p-4 border border-slate-200">
            <h4 className="font-bold text-slate-800 text-xs mb-3 flex items-center space-x-1.5">
              <Building className="w-3.5 h-3.5 text-ebl-navy-primary" />
              <span>Customer & Employment Information</span>
            </h4>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <span className="text-slate-500">Company / Office:</span>
                <p className="font-semibold text-slate-800">{currentRecord.companyName}</p>
              </div>
              <div>
                <span className="text-slate-500">Office Address:</span>
                <p className="font-semibold text-slate-800">{currentRecord.officeAddress || 'N/A'}</p>
              </div>
              <div>
                <span className="text-slate-500">Mobile Number:</span>
                <p className="font-semibold text-slate-800">{currentRecord.mobile}</p>
              </div>
              {currentRecord.altMobile && (
                <div>
                  <span className="text-slate-500">Alt Mobile:</span>
                  <p className="font-semibold text-slate-800">{currentRecord.altMobile}</p>
                </div>
              )}
              <div>
                <span className="text-slate-500">Email:</span>
                <p className="font-semibold text-slate-800">{currentRecord.email || 'N/A'}</p>
              </div>
              <div>
                <span className="text-slate-500">Product Type:</span>
                <p className="font-semibold text-ebl-navy-primary">{currentRecord.productType}</p>
              </div>
            </div>
          </div>

          {/* Section B: Pending Documents Checklist */}
          <div>
            <h4 className="font-bold text-slate-800 text-xs mb-2">Pending Documents Checklist</h4>
            {currentRecord.pendingDocuments && currentRecord.pendingDocuments.length > 0 ? (
              <div className="flex flex-wrap gap-1.5">
                {currentRecord.pendingDocuments.map((doc, i) => (
                  <span
                    key={i}
                    className="inline-flex items-center px-2 py-1 rounded bg-amber-50 border border-amber-200 text-amber-800 text-[11px] font-medium"
                  >
                    ⚠ {doc}
                  </span>
                ))}
              </div>
            ) : (
              <p className="text-emerald-700 bg-emerald-50 px-3 py-1.5 rounded-lg border border-emerald-200 inline-block font-medium">
                ✓ No pending documents. File is complete.
              </p>
            )}
          </div>

          {/* Section C: Remarks */}
          {currentRecord.remarks && (
            <div>
              <h4 className="font-bold text-slate-800 text-xs mb-1.5">Remarks / Requirements</h4>
              <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg text-slate-700 whitespace-pre-line">
                {currentRecord.remarks}
              </div>
            </div>
          )}

          {/* Section D: CPV Status */}
          <div className="bg-slate-50 rounded-xl p-4 border border-slate-200">
            <h4 className="font-bold text-slate-800 text-xs mb-3 flex items-center justify-between">
              <span>Contact Point Verification (CPV) Details</span>
              <CpvStatusBadge status={currentRecord.cpvStatus} />
            </h4>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <span className="text-slate-500">Verification Date:</span>
                <p className="font-semibold text-slate-800">{currentRecord.cpvDate || 'N/A'}</p>
              </div>
              <div>
                <span className="text-slate-500">Visited Address:</span>
                <p className="font-semibold text-slate-800">{currentRecord.cpvAddress || 'N/A'}</p>
              </div>
              <div className="sm:col-span-2">
                <span className="text-slate-500">Remarks / Findings:</span>
                <p className="font-semibold text-slate-800">{currentRecord.cpvRemarks || 'N/A'}</p>
              </div>
            </div>
          </div>

          {/* Section E: Attachments */}
          <div>
            <h4 className="font-bold text-slate-800 text-xs mb-2">
              Attached Documents ({attachments.length})
            </h4>
            {attachments.length === 0 ? (
              <p className="text-slate-400 italic">No files attached to this customer record.</p>
            ) : (
              <div className="space-y-2">
                {attachments.map(att => (
                  <div
                    key={att.attachmentId}
                    className="flex items-center justify-between p-2.5 bg-slate-50 border border-slate-200 rounded-lg hover:bg-slate-100 transition"
                  >
                    <div className="flex items-center space-x-2">
                      <FileText className="w-4 h-4 text-ebl-navy-primary" />
                      <div>
                        <div className="font-semibold text-slate-800">{att.fileName}</div>
                        <div className="text-[10px] text-slate-400">
                          {att.category} • {(att.fileSizeBytes / 1024).toFixed(0)} KB • By {att.uploadedBy}
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center space-x-1.5">
                      {att.fileUri && att.fileUri.startsWith('http') && (
                        <a
                          href={att.fileUri}
                          target="_blank"
                          rel="noreferrer"
                          className="px-2.5 py-1 text-[11px] bg-sky-50 text-sky-700 border border-sky-200 rounded hover:bg-sky-100 font-medium flex items-center space-x-1"
                        >
                          <Eye className="w-3 h-3" />
                          <span>Google Drive</span>
                        </a>
                      )}
                      <button
                        onClick={() => setPreviewDoc(att)}
                        className="px-2.5 py-1 text-[11px] bg-white border border-slate-300 rounded hover:bg-slate-50 font-medium text-slate-700 flex items-center space-x-1"
                      >
                        <Eye className="w-3 h-3 text-slate-500" />
                        <span>View</span>
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Section F: Timestamps & Audit */}
          <div className="text-[11px] text-slate-400 pt-2 border-t border-slate-100 flex flex-wrap justify-between gap-2">
            <span>Created: {formatDateTime(currentRecord.createdAt)} by {currentRecord.createdBy}</span>
            <span>Last Updated: {formatDateTime(currentRecord.updatedAt)} by {currentRecord.updatedBy}</span>
          </div>
        </div>

        {/* Modal Footer */}
        <div className="bg-slate-50 px-6 py-3 border-t border-slate-200 flex justify-end space-x-2">
          <button
            onClick={onClose}
            className="px-4 py-2 border border-slate-300 rounded-lg hover:bg-slate-100 font-medium text-slate-700"
          >
            Close
          </button>
          <button
            onClick={() => {
              onClose();
              onEdit(currentRecord.fileId);
            }}
            className="px-4 py-2 bg-ebl-navy-primary text-white rounded-lg hover:bg-ebl-navy-secondary font-bold flex items-center space-x-1.5"
          >
            <Edit className="w-3.5 h-3.5" />
            <span>Edit Record</span>
          </button>
        </div>
      </div>

      {/* Doc Preview Modal */}
      {previewDoc && (
        <div className="fixed inset-0 bg-slate-900/70 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-xl max-w-lg w-full p-6 shadow-2xl border border-slate-200">
            <div className="flex justify-between items-center mb-4">
              <h3 className="font-bold text-sm text-slate-800">{previewDoc.fileName}</h3>
              <button onClick={() => setPreviewDoc(null)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Check if preview is an image (data URI or image URL) */}
            {previewDoc.fileUri && previewDoc.fileUri.startsWith('data:image') ? (
              <div className="max-h-80 overflow-auto bg-slate-50 rounded-lg p-2 border border-slate-200 flex justify-center">
                <img
                  src={previewDoc.fileUri}
                  alt={previewDoc.fileName}
                  className="max-h-72 object-contain rounded"
                />
              </div>
            ) : previewDoc.storagePath && previewDoc.storagePath.startsWith('data:image') ? (
              <div className="max-h-80 overflow-auto bg-slate-50 rounded-lg p-2 border border-slate-200 flex justify-center">
                <img
                  src={previewDoc.storagePath}
                  alt={previewDoc.fileName}
                  className="max-h-72 object-contain rounded"
                />
              </div>
            ) : (
              <div className="h-48 bg-slate-100 border border-slate-200 rounded-lg flex flex-col items-center justify-center text-slate-500">
                <FileText className="w-12 h-12 text-ebl-navy-primary mb-2" />
                <p className="font-semibold text-xs text-slate-700">{previewDoc.category} Document</p>
                <p className="text-[11px] text-slate-400">
                  {previewDoc.fileName} ({((previewDoc.fileSizeBytes || 0) / 1024).toFixed(0)} KB)
                </p>
                {previewDoc.fileUri && previewDoc.fileUri.startsWith('http') && (
                  <a
                    href={previewDoc.fileUri}
                    target="_blank"
                    rel="noreferrer"
                    className="mt-3 px-3 py-1.5 bg-sky-600 hover:bg-sky-700 text-white rounded-lg font-bold text-xs inline-flex items-center space-x-1"
                  >
                    <span>Open in Google Drive ↗</span>
                  </a>
                )}
              </div>
            )}

            <div className="mt-4 flex justify-end">
              <button
                onClick={() => setPreviewDoc(null)}
                className="px-4 py-2 bg-ebl-navy-primary text-white rounded-lg text-xs font-semibold"
              >
                Close Preview
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
