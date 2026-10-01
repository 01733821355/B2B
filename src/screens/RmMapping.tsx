import React, { useState } from 'react';
import { User, AccountStatus, ScreenId } from '../types';
import { storageService } from '../services/storageService';
import { generateSalt, hashPassword, generateTemporaryPassword } from '../utils/securityUtils';
import { formatDateOnly, formatDateTime } from '../utils/dateUtils';
import { AccountStatusBadge } from '../components/StatusBadge';
import {
  Users,
  Plus,
  Search,
  Download,
  Printer,
  Edit,
  Key,
  Shield,
  X,
  Check,
  Ban
} from 'lucide-react';

interface RmMappingProps {
  currentUser: User;
  onNavigate: (screen: ScreenId) => void;
}

export const RmMapping: React.FC<RmMappingProps> = ({ currentUser }) => {
  const [rms, setRms] = useState<User[]>(() =>
    storageService.getUsers().filter(u => u.role === 'RM')
  );
  const [searchQuery, setSearchQuery] = useState('');

  // Modals
  const [showAddModal, setShowAddModal] = useState(false);
  const [editingRm, setEditingRm] = useState<User | null>(null);
  const [resettingRm, setResettingRm] = useState<User | null>(null);
  const [tempPasswordGenerated, setTempPasswordGenerated] = useState('');
  const [showExportModal, setShowExportModal] = useState(false);
  const [csvContent, setCsvContent] = useState('');

  // Form fields for Add RM
  const [newRmCode, setNewRmCode] = useState('');
  const [newName, setNewName] = useState('');
  const [newMobile, setNewMobile] = useState('');
  const [newEmail, setNewEmail] = useState('');
  const [newAddress, setNewAddress] = useState('');
  const [addError, setAddError] = useState<string | null>(null);

  const refreshRms = () => {
    setRms(storageService.getUsers().filter(u => u.role === 'RM'));
  };

  const handleCreateRm = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newRmCode.trim() || !newName.trim()) {
      setAddError('RM Code and Full Name are mandatory.');
      return;
    }

    const allUsers = storageService.getUsers();
    if (allUsers.some(u => u.rmCode.toLowerCase() === newRmCode.trim().toLowerCase())) {
      setAddError(`RM Code "${newRmCode}" is already in use.`);
      return;
    }

    const initialPassword = generateTemporaryPassword();
    const salt = generateSalt();
    const hash = await hashPassword(initialPassword, salt);

    const newUser: User = {
      rmCode: newRmCode.trim(),
      name: newName.trim(),
      role: 'RM',
      passwordHash: hash,
      salt,
      mobile: newMobile.trim(),
      email: newEmail.trim(),
      officeAddress: newAddress.trim() || 'EBL Branch',
      accountStatus: 'ACTIVE',
      mustChangePassword: true,
      createdAt: Date.now(),
      authUid: `AUTH_RM_${newRmCode.trim()}`
    };

    allUsers.push(newUser);
    storageService.saveUsers(allUsers);
    storageService.addAuditLog('RM_CREATE', `Created and activated RM ${newUser.rmCode} (${newUser.name})`);

    refreshRms();
    setTempPasswordGenerated(initialPassword);
    setNewRmCode('');
    setNewName('');
    setNewMobile('');
    setNewEmail('');
    setNewAddress('');
  };

  const handleUpdateStatus = (rmCode: string, newStatus: AccountStatus) => {
    const allUsers = storageService.getUsers();
    const idx = allUsers.findIndex(u => u.rmCode === rmCode);
    if (idx >= 0) {
      allUsers[idx].accountStatus = newStatus;
      storageService.saveUsers(allUsers);
      storageService.addAuditLog('RM_STATUS_CHANGE', `Changed RM ${rmCode} status to ${newStatus}`);
      refreshRms();
    }
  };

  const handleResetPassword = async (rm: User) => {
    const tempPass = generateTemporaryPassword();
    const salt = generateSalt();
    const hash = await hashPassword(tempPass, salt);

    const allUsers = storageService.getUsers();
    const idx = allUsers.findIndex(u => u.rmCode === rm.rmCode);
    if (idx >= 0) {
      allUsers[idx].passwordHash = hash;
      allUsers[idx].salt = salt;
      allUsers[idx].mustChangePassword = true;
      storageService.saveUsers(allUsers);
      storageService.addAuditLog('PASSWORD_RESET', `Admin reset password for RM ${rm.rmCode}`);
      refreshRms();
      setTempPasswordGenerated(tempPass);
      setResettingRm(null);
    }
  };

  const handleExportCsv = () => {
    const headers = ['RM_CODE', 'RM_NAME', 'MOBILE', 'EMAIL', 'OFFICE_ADDRESS', 'ACCOUNT_STATUS', 'CREATED_AT', 'LAST_LOGIN'];
    const rows = filteredRms.map(r => [
      `"${r.rmCode}"`,
      `"${r.name}"`,
      `"${r.mobile}"`,
      `"${r.email || ''}"`,
      `"${r.officeAddress}"`,
      `"${r.accountStatus}"`,
      `"${formatDateTime(r.createdAt)}"`,
      `"${formatDateTime(r.lastLogin)}"`
    ]);

    const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
    setCsvContent(csv);
    setShowExportModal(true);
  };

  const handlePrint = () => {
    window.print();
  };

  const filteredRms = rms.filter(r => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase().trim();
    return r.name.toLowerCase().includes(q) || r.rmCode.toLowerCase().includes(q) || r.mobile.includes(q);
  });

  return (
    <div className="space-y-6 text-xs">
      {/* Top Banner */}
      <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2">
            <Users className="w-5 h-5 text-ebl-navy-primary" />
            <h1 className="text-base font-bold text-ebl-navy-dark">RM Mapping & Account Provisioning</h1>
          </div>
          <p className="text-slate-500 mt-0.5">
            Manage relationship officer codes, access provisioning, and credential issuance
          </p>
        </div>

        <div className="flex items-center space-x-2 no-print">
          <button
            onClick={handleExportCsv}
            className="flex items-center space-x-1.5 px-3 py-2 border border-slate-300 rounded-lg hover:bg-slate-50 font-semibold text-slate-700 transition"
          >
            <Download className="w-4 h-4" />
            <span>Export CSV</span>
          </button>
          <button
            onClick={handlePrint}
            className="flex items-center space-x-1.5 px-3 py-2 border border-slate-300 rounded-lg hover:bg-slate-50 font-semibold text-slate-700 transition"
          >
            <Printer className="w-4 h-4" />
            <span>Print</span>
          </button>
          <button
            onClick={() => {
              setTempPasswordGenerated('');
              setAddError(null);
              setShowAddModal(true);
            }}
            className="flex items-center space-x-1.5 px-4 py-2 bg-ebl-navy-primary text-white rounded-lg font-bold hover:bg-ebl-navy-secondary transition shadow-sm"
          >
            <Plus className="w-4 h-4" />
            <span>Add RM</span>
          </button>
        </div>
      </div>

      {/* Temporary Password Announcement Alert */}
      {tempPasswordGenerated && (
        <div className="bg-emerald-50 border border-emerald-300 rounded-xl p-4 flex items-center justify-between text-emerald-900 shadow-sm">
          <div>
            <p className="font-bold">Initial / Temporary Password Generated:</p>
            <p className="text-sm font-mono font-bold bg-white px-3 py-1 rounded border border-emerald-200 inline-block mt-1">
              {tempPasswordGenerated}
            </p>
            <p className="text-[11px] text-emerald-700 mt-1">
              Provide this securely to the RM. The RM must change this password upon first login.
            </p>
          </div>
          <button
            onClick={() => setTempPasswordGenerated('')}
            className="text-emerald-700 hover:text-emerald-900"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
      )}

      {/* Search Input */}
      <div className="relative no-print">
        <input
          type="text"
          value={searchQuery}
          onChange={e => setSearchQuery(e.target.value)}
          placeholder="Search RM by Name, RM Code, or Mobile Number..."
          className="w-full pl-9 pr-4 py-2 bg-white border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none"
        />
        <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
      </div>

      {/* RM Mapping Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-slate-50 border-b border-slate-200 text-slate-600 font-bold text-[11px] uppercase tracking-wider">
              <th className="py-3 px-4">RM Code</th>
              <th className="py-3 px-4">RM Name</th>
              <th className="py-3 px-4">Mobile</th>
              <th className="py-3 px-4">Email</th>
              <th className="py-3 px-4">Branch / Office</th>
              <th className="py-3 px-4">Account Status</th>
              <th className="py-3 px-4">Created Date</th>
              <th className="py-3 px-4">Last Login</th>
              <th className="py-3 px-4 text-right no-print">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 text-xs">
            {filteredRms.map(rm => (
              <tr key={rm.rmCode} className="hover:bg-slate-50/80 transition">
                <td className="py-3 px-4 font-mono font-bold text-ebl-navy-primary">
                  {rm.rmCode}
                </td>
                <td className="py-3 px-4 font-semibold text-slate-800">
                  {rm.name}
                </td>
                <td className="py-3 px-4 text-slate-600">
                  {rm.mobile}
                </td>
                <td className="py-3 px-4 text-slate-600">
                  {rm.email || 'N/A'}
                </td>
                <td className="py-3 px-4 text-slate-700">
                  {rm.officeAddress}
                </td>
                <td className="py-3 px-4">
                  <AccountStatusBadge status={rm.accountStatus} />
                </td>
                <td className="py-3 px-4 text-slate-500">
                  {formatDateOnly(rm.createdAt)}
                </td>
                <td className="py-3 px-4 text-slate-400">
                  {formatDateTime(rm.lastLogin)}
                </td>
                <td className="py-3 px-4 text-right no-print">
                  <div className="flex items-center justify-end space-x-1">
                    <button
                      onClick={() => handleResetPassword(rm)}
                      className="px-2 py-1 rounded bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold"
                      title="Reset Password"
                    >
                      Reset Key
                    </button>
                    {rm.accountStatus === 'ACTIVE' ? (
                      <button
                        onClick={() => handleUpdateStatus(rm.rmCode, 'INACTIVE')}
                        className="px-2 py-1 rounded bg-rose-50 hover:bg-rose-100 text-rose-700 font-semibold"
                        title="Deactivate Account"
                      >
                        Deactivate
                      </button>
                    ) : (
                      <button
                        onClick={() => handleUpdateStatus(rm.rmCode, 'ACTIVE')}
                        className="px-2 py-1 rounded bg-emerald-50 hover:bg-emerald-100 text-emerald-700 font-semibold"
                        title="Activate Account"
                      >
                        Activate
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Add RM Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-fadeIn">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-200">
            <div className="flex justify-between items-center mb-4">
              <h3 className="font-bold text-sm text-ebl-navy-dark">Create New RM Account</h3>
              <button onClick={() => setShowAddModal(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            {addError && (
              <div className="mb-3 p-2.5 bg-rose-50 border border-rose-200 text-rose-700 rounded-lg text-xs">
                {addError}
              </div>
            )}

            <form onSubmit={handleCreateRm} className="space-y-3">
              <div>
                <label className="block font-semibold text-slate-700 mb-1">Unique RM Code *</label>
                <input
                  type="text"
                  value={newRmCode}
                  onChange={e => setNewRmCode(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none"
                  placeholder="e.g. 104396"
                  required
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">RM Full Name *</label>
                <input
                  type="text"
                  value={newName}
                  onChange={e => setNewName(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none"
                  placeholder="e.g. Anisul Haque"
                  required
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Mobile Number</label>
                <input
                  type="tel"
                  value={newMobile}
                  onChange={e => setNewMobile(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none"
                  placeholder="e.g. 01712345678"
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Corporate Email</label>
                <input
                  type="email"
                  value={newEmail}
                  onChange={e => setNewEmail(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none"
                  placeholder="e.g. anisul.ebl@ebl.com.bd"
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Office / Branch Location</label>
                <input
                  type="text"
                  value={newAddress}
                  onChange={e => setNewAddress(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none"
                  placeholder="e.g. EBL Banani Branch"
                />
              </div>

              <div className="pt-3 flex justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-4 py-2 border border-slate-300 rounded-lg text-slate-600 hover:bg-slate-50 font-medium"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 bg-ebl-navy-primary text-white rounded-lg font-bold hover:bg-ebl-navy-secondary shadow"
                >
                  Create Account
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* CSV Export Modal */}
      {showExportModal && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-slate-200">
            <div className="flex justify-between items-center mb-3">
              <h3 className="font-bold text-sm text-slate-800">RM Mapping Export (CSV)</h3>
              <button onClick={() => setShowExportModal(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>
            <p className="text-slate-500 text-xs mb-3">
              Copy the CSV content below or save it as an .xlsx / .csv file:
            </p>
            <textarea
              readOnly
              rows={8}
              value={csvContent}
              className="w-full p-3 font-mono text-[11px] bg-slate-50 border border-slate-300 rounded-lg"
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
