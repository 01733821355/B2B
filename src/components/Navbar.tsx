import React, { useState, useEffect } from 'react';
import { User, ScreenId } from '../types';
import { storageService } from '../services/storageService';
import { formatTimeOnly } from '../utils/dateUtils';
import {
  Shield,
  Key,
  LogOut,
  User as UserIcon,
  CloudCheck,
  CloudUpload,
  RefreshCw,
  Clock
} from 'lucide-react';

interface NavbarProps {
  user: User;
  onLogout: () => void;
  onChangePassword: () => void;
  onNavigate: (screen: ScreenId) => void;
  currentScreen: ScreenId;
}

export const Navbar: React.FC<NavbarProps> = ({
  user,
  onLogout,
  onChangePassword,
  onNavigate
}) => {
  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const [dhakaTime, setDhakaTime] = useState(formatTimeOnly());
  const syncStatus = storageService.getSyncStatus();

  useEffect(() => {
    const timer = setInterval(() => {
      setDhakaTime(formatTimeOnly());
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const getRoleBadgeColor = () => {
    switch (user.role) {
      case 'ADMIN':
        return 'bg-red-600 text-white';
      case 'MENTOR':
        return 'bg-purple-600 text-white';
      case 'RM':
      default:
        return 'bg-ebl-navy-secondary text-white';
    }
  };

  return (
    <header className="bg-ebl-navy-dark border-b border-ebl-navy-primary text-white sticky top-0 z-40 shadow-md">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Brand Logo & Title */}
          <div className="flex items-center space-x-3 cursor-pointer" onClick={() => onNavigate(user.role === 'ADMIN' ? 'admin_dashboard' : user.role === 'MENTOR' ? 'mentor_dashboard' : 'rm_dashboard')}>
            <div className="w-10 h-10 rounded-full bg-ebl-gold flex items-center justify-center font-black text-ebl-navy-dark text-sm tracking-wider shadow-inner">
              EBL
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <span className="font-bold text-base sm:text-lg tracking-tight">Eastern Bank PLC</span>
                <span className={`px-2 py-0.5 rounded text-[10px] font-bold tracking-wider uppercase ${getRoleBadgeColor()}`}>
                  {user.role}: {user.rmCode}
                </span>
              </div>
              <p className="text-xs text-slate-300 hidden sm:block">
                RM File Management & Performance Dashboard
              </p>
            </div>
          </div>

          {/* Right items: Dhaka Time, Sync status, Profile */}
          <div className="flex items-center space-x-2 sm:space-x-4">
            {/* Dhaka Time Clock */}
            <div className="hidden md:flex items-center space-x-1.5 bg-slate-800/80 px-2.5 py-1 rounded-md text-xs text-slate-200 border border-slate-700">
              <Clock className="w-3.5 h-3.5 text-ebl-gold" />
              <span>Dhaka: <strong className="font-mono">{dhakaTime}</strong></span>
            </div>

            {/* Google Sheets Sync Pill */}
            <button
              onClick={() => onNavigate('sheets_sync')}
              className="flex items-center space-x-1.5 px-2.5 py-1.5 rounded-md text-xs bg-slate-800/80 hover:bg-slate-700 border border-slate-700 text-slate-200 transition"
              title="Google Sheets Synchronization Status"
            >
              {syncStatus.pendingRecordsCount > 0 ? (
                <>
                  <CloudUpload className="w-4 h-4 text-amber-400 animate-pulse" />
                  <span className="hidden sm:inline">Pending:</span>
                  <span className="bg-amber-500 text-black font-bold text-[10px] px-1.5 py-0.2 rounded-full">
                    {syncStatus.pendingRecordsCount}
                  </span>
                </>
              ) : (
                <>
                  <span className="w-2 h-2 rounded-full bg-emerald-400"></span>
                  <span className="hidden sm:inline">Sheets Synced</span>
                </>
              )}
            </button>

            {/* User Profile Menu */}
            <div className="relative">
              <button
                onClick={() => setShowProfileMenu(!showProfileMenu)}
                className="flex items-center space-x-2 p-1.5 rounded-lg hover:bg-slate-800 text-slate-200 focus:outline-none"
              >
                <div className="w-8 h-8 rounded-full bg-ebl-navy-primary border border-slate-600 flex items-center justify-center text-ebl-gold font-bold text-xs">
                  {user.name.charAt(0)}
                </div>
                <div className="text-left hidden lg:block">
                  <div className="text-xs font-semibold leading-tight">{user.name}</div>
                  <div className="text-[10px] text-slate-400 leading-tight">RM {user.rmCode}</div>
                </div>
              </button>

              {/* Dropdown */}
              {showProfileMenu && (
                <div
                  className="absolute right-0 mt-2 w-56 bg-white rounded-lg shadow-xl py-1 text-slate-800 border border-slate-200 z-50 animate-fadeIn"
                  onClick={() => setShowProfileMenu(false)}
                >
                  <div className="px-4 py-2 border-b border-slate-100">
                    <p className="text-xs font-bold text-ebl-navy-dark">{user.name}</p>
                    <p className="text-[11px] text-slate-500">{user.email || 'No email registered'}</p>
                    <p className="text-[10px] text-slate-400 mt-0.5">{user.officeAddress}</p>
                  </div>

                  <button
                    onClick={onChangePassword}
                    className="w-full text-left px-4 py-2 text-xs text-slate-700 hover:bg-slate-50 flex items-center space-x-2"
                  >
                    <Key className="w-3.5 h-3.5 text-slate-500" />
                    <span>Change Password</span>
                  </button>

                  <div className="border-t border-slate-100 my-1"></div>

                  <button
                    onClick={onLogout}
                    className="w-full text-left px-4 py-2 text-xs text-red-600 hover:bg-red-50 flex items-center space-x-2"
                  >
                    <LogOut className="w-3.5 h-3.5" />
                    <span>Sign Out</span>
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </header>
  );
};
