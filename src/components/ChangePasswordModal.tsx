import React, { useState } from 'react';
import { User } from '../types';
import { storageService } from '../services/storageService';
import { verifyPassword, hashPassword, generateSalt } from '../utils/securityUtils';
import { Lock, Eye, EyeOff, X } from 'lucide-react';

interface ChangePasswordModalProps {
  user: User;
  isForced?: boolean;
  onClose: () => void;
  onSuccess: (updatedUser: User) => void;
}

export const ChangePasswordModal: React.FC<ChangePasswordModalProps> = ({
  user,
  isForced = false,
  onClose,
  onSuccess
}) => {
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [showOld, setShowOld] = useState(false);
  const [showNew, setShowNew] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!oldPassword || !newPassword || !confirmPassword) {
      setError('Please fill in all fields.');
      return;
    }
    if (newPassword.length < 6) {
      setError('New password must be at least 6 characters.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('New password and confirm password do not match.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const isValid = await verifyPassword(oldPassword, user.salt, user.passwordHash);
      if (!isValid) {
        setError('Current password is incorrect.');
        setLoading(false);
        return;
      }

      const newSalt = generateSalt();
      const newHash = await hashPassword(newPassword, newSalt);

      const users = storageService.getUsers();
      const idx = users.findIndex(u => u.rmCode === user.rmCode);
      const updatedUser: User = {
        ...user,
        passwordHash: newHash,
        salt: newSalt,
        mustChangePassword: false
      };

      if (idx >= 0) {
        users[idx] = updatedUser;
        storageService.saveUsers(users);
      }
      storageService.setCurrentUser(updatedUser);
      storageService.addAuditLog('PASSWORD_CHANGE', `User ${user.rmCode} updated password.`);

      onSuccess(updatedUser);
    } catch (err: any) {
      setError(err.message || 'Failed to update password.');
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-fadeIn">
      <div className="bg-white rounded-2xl max-w-md w-full shadow-2xl overflow-hidden border border-slate-200">
        <div className="bg-ebl-navy-dark px-6 py-4 flex items-center justify-between text-white">
          <div className="flex items-center space-x-2">
            <Lock className="w-5 h-5 text-ebl-gold" />
            <h3 className="font-bold text-base">
              {isForced ? 'Password Update Required' : 'Change Password'}
            </h3>
          </div>
          {!isForced && (
            <button onClick={onClose} className="text-slate-400 hover:text-white">
              <X className="w-5 h-5" />
            </button>
          )}
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4 text-xs">
          {isForced && (
            <div className="bg-amber-50 border border-amber-200 text-amber-800 p-3 rounded-lg text-xs">
              For initial security compliance, you must set a new individual password before accessing the system.
            </div>
          )}

          {error && (
            <div className="bg-rose-50 border border-rose-200 text-rose-700 p-3 rounded-lg text-xs font-medium">
              {error}
            </div>
          )}

          <div>
            <label className="block font-semibold text-slate-700 mb-1">Current Password</label>
            <div className="relative">
              <input
                type={showOld ? 'text' : 'password'}
                value={oldPassword}
                onChange={e => setOldPassword(e.target.value)}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none pr-10"
                placeholder="Enter current password"
              />
              <button
                type="button"
                onClick={() => setShowOld(!showOld)}
                className="absolute right-3 top-2.5 text-slate-400 hover:text-slate-600"
              >
                {showOld ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>

          <div>
            <label className="block font-semibold text-slate-700 mb-1">New Password (min 6 characters)</label>
            <div className="relative">
              <input
                type={showNew ? 'text' : 'password'}
                value={newPassword}
                onChange={e => setNewPassword(e.target.value)}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none pr-10"
                placeholder="Enter new password"
              />
              <button
                type="button"
                onClick={() => setShowNew(!showNew)}
                className="absolute right-3 top-2.5 text-slate-400 hover:text-slate-600"
              >
                {showNew ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>

          <div>
            <label className="block font-semibold text-slate-700 mb-1">Confirm New Password</label>
            <input
              type={showNew ? 'text' : 'password'}
              value={confirmPassword}
              onChange={e => setConfirmPassword(e.target.value)}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none"
              placeholder="Re-type new password"
            />
          </div>

          <div className="pt-2 flex justify-end space-x-2">
            {!isForced && (
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 border border-slate-300 rounded-lg hover:bg-slate-50 font-medium text-slate-700"
              >
                Cancel
              </button>
            )}
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-2 bg-ebl-navy-primary text-white rounded-lg hover:bg-ebl-navy-secondary font-bold transition flex items-center justify-center min-w-[120px]"
            >
              {loading ? 'Updating...' : 'Save Password'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
