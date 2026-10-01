import React, { useState } from 'react';
import { User } from '../types';
import { storageService } from '../services/storageService';
import { verifyPassword } from '../utils/securityUtils';
import { Lock, User as UserIcon, Shield, Eye, EyeOff } from 'lucide-react';

interface LoginScreenProps {
  onLoginSuccess: (user: User) => void;
}

export const LoginScreen: React.FC<LoginScreenProps> = ({ onLoginSuccess }) => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password.trim()) {
      setError('Please provide both RM Code / Username and password.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const users = storageService.getUsers();
      const user = users.find(u => u.rmCode.toLowerCase() === username.trim().toLowerCase());

      if (!user) {
        setError('Invalid credentials. Please verify your RM Code / Username.');
        setLoading(false);
        return;
      }

      if (user.accountStatus === 'INACTIVE') {
        setError('This account is inactive. Please contact bank administration.');
        setLoading(false);
        return;
      }
      if (user.accountStatus === 'SUSPENDED') {
        setError('This account is suspended due to security compliance.');
        setLoading(false);
        return;
      }

      const isValid = await verifyPassword(password.trim(), user.salt, user.passwordHash);
      if (!isValid) {
        setError('Invalid credentials. Please verify your password.');
        setLoading(false);
        return;
      }

      // Update last login
      user.lastLogin = Date.now();
      const userIdx = users.findIndex(u => u.rmCode === user.rmCode);
      if (userIdx >= 0) {
        users[userIdx] = user;
        storageService.saveUsers(users);
      }

      storageService.setCurrentUser(user);
      storageService.addAuditLog('LOGIN', `User ${user.name} (${user.rmCode}) logged in successfully.`);

      onLoginSuccess(user);
    } catch (err: any) {
      setError(err.message || 'Login failed due to a system error.');
      setLoading(false);
    }
  };

  const handleQuickFill = (u: string, p: string) => {
    setUsername(u);
    setPassword(p);
    setError(null);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-ebl-navy-dark via-ebl-navy-primary to-slate-900 flex flex-col justify-center items-center p-4">
      {/* Container */}
      <div className="max-w-md w-full">
        {/* Brand Card */}
        <div className="text-center mb-6">
          <div className="inline-flex w-16 h-16 rounded-full bg-ebl-gold items-center justify-center font-black text-ebl-navy-dark text-2xl shadow-lg border-2 border-white/20 mb-3">
            EBL
          </div>
          <h1 className="text-2xl font-black text-white tracking-tight">Eastern Bank PLC</h1>
          <p className="text-xs text-ebl-gold font-medium mt-0.5">
            RM File Management & Performance Dashboard
          </p>
        </div>

        {/* Login Box */}
        <div className="bg-white rounded-2xl shadow-2xl p-6 sm:p-8 border border-slate-200">
          <div className="mb-6">
            <h2 className="text-lg font-bold text-ebl-navy-dark">Sign In to Portal</h2>
            <p className="text-xs text-slate-500">Universal authentication for RM, Admin, and Mentor</p>
          </div>

          {error && (
            <div className="mb-4 bg-rose-50 border border-rose-200 text-rose-700 p-3 rounded-lg text-xs font-medium animate-shake">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4 text-xs">
            <div>
              <label className="block font-semibold text-slate-700 mb-1">
                RM Code / Username
              </label>
              <div className="relative">
                <input
                  type="text"
                  value={username}
                  onChange={e => setUsername(e.target.value)}
                  className="w-full pl-9 pr-3 py-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none"
                  placeholder="e.g. 104393 or Admin0"
                  autoFocus
                />
                <UserIcon className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
              </div>
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">
                Password
              </label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  className="w-full pl-9 pr-10 py-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none"
                  placeholder="Enter password"
                />
                <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-3 text-slate-400 hover:text-slate-600"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 bg-ebl-navy-primary text-white rounded-lg hover:bg-ebl-navy-secondary font-bold text-sm transition shadow-md hover:shadow-lg disabled:opacity-50"
            >
              {loading ? 'Authenticating...' : 'Sign In'}
            </button>
          </form>

          {/* Quick Demo Access Pills */}
          <div className="mt-6 pt-5 border-t border-slate-100">
            <span className="block text-[11px] text-slate-400 font-semibold uppercase tracking-wider mb-2 text-center">
              Quick Demo Access
            </span>
            <div className="grid grid-cols-3 gap-2">
              <button
                type="button"
                onClick={() => handleQuickFill('104393', 'password123')}
                className="py-1.5 px-2 bg-slate-50 hover:bg-slate-100 border border-slate-200 rounded text-[11px] text-slate-700 font-medium text-center truncate"
                title="RM 104393"
              >
                RM: 104393
              </button>
              <button
                type="button"
                onClick={() => handleQuickFill('Admin0', '#123456A')}
                className="py-1.5 px-2 bg-slate-50 hover:bg-slate-100 border border-slate-200 rounded text-[11px] text-slate-700 font-medium text-center truncate"
                title="Admin0"
              >
                Admin0
              </button>
              <button
                type="button"
                onClick={() => handleQuickFill('12345', '12345')}
                className="py-1.5 px-2 bg-slate-50 hover:bg-slate-100 border border-slate-200 rounded text-[11px] text-slate-700 font-medium text-center truncate"
                title="Mentor 12345"
              >
                Mentor: 12345
              </button>
            </div>
          </div>
        </div>

        {/* Security Notice Footer */}
        <div className="text-center mt-6 text-slate-300 text-xs flex items-center justify-center space-x-1">
          <Shield className="w-3.5 h-3.5 text-ebl-gold" />
          <span>EBL Bank 256-Bit Encrypted Session • Asia/Dhaka BST Timezone</span>
        </div>
      </div>
    </div>
  );
};
