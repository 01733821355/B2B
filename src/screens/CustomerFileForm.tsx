import React, { useState, useEffect } from 'react';
import { CustomerFile, User, ApplicationStatus, ActiveStatus, CpvStatus, ScreenId, FileAttachment } from '../types';
import { storageService } from '../services/storageService';
import { generateFileId, generateId } from '../utils/securityUtils';
import { getCurrentGeoLocation, GeoLocationResult } from '../utils/geoUtils';
import { formatDateOnly } from '../utils/dateUtils';
import { CpvStatusBadge } from '../components/StatusBadge';
import {
  Save,
  X,
  Upload,
  FileText,
  Trash2,
  Eye,
  CheckSquare,
  Square,
  AlertCircle,
  MapPin,
  Navigation,
  ExternalLink,
  CheckCircle2
} from 'lucide-react';

interface CustomerFileFormProps {
  currentUser: User;
  editFileId?: string;
  onNavigate: (screen: ScreenId) => void;
  onSuccess: () => void;
}

export const CustomerFileForm: React.FC<CustomerFileFormProps> = ({
  currentUser,
  editFileId,
  onNavigate,
  onSuccess
}) => {
  const isEditing = Boolean(editFileId);
  const isPrivileged = currentUser.role === 'ADMIN' || currentUser.role === 'MENTOR';

  // Form states
  const [fileId, setFileId] = useState('');
  const [customerName, setCustomerName] = useState('');
  const [companyName, setCompanyName] = useState('');
  const [officeAddress, setOfficeAddress] = useState('');
  const [mobile, setMobile] = useState('');
  const [altMobile, setAltMobile] = useState('');
  const [email, setEmail] = useState('');
  const [assignedRmCode, setAssignedRmCode] = useState(currentUser.rmCode);

  const productOptions = ['Credit Card', 'B2B', 'Corporate Card', 'Split', 'Limit Enhancement'];
  const [productType, setProductType] = useState('Credit Card');

  const statusOptions: ApplicationStatus[] = [
    'Collected',
    'Submitted',
    'Approved',
    'Declined',
    'Query',
    'Return to Source',
    'Condition',
    'STC'
  ];
  const [applicationStatus, setApplicationStatus] = useState<ApplicationStatus>('Collected');

  const activeOptions: ActiveStatus[] = ['N', 'Y', 'C'];
  const [activeStatus, setActiveStatus] = useState<ActiveStatus>('N');

  const allPendingDocs = [
    'NID',
    'TIN',
    'Office ID',
    'Salary Certificate',
    'BS (Bank Statement)',
    'BIN',
    'Trade License 2024-25',
    'Trade License 2025-26',
    'Trade License 2026-27',
    'Loan Certificate',
    'Card Statement (Month)',
    'Card Copy'
  ];
  const [pendingDocuments, setPendingDocuments] = useState<string[]>([]);

  const [remarks, setRemarks] = useState('');

  // CPV
  const cpvStatusOptions: CpvStatus[] = ['Pending', 'Completed', 'Failed', 'Not Required'];
  const [cpvStatus, setCpvStatus] = useState<CpvStatus>('Pending');
  const [cpvDate, setCpvDate] = useState(new Date().toISOString().split('T')[0]);
  const [cpvAddress, setCpvAddress] = useState('');
  const [cpvRemarks, setCpvRemarks] = useState('');

  // Attachments
  const [attachments, setAttachments] = useState<FileAttachment[]>([]);
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [uploadCategory, setUploadCategory] = useState('NID');
  const [uploadFileName, setUploadFileName] = useState('');
  const [previewDoc, setPreviewDoc] = useState<FileAttachment | null>(null);

  const [error, setError] = useState<string | null>(null);
  const [originalFile, setOriginalFile] = useState<CustomerFile | null>(null);

  // GPS auto-detection states
  const [detectingOfficeGeo, setDetectingOfficeGeo] = useState(false);
  const [officeGeo, setOfficeGeo] = useState<GeoLocationResult | null>(null);

  const [detectingCpvGeo, setDetectingCpvGeo] = useState(false);
  const [cpvGeo, setCpvGeo] = useState<GeoLocationResult | null>(null);

  const handleDetectOfficeLocation = async () => {
    setDetectingOfficeGeo(true);
    setError(null);
    try {
      const res = await getCurrentGeoLocation();
      setOfficeGeo(res);
      setOfficeAddress(res.address);
      storageService.updateRmLocation(
        assignedRmCode,
        res.latitude,
        res.longitude,
        res.address,
        `Tagged Office Location for ${customerName || 'Customer File'}`
      );
    } catch (err: any) {
      setError(`Location Detection: ${err.message || 'Could not retrieve coordinates'}`);
    } finally {
      setDetectingOfficeGeo(false);
    }
  };

  const handleDetectCpvLocation = async () => {
    setDetectingCpvGeo(true);
    setError(null);
    try {
      const res = await getCurrentGeoLocation();
      setCpvGeo(res);
      setCpvAddress(res.address);
      storageService.updateRmLocation(
        assignedRmCode,
        res.latitude,
        res.longitude,
        res.address,
        `CPV Physical Field Inspection for ${customerName || 'Customer File'}`
      );
    } catch (err: any) {
      setError(`CPV Location Detection: ${err.message || 'Could not retrieve coordinates'}`);
    } finally {
      setDetectingCpvGeo(false);
    }
  };

  useEffect(() => {
    if (editFileId) {
      const files = storageService.getCustomerFiles();
      const existing = files.find(f => f.fileId === editFileId);
      if (existing) {
        setOriginalFile(existing);
        setFileId(existing.fileId);
        setCustomerName(existing.customerName);
        setCompanyName(existing.companyName);
        setOfficeAddress(existing.officeAddress);
        setMobile(existing.mobile);
        setAltMobile(existing.altMobile || '');
        setEmail(existing.email || '');
        setAssignedRmCode(existing.assignedRmCode);
        setProductType(existing.productType);
        setApplicationStatus(existing.applicationStatus);
        setActiveStatus(existing.activeStatus);
        setPendingDocuments(existing.pendingDocuments || []);
        setRemarks(existing.remarks || '');
        setCpvStatus(existing.cpvStatus);
        setCpvDate(existing.cpvDate || '');
        setCpvAddress(existing.cpvAddress || '');
        setCpvRemarks(existing.cpvRemarks || '');

        const atts = storageService.getAttachments().filter(a => a.fileId === editFileId);
        setAttachments(atts);
      }
    } else {
      setFileId(generateFileId(currentUser.rmCode));
      setAssignedRmCode(currentUser.rmCode);
    }
  }, [editFileId, currentUser.rmCode]);

  const togglePendingDoc = (doc: string) => {
    if (pendingDocuments.includes(doc)) {
      setPendingDocuments(pendingDocuments.filter(d => d !== doc));
    } else {
      setPendingDocuments([...pendingDocuments, doc]);
    }
  };

  const handleAddAttachment = (e: React.FormEvent) => {
    e.preventDefault();
    const finalName = uploadFileName.trim() || `${uploadCategory}_doc.pdf`;
    const isPdf = finalName.endsWith('.pdf');
    const newAtt: FileAttachment = {
      attachmentId: 'ATT-' + generateId(),
      fileId,
      category: uploadCategory,
      fileName: finalName,
      fileType: isPdf ? 'application/pdf' : 'image/jpeg',
      storagePath: `files/${assignedRmCode}/${fileId}/${finalName}`,
      fileUri: '',
      fileSizeBytes: Math.floor(250 + Math.random() * 800) * 1024,
      uploadedBy: currentUser.rmCode,
      uploadedAt: Date.now()
    };

    const allAtts = storageService.getAttachments();
    allAtts.push(newAtt);
    storageService.saveAttachments(allAtts);
    setAttachments([...attachments, newAtt]);
    setShowUploadModal(false);
    setUploadFileName('');
  };

  const handleDeleteAttachment = (attId: string) => {
    const allAtts = storageService.getAttachments().filter(a => a.attachmentId !== attId);
    storageService.saveAttachments(allAtts);
    setAttachments(attachments.filter(a => a.attachmentId !== attId));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!customerName.trim() || !companyName.trim() || !mobile.trim()) {
      setError('Please fill in Customer Name, Company Name, and Mobile Number.');
      return;
    }

    const now = Date.now();
    const resolvedRmCode = isPrivileged ? assignedRmCode : currentUser.rmCode;

    const fileData: CustomerFile = {
      fileId,
      customerName: customerName.trim(),
      companyName: companyName.trim(),
      officeAddress: officeAddress.trim(),
      mobile: mobile.trim(),
      altMobile: altMobile.trim() || undefined,
      email: email.trim() || undefined,
      productType,
      applicationStatus,
      activeStatus,
      assignedRmCode: resolvedRmCode,
      pendingDocuments,
      remarks: remarks.trim(),
      cpvStatus,
      cpvDate,
      cpvAddress: cpvAddress.trim(),
      cpvRemarks: cpvRemarks.trim(),
      cpvLastUpdatedBy: currentUser.rmCode,
      createdAt: originalFile ? originalFile.createdAt : now,
      updatedAt: now,
      submittedAt: applicationStatus === 'Submitted' ? (originalFile?.submittedAt || now) : originalFile?.submittedAt,
      approvedAt: applicationStatus === 'Approved' ? (originalFile?.approvedAt || now) : originalFile?.approvedAt,
      createdBy: originalFile ? originalFile.createdBy : currentUser.rmCode,
      updatedBy: currentUser.rmCode,
      isDeleted: false,
      isSynced: false
    };

    const files = storageService.getCustomerFiles();
    const existingIdx = files.findIndex(f => f.fileId === fileId);

    if (existingIdx >= 0) {
      files[existingIdx] = fileData;
      storageService.addAuditLog('FILE_UPDATE', `Updated file ${fileId} (${customerName})`, fileId, resolvedRmCode);
    } else {
      files.unshift(fileData);
      storageService.addAuditLog('FILE_CREATE', `Created file ${fileId} for ${customerName}`, fileId, resolvedRmCode);
    }

    storageService.saveCustomerFiles(files);
    onSuccess();
  };

  const rms = storageService.getUsers().filter(u => u.role === 'RM');

  return (
    <div className="max-w-4xl mx-auto space-y-6 pb-12 text-xs">
      {/* Top Banner */}
      <div className="bg-ebl-navy-dark text-white rounded-2xl p-6 shadow-md border border-ebl-navy-primary flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold">{isEditing ? 'Edit Customer File' : 'New Customer File Entry'}</h1>
          <p className="text-xs text-slate-300 mt-1">
            File ID: <span className="font-mono text-ebl-gold font-bold">{fileId}</span> • RM: {assignedRmCode}
          </p>
        </div>
        <button
          onClick={() => onNavigate('customer_list')}
          className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      {error && (
        <div className="bg-rose-50 border border-rose-200 text-rose-700 p-3.5 rounded-xl font-medium flex items-center space-x-2">
          <AlertCircle className="w-4 h-4 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* SECTION A: Customer Information */}
        <div className="bg-white rounded-xl p-5 border border-slate-200 shadow-sm space-y-4">
          <h3 className="font-bold text-sm text-ebl-navy-dark border-b border-slate-100 pb-2">
            A. Customer & Employment Information
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block font-semibold text-slate-700 mb-1">Customer Full Name *</label>
              <input
                type="text"
                value={customerName}
                onChange={e => setCustomerName(e.target.value)}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none"
                placeholder="e.g. Mahbubur Rahman"
                required
              />
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">Office / Company Name *</label>
              <input
                type="text"
                value={companyName}
                onChange={e => setCompanyName(e.target.value)}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none"
                placeholder="e.g. Square Pharmaceuticals Ltd"
                required
              />
            </div>

            <div className="sm:col-span-2">
              <label className="block font-semibold text-slate-700 mb-1">Office Address</label>
              <input
                type="text"
                value={officeAddress}
                onChange={e => setOfficeAddress(e.target.value)}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none"
                placeholder="e.g. Square Centre, 48 Mohakhali C/A, Dhaka"
              />
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">Mobile Number *</label>
              <input
                type="tel"
                value={mobile}
                onChange={e => setMobile(e.target.value)}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none"
                placeholder="e.g. 01713001122"
                required
              />
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">Alternative Mobile Number</label>
              <input
                type="tel"
                value={altMobile}
                onChange={e => setAltMobile(e.target.value)}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none"
                placeholder="Optional"
              />
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">Email Address</label>
              <input
                type="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none"
                placeholder="Optional"
              />
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">Assigned RM Code</label>
              {isPrivileged ? (
                <select
                  value={assignedRmCode}
                  onChange={e => setAssignedRmCode(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none bg-white font-medium"
                >
                  {rms.map(r => (
                    <option key={r.rmCode} value={r.rmCode}>
                      {r.rmCode} - {r.name}
                    </option>
                  ))}
                </select>
              ) : (
                <input
                  type="text"
                  value={currentUser.rmCode}
                  disabled
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg bg-slate-100 text-slate-600 font-semibold"
                />
              )}
            </div>
          </div>
        </div>

        {/* SECTION B, C, D: Product & Status */}
        <div className="bg-white rounded-xl p-5 border border-slate-200 shadow-sm space-y-4">
          <h3 className="font-bold text-sm text-ebl-navy-dark border-b border-slate-100 pb-2">
            B, C, D. Product Type & Status Classification
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block font-semibold text-slate-700 mb-1">Product Type</label>
              <select
                value={productType}
                onChange={e => setProductType(e.target.value)}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none bg-white"
              >
                {productOptions.map(p => (
                  <option key={p} value={p}>{p}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">Application Status</label>
              <select
                value={applicationStatus}
                onChange={e => setApplicationStatus(e.target.value as ApplicationStatus)}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none bg-white font-medium"
              >
                {statusOptions.map(s => (
                  <option key={s} value={s}>{s}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">Active Status (Y/N/C)</label>
              <select
                value={activeStatus}
                onChange={e => setActiveStatus(e.target.value as ActiveStatus)}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none bg-white"
              >
                <option value="N">N - Inactive / Processing</option>
                <option value="Y">Y - Active Card</option>
                <option value="C">C - Cancelled / Closed</option>
              </select>
            </div>
          </div>
        </div>

        {/* SECTION E: Pending Documents Checklist */}
        <div className="bg-white rounded-xl p-5 border border-slate-200 shadow-sm space-y-3">
          <div className="flex items-center justify-between border-b border-slate-100 pb-2">
            <h3 className="font-bold text-sm text-ebl-navy-dark">
              E. Pending Documents Checklist ({pendingDocuments.length} Pending)
            </h3>
            <span className="text-[11px] text-slate-400">Select any missing documentation</span>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-2 pt-1">
            {allPendingDocs.map(doc => {
              const isChecked = pendingDocuments.includes(doc);
              return (
                <button
                  key={doc}
                  type="button"
                  onClick={() => togglePendingDoc(doc)}
                  className={`flex items-center space-x-2 p-2 rounded-lg border text-left transition ${
                    isChecked
                      ? 'bg-amber-50 border-amber-300 text-amber-900 font-semibold'
                      : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'
                  }`}
                >
                  {isChecked ? (
                    <CheckSquare className="w-4 h-4 text-amber-600 flex-shrink-0" />
                  ) : (
                    <Square className="w-4 h-4 text-slate-300 flex-shrink-0" />
                  )}
                  <span className="truncate">{doc}</span>
                </button>
              );
            })}
          </div>
        </div>

        {/* SECTION F: Remarks */}
        <div className="bg-white rounded-xl p-5 border border-slate-200 shadow-sm space-y-2">
          <h3 className="font-bold text-sm text-ebl-navy-dark">F. Remarks / Requirements</h3>
          <textarea
            rows={3}
            value={remarks}
            onChange={e => setRemarks(e.target.value)}
            className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none"
            placeholder="Follow-up notes, missing requirements, customer queries, or special instructions..."
          />
        </div>

        {/* SECTION G: Attachments */}
        <div className="bg-white rounded-xl p-5 border border-slate-200 shadow-sm space-y-3">
          <div className="flex items-center justify-between border-b border-slate-100 pb-2">
            <h3 className="font-bold text-sm text-ebl-navy-dark">
              G. Document & Supporting Files ({attachments.length})
            </h3>
            <button
              type="button"
              onClick={() => setShowUploadModal(true)}
              className="flex items-center space-x-1.5 px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg font-semibold text-xs"
            >
              <Upload className="w-3.5 h-3.5" />
              <span>Attach File</span>
            </button>
          </div>

          {attachments.length === 0 ? (
            <p className="text-slate-400 italic py-2">No attachments yet. Tap "Attach File" to upload NID, Statements, etc.</p>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {attachments.map(att => (
                <div
                  key={att.attachmentId}
                  className="flex items-center justify-between p-2.5 bg-slate-50 border border-slate-200 rounded-lg"
                >
                  <div className="flex items-center space-x-2 truncate">
                    <FileText className="w-4 h-4 text-ebl-navy-primary flex-shrink-0" />
                    <div className="truncate">
                      <p className="font-semibold text-slate-800 truncate">{att.fileName}</p>
                      <p className="text-[10px] text-slate-400">
                        {att.category} • {(att.fileSizeBytes / 1024).toFixed(0)} KB
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center space-x-1">
                    <button
                      type="button"
                      onClick={() => setPreviewDoc(att)}
                      className="p-1 text-slate-500 hover:text-slate-800"
                      title="Preview"
                    >
                      <Eye className="w-4 h-4" />
                    </button>
                    <button
                      type="button"
                      onClick={() => handleDeleteAttachment(att.attachmentId)}
                      className="p-1 text-rose-500 hover:text-rose-700"
                      title="Delete"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* SECTION H: CPV Section */}
        <div className="bg-white rounded-xl p-5 border border-slate-200 shadow-sm space-y-4">
          <div className="flex items-center justify-between border-b border-slate-100 pb-2">
            <h3 className="font-bold text-sm text-ebl-navy-dark">H. Contact Point Verification (CPV)</h3>
            <CpvStatusBadge status={cpvStatus} />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block font-semibold text-slate-700 mb-1">CPV Status</label>
              <select
                value={cpvStatus}
                onChange={e => setCpvStatus(e.target.value as CpvStatus)}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none bg-white font-medium"
              >
                {cpvStatusOptions.map(c => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">CPV Verification Date</label>
              <input
                type="date"
                value={cpvDate}
                onChange={e => setCpvDate(e.target.value)}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none"
              />
            </div>

            <div className="sm:col-span-2">
              <label className="block font-semibold text-slate-700 mb-1">CPV Visited Address</label>
              <input
                type="text"
                value={cpvAddress}
                onChange={e => setCpvAddress(e.target.value)}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none"
                placeholder="e.g. Office Address verified in person"
              />
            </div>

            <div className="sm:col-span-2">
              <label className="block font-semibold text-slate-700 mb-1">CPV Remarks / Verification Details</label>
              <input
                type="text"
                value={cpvRemarks}
                onChange={e => setCpvRemarks(e.target.value)}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none"
                placeholder="HR confirmed designation, visiting card verified..."
              />
            </div>
          </div>
        </div>

        {/* Submit Actions */}
        <div className="flex items-center justify-end space-x-3 pt-4">
          <button
            type="button"
            onClick={() => onNavigate('customer_list')}
            className="px-5 py-2.5 border border-slate-300 rounded-xl hover:bg-slate-100 font-semibold text-slate-700 transition"
          >
            Cancel
          </button>
          <button
            type="submit"
            className="px-6 py-2.5 bg-ebl-navy-primary hover:bg-ebl-navy-secondary text-white rounded-xl font-bold transition flex items-center space-x-2 shadow-md hover:shadow-lg"
          >
            <Save className="w-4 h-4" />
            <span>{isEditing ? 'Update Record' : 'Save Customer File'}</span>
          </button>
        </div>
      </form>

      {/* Upload Attachment Modal */}
      {showUploadModal && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-fadeIn">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-200">
            <h3 className="font-bold text-sm text-slate-800 mb-4">Attach Customer Document</h3>
            <form onSubmit={handleAddAttachment} className="space-y-4">
              <div>
                <label className="block font-semibold text-slate-700 mb-1">Document Category</label>
                <select
                  value={uploadCategory}
                  onChange={e => setUploadCategory(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none bg-white"
                >
                  <option value="NID">NID / Smart Card</option>
                  <option value="Bank Statement">Bank Statement</option>
                  <option value="Salary Certificate">Salary Certificate</option>
                  <option value="Office ID">Office ID</option>
                  <option value="Trade License">Trade License</option>
                  <option value="CPV Photo">CPV Physical Photo</option>
                  <option value="General">General Supporting</option>
                </select>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">File Name</label>
                <input
                  type="text"
                  value={uploadFileName}
                  onChange={e => setUploadFileName(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none"
                  placeholder="e.g. NID_Mahbubur_Front_Back.pdf"
                />
              </div>

              <div className="flex justify-end space-x-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowUploadModal(false)}
                  className="px-4 py-2 border border-slate-300 rounded-lg text-slate-600 hover:bg-slate-50 font-medium"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-ebl-navy-primary text-white rounded-lg font-bold hover:bg-ebl-navy-secondary"
                >
                  Attach
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Doc Preview Modal */}
      {previewDoc && (
        <div className="fixed inset-0 bg-slate-900/70 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-2xl border border-slate-200">
            <div className="flex justify-between items-center mb-4">
              <h3 className="font-bold text-sm text-slate-800">{previewDoc.fileName}</h3>
              <button onClick={() => setPreviewDoc(null)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="h-44 bg-slate-100 border border-slate-200 rounded-lg flex flex-col items-center justify-center text-slate-500">
              <FileText className="w-12 h-12 text-ebl-navy-primary mb-2" />
              <p className="font-semibold text-xs text-slate-700">{previewDoc.category}</p>
              <p className="text-[11px] text-slate-400">Encrypted Storage Reference</p>
            </div>
            <div className="mt-4 flex justify-end">
              <button
                onClick={() => setPreviewDoc(null)}
                className="px-4 py-1.5 bg-ebl-navy-primary text-white rounded-lg font-semibold"
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
