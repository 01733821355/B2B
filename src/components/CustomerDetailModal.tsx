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
}

export const CustomerDetailModal: React.FC<CustomerDetailModalProps> = ({
  file,
  currentUser,
  onClose,
  onEdit
}) => {
  const attachments = storageService.getAttachments().filter(a => a.fileId === file.fileId);
  const [previewDoc, setPreviewDoc] = useState<any | null>(null);

  return (
    <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-fadeIn">
      <div className="bg-white rounded-2xl max-w-2xl w-full max-h-[90vh] shadow-2xl flex flex-col overflow-hidden border border-slate-200 text-xs">
        {/* Modal Header */}
        <div className="bg-ebl-navy-dark text-white px-6 py-4 flex items-center justify-between border-b border-ebl-navy-primary">
          <div>
            <div className="flex items-center space-x-2">
              <h2 className="text-base font-bold">{file.customerName}</h2>
              <span className="text-xs text-ebl-gold font-mono">{file.fileId}</span>
            </div>
            <p className="text-[11px] text-slate-300">
              {file.companyName} • RM Code: <strong>{file.assignedRmCode}</strong>
            </p>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-white p-1">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-6 overflow-y-auto space-y-5">
          {/* Status Badges */}
          <div className="flex flex-wrap items-center gap-2 pb-2 border-b border-slate-100">
            <ApplicationStatusBadge status={file.applicationStatus} />
            <ActiveStatusBadge status={file.activeStatus} />
            <CpvStatusBadge status={file.cpvStatus} />
            <span className="text-[11px] text-slate-500 ml-auto">
              Updated: {formatDateOnly(file.updatedAt)}
            </span>
          </div>

          {/* Section A: Customer Details Grid */}
          <div className="bg-slate-50 rounded-xl p-4 border border-slate-200">
            <h4 className="font-bold text-slate-800 text-xs mb-3 flex items-center space-x-1.5">
              <Building className="w-3.5 h-3.5 text-ebl-navy-primary" />
              <span>Customer & Employment Information</span>
            </h4>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <span className="text-slate-500">Company / Office:</span>
                <p className="font-semibold text-slate-800">{file.companyName}</p>
              </div>
              <div>
                <span className="text-slate-500">Office Address:</span>
                <p className="font-semibold text-slate-800">{file.officeAddress || 'N/A'}</p>
              </div>
              <div>
                <span className="text-slate-500">Mobile Number:</span>
                <p className="font-semibold text-slate-800">{file.mobile}</p>
              </div>
              {file.altMobile && (
                <div>
                  <span className="text-slate-500">Alt Mobile:</span>
                  <p className="font-semibold text-slate-800">{file.altMobile}</p>
                </div>
              )}
              <div>
                <span className="text-slate-500">Email:</span>
                <p className="font-semibold text-slate-800">{file.email || 'N/A'}</p>
              </div>
              <div>
                <span className="text-slate-500">Product Type:</span>
                <p className="font-semibold text-ebl-navy-primary">{file.productType}</p>
              </div>
            </div>
          </div>

          {/* Section B: Pending Documents Checklist */}
          <div>
            <h4 className="font-bold text-slate-800 text-xs mb-2">Pending Documents Checklist</h4>
            {file.pendingDocuments && file.pendingDocuments.length > 0 ? (
              <div className="flex flex-wrap gap-1.5">
                {file.pendingDocuments.map((doc, i) => (
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
          {file.remarks && (
            <div>
              <h4 className="font-bold text-slate-800 text-xs mb-1.5">Remarks / Requirements</h4>
              <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg text-slate-700 whitespace-pre-line">
                {file.remarks}
              </div>
            </div>
          )}

          {/* Section D: CPV Status */}
          <div className="bg-slate-50 rounded-xl p-4 border border-slate-200">
            <h4 className="font-bold text-slate-800 text-xs mb-3 flex items-center justify-between">
              <span>Contact Point Verification (CPV) Details</span>
              <CpvStatusBadge status={file.cpvStatus} />
            </h4>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <span className="text-slate-500">Verification Date:</span>
                <p className="font-semibold text-slate-800">{file.cpvDate || 'N/A'}</p>
              </div>
              <div>
                <span className="text-slate-500">Visited Address:</span>
                <p className="font-semibold text-slate-800">{file.cpvAddress || 'N/A'}</p>
              </div>
              <div className="sm:col-span-2">
                <span className="text-slate-500">Remarks / Findings:</span>
                <p className="font-semibold text-slate-800">{file.cpvRemarks || 'N/A'}</p>
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
                    <button
                      onClick={() => setPreviewDoc(att)}
                      className="px-2.5 py-1 text-[11px] bg-white border border-slate-300 rounded hover:bg-slate-50 font-medium text-slate-700 flex items-center space-x-1"
                    >
                      <Eye className="w-3 h-3 text-slate-500" />
                      <span>Preview</span>
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Section F: Timestamps & Audit */}
          <div className="text-[11px] text-slate-400 pt-2 border-t border-slate-100 flex flex-wrap justify-between gap-2">
            <span>Created: {formatDateTime(file.createdAt)} by {file.createdBy}</span>
            <span>Last Updated: {formatDateTime(file.updatedAt)} by {file.updatedBy}</span>
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
              onEdit(file.fileId);
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
            <div className="h-48 bg-slate-100 border border-slate-200 rounded-lg flex flex-col items-center justify-center text-slate-500">
              <FileText className="w-12 h-12 text-ebl-navy-primary mb-2" />
              <p className="font-semibold text-xs text-slate-700">{previewDoc.category} Document</p>
              <p className="text-[11px] text-slate-400">Secure Storage Encrypted</p>
            </div>
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
